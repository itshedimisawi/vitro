package io.github.itshedimisawi.vitro.internal

import android.content.res.Configuration
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import io.github.itshedimisawi.vitro.ScrollEdgeEffectStyle

internal class ScrollEdgeElement(
    private val top: Dp,
    private val bottom: Dp,
    private val style: ScrollEdgeEffectStyle,
    private val color: Color,
) : ModifierNodeElement<ScrollEdgeNode>() {

    override fun create(): ScrollEdgeNode = ScrollEdgeNode(top, bottom, style, color)

    override fun update(node: ScrollEdgeNode) {
        node.update(top, bottom, style, color)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "scrollEdgeEffect"
        properties["top"] = top
        properties["bottom"] = bottom
        properties["style"] = style
        properties["color"] = color
    }

    override fun equals(other: Any?): Boolean =
        other is ScrollEdgeElement &&
            other.top == top &&
            other.bottom == bottom &&
            other.style == style &&
            other.color == color

    override fun hashCode(): Int =
        ((top.hashCode() * 31 + bottom.hashCode()) * 31 + style.hashCode()) * 31 + color.hashCode()
}

internal class ScrollEdgeNode(
    private var top: Dp,
    private var bottom: Dp,
    private var style: ScrollEdgeEffectStyle,
    private var color: Color,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    fun update(top: Dp, bottom: Dp, style: ScrollEdgeEffectStyle, color: Color) {
        this.top = top
        this.bottom = bottom
        this.style = style
        this.color = color
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val mode = currentValueOf(LocalConfiguration).uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNight = mode == Configuration.UI_MODE_NIGHT_YES
        val into = when {
            style == ScrollEdgeEffectStyle.Shaded || isNight && !color.isSpecified -> Color.Black
            color.isSpecified -> color
            else -> Color.White
        }
        val strength = if (style == ScrollEdgeEffectStyle.Shaded && !isNight) ShadeStrength else Strength
        val stops = Array(Ease.size) { index ->
            index / (Ease.size - 1f) to into.copy(alpha = into.alpha * strength * Ease[index])
        }
        val reach = Reach.toPx()
        val ramp = Ramp.toPx()

        if (top.isSpecified) {
            val start = top.toPx() + reach
            drawRect(
                brush = Brush.verticalGradient(*stops, startY = start, endY = start - ramp),
                size = Size(size.width, start.coerceAtMost(size.height)),
            )
        }
        if (bottom.isSpecified) {
            val start = (size.height - bottom.toPx() - reach).coerceAtLeast(0f)
            drawRect(
                brush = Brush.verticalGradient(*stops, startY = start, endY = start + ramp),
                topLeft = Offset(0f, start),
                size = Size(size.width, size.height - start),
            )
        }
    }

    private companion object {
        // The effect begins this far past the covered height and takes `Ramp` to reach full
        // strength, which it then keeps to the edge.
        val Reach = 46.dp
        val Ramp = 120.dp
        const val Strength = 0.855f
        const val ShadeStrength = 0.3f

        // Share of full strength at equal steps along the ramp.
        val Ease = floatArrayOf(0f, 0.035f, 0.105f, 0.22f, 0.4f, 0.575f, 0.73f, 0.88f, 0.96f, 0.99f, 1f)
    }
}
