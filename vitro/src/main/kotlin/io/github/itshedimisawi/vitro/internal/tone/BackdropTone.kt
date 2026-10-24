package io.github.itshedimisawi.vitro.internal.tone

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import io.github.itshedimisawi.vitro.internal.material.LumaWeights
import io.github.itshedimisawi.vitro.internal.render.GlassPane
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow

/**
 * The colours of a backdrop on a grid of [cell]-pixel squares, as red, green and blue triplets,
 * with how much of each square the content covers. Uncovered areas do not count.
 */
internal class BackdropTone internal constructor(
    private val columns: Int,
    private val rows: Int,
    private val cell: Float,
    private val colors: FloatArray,
    private val cover: FloatArray = FloatArray(columns * rows) { 1f },
) {
    init {
        require(colors.size == columns * rows * 3 && cover.size == columns * rows)
    }

    // The light of each cell, worked out once since moving glass asks for it every frame.
    private val light = FloatArray(columns * rows) { cell -> lightOf(colors, cell * 3) }

    /**
     * Mean light given off inside [area], in backdrop pixels, as seen through the glass [beneath],
     * from 0 to 1. Bright spots and saturated colours weigh in as light does. NaN off the content.
     */
    fun luminance(area: Rect, beneath: List<GlassPane>): Float {
        val left = floor(area.left / cell).toInt().coerceAtLeast(0)
        val top = floor(area.top / cell).toInt().coerceAtLeast(0)
        val right = ceil(area.right / cell).toInt().coerceAtMost(columns)
        val bottom = ceil(area.bottom / cell).toInt().coerceAtMost(rows)
        if (left >= right || top >= bottom) return Float.NaN
        val seen = FloatArray(3)
        var sum = 0f
        var covered = 0f
        for (row in top until bottom) {
            for (column in left until right) {
                val at = row * columns + column
                val weight = cover[at]
                if (weight <= 0f) continue
                covered += weight
                if (beneath.isEmpty()) {
                    sum += light[at] * weight
                    continue
                }
                colors.copyInto(seen, 0, at * 3, at * 3 + 3)
                val spot = Offset((column + 0.5f) * cell, (row + 0.5f) * cell)
                for (pane in beneath) {
                    if (pane.outline.contains(spot)) pane.shade.through(seen)
                }
                sum += lightOf(seen, 0) * weight
            }
        }
        return if (covered > 0f) sum / covered else Float.NaN
    }

    /**
     * Mean luminance of what lies inside [areas], each counted by its size. NaN if none of them
     * meets the content.
     */
    fun luminance(areas: List<Rect>): Float {
        var sum = 0f
        var weight = 0f
        for (area in areas) {
            val luminance = luminance(area, emptyList())
            if (!luminance.isNaN()) {
                sum += luminance * area.width * area.height
                weight += area.width * area.height
            }
        }
        return if (weight > 0f) sum / weight else Float.NaN
    }

    companion object {

        /** Reads a picture of the backdrop drawn at one pixel per [cell] pixels. */
        fun read(shot: ImageBitmap, cell: Float): BackdropTone {
            val bitmap = shot.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val colors = FloatArray(pixels.size * 3)
            val cover = FloatArray(pixels.size)
            for (index in pixels.indices) {
                val pixel = pixels[index]
                cover[index] = (pixel ushr 24) / 255f
                colors[index * 3] = (pixel shr 16 and 0xFF) / 255f
                colors[index * 3 + 1] = (pixel shr 8 and 0xFF) / 255f
                colors[index * 3 + 2] = (pixel and 0xFF) / 255f
            }
            return BackdropTone(bitmap.width, bitmap.height, cell, colors, cover)
        }

        /**
         * Reads a picture of the backdrop drawn at one pixel per [cell] pixels, given as
         * premultiplied red, green, blue and alpha bytes in rows [rowStride] bytes apart.
         */
        fun read(pixels: ByteBuffer, width: Int, height: Int, rowStride: Int, cell: Float): BackdropTone {
            val colors = FloatArray(width * height * 3)
            val cover = FloatArray(width * height)
            for (row in 0 until height) {
                var at = row * rowStride
                for (column in 0 until width) {
                    val index = row * width + column
                    val alpha = pixels.get(at + 3).toInt() and 0xFF
                    cover[index] = alpha / 255f
                    if (alpha > 0) {
                        val scale = 1f / alpha
                        colors[index * 3] = min((pixels.get(at).toInt() and 0xFF) * scale, 1f)
                        colors[index * 3 + 1] = min((pixels.get(at + 1).toInt() and 0xFF) * scale, 1f)
                        colors[index * 3 + 2] = min((pixels.get(at + 2).toInt() and 0xFF) * scale, 1f)
                    }
                    at += 4
                }
            }
            return BackdropTone(width, height, cell, colors, cover)
        }

        // The light a colour gives off: each channel's stored value turned linear, then weighted.
        private fun lightOf(color: FloatArray, at: Int): Float =
            emitted(color[at]) * LumaWeights[0] + emitted(color[at + 1]) * LumaWeights[1] +
                emitted(color[at + 2]) * LumaWeights[2]

        private fun emitted(value: Float): Float = value.coerceIn(0f, 1f).pow(2.2f)
    }
}
