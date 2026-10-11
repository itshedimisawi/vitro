package io.github.itshedimisawi.vitro.internal

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        vivid.setToMaterial(scene)
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
                val softened = withContext(Dispatchers.Default) { soften(shot, radius) }
                frameOrigin = origin
                frameView = taken
                frame = softened
            }
        }
    }

    private fun soften(shot: ImageBitmap, radius: Float): ImageBitmap {
        val bitmap = shot.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
        val sigma = radius * 0.57735f + 0.5f
        val reach = ((sqrt(4f * sigma * sigma + 1f) - 1f) / 2f).roundToInt()
        if (reach < 1) return bitmap.asImageBitmap()

        val width = bitmap.width
        val height = bitmap.height
        var pixels = IntArray(width * height)
        var scratch = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        repeat(3) {
            average(pixels, scratch, width, height, reach)
            average(scratch, pixels, height, width, reach)
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap.asImageBitmap()
    }

    // Box-filters every row of `source` and writes the result transposed, so two calls blur both axes.
    private fun average(source: IntArray, target: IntArray, width: Int, height: Int, reach: Int) {
        val window = reach * 2 + 1
        for (row in 0 until height) {
            val start = row * width
            var alpha = 0
            var red = 0
            var green = 0
            var blue = 0
            for (offset in -reach..reach) {
                val pixel = source[start + offset.coerceIn(0, width - 1)]
                alpha += pixel ushr 24
                red += pixel shr 16 and 0xFF
                green += pixel shr 8 and 0xFF
                blue += pixel and 0xFF
            }
            for (column in 0 until width) {
                target[column * height + row] = (alpha / window shl 24) or
                    (red / window shl 16) or
                    (green / window shl 8) or
                    (blue / window)
                val leaving = source[start + (column - reach).coerceAtLeast(0)]
                val entering = source[start + (column + reach + 1).coerceAtMost(width - 1)]
                alpha += (entering ushr 24) - (leaving ushr 24)
                red += (entering shr 16 and 0xFF) - (leaving shr 16 and 0xFF)
                green += (entering shr 8 and 0xFF) - (leaving shr 8 and 0xFF)
                blue += (entering and 0xFF) - (leaving and 0xFF)
            }
        }
    }
}
