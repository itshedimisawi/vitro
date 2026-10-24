package io.github.itshedimisawi.vitro.internal.tone

import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Reads a small layer back from the GPU through a renderer and buffer kept between readings. It
 * draws without stalling the render thread, and pixels are read on a background thread.
 */
@RequiresApi(29)
internal class ToneCapture {

    private val node = RenderNode("VitroTone")
    private val renderer = HardwareRenderer().apply {
        setContentRoot(node)
        isOpaque = false
    }
    private val scope = CanvasDrawScope()
    private var reader: ImageReader? = null
    private val waiting = AtomicReference<Waiting?>(null)

    private class Waiting(val cell: Float, val continuation: CancellableContinuation<BackdropTone?>)

    /**
     * Draws [miniature] and reads it back, one pixel per [cell] pixels of the content. Null when
     * the renderer could not draw it.
     */
    suspend fun read(miniature: GraphicsLayer, cell: Float, density: Density): BackdropTone? {
        val size = miniature.size
        if (size.width <= 0 || size.height <= 0) return null
        prepare(size, density)
        node.setPosition(0, 0, size.width, size.height)
        val canvas = node.beginRecording(size.width, size.height)
        try {
            scope.draw(density, LayoutDirection.Ltr, Canvas(canvas), size.toSize()) { drawLayer(miniature) }
        } finally {
            node.endRecording()
        }
        return suspendCancellableCoroutine { continuation ->
            waiting.set(Waiting(cell, continuation))
            continuation.invokeOnCancellation { waiting.set(null) }
            val result = renderer.createRenderRequest().syncAndDraw()
            if (result and (HardwareRenderer.SYNC_FRAME_DROPPED or HardwareRenderer.SYNC_CONTEXT_IS_STOPPED) != 0) {
                waiting.getAndSet(null)?.continuation?.resume(null)
            }
        }
    }

    fun release() {
        waiting.getAndSet(null)?.continuation?.cancel()
        renderer.destroy()
        reader?.close()
        reader = null
        node.discardDisplayList()
    }

    private fun prepare(size: IntSize, density: Density) {
        reader?.let { if (it.width == size.width && it.height == size.height) return }
        reader?.close()
        val usage = HardwareBuffer.USAGE_CPU_READ_OFTEN or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        val created = ImageReader.newInstance(size.width, size.height, PixelFormat.RGBA_8888, 2, usage)
        created.setOnImageAvailableListener(::onImage, handler)
        renderer.setSurface(created.surface)
        // Shadows in the content are lit as the platform lights a window, from above its top centre.
        with(density) { renderer.setLightSourceGeometry(size.width / 2f, 0f, LightZ.toPx(), LightRadius.toPx()) }
        renderer.setLightSourceAlpha(AmbientShadowAlpha, SpotShadowAlpha)
        reader = created
    }

    // Runs on the background thread.
    private fun onImage(reader: ImageReader) {
        val image = try {
            reader.acquireLatestImage()
        } catch (_: IllegalStateException) {
            null
        } ?: return
        val waiting = waiting.getAndSet(null)
        val tone = try {
            waiting?.let {
                val plane = image.planes[0]
                BackdropTone.read(plane.buffer, image.width, image.height, plane.rowStride, it.cell)
            }
        } finally {
            image.close()
        }
        waiting?.continuation?.resume(tone)
    }

    private companion object {
        val handler: Handler by lazy { Handler(HandlerThread("VitroTone").apply { start() }.looper) }

        val LightZ = 600.dp
        val LightRadius = 800.dp
        const val AmbientShadowAlpha = 0.039f
        const val SpotShadowAlpha = 0.19f
    }
}
