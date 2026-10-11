package io.github.itshedimisawi.vitro.internal

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.requireView
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import io.github.itshedimisawi.vitro.ScrollEdgeEffectStyle
import kotlin.math.max
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
    style: ScrollEdgeEffectStyle,
    private var color: Color,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private var style by mutableStateOf(style)
    private var bars by mutableStateOf(Bars.None)
    private var tone by mutableStateOf<BackdropTone?>(null)
    private val reader = ToneReader { tone = it }
    private var content: GraphicsLayer? = null

    private val shade = arrayOf(Animatable(0f), Animatable(0f))

    fun update(top: Dp, bottom: Dp, style: ScrollEdgeEffectStyle, color: Color) {
        this.top = top
        this.bottom = bottom
        this.style = style
        this.color = color
        invalidateDraw()
    }

    override fun onAttach() {
        for (edge in shade.indices) {
            coroutineScope.launch {
                var wasSampled = false
                snapshotFlow { wanted(edge) }.collectLatest { (target, isSampled) ->
                    val level = shade[edge]
                    val isEased = isSampled && wasSampled
                    wasSampled = isSampled
                    when {
                        !isEased -> level.snapTo(target)
                        target > level.value -> level.animateTo(target, ShadeIn)
                        else -> level.animateTo(target, ShadeOut)
                    }
                }
            }
        }
    }

    override fun onDetach() {
        val context = requireGraphicsContext()
        reader.release(context)
        content?.let { context.releaseGraphicsLayer(it) }
        content = null
        tone = null
        bars = Bars.None
    }

    override fun ContentDrawScope.draw() {
        val reach = Reach.toPx()
        val found = findBars(reach)
        if (found != bars) bars = found

        if (style == ScrollEdgeEffectStyle.Automatic && !found.isEmpty) {
            val context = requireGraphicsContext()
            val layer = content ?: context.createGraphicsLayer().also { content = it }
            layer.record { this@draw.drawContent() }
            drawLayer(layer)
            with(reader) { read(layer, context, coroutineScope, requireView()) }
        } else {
            reader.pause()
            drawContent()
        }

        val mode = currentValueOf(LocalConfiguration).uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNight = mode == Configuration.UI_MODE_NIGHT_YES
        val page = when {
            color.isSpecified -> color
            isNight -> Color.Black
            else -> Color.White
        }
        val shadeStrength = if (isNight) Strength else ShadeStrength
        val ramp = Ramp.toPx()

        if (top.isSpecified || found.top.isNotEmpty()) {
            val start = found.covered(isTop = true, given = top.pxOrZero(this), extent = size.height) + reach
            drawRect(
                brush = veil(page, shade[0].value, shadeStrength, startY = start, endY = start - ramp),
                size = Size(size.width, start.coerceAtMost(size.height)),
            )
        }
        if (bottom.isSpecified || found.bottom.isNotEmpty()) {
            val covered = found.covered(isTop = false, given = bottom.pxOrZero(this), extent = size.height)
            val start = (size.height - covered - reach).coerceAtLeast(0f)
            drawRect(
                brush = veil(page, shade[1].value, shadeStrength, startY = start, endY = start + ramp),
                topLeft = Offset(0f, start),
                size = Size(size.width, size.height - start),
            )
        }
    }

    // One veil for both looks: the page colour and the shade are mixed by how far the edge is
    // toward the shade, so an edge that changes look never passes through a lighter veil.
    private fun veil(page: Color, shaded: Float, shadeStrength: Float, startY: Float, endY: Float): Brush {
        val soft = page.alpha * Strength * (1f - shaded)
        val dark = shadeStrength * shaded
        val strength = soft + dark
        val into = if (strength > 0f) {
            Color(page.red * soft / strength, page.green * soft / strength, page.blue * soft / strength)
        } else {
            page
        }
        val stops = Array(Ease.size) { index ->
            index / (Ease.size - 1f) to into.copy(alpha = strength * Ease[index])
        }
        return Brush.verticalGradient(*stops, startY = startY, endY = endY)
    }

    private fun ContentDrawScope.findBars(reach: Float): Bars {
        val backdrop = (findNearestAncestor(BackdropNode) as? BackdropNode)?.backdrop ?: return Bars.None
        val source = backdrop.coordinates
        if (source == null || !source.isAttached || backdrop.panes.isEmpty()) return Bars.None
        val origin = source.localPositionOf(requireLayoutCoordinates(), Offset.Zero)
        val outlines = ArrayList<Rect>(backdrop.panes.size)
        for (pane in backdrop.panes) {
            if (pane.isContent) continue
            val outline = pane.outline.translate(-origin.x, -origin.y)
            if (!outline.isEmpty && outline.right > 0f && outline.left < size.width) outlines += outline
        }
        val top = stack(outlines, size.height, reach, top.pxOrZero(this), isTop = true)
        val bottom = stack(outlines, size.height, reach, bottom.pxOrZero(this), isTop = false)
        return if (top.isEmpty() && bottom.isEmpty()) Bars.None else Bars(top, bottom)
    }

    // Glass belongs to an edge while the gap between it and the edge, or glass already counted,
    // is narrower than the effect's reach. Glass that runs past the middle is not a bar.
    private fun stack(outlines: List<Rect>, extent: Float, reach: Float, given: Float, isTop: Boolean): List<Rect> {
        var taken: ArrayList<Rect>? = null
        var covered = given
        var isGrowing = true
        while (isGrowing) {
            isGrowing = false
            for (outline in outlines) {
                if (taken?.contains(outline) == true) continue
                val near = if (isTop) outline.top else extent - outline.bottom
                val far = if (isTop) outline.bottom else extent - outline.top
                if (near <= covered + reach && far <= extent / 2f) {
                    (taken ?: ArrayList<Rect>().also { taken = it }) += outline
                    covered = max(covered, far)
                    isGrowing = true
                }
            }
        }
        return taken ?: emptyList()
    }

    private fun wanted(edge: Int): Pair<Float, Boolean> = when (style) {
        ScrollEdgeEffectStyle.Soft -> 0f to false
        ScrollEdgeEffectStyle.Shaded -> 1f to false
        ScrollEdgeEffectStyle.Automatic -> {
            val luminance = tone?.luminance(if (edge == 0) bars.top else bars.bottom) ?: Float.NaN
            if (luminance.isNaN()) 0f to false else shadeOver(luminance) to true
        }
    }

    private fun Dp.pxOrZero(scope: ContentDrawScope): Float = if (isSpecified) with(scope) { toPx() } else 0f

    private companion object {
        // The effect begins this far past the covered height and takes `Ramp` to reach full
        // strength, which it then keeps to the edge.
        val Reach = 46.dp
        val Ramp = 120.dp
        const val Strength = 0.855f
        const val ShadeStrength = 0.3f

        val Ease = floatArrayOf(0f, 0.035f, 0.105f, 0.22f, 0.4f, 0.575f, 0.73f, 0.88f, 0.96f, 0.99f, 1f)

        val ShadeIn = spring<Float>(dampingRatio = 1f, stiffness = 150f)
        val ShadeOut = spring<Float>(dampingRatio = 1f, stiffness = 400f)
    }
}

/** The glass outlines found at the top and at the bottom of some content. */
private data class Bars(val top: List<Rect>, val bottom: List<Rect>) {

    val isEmpty: Boolean get() = top.isEmpty() && bottom.isEmpty()

    /** Height covered at an edge: what the caller has [given] or what the glass there reaches, whichever is more. */
    fun covered(isTop: Boolean, given: Float, extent: Float): Float {
        var covered = given
        for (outline in if (isTop) top else bottom) {
            covered = max(covered, if (isTop) outline.bottom else extent - outline.top)
        }
        return covered
    }

    companion object {
        val None = Bars(emptyList(), emptyList())
    }
}
