package io.github.itshedimisawi.vitro.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassPane

internal class GlassScene(
    val layer: GraphicsLayer,
    var backdrop: GraphicsLayer,
    var glass: Glass,
) {
    // Glass lying under this one: the first `beneath` entries of `panes`.
    var panes: List<GlassPane> = emptyList()
    var beneath: Int = 0
    var origin: Offset = Offset.Zero
    var touch: Offset = Offset.Zero
    var pressure: Float = 0f
    var glow: Float = 0f
    var revision: Int = 0
    var isDark: Boolean = false
    var scaleX: Float = 1f
    var scaleY: Float = 1f
    val radii: FloatArray = FloatArray(4)
}

/**
 * Draws what this glass looks through, in the backdrop's coordinates: the backdrop itself, then
 * every piece of glass placed beneath this one.
 */
internal fun DrawScope.drawBeneath(scene: GlassScene) {
    drawLayer(scene.backdrop)
    for (index in 0 until minOf(scene.beneath, scene.panes.size)) {
        val pane = scene.panes[index]
        translate(pane.origin.x, pane.origin.y) { drawLayer(pane.layer) }
    }
}

internal interface GlassRenderer {

    fun DrawScope.render(scene: GlassScene)

    fun release() {}
}

internal const val LightVeil = 1.32f
internal const val LightFade = 1.34f
internal const val LightWash = 0.58f
internal const val DarkGain = 0.08f
internal const val DarkLift = 0.19f
internal const val DarkShade = 1f
internal const val DarkKnee = 0.33f
internal const val DarkShadow = 0.15f
internal const val DarkRimGain = 0.35f
internal const val DarkRimLift = 0.12f
internal const val LightRim = 0.91f
internal const val PressGlow = 0.05f
internal const val PressGlowPerGrowth = 0.35f

private val LumaWeights = floatArrayOf(0.2126f, 0.7152f, 0.0722f)

internal fun GlassScene.shadowOpacity(): Float =
    glass.shadowOpacity * if (isDark) DarkShadow else 1f

internal fun ColorMatrix.setToMaterial(scene: GlassScene) {
    val veil = scene.glass.brightness.coerceIn(0f, 1f)
    val saturation = scene.glass.saturation * if (scene.isDark) 1f else 1f - LightWash * veil
    val gain = if (scene.isDark) 1f + DarkGain * veil else 1f
    val lift = if (scene.isDark) DarkLift * veil * 0.25f else LightVeil * veil
    val fade = if (scene.isDark) 0f else LightFade * veil
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

internal fun lightDirection(angle: Float): Offset {
    val radians = Math.toRadians(angle.toDouble())
    return Offset(kotlin.math.sin(radians).toFloat(), -kotlin.math.cos(radians).toFloat())
}
