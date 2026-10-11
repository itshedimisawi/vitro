package io.github.itshedimisawi.vitro.internal

import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassAppearance
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.GlassInteractionState
import io.github.itshedimisawi.vitro.GlassPane
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
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
        properties["glass"] = glass
        properties["shape"] = shape
    }

    override fun equals(other: Any?): Boolean =
        other is GlassEffectElement &&
            other.backdrop === backdrop &&
            other.glass == glass &&
            other.shape == shape &&
            other.interaction === interaction &&
            other.onAppearanceChanged === onAppearanceChanged

    override fun hashCode(): Int =
        31 * (31 * (31 * (31 * backdrop.hashCode() + glass.hashCode()) + shape.hashCode()) +
            interaction.hashCode()) + onAppearanceChanged.hashCode()
}

internal class GlassNode(
    backdrop: GlassBackdrop,
    glass: Glass,
    private var corners: CornerBasedShape,
    interaction: GlassInteractionState?,
    private var onAppearanceChanged: ((GlassAppearance) -> Unit)?,
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
    private var redraws by mutableIntStateOf(0)

    private var placement by mutableStateOf(Offset.Unspecified)
    private var placedAt = 0L
    private val stretch = Animatable(Offset.Zero, Offset.VectorConverter)
    private val ownTouch = GlassInteractionState()
    private var touch = interaction ?: ownTouch
    private var measured = Size.Zero

    var backdrop = backdrop
        private set
    var glass by mutableStateOf(glass)
        private set
    var material = glass.material()
        private set
    private var isNight by mutableStateOf(false)
    private val darkness = Animatable(0f)
    private var toning: Job? = null
    private var isWatchingTone = false

    fun update(
        backdrop: GlassBackdrop,
        glass: Glass,
        shape: CornerBasedShape,
        interaction: GlassInteractionState?,
        onAppearanceChanged: ((GlassAppearance) -> Unit)?,
    ) {
        this.onAppearanceChanged = onAppearanceChanged
        this.glass = glass
        material = glass.material()
        if (backdrop !== this.backdrop) {
            watchTone(false)
            pane?.let {
                this.backdrop.panes.remove(it)
                backdrop.panes.add(it)
                beneath = glassBeneath(it, backdrop)
            }
            this.backdrop = backdrop
            pane?.let { followTone(it) }
        }
        watchTone(glass.appearance == GlassAppearance.Automatic)
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
        pane = GlassPane(context.createGraphicsLayer(), context.createGraphicsLayer()).also {
            backdrop.panes.add(it)
            beneath = glassBeneath(it, backdrop)
            followTone(it)
        }
        watchTone(glass.appearance == GlassAppearance.Automatic)
        renderer = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> ShaderRenderer()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> BlurRenderer()
            else -> SnapshotRenderer(context, coroutineScope)
        }
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
        container?.leave(this)
        container = null
        renderer?.release()
        renderer = null
        scene?.let { requireGraphicsContext().releaseGraphicsLayer(it.layer) }
        scene = null
        pane?.let {
            backdrop.panes.remove(it)
            requireGraphicsContext().releaseGraphicsLayer(it.surface)
            requireGraphicsContext().releaseGraphicsLayer(it.layer)
        }
        pane = null
        beneath = null
        watchTone(false)
        placement = Offset.Unspecified
    }

    /** The outline of this glass as [arrange] last left it. */
    val shape: GlassShape? get() = scene?.shape

    /**
     * Works out where this glass sits and what the touch on it has done to it, and publishes
     * that for the glass around it. False while there is nothing to draw.
     */
    fun arrange(): Boolean {
        val scene = scene
        val pane = pane
        val source = backdrop.coordinates
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
        // Held glass swells, drifts toward the finger and stretches along the pull.
        val held = touch.scale
        val position = source.localPositionOf(requireLayoutCoordinates(), Offset.Zero)
        shape.origin = position + touch.offset
        shape.size = measured
        scene.origin = shape.origin
        scene.extent = measured
        scene.revision = backdrop.generation
        isNight = isNightMode()
        shape.darkness = darkness.value
        val finger = touch.position
        shape.touch = Offset(finger.x.coerceIn(0f, measured.width), finger.y.coerceIn(0f, measured.height))
        shape.pressure = touch.pressure.coerceAtLeast(0f)
        shape.glow = shape.pressure * (PressGlow + PressGlowPerGrowth * touch.growth)
        resolveRadii(shape.radii)

        val pull = stretch.value
        shape.scaleX = held.scaleX + pull.x - pull.y * 0.5f
        shape.scaleY = held.scaleY + pull.y - pull.x * 0.5f

        pane.origin = position
        pane.outline = Rect(shape.origin, measured)
        pane.shade = shape.shade()
        pane.within = enclosingGlass(backdrop, pane.within)
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

        val around = container
        if (around != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // The first piece of a group draws the material for all of them; each adds its content.
            pane.bounds = shape.grown
            pane.view = Rect(shape.origin, Size.Zero)
            pane.surface.record {}
            if (!with(around) { drawGroupLedBy(this@GlassNode) }) drawAgain()
            pane.layer.record {
                translate(shift.x, shift.y) {
                    scale(shape.scaleX, shape.scaleY, pivot = bounds.center) {
                        this@draw.drawContent()
                    }
                }
            }
            drawLayer(pane.layer)
            return
        }

        // Everything this glass draws is kept, so glass placed on top of it can show it too: the
        // material on its own for glass inside this piece, and the whole for the rest.
        // While recording, this scope stands in as its own density, so reading a density from it
        // would never return. The renderer therefore draws with the real one handed to it.
        val screen = requireDensity()
        val direction = layoutDirection
        // Where this glass paints and what it takes in are published before anything is drawn,
        // so each piece knows which of the others it has in view.
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
            translate(shift.x, shift.y) {
                scale(shape.scaleX, shape.scaleY, pivot = bounds.center) {
                    this@draw.drawContent()
                }
            }
        }
        drawLayer(pane.layer)
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val current = coordinates.positionInRoot()
        val previous = placement
        val now = System.nanoTime()
        val elapsed = (now - placedAt) / 1_000_000f
        placement = current
        placedAt = now
        if (glass.isInteractive && previous.isSpecified && current != previous && elapsed > 0f) {
            follow((current - previous) / elapsed)
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

    // The glass under this piece that paints inside its view: pieces added before it and pieces
    // it is inside of, but never one placed inside it, which is part of its own content.
    // Derived, so this glass is redrawn when glass in its view changes and not when any moves.
    private fun glassBeneath(pane: GlassPane, backdrop: GlassBackdrop) = derivedStateOf {
        val panes = backdrop.panes
        val index = panes.indexOf(pane)
        val within = pane.within
        panes.filterIndexed { position, other ->
            other !== pane &&
                (other in within || position < index && pane !in other.within) &&
                other.bounds.overlaps(pane.view)
        }
    }

    // Asks for one more drawing once this one is over, for a renderer that has room to give back.
    private fun drawAgain() {
        coroutineScope.launch { redraws++ }
    }

    private fun watchTone(isWanted: Boolean) {
        if (isWanted == isWatchingTone) return
        isWatchingTone = isWanted
        backdrop.toneWatchers += if (isWanted) 1 else -1
    }

    // Keeps the material in step with the appearance asked for and, when that is automatic, with
    // what lies under this glass: the backdrop as seen through the glass beneath it. A change
    // brought by that is eased in; the rest is immediate.
    private fun followTone(pane: GlassPane) {
        val backdrop = backdrop
        val beneath = beneath
        toning?.cancel()
        toning = coroutineScope.launch {
            var shown: GlassAppearance? = null
            var wasSampled = false
            snapshotFlow {
                val luminance = when (glass.appearance) {
                    GlassAppearance.Automatic ->
                        backdrop.tone?.luminance(pane.outline, beneath?.value.orEmpty()) ?: Float.NaN
                    else -> Float.NaN
                }
                val wanted = when {
                    glass.appearance == GlassAppearance.Light -> 0f
                    glass.appearance == GlassAppearance.Dark -> 1f
                    luminance.isNaN() -> if (isNight) 1f else 0f
                    else -> darknessOver(luminance, isNight)
                }
                wanted to !luminance.isNaN()
            }.collectLatest { (wanted, isSampled) ->
                val appearance = if (wanted >= 0.5f) GlassAppearance.Dark else GlassAppearance.Light
                if (appearance != shown) {
                    shown = appearance
                    onAppearanceChanged?.invoke(appearance)
                }
                val isEased = isSampled && wasSampled
                wasSampled = isSampled
                if (isEased) darkness.animateTo(wanted, ToneSpring) else darkness.snapTo(wanted)
            }
        }
    }

    private fun isNightMode(): Boolean {
        val mode = currentValueOf(LocalConfiguration).uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun follow(velocity: Offset) {
        val scale = StretchPerVelocity / requireDensity().density
        val target = Offset(
            (abs(velocity.x) * scale).coerceAtMost(MaxStretch),
            (abs(velocity.y) * scale).coerceAtMost(MaxStretch),
        )
        coroutineScope.launch {
            stretch.snapTo((stretch.value + target) / 2f)
            stretch.animateTo(Offset.Zero, spring(dampingRatio = 0.4f, stiffness = 180f))
        }
    }

    private fun settle() {
        if (!isAttached) return
        touch.settle(coroutineScope)
        coroutineScope.launch { stretch.snapTo(Offset.Zero) }
    }

    private fun resolveRadii(target: FloatArray) {
        val size = measured
        val density = requireDensity()
        val limit = size.minDimension / 2f
        val isLtr = requireLayoutDirection() == LayoutDirection.Ltr
        val topStart = corners.topStart.toPx(size, density).coerceIn(0f, limit)
        val topEnd = corners.topEnd.toPx(size, density).coerceIn(0f, limit)
        val bottomEnd = corners.bottomEnd.toPx(size, density).coerceIn(0f, limit)
        val bottomStart = corners.bottomStart.toPx(size, density).coerceIn(0f, limit)
        target[0] = if (isLtr) topStart else topEnd
        target[1] = if (isLtr) topEnd else topStart
        target[2] = if (isLtr) bottomEnd else bottomStart
        target[3] = if (isLtr) bottomStart else bottomEnd
    }

    companion object {
        private val ToneSpring = spring<Float>(dampingRatio = 1f, stiffness = 120f)
        private const val StretchPerVelocity = 0.2f
        private const val MaxStretch = 0.14f
    }
}

/**
 * The glass whose content this node is part of, outermost first. Returns [known] when nothing has
 * changed, so the common case of standing alone costs nothing.
 */
internal fun DelegatableNode.enclosingGlass(backdrop: GlassBackdrop, known: List<GlassPane>): List<GlassPane> {
    var found: ArrayList<GlassPane>? = null
    traverseAncestors(GlassNode) { ancestor ->
        (ancestor as GlassNode).pane?.let { outer ->
            if (outer in backdrop.panes) (found ?: ArrayList<GlassPane>().also { found = it }).add(0, outer)
        }
        true
    }
    val enclosing = found ?: emptyList()
    return if (enclosing == known) known else enclosing
}
