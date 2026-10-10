package io.github.itshedimisawi.vitro.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import io.github.itshedimisawi.vitro.Glass

internal class GlassScene(
    val layer: GraphicsLayer,
    var backdrop: GraphicsLayer,
    var glass: Glass,
) {
    var origin: Offset = Offset.Zero
    var touch: Offset = Offset.Zero
    var pressure: Float = 0f
    var revision: Int = 0
    var isDark: Boolean = false
    var scaleX: Float = 1f
    var scaleY: Float = 1f
    val radii: FloatArray = FloatArray(4)
}

internal interface GlassRenderer {

    fun DrawScope.render(scene: GlassScene)

    fun release() {}
}

internal const val LightVeil = 1.32f
internal const val DarkGain = 0.14f
internal const val DarkLift = 0.19f
internal const val DarkShadow = 0.4f
internal const val PressClearing = 0.6f
internal const val PressDispersion = 0.06f

private val LumaWeights = floatArrayOf(0.2126f, 0.7152f, 0.0722f)

internal fun GlassScene.shadowOpacity(): Float =
    glass.shadowOpacity * if (isDark) DarkShadow else 1f

internal fun ColorMatrix.setToMaterial(scene: GlassScene) {
    val saturation = scene.glass.saturation
    val veil = (scene.glass.brightness * (1f - PressClearing * scene.pressure)).coerceIn(0f, 1f)
    val gain = if (scene.isDark) 1f + DarkGain * veil else 1f
    val lift = if (scene.isDark) DarkLift * veil * 0.25f else LightVeil * veil
    val fade = if (scene.isDark) 0f else lift
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
