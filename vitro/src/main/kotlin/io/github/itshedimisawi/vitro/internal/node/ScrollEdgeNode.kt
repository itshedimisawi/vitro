package io.github.itshedimisawi.vitro.internal.node

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.UnplacedAwareModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.requireView
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.ScrollEdgeEffectStyle
import io.github.itshedimisawi.vitro.internal.tone.BackdropTone
import io.github.itshedimisawi.vitro.internal.tone.ToneReader
import io.github.itshedimisawi.vitro.internal.tone.followReadings
import io.github.itshedimisawi.vitro.internal.tone.shadeOver
import kotlin.math.max
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class ScrollEdgeElement(
    private val top: Dp,
    private val bottom: Dp,
    private val style: ScrollEdgeEffectStyle,
    private val color: Color,
    private val isTopHidden: Boolean,
    private val isBottomHidden: Boolean,
) : ModifierNodeElement<ScrollEdgeNode>() {

    override fun create(): ScrollEdgeNode = ScrollEdgeNode(top, bottom, style, color, isTopHidden, isBottomHidden)

    override fun update(node: ScrollEdgeNode) {
        node.update(top, bottom, style, color, isTopHidden, isBottomHidden)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "scrollEdgeEffect"
        properties["top"] = top
        properties["bottom"] = bottom
        properties["style"] = style
        properties["color"] = color
        properties["isTopHidden"] = isTopHidden
        properties["isBottomHidden"] = isBottomHidden
    }

    override fun equals(other: Any?): Boolean =
        other is ScrollEdgeElement &&
            other.top == top &&
            other.bottom == bottom &&
            other.style == style &&
            other.color == color &&
            other.isTopHidden == isTopHidden &&
            other.isBottomHidden == isBottomHidden

    override fun hashCode(): Int {
        var result = top.hashCode()
        result = 31 * result + bottom.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + isTopHidden.hashCode()
        result = 31 * result + isBottomHidden.hashCode()
        return result
    }
}

internal class ScrollEdgeNode(
    private var top: Dp,
    private var bottom: Dp,
    style: ScrollEdgeEffectStyle,
    private var color: Color,
    isTopHidden: Boolean,
    isBottomHidden: Boolean,
) : Modifier.Node(), DrawModifierNode, UnplacedAwareModifierNode, CompositionLocalConsumerModifierNode {

    private var style by mutableStateOf(style)
    private var bars by mutableStateOf(Bars.None)
    private var tone by mutableStateOf<BackdropTone?>(null)
    private val reader = ToneReader { tone = it }
    private var content: GraphicsLayer? = null

    private val veils = ArrayList<Pair<Rect, Brush>>(2)
    private var veilOrigin = Offset.Zero
    private var owner: GlassBackdrop? = null

    private val shade = arrayOf(Animatable(0f), Animatable(0f))

    // Which edges are hidden, and how far each is shown, so an edge fades in and out.
    private val isHidden = mutableStateListOf(isTopHidden, isBottomHidden)
    private val shown = arrayOf(Animatable(if (isTopHidden) 0f else 1f), Animatable(if (isBottomHidden) 0f else 1f))

    fun update(
        top: Dp,
        bottom: Dp,
        style: ScrollEdgeEffectStyle,
        color: Color,
        isTopHidden: Boolean,
        isBottomHidden: Boolean,
    ) {
        this.top = top
        this.bottom = bottom
        this.style = style
        this.color = color
        if (isHidden[0] != isTopHidden) isHidden[0] = isTopHidden
        if (isHidden[1] != isBottomHidden) isHidden[1] = isBottomHidden
        invalidateDraw()
    }

    override fun onAttach() {
        for (edge in shade.indices) {
            coroutineScope.launch { shade[edge].followReadings(ShadeIn, ShadeOut) { wanted(edge) } }
            coroutineScope.launch {
                snapshotFlow { if (isHidden[edge]) 0f else 1f }.collectLatest { target ->
                    shown[edge].animateTo(target, Reveal)
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
        owner?.let { if (it.edge === this) it.edge = null }
        owner = null
        veils.clear()
    }

    // Unplaced content is not drawn, so there is nothing to read until it is placed again.
    override fun onUnplaced() {
        reader.pause()
    }

    /** Draws this frame's veils again in backdrop coordinates, over the glass inside the content. */
    fun DrawScope.drawVeils(blendMode: BlendMode) {
        translate(veilOrigin.x, veilOrigin.y) {
            for ((area, brush) in veils) drawRect(brush, area.topLeft, area.size, blendMode = blendMode)
        }
    }

    override fun ContentDrawScope.draw() {
        val reach = Reach.toPx()
        val found = barsInView(reach)
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

        val isNight = isNightMode()
        val page = when {
            color.isSpecified -> color
            isNight -> Color.Black
            else -> Color.White
        }
        val shadeStrength = if (isNight) Strength else ShadeStrength
        val ramp = Ramp.toPx()

        veils.clear()
        val topShown = shown[0].value
        if (topShown > 0f && (top.isSpecified || found.top.isNotEmpty())) {
            val start = found.covered(isTop = true, given = top.pxOrZero(this), extent = size.height) + reach
            veils += Rect(0f, 0f, size.width, start.coerceAtMost(size.height)) to
                veil(page, shade[0].value, shadeStrength, topShown, startY = start, endY = start - ramp)
        }
        val bottomShown = shown[1].value
        if (bottomShown > 0f && (bottom.isSpecified || found.bottom.isNotEmpty())) {
            val covered = found.covered(isTop = false, given = bottom.pxOrZero(this), extent = size.height)
            val start = (size.height - covered - reach).coerceAtLeast(0f)
            veils += Rect(0f, start, size.width, size.height) to
                veil(page, shade[1].value, shadeStrength, bottomShown, startY = start, endY = start + ramp)
        }
        for ((area, brush) in veils) drawRect(brush, area.topLeft, area.size)
    }

    // One veil mixes the page colour and the shade, so changing style never passes through a
    // lighter veil. [shown] scales it while the edge fades in or out.
    private fun veil(page: Color, shaded: Float, shadeStrength: Float, shown: Float, startY: Float, endY: Float): Brush {
        val soft = page.alpha * Strength * (1f - shaded)
        val dark = shadeStrength * shaded
        val strength = soft + dark
        val into = if (strength > 0f) {
            Color(page.red * soft / strength, page.green * soft / strength, page.blue * soft / strength)
        } else {
            page
        }
        val stops = Array(Ease.size) { index ->
            index / (Ease.size - 1f) to into.copy(alpha = strength * shown * Ease[index])
        }
        return Brush.verticalGradient(*stops, startY = startY, endY = endY)
    }

    private fun ContentDrawScope.barsInView(reach: Float): Bars {
        val backdrop = enclosingBackdrop() ?: return Bars.None
        if (owner !== backdrop) {
            owner?.let { if (it.edge === this@ScrollEdgeNode) it.edge = null }
            owner = backdrop
            backdrop.edge = this@ScrollEdgeNode
        }
        val source = backdrop.coordinates
        if (source == null || !source.isAttached) return Bars.None
        val origin = source.localPositionOf(requireLayoutCoordinates(), Offset.Zero)
        veilOrigin = origin
        if (backdrop.panes.isEmpty()) return Bars.None
        val outlines = ArrayList<Rect>(backdrop.panes.size)
        for (pane in backdrop.panes) {
            if (pane.isContent) continue
            val outline = pane.outline.translate(-origin.x, -origin.y)
            if (!outline.isEmpty && outline.right > 0f && outline.left < size.width) outlines += outline
        }
        return findBars(outlines, size.height, reach, top.pxOrZero(this), bottom.pxOrZero(this))
    }

    private fun wanted(edge: Int): Pair<Float, Boolean> = when (style) {
        ScrollEdgeEffectStyle.Soft -> 0f to false
        ScrollEdgeEffectStyle.Shaded -> 1f to false
        else -> {
            val luminance = tone?.luminance(if (edge == 0) bars.top else bars.bottom) ?: Float.NaN
            if (luminance.isNaN()) 0f to false else shadeOver(luminance) to true
        }
    }

    private fun Dp.pxOrZero(scope: ContentDrawScope): Float = if (isSpecified) with(scope) { toPx() } else 0f

    private companion object {
        // The effect starts this far past the covered height and reaches full strength over `Ramp`.
        val Reach = 46.dp
        val Ramp = 120.dp
        const val Strength = 0.855f
        const val ShadeStrength = 0.3f

        val Ease = floatArrayOf(0f, 0.035f, 0.105f, 0.22f, 0.4f, 0.575f, 0.73f, 0.88f, 0.96f, 0.99f, 1f)

        val ShadeIn = spring<Float>(dampingRatio = 1f, stiffness = 150f)
        val ShadeOut = spring<Float>(dampingRatio = 1f, stiffness = 400f)
        val Reveal = spring<Float>(dampingRatio = 1f, stiffness = 300f)
    }
}

/**
 * The glass among [outlines] that forms bars at the top and bottom of content [extent] tall, where
 * [top] and [bottom] are already covered by something that is not glass.
 */
internal fun findBars(outlines: List<Rect>, extent: Float, reach: Float, top: Float, bottom: Float): Bars {
    val atTop = stack(outlines, extent, reach, top, isTop = true)
    val atBottom = stack(outlines, extent, reach, bottom, isTop = false)
    return if (atTop.isEmpty() && atBottom.isEmpty()) Bars.None else Bars(atTop, atBottom)
}

// Glass joins an edge while its gap to the edge, or to glass already counted, is within reach.
// Glass that runs past the middle is not a bar.
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

/** The glass outlines found at the top and at the bottom of some content. */
internal data class Bars(val top: List<Rect>, val bottom: List<Rect>) {

    val isEmpty: Boolean get() = top.isEmpty() && bottom.isEmpty()

    /** Height covered at an edge: the [given] height or the glass there, whichever reaches further. */
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
