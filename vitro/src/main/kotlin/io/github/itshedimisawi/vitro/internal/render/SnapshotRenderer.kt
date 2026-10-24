package io.github.itshedimisawi.vitro.internal.render

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.itshedimisawi.vitro.internal.material.setToMaterial
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android 6 to 11: a scaled-down snapshot of the view, blurred on the CPU and drawn late, with a
 * colour matrix and the shared [FallbackPainter].
 */
internal class SnapshotRenderer(
    private val context: GraphicsContext,
    private val scope: CoroutineScope,
) : GlassRenderer {

    private val capture = context.createGraphicsLayer()
    private val painter = FallbackPainter()
    private val vivid = ColorMatrix()

    private var frame by mutableStateOf<ImageBitmap?>(null)
    private var frameOrigin = Offset.Zero
    private var frameView = Rect.Zero
    private var requested = 0
    private var wantedOrigin = Offset.Zero
    private var wantedView = Rect.Zero
    private var wantedRadius = 0f
    private var isStale = false
    private var job: Job? = null

    override fun DrawScope.view(scene: GlassScene): Rect = blurredView(scene)

    override fun DrawScope.render(scene: GlassScene) {
        val glass = scene.glass
        val blur = glass.blurRadius.toPx()
        val reduction = (blur / 3f).coerceIn(1f, 8f)
        val view = scene.view
        val width = view.width.toInt()
        val height = view.height.toInt()

        capture.record(
            size = IntSize(ceil(width / reduction).toInt(), ceil(height / reduction).toInt()),
        ) {
            scale(1f / reduction, 1f / reduction, pivot = Offset.Zero) { drawView(scene) }
        }

        var key = scene.revision
        key = 31 * key + scene.origin.hashCode()
        key = 31 * key + view.hashCode()
        key = 31 * key + blur.hashCode()
        if (key != requested) {
            requested = key
            wantedOrigin = scene.origin
            wantedView = view
            wantedRadius = blur / reduction
            refresh()
        }

        val image = frame
        vivid.setToMaterial(scene.glass, scene.shape.darkness)
        with(painter) {
            paint(scene) {
                if (image == null) {
                    translate(view.left, view.top) { drawView(scene) }
                } else {
                    // Keeps a slightly late snapshot pinned to the content while the glass moves
                    // or changes size: it is drawn where it was taken.
                    val drift = frameOrigin - scene.origin
                    drawImage(
                        image = image,
                        dstOffset = IntOffset(
                            (drift.x + frameView.left).roundToInt(),
                            (drift.y + frameView.top).roundToInt(),
                        ),
                        dstSize = IntSize(frameView.width.toInt(), frameView.height.toInt()),
                        colorFilter = ColorFilter.colorMatrix(vivid),
                        filterQuality = FilterQuality.Low,
                    )
                }
            }
        }
    }

    override fun release() {
        job?.cancel()
        context.releaseGraphicsLayer(capture)
    }

    private fun refresh() {
        isStale = true
        if (job?.isActive == true) return
        job = scope.launch {
            while (isStale) {
                isStale = false
                val origin = wantedOrigin
                val taken = wantedView
                val radius = wantedRadius
                val shot = capture.toImageBitmap()
                val softened = withContext(Dispatchers.Default) { boxBlur(shot, radius) }
                frameOrigin = origin
                frameView = taken
                frame = softened
            }
        }
    }
}
