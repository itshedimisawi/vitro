package io.github.itshedimisawi.vitro.internal

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import io.github.itshedimisawi.vitro.GlassPane
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow

/**
 * The colours of a backdrop on a coarse grid: one colour per square of [cell] pixels, kept as
 * red, green and blue one after another.
 */
internal class BackdropTone private constructor(
    private val columns: Int,
    private val rows: Int,
    private val cell: Float,
    private val colors: FloatArray,
) {

    /**
     * Mean luminance of what lies inside [area], given in backdrop pixels: the backdrop as seen
     * through the glass [beneath], lowest first. It is the light the content gives off, from 0
     * to 1, so a few bright spots count for more than their share of the area and a saturated
     * colour counts for more than a grey that looks as dark. NaN if the area misses the backdrop.
     */
    fun luminance(area: Rect, beneath: List<GlassPane>): Float {
        val left = floor(area.left / cell).toInt().coerceAtLeast(0)
        val top = floor(area.top / cell).toInt().coerceAtLeast(0)
        val right = ceil(area.right / cell).toInt().coerceAtMost(columns)
        val bottom = ceil(area.bottom / cell).toInt().coerceAtMost(rows)
        if (left >= right || top >= bottom) return Float.NaN
        val seen = FloatArray(3)
        var sum = 0f
        for (row in top until bottom) {
            for (column in left until right) {
                colors.copyInto(seen, 0, (row * columns + column) * 3, (row * columns + column) * 3 + 3)
                if (beneath.isNotEmpty()) {
                    val spot = Offset((column + 0.5f) * cell, (row + 0.5f) * cell)
                    for (pane in beneath) {
                        if (pane.outline.contains(spot)) pane.shade.through(seen)
                    }
                }
                sum += emitted(seen[0]) * LumaWeights[0] + emitted(seen[1]) * LumaWeights[1] + emitted(seen[2]) * LumaWeights[2]
            }
        }
        return sum / ((right - left) * (bottom - top))
    }

    companion object {

        /** Reads a picture of the backdrop drawn at one pixel per [cell] pixels. */
        fun read(shot: ImageBitmap, cell: Float): BackdropTone {
            val bitmap = shot.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val colors = FloatArray(pixels.size * 3)
            for (index in pixels.indices) {
                val pixel = pixels[index]
                val cover = (pixel ushr 24) / 255f
                colors[index * 3] = (pixel shr 16 and 0xFF) / 255f * cover
                colors[index * 3 + 1] = (pixel shr 8 and 0xFF) / 255f * cover
                colors[index * 3 + 2] = (pixel and 0xFF) / 255f * cover
            }
            return BackdropTone(bitmap.width, bitmap.height, cell, colors)
        }

        // The light a channel gives off for its stored value.
        private fun emitted(value: Float): Float = value.coerceIn(0f, 1f).pow(2.2f)
    }
}

/**
 * How dark glass should be, from 0 for the light material to 1 for the dark one, over content of
 * the given [luminance]. The interface theme decides, and only content at the far end overrides
 * it: near-black content in a light interface, near-white content in a dark one.
 */
internal fun darknessOver(luminance: Float, isNight: Boolean): Float {
    val start = if (isNight) NightFlipStart else DayFlipStart
    val end = if (isNight) NightFlipEnd else DayFlipEnd
    return 1f - ((luminance - start) / (end - start)).coerceIn(0f, 1f)
}

private const val DayFlipStart = 0.0263f
private const val DayFlipEnd = 0.036f
private const val NightFlipStart = 0.681f
private const val NightFlipEnd = 0.793f
