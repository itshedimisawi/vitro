package io.github.itshedimisawi.vitro.internal.render

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.itshedimisawi.vitro.internal.material.blurSigma
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Returns a blurred copy of [shot] that approximates the platform blur of [radius] pixels with
 * three box-filter passes on each axis. Runs on the CPU, off the main thread.
 */
internal fun boxBlur(shot: ImageBitmap, radius: Float): ImageBitmap {
    val bitmap = shot.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
    val sigma = blurSigma(radius)
    val reach = ((sqrt(4f * sigma * sigma + 1f) - 1f) / 2f).roundToInt()
    if (reach < 1) return bitmap.asImageBitmap()

    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    val scratch = IntArray(width * height)
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
