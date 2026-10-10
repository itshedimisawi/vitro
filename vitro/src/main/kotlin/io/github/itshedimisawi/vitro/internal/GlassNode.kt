package io.github.itshedimisawi.vitro.internal

import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
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
import androidx.compose.ui.unit.dp
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassAppearance
import io.github.itshedimisawi.vitro.GlassBackdrop
import kotlin.math.abs
import kotlinx.coroutines.launch

internal class GlassEffectElement(
    private val backdrop: GlassBackdrop,
    private val glass: Glass,
    private val shape: CornerBasedShape,
) : ModifierNodeElement<GlassNode>() {

    override fun create(): GlassNode = GlassNode(backdrop, glass, shape)

    override fun update(node: GlassNode) {
        node.update(backdrop, glass, shape)
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
            other.shape == shape

    override fun hashCode(): Int =
        31 * (31 * backdrop.hashCode() + glass.hashCode()) + shape.hashCode()
}

internal class GlassNode(
    private var backdrop: GlassBackdrop,
    private var glass: Glass,
    private var shape: CornerBasedShape,
) : Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    PointerInputModifierNode,
    CompositionLocalConsumerModifierNode {

    private var scene: GlassScene? = null
    private var renderer: GlassRenderer? = null

    private var placement by mutableStateOf(Offset.Unspecified)
    private var placedAt = 0L
    private var touch by mutableStateOf(Offset.Zero)
    private val pressure = Animatable(0f)
    private val stretch = Animatable(Offset.Zero, Offset.VectorConverter)

    fun update(backdrop: GlassBackdrop, glass: Glass, shape: CornerBasedShape) {
        this.backdrop = backdrop
        this.glass = glass
        this.shape = shape
        if (!glass.isInteractive) settle()
        invalidateDraw()
    }

    override fun onAttach() {
        val context = requireGraphicsContext()
        scene = GlassScene(context.createGraphicsLayer(), backdrop.layer, glass)
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
        placement = Offset.Unspecified
    }

    override fun ContentDrawScope.draw() {
        val scene = scene
        val renderer = renderer
        val source = backdrop.coordinates
        if (scene == null || renderer == null || source == null || !source.isAttached ||
            size.minDimension <= 0f
        ) {
            drawContent()
            return
        }

        placement
        scene.backdrop = backdrop.layer
        scene.glass = glass
        scene.origin = source.localPositionOf(requireLayoutCoordinates(), Offset.Zero)
        scene.revision = backdrop.generation
        scene.isDark = when (glass.appearance) {
            GlassAppearance.Light -> false
            GlassAppearance.Dark -> true
            GlassAppearance.Automatic -> isNightMode()
        }
        scene.touch = touch
        scene.pressure = pressure.value
        resolveRadii(scene.radii)

        val swell = pressure.value * PressGrowth.toPx() / size.maxDimension
        val pull = stretch.value
        scene.scaleX = 1f + swell + pull.x - pull.y * 0.5f
        scene.scaleY = 1f + swell + pull.y - pull.x * 0.5f
        with(renderer) { render(scene) }
        scale(scene.scaleX, scene.scaleY) { this@draw.drawContent() }
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
        touch = change.position
        when (pointerEvent.type) {
            PointerEventType.Press -> press(1f)
            PointerEventType.Release -> press(0f)
        }
    }

    override fun onCancelPointerInput() {
        if (pressure.targetValue != 0f) press(0f)
    }

    override fun sharePointerInputWithSiblings(): Boolean = !glass.isInteractive

    private fun isNightMode(): Boolean {
        val mode = currentValueOf(LocalConfiguration).uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun press(target: Float) {
        coroutineScope.launch {
            pressure.animateTo(target, spring(dampingRatio = 0.55f, stiffness = 320f))
        }
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
        coroutineScope.launch {
            pressure.snapTo(0f)
            stretch.snapTo(Offset.Zero)
        }
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
        val PressGrowth = 14.dp
        const val StretchPerVelocity = 0.2f
        const val MaxStretch = 0.14f
    }
}
