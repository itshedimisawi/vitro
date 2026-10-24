package io.github.itshedimisawi.vitro.internal.node

import android.content.res.Configuration
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.LocalGlassDarkTheme
import io.github.itshedimisawi.vitro.internal.render.GlassPane
import io.github.itshedimisawi.vitro.internal.render.GlassRenderer
import io.github.itshedimisawi.vitro.internal.render.GlassScene
import io.github.itshedimisawi.vitro.internal.render.paintedBounds

/** Whether the interface is in its dark theme: as the app provides it, or else as the system has it. */
internal fun CompositionLocalConsumerModifierNode.isNightMode(): Boolean {
    currentValueOf(LocalGlassDarkTheme)?.let { return it }
    val mode = currentValueOf(LocalConfiguration).uiMode and Configuration.UI_MODE_NIGHT_MASK
    return mode == Configuration.UI_MODE_NIGHT_YES
}

/**
 * Writes the corner radii of this shape at [size] into [target], clockwise from the top left,
 * each no larger than half the shorter side.
 */
internal fun CornerBasedShape.resolveRadii(
    size: Size,
    density: Density,
    layoutDirection: LayoutDirection,
    target: FloatArray,
) {
    val limit = size.minDimension / 2f
    val isLtr = layoutDirection == LayoutDirection.Ltr
    val topStart = topStart.toPx(size, density).coerceIn(0f, limit)
    val topEnd = topEnd.toPx(size, density).coerceIn(0f, limit)
    val bottomEnd = bottomEnd.toPx(size, density).coerceIn(0f, limit)
    val bottomStart = bottomStart.toPx(size, density).coerceIn(0f, limit)
    target[0] = if (isLtr) topStart else topEnd
    target[1] = if (isLtr) topEnd else topStart
    target[2] = if (isLtr) bottomEnd else bottomStart
    target[3] = if (isLtr) bottomStart else bottomEnd
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

/** The backdrop whose content this node is part of, if any. */
internal fun DelegatableNode.enclosingBackdrop(): GlassBackdrop? =
    (findNearestAncestor(BackdropNode) as? BackdropNode)?.backdrop

/**
 * Records the shadow and material of [scene] into the surface of [pane], and publishes the area
 * the pane paints and takes in. False while the renderer holds spare room to give back.
 */
internal fun ContentDrawScope.recordMaterial(
    renderer: GlassRenderer,
    scene: GlassScene,
    pane: GlassPane,
    beneath: State<List<GlassPane>>?,
    density: Density,
): Boolean {
    scene.view = with(renderer) { view(scene) }
    pane.bounds = paintedBounds(scene).translate(scene.origin)
    pane.view = scene.view.translate(scene.origin)
    scene.beneath = beneath?.value.orEmpty()
    scene.enclosing = pane.within
    val shift = scene.origin - pane.origin
    val direction = layoutDirection
    val bounds = size
    // A recording scope is its own density and cannot be read from, so the real one is passed in.
    pane.surface.record {
        draw(density, direction, drawContext.canvas, bounds, drawContext.graphicsLayer) {
            translate(shift.x, shift.y) { with(renderer) { render(scene) } }
        }
    }
    return scene.isSettled
}
