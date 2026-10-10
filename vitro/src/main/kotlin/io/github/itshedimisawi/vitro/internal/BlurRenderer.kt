package io.github.itshedimisawi.vitro.internal

import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil

@RequiresApi(31)
internal class BlurRenderer : GlassRenderer {

    private val painter = FallbackPainter()
    private val vivid = ColorMatrix()

    override fun DrawScope.render(scene: GlassScene) {
        val glass = scene.glass
        val blur = glass.blurRadius.toPx()
        val margin = ceil(blur).toInt()

        val layer = scene.layer
        val extent = IntSize(
            ceil(size.width).toInt() + margin * 2,
            ceil(size.height).toInt() + margin * 2,
        )
        layer.record(size = extent) {
            translate(margin - scene.origin.x, margin - scene.origin.y) {
                drawLayer(scene.backdrop)
            }
        }
        layer.topLeft = IntOffset(-margin, -margin)
        layer.renderEffect = if (blur > 0f) BlurEffect(blur, blur, TileMode.Clamp) else null
        vivid.setToSaturation(glass.saturation)
        layer.colorFilter = ColorFilter.colorMatrix(vivid)

        with(painter) { paint(scene) { drawLayer(layer) } }
    }
}
