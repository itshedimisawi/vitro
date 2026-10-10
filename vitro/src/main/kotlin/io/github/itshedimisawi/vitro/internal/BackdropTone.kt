package io.github.itshedimisawi.vitro.internal

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlin.math.ceil
import kotlin.math.floor

/**
 * How bright a backdrop is, on a coarse grid: one value per square of [cell] pixels, from 0 for
 * black to 1 for white.
 */
internal class BackdropTone private constructor(
    private val columns: Int,
    private val rows: Int,
    private val cell: Float,
    private val brightness: FloatArray,
) {

    /** Mean brightness inside [area], given in backdrop pixels, or NaN if the area misses the backdrop. */
    fun mean(area: Rect): Float {
        val left = floor(area.left / cell).toInt().coerceAtLeast(0)
        val top = floor(area.top / cell).toInt().coerceAtLeast(0)
        val right = ceil(area.right / cell).toInt().coerceAtMost(columns)
        val bottom = ceil(area.bottom / cell).toInt().coerceAtMost(rows)
        if (left >= right || top >= bottom) return Float.NaN
        var sum = 0f
        for (row in top until bottom) {
            for (column in left until right) sum += brightness[row * columns + column]
        }
        return sum / ((right - left) * (bottom - top))
    }

    companion object {

        /** Reads a picture of the backdrop drawn at one pixel per [cell] pixels. */
        fun read(shot: ImageBitmap, cell: Float): BackdropTone {
            val bitmap = shot.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val brightness = FloatArray(pixels.size) { index ->
                val pixel = pixels[index]
                val red = (pixel shr 16 and 0xFF) * 0.2126f
                val green = (pixel shr 8 and 0xFF) * 0.7152f
                val blue = (pixel and 0xFF) * 0.0722f
                (red + green + blue) / 255f * ((pixel ushr 24) / 255f)
            }
            return BackdropTone(bitmap.width, bitmap.height, cell, brightness)
        }
    }
}

/**
 * How dark glass should be, from 0 for the light material to 1 for the dark one, over content of
 * the given [brightness]. The interface theme decides, and only content at the far end overrides
 * it: near-black content in a light interface, near-white content in a dark one.
 */
internal fun darknessOver(brightness: Float, isNight: Boolean): Float {
    val start = if (isNight) NightFlipStart else DayFlipStart
    val end = if (isNight) NightFlipEnd else DayFlipEnd
    return 1f - ((brightness - start) / (end - start)).coerceIn(0f, 1f)
}

private const val DayFlipStart = 0.17f
private const val DayFlipEnd = 0.28f
private const val NightFlipStart = 0.84f
private const val NightFlipEnd = 0.90f
