package io.github.itshedimisawi.vitro.internal.render

import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.itshedimisawi.vitro.internal.material.setToMaterial

/** Android 12: platform blur and a colour matrix, then the shared [FallbackPainter]. */
@RequiresApi(31)
internal class BlurRenderer : GlassRenderer {

    private val painter = FallbackPainter()
    private val vivid = ColorMatrix()

    override fun DrawScope.view(scene: GlassScene): Rect = blurredView(scene)

    override fun DrawScope.render(scene: GlassScene) {
        val glass = scene.glass
        val blur = glass.blurRadius.toPx()

        val layer = scene.layer
        val view = scene.view
        layer.record(IntSize(view.width.toInt(), view.height.toInt())) { drawView(scene) }
        layer.topLeft = IntOffset(view.left.toInt(), view.top.toInt())
        layer.renderEffect = if (blur > 0f) BlurEffect(blur, blur, TileMode.Clamp) else null
        vivid.setToMaterial(scene.glass, scene.shape.darkness)
        layer.colorFilter = ColorFilter.colorMatrix(vivid)

        with(painter) { paint(scene) { drawLayer(layer) } }
    }
}
