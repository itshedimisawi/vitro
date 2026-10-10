package io.github.itshedimisawi.vitro.internal

import androidx.compose.ui.geometry.Offset
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

internal const val RefractiveIndex = 1.5f

internal fun lightDirection(angle: Float): Offset {
    val radians = Math.toRadians(angle.toDouble())
    return Offset(kotlin.math.sin(radians).toFloat(), -kotlin.math.cos(radians).toFloat())
}
