package io.github.itshedimisawi.vitro.internal.material

import androidx.compose.ui.graphics.ColorMatrix
import io.github.itshedimisawi.vitro.Glass

// A colour matrix can only follow a straight line, so the fallbacks use the line closest to the
// dark material's curve: content is lifted by `first` and loses `second` times its brightness.
internal val DarkLine: Pair<Float, Float> by lazy {
    val steps = 64
    var sumBend = 0f
    var sumCross = 0f
    var sumSquare = 0f
    for (step in 0..steps) {
        val brightness = step / steps.toFloat()
        val bend = darkBend(brightness)
        sumBend += bend
        sumCross += (brightness - 0.5f) * bend
        sumSquare += (brightness - 0.5f) * (brightness - 0.5f)
    }
    val slope = sumCross / sumSquare
    (sumBend / (steps + 1) - slope * 0.5f) to -slope
}

/** Sets this matrix to the material of [glass], [darkness] of the way toward the dark one. */
internal fun ColorMatrix.setToMaterial(glass: Glass, darkness: Float) {
    val veil = glass.brightness.coerceIn(0f, 1f)
    val light = 1f - darkness
    val saturation = glass.saturation * (1f - LightWash * veil * light) * (1f + DarkVivid * veil * darkness)
    val gain = 1f + DarkGain * veil * darkness
    val lift = LightVeil * veil * light + DarkLine.first * veil * darkness
    val fade = LightFade * veil * light + DarkLine.second * veil * darkness
    reset()
    for (row in 0..2) {
        for (column in 0..2) {
            val kept = if (row == column) saturation else 0f
            val mixed = kept + LumaWeights[column] * (1f - saturation)
            this[row, column] = gain * mixed - fade * LumaWeights[column]
        }
        this[row, 4] = lift * 255f
    }
}
