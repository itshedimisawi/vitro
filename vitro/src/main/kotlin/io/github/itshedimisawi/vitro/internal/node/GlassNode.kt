package io.github.itshedimisawi.vitro.internal.node

import android.os.Build
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
import androidx.compose.ui.graphics.drawscope.draw
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
import io.github.itshedimisawi.vitro.GlassInteractionState
import io.github.itshedimisawi.vitro.internal.material.GlassShade
import io.github.itshedimisawi.vitro.internal.material.PressGlow
import io.github.itshedimisawi.vitro.internal.material.PressGlowPerGrowth
import io.github.itshedimisawi.vitro.internal.material.material
import io.github.itshedimisawi.vitro.internal.render.GlassPane
import io.github.itshedimisawi.vitro.internal.render.GlassRenderer
import io.github.itshedimisawi.vitro.internal.render.GlassScene
import io.github.itshedimisawi.vitro.internal.render.GlassShape
import io.github.itshedimisawi.vitro.internal.render.createGlassRenderer
import io.github.itshedimisawi.vitro.internal.render.paintedBounds
import kotlinx.coroutines.launch

internal class GlassEffectElement(
    private val backdrop: GlassBackdrop,
    private val glass: Glass,
    private val shape: CornerBasedShape,
    private val interaction: GlassInteractionState?,
    private val onAppearanceChanged: ((GlassAppearance) -> Unit)?,
) : ModifierNodeElement<GlassNode>() {

    override fun create(): GlassNode =
        GlassNode(backdrop, glass, shape, interaction, onAppearanceChanged)

    override fun update(node: GlassNode) {
        node.update(backdrop, glass, shape, interaction, onAppearanceChanged)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glassEffect"
        properties["backdrop"] = backdrop
        properties["glass"] = glass
        properties["shape"] = shape
        properties["interaction"] = interaction
        properties["onAppearanceChanged"] = onAppearanceChanged
    }

    override fun equals(other: Any?): Boolean =
        other is GlassEffectElement &&
            other.backdrop === backdrop &&
            other.glass == glass &&
            other.shape == shape &&
            other.interaction === interaction &&
            other.onAppearanceChanged === onAppearanceChanged

    override fun hashCode(): Int {
        var result = backdrop.hashCode()
        result = 31 * result + glass.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + interaction.hashCode()
        result = 31 * result + onAppearanceChanged.hashCode()
        return result
    }
}

internal class GlassNode(
    backdrop: GlassBackdrop,
    glass: Glass,
    private var corners: CornerBasedShape,
    interaction: GlassInteractionState?,
    onAppearanceChanged: ((GlassAppearance) -> Unit)?,
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
    private val appearance = AppearanceFollower(onAppearanceChanged)
    private val ownTouch = GlassInteractionState()
    private var touch = interaction ?: ownTouch
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
        interaction: GlassInteractionState?,
        onAppearanceChanged: ((GlassAppearance) -> Unit)?,
    ) {
        appearance.onChanged = onAppearanceChanged
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
        touch = interaction ?: ownTouch
        touch.resize(measured, requireDensity().density)
        if (!glass.isInteractive) settle()
        invalidateDraw()
    }

    override fun onRemeasured(size: IntSize) {
        measured = size.toSize()
        touch.resize(measured, requireDensity().density)
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
        renderer = createGlassRenderer(context, coroutineScope)
        // Glass in a container is drawn by it, joined with its neighbours. A piece inside the
        // content of another piece of the same container stays on its own.
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
        val held = touch.scale
        val position = source.localPositionOf(requireLayoutCoordinates(), Offset.Zero)
        shape.origin = position + touch.offset
        shape.size = measured
        scene.origin = shape.origin
        scene.extent = measured
        scene.revision = backdrop.generation
        isNight = isNightMode()
        shape.darkness = appearance.value
        val finger = touch.position
        shape.touch = Offset(finger.x.coerceIn(0f, measured.width), finger.y.coerceIn(0f, measured.height))
        shape.pressure = touch.pressure.coerceAtLeast(0f)
        shape.glow = shape.pressure * (PressGlow + PressGlowPerGrowth * touch.growth)
        corners.resolveRadii(measured, requireDensity(), requireLayoutDirection(), shape.radii)

        val pull = motion.value
        shape.scaleX = held.scaleX + pull.x - pull.y * 0.5f
        shape.scaleY = held.scaleY + pull.y - pull.x * 0.5f

        pane.origin = position
        pane.outline = Rect(shape.origin, measured)
        pane.shade = shape.shade()
        pane.within = enclosingGlass(backdrop, pane.within)
        pane.clip = if (isInside) clipAround(source, Rect(position, measured)) else null
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
        if (pane.clip?.isEmpty == true) {
            pane.bounds = Rect(shape.origin, Size.Zero)
            pane.view = pane.bounds
            return
        }

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

        // While recording, this scope stands in as its own density, so reading a density from it
        // would never return. The renderer therefore draws with the real one handed to it.
        val screen = requireDensity()
        val direction = layoutDirection
        scene.view = with(renderer) { view(scene) }
        pane.bounds = paintedBounds(scene).translate(scene.origin)
        pane.view = scene.view.translate(scene.origin)
        scene.beneath = beneath?.value.orEmpty()
        scene.enclosing = pane.within
        pane.surface.record {
            draw(screen, direction, drawContext.canvas, bounds, drawContext.graphicsLayer) {
                translate(shift.x, shift.y) { with(renderer) { render(scene) } }
            }
        }
        if (!scene.isSettled) drawAgain()
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
            PointerEventType.Press -> touch.press(coroutineScope, change.position)
            PointerEventType.Move -> touch.move(change.position)
            PointerEventType.Release -> touch.release(coroutineScope)
        }
    }

    override fun onCancelPointerInput() {
        touch.release(coroutineScope)
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

    private fun isPartOf(backdrop: GlassBackdrop): Boolean =
        (findNearestAncestor(BackdropNode) as? BackdropNode)?.backdrop === backdrop

    // Where the parents of this glass let it show, in the backdrop: open on every side they
    // leave alone, so shadow and swell still draw past its own bounds there.
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

    private fun drawAgain() {
        coroutineScope.launch { redraws++ }
    }

    private fun settle() {
        if (!isAttached) return
        touch.settle(coroutineScope)
        motion.reset(coroutineScope)
    }

    companion object {
        private const val Unbounded = 100000f
    }
}
