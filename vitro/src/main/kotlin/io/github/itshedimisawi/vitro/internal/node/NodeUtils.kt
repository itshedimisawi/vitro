package io.github.itshedimisawi.vitro.internal.node

import android.content.res.Configuration
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.internal.render.GlassPane

/** Whether the interface is in its dark theme. */
internal fun CompositionLocalConsumerModifierNode.isNightMode(): Boolean {
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
