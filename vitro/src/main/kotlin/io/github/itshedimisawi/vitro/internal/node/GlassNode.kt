package io.github.itshedimisawi.vitro.internal.node

import android.os.Build
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassAppearance
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.GlassEffectState
import io.github.itshedimisawi.vitro.internal.material.GlassShade
import io.github.itshedimisawi.vitro.internal.material.PressGlow
import io.github.itshedimisawi.vitro.internal.material.PressGlowPerGrowth
import io.github.itshedimisawi.vitro.internal.material.material
import io.github.itshedimisawi.vitro.internal.render.GlassPane
import io.github.itshedimisawi.vitro.internal.render.GlassRenderer
import io.github.itshedimisawi.vitro.internal.render.GlassScene
import io.github.itshedimisawi.vitro.internal.render.GlassShape
import io.github.itshedimisawi.vitro.internal.render.createGlassRenderer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class GlassEffectElement(
    private val backdrop: GlassBackdrop,
    private val glass: Glass,
    private val shape: CornerBasedShape,
    private val state: GlassEffectState?,
    private val interactionSource: InteractionSource?,
) : ModifierNodeElement<GlassNode>() {

    override fun create(): GlassNode = GlassNode(backdrop, glass, shape, state, interactionSource)

    override fun update(node: GlassNode) {
        node.update(backdrop, glass, shape, state, interactionSource)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glassEffect"
        properties["backdrop"] = backdrop
        properties["glass"] = glass
        properties["shape"] = shape
        properties["state"] = state
        properties["interactionSource"] = interactionSource
    }

    override fun equals(other: Any?): Boolean =
        other is GlassEffectElement &&
            other.backdrop === backdrop &&
            other.glass == glass &&
            other.shape == shape &&
            other.state === state &&
            other.interactionSource === interactionSource

    override fun hashCode(): Int {
        var result = backdrop.hashCode()
        result = 31 * result + glass.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + interactionSource.hashCode()
        return result
    }
}

internal class GlassNode(
    backdrop: GlassBackdrop,
    glass: Glass,
    private var corners: CornerBasedShape,
    state: GlassEffectState?,
    private var interactionSource: InteractionSource?,
) : Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    LayoutAwareModifierNode,
    PointerInputModifierNode,
    TraversableNode,
    CompositionLocalConsumerModifierNode {

    override val traverseKey: Any get() = GlassNode

    private var scene: GlassScene? = null
    private var renderer: GlassRenderer? = null
    var pane: GlassPane? = null
        private set
    private var beneath: State<List<GlassPane>>? = null
    private var container: GlassContainerNode? = null

    /** Whether this glass is part of the content its backdrop records. */
    var isInside = false
        private set
    private var redraws by mutableIntStateOf(0)

    private var placement by mutableStateOf(Offset.Unspecified)
    private var placed = Offset.Unspecified
    private var isHidden = false
    private val motion = MotionStretch()
    private val ownState = GlassEffectState(GlassAppearance.Light)
    private var effectState = state ?: ownState
    private val appearance = AppearanceFollower { effectState.appearance = it }
    private var interactions: Job? = null
    private var isPressedByInteraction = false
    private var measured = Size.Zero
    private var isNight by mutableStateOf(false)

    var backdrop = backdrop
        private set
    var glass by mutableStateOf(glass)
        private set
    var material = glass.material()
        private set

    private val wantsTone: Boolean
        get() = glass.appearance == GlassAppearance.Automatic && !glass.isIdentity

    /** The outline of this glass as [arrange] last left it. */
    val shape: GlassShape? get() = scene?.shape

    fun update(
        backdrop: GlassBackdrop,
        glass: Glass,
        shape: CornerBasedShape,
        state: GlassEffectState?,
        interactionSource: InteractionSource?,
    ) {
        this.glass = glass
        material = glass.material()
        if (backdrop !== this.backdrop) {
            appearance.watch(null)
            pane?.let {
                this.backdrop.removePane(it)
                isInside = isPartOf(backdrop)
                backdrop.addPane(it, isInside)
                beneath = backdrop.glassBeneath(it)
            }
            this.backdrop = backdrop
            pane?.let { followTone(it) }
        }
        appearance.watch(if (wantsTone) backdrop else null)
        corners = shape
        val wanted = state ?: ownState
        if (wanted !== effectState) {
            wanted.appearance = effectState.appearance
            effectState = wanted
        }
        effectState.tint = glass.tint
        effectState.resize(measured, requireDensity().density)
        if (interactionSource !== this.interactionSource) {
            this.interactionSource = interactionSource
            followInteractions()
        }
        if (!glass.isInteractive) settle()
        invalidateDraw()
    }

    override fun onRemeasured(size: IntSize) {
        measured = size.toSize()
        effectState.resize(measured, requireDensity().density)
    }

    override fun onAttach() {
        val context = requireGraphicsContext()
        scene = GlassScene(context.createGraphicsLayer(), backdrop.layer, glass)
        isNight = isNightMode()
        isInside = isPartOf(backdrop)
        pane = GlassPane(context.createGraphicsLayer(), context.createGraphicsLayer()).also {
            backdrop.addPane(it, isInside)
            beneath = backdrop.glassBeneath(it)
            followTone(it)
        }
        appearance.watch(if (wantsTone) backdrop else null)
        effectState.tint = glass.tint
        followInteractions()
        renderer = createGlassRenderer(context, coroutineScope)
        // Glass in a container is drawn by it, except a piece inside another piece of the same one.
        val around = findNearestAncestor(GlassContainerNode) as? GlassContainerNode
        val outer = findNearestAncestor(GlassNode) as? GlassNode
        if (around != null && around.isJoining && outer?.container !== around) {
            container = around
            around.join(this)
        }
    }

    override fun onDetach() {
        val context = requireGraphicsContext()
        container?.leave(this)
        container = null
        renderer?.release()
        renderer = null
        scene?.let { context.releaseGraphicsLayer(it.layer) }
        scene = null
        pane?.let {
            backdrop.removePane(it)
            context.releaseGraphicsLayer(it.surface)
            context.releaseGraphicsLayer(it.layer)
        }
        pane = null
        beneath = null
        appearance.stop()
        isPressedByInteraction = false
        placement = Offset.Unspecified
        placed = Offset.Unspecified
        isHidden = false
    }

    /**
     * Works out where this glass sits and what the touch on it has done to it, and publishes
     * that for the glass around it. False while there is nothing to draw.
     */
    fun arrange(): Boolean {
        val scene = scene
        val pane = pane
        val source = backdrop.coordinates
        if (pane != null && glass.isIdentity) {
            pane.clear()
            return false
        }
        if (scene == null || pane == null || source == null || !isAttached || !source.isAttached ||
            measured.minDimension <= 0f
        ) {
            return false
        }

        placement
        scene.backdrop = backdrop.layer
        scene.glass = glass
        val shape = scene.shape
        shape.glass = glass
        val held = effectState.scale
        val position = source.localPositionOf(requireLayoutCoordinates(), Offset.Zero)
        shape.origin = position + effectState.offset
        shape.size = measured
        scene.origin = shape.origin
        scene.extent = measured
        scene.revision = backdrop.generation
        isNight = isNightMode()
        shape.darkness = appearance.value
        val finger = effectState.position
        shape.touch = Offset(finger.x.coerceIn(0f, measured.width), finger.y.coerceIn(0f, measured.height))
        shape.pressure = effectState.pressure.coerceAtLeast(0f)
        shape.glow = shape.pressure * (PressGlow + PressGlowPerGrowth * effectState.growth)
        corners.resolveRadii(measured, requireDensity(), requireLayoutDirection(), shape.radii)

        val pull = motion.value
        shape.scaleX = held.scaleX + pull.x - pull.y * 0.5f
        shape.scaleY = held.scaleY + pull.y - pull.x * 0.5f

        pane.origin = position
        pane.outline = Rect(shape.origin, measured)
        pane.shade = shape.shade()
        pane.within = enclosingGlass(backdrop, pane.within)
        pane.clip = if (isInside) clipAround(source, Rect(position, measured)) else null
        shape.clip = pane.clip
        scene.visible = pane.clip?.translate(-shape.origin.x, -shape.origin.y)
        return true
    }

    override fun ContentDrawScope.draw() {
        val scene = scene
        val renderer = renderer
        val pane = pane
        if (scene == null || renderer == null || pane == null || !arrange()) {
            drawContent()
            return
        }
        val shape = scene.shape
        val shift = shape.origin - pane.origin
        val bounds = size
        redraws

        // The first piece draws its whole group, even while it is itself out of sight.
        val around = container
        if (around != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pane.bounds = shape.grown
            pane.view = Rect(shape.origin, Size.Zero)
            pane.surface.record {}
            if (!with(around) { drawGroupLedBy(this@GlassNode) }) drawAgain()
            pane.layer.record { drawContentOn(this@draw, shape, shift, bounds) }
            if (!isInside) drawLayer(pane.layer)
            return
        }
        if (pane.clip?.isEmpty == true) {
            pane.bounds = Rect(shape.origin, Size.Zero)
            pane.view = pane.bounds
            return
        }

        if (!recordMaterial(renderer, scene, pane, beneath, requireDensity())) drawAgain()
        pane.layer.record {
            drawLayer(pane.surface)
            drawContentOn(this@draw, shape, shift, bounds)
        }
        if (!isInside) drawLayer(pane.layer)
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val current = coordinates.positionInRoot()
        val previous = placed
        placed = coordinates.positionInParent()
        val wasHidden = isHidden
        isHidden = isInside && isOutOfSight(coordinates)
        if (isHidden && wasHidden) return
        placement = current
        if (glass.isInteractive && previous.isSpecified && placed != previous) {
            motion.follow(coroutineScope, previous, requireDensity().density) { placed }
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (!glass.isInteractive || pass != PointerEventPass.Main) return
        val change = pointerEvent.changes.firstOrNull() ?: return
        when (pointerEvent.type) {
            PointerEventType.Press -> {
                isPressedByInteraction = false
                effectState.press(coroutineScope, change.position)
            }
            PointerEventType.Move -> effectState.move(change.position)
            PointerEventType.Release -> effectState.release(coroutineScope)
        }
    }

    override fun onCancelPointerInput() {
        if (!isPressedByInteraction) effectState.release(coroutineScope)
    }

    override fun sharePointerInputWithSiblings(): Boolean = !glass.isInteractive

    private fun DrawScope.drawContentOn(content: ContentDrawScope, shape: GlassShape, shift: Offset, bounds: Size) {
        translate(shift.x, shift.y) {
            scale(shape.scaleX, shape.scaleY, pivot = bounds.center) { content.drawContent() }
        }
    }

    private fun isOutOfSight(coordinates: LayoutCoordinates): Boolean {
        val source = backdrop.coordinates ?: return false
        return source.isAttached && source.localBoundingBoxOf(coordinates, clipBounds = true).isEmpty
    }

    private fun isPartOf(backdrop: GlassBackdrop): Boolean = enclosingBackdrop() === backdrop

    // Where the parents of this glass let it show, in the backdrop. Sides they leave alone stay
    // open, so shadow and swell can draw past the bounds there.
    private fun clipAround(source: LayoutCoordinates, full: Rect): Rect? {
        val seen = source.localBoundingBoxOf(requireLayoutCoordinates(), clipBounds = true)
        if (seen.isEmpty) return Rect.Zero
        val isCut = seen.left > full.left + 0.5f || seen.top > full.top + 0.5f ||
            seen.right < full.right - 0.5f || seen.bottom < full.bottom - 0.5f
        if (!isCut) return null
        return Rect(
            left = if (seen.left > full.left + 0.5f) seen.left else -Unbounded,
            top = if (seen.top > full.top + 0.5f) seen.top else -Unbounded,
            right = if (seen.right < full.right - 0.5f) seen.right else Unbounded,
            bottom = if (seen.bottom < full.bottom - 0.5f) seen.bottom else Unbounded,
        )
    }

    private fun GlassPane.clear() {
        outline = Rect.Zero
        bounds = Rect.Zero
        view = Rect.Zero
        shade = GlassShade.None
        clip = if (isContent) Rect.Zero else null
    }

    private fun followTone(pane: GlassPane) {
        appearance.follow(coroutineScope, backdrop, pane, beneath, { glass }, { isNight })
    }

    // Presses that don't come from a finger on the glass, such as a keyboard click. A finger press
    // owns the glass until it lifts, so the matching interactions are left to it.
    private fun followInteractions() {
        interactions?.cancel()
        val source = interactionSource ?: return
        interactions = coroutineScope.launch {
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> if (glass.isInteractive && !effectState.isHeld) {
                        isPressedByInteraction = true
                        effectState.press(coroutineScope, interaction.pressPosition)
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> if (isPressedByInteraction) {
                        isPressedByInteraction = false
                        effectState.release(coroutineScope)
                    }
                }
            }
        }
    }

    private fun drawAgain() {
        coroutineScope.launch { redraws++ }
    }

    private fun settle() {
        if (!isAttached) return
        isPressedByInteraction = false
        effectState.settle(coroutineScope)
        motion.reset(coroutineScope)
    }

    companion object {
        private const val Unbounded = 100000f
    }
}
