package io.github.itshedimisawi.vitro.internal.material

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * How a piece of glass changes the colour of what shows through it, following the shader's colour
 * steps: saturation, veil, darkness, tint and dimming. Refraction, blur and the rim are left out.
 */
internal data class GlassShade(
    val saturation: Float,
    val veil: Float,
    val darkness: Float,
    val tint: Color,
    val tintStrength: Float,
    val dimming: Float,
) {
    /** Replaces the red, green and blue in [color] with what shows through the glass. */
    fun through(color: FloatArray) {
        val grey = color[0] * LumaWeights[0] + color[1] * LumaWeights[1] + color[2] * LumaWeights[2]
        var luma = 0f
        for (channel in 0..2) {
            color[channel] = max(grey + (color[channel] - grey) * saturation, 0f)
            luma += color[channel] * LumaWeights[channel]
        }
        luma = min(luma, 1f)
        for (channel in 0..2) {
            val value = color[channel]
            var pale = value * (1f + veil)
            var deep = pale
            if (veil >= 0f) {
                pale = value + (luma - value) * LightWash * veil + veil * (LightVeil - LightFade * luma)
                val vivid = max(luma + (value - luma) * (1f + DarkVivid * veil), 0f)
                deep = max(vivid * (1f + DarkGain * veil) + veil * darkBend(luma), 0f)
            }
            val veiled = pale + (deep - pale) * darkness
            val tinted = veiled + (tintChannel(channel) - veiled) * tintStrength
            color[channel] = (tinted * (1f - dimming)).coerceIn(0f, 1f)
        }
    }

    private fun tintChannel(channel: Int): Float = when (channel) {
        0 -> tint.red
        1 -> tint.green
        else -> tint.blue
    }

    companion object {
        val None = GlassShade(
            saturation = 1f,
            veil = 0f,
            darkness = 0f,
            tint = Color.Black,
            tintStrength = 0f,
            dimming = 0f,
        )
    }
}
