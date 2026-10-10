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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutCoordinates
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
import kotlinx.coroutines.launch

internal class GlassEffectElement(
    private val backdrop: GlassBackdrop,
    private val glass: Glass,
    private val shape: CornerBasedShape,
    private val interaction: GlassInteractionState?,
) : ModifierNodeElement<GlassNode>() {

    override fun create(): GlassNode = GlassNode(backdrop, glass, shape, interaction)

    override fun update(node: GlassNode) {
        node.update(backdrop, glass, shape, interaction)
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
            other.interaction === interaction

    override fun hashCode(): Int =
        31 * (31 * (31 * backdrop.hashCode() + glass.hashCode()) + shape.hashCode()) +
            interaction.hashCode()
}

internal class GlassNode(
    private var backdrop: GlassBackdrop,
    private var glass: Glass,
    private var shape: CornerBasedShape,
    interaction: GlassInteractionState?,
) : Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    LayoutAwareModifierNode,
    PointerInputModifierNode,
    CompositionLocalConsumerModifierNode {

    private var scene: GlassScene? = null
    private var renderer: GlassRenderer? = null
    private var pane: GlassPane? = null
    private var beneath: State<List<GlassPane>>? = null

    private var placement by mutableStateOf(Offset.Unspecified)
    private var placedAt = 0L
    private val stretch = Animatable(Offset.Zero, Offset.VectorConverter)
    private val ownTouch = GlassInteractionState()
    private var touch = interaction ?: ownTouch
    private var measured = Size.Zero

    fun update(
        backdrop: GlassBackdrop,
        glass: Glass,
        shape: CornerBasedShape,
        interaction: GlassInteractionState?,
    ) {
        if (backdrop !== this.backdrop) {
            pane?.let {
                this.backdrop.panes.remove(it)
                backdrop.panes.add(it)
                beneath = glassBeneath(it, backdrop)
            }
        }
        this.backdrop = backdrop
        this.glass = glass
        this.shape = shape
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
        pane = GlassPane(context.createGraphicsLayer()).also {
            backdrop.panes.add(it)
            beneath = glassBeneath(it, backdrop)
        }
        renderer = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> ShaderRenderer()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> BlurRenderer()
            else -> SnapshotRenderer(context, coroutineScope)
        }
    }

    override fun onDetach() {
        renderer?.release()
        renderer = null
        scene?.let { requireGraphicsContext().releaseGraphicsLayer(it.layer) }
        scene = null
        pane?.let {
            backdrop.panes.remove(it)
            requireGraphicsContext().releaseGraphicsLayer(it.layer)
        }
        pane = null
        beneath = null
        placement = Offset.Unspecified
    }

    override fun ContentDrawScope.draw() {
        val scene = scene
        val renderer = renderer
        val pane = pane
        val source = backdrop.coordinates
        if (scene == null || renderer == null || pane == null || source == null ||
            !source.isAttached || size.minDimension <= 0f
        ) {
            drawContent()
            return
        }

        placement
        scene.backdrop = backdrop.layer
        scene.glass = glass
        // Held glass swells, drifts toward the finger and stretches along the pull.
        val held = touch.scale
        val shift = touch.offset

        val position = source.localPositionOf(requireLayoutCoordinates(), Offset.Zero)
        scene.origin = position + shift
        scene.revision = backdrop.generation
        scene.isDark = when (glass.appearance) {
            GlassAppearance.Light -> false
            GlassAppearance.Dark -> true
            GlassAppearance.Automatic -> isNightMode()
        }
        val finger = touch.position
        scene.touch = Offset(finger.x.coerceIn(0f, size.width), finger.y.coerceIn(0f, size.height))
        scene.pressure = touch.pressure.coerceAtLeast(0f)
        scene.glow = scene.pressure * (PressGlow + PressGlowPerGrowth * touch.growth)
        resolveRadii(scene.radii)

        val pull = stretch.value
        scene.scaleX = held.scaleX + pull.x - pull.y * 0.5f
        scene.scaleY = held.scaleY + pull.y - pull.x * 0.5f
        // Everything this glass draws is kept, so glass placed on top of it can show it too.
        // While recording, this scope stands in as its own density, so reading a density from it
        // would never return. The renderer therefore draws with the real one handed to it.
        val screen = requireDensity()
        val direction = layoutDirection
        val bounds = size
        // Where this glass paints and what it takes in are published before anything is drawn,
        // so each piece knows which of the others it has in view.
        scene.view = with(renderer) { view(scene) }
        pane.origin = position
        pane.bounds = paintedBounds(scene).translate(scene.origin)
        pane.view = scene.view.translate(scene.origin)
        scene.beneath = beneath?.value.orEmpty()
        pane.layer.record {
            draw(screen, direction, drawContext.canvas, bounds, drawContext.graphicsLayer) {
                translate(shift.x, shift.y) { with(renderer) { render(scene) } }
            }
            translate(shift.x, shift.y) {
                scale(scene.scaleX, scene.scaleY, pivot = bounds.center) {
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

    // Derived, so this glass is redrawn when glass in its view changes and not when any moves.
    private fun glassBeneath(pane: GlassPane, backdrop: GlassBackdrop) = derivedStateOf {
        val panes = backdrop.panes
        val index = panes.indexOf(pane)
        if (index <= 0) emptyList() else panes.subList(0, index).filter { it.bounds.overlaps(pane.view) }
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

    private fun ContentDrawScope.resolveRadii(target: FloatArray) {
        val limit = size.minDimension / 2f
        val isLtr = layoutDirection == LayoutDirection.Ltr
        val topStart = shape.topStart.toPx(size, this).coerceIn(0f, limit)
        val topEnd = shape.topEnd.toPx(size, this).coerceIn(0f, limit)
        val bottomEnd = shape.bottomEnd.toPx(size, this).coerceIn(0f, limit)
        val bottomStart = shape.bottomStart.toPx(size, this).coerceIn(0f, limit)
        target[0] = if (isLtr) topStart else topEnd
        target[1] = if (isLtr) topEnd else topStart
        target[2] = if (isLtr) bottomEnd else bottomStart
        target[3] = if (isLtr) bottomStart else bottomEnd
    }

    private companion object {
        const val StretchPerVelocity = 0.2f
        const val MaxStretch = 0.14f
    }
}
