package io.github.itshedimisawi.vitro.internal.node

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import io.github.itshedimisawi.vitro.internal.render.GlassShaders
import io.github.itshedimisawi.vitro.internal.render.joinedWith
import io.github.itshedimisawi.vitro.internal.render.paintedArea

internal class GlassContainerElement(
    private val spacing: Dp,
) : ModifierNodeElement<GlassContainerNode>() {

    override fun create(): GlassContainerNode = GlassContainerNode(spacing)

    override fun update(node: GlassContainerNode) {
        node.spacing = spacing
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glassEffectContainer"
        properties["spacing"] = spacing
    }

    override fun equals(other: Any?): Boolean = other is GlassContainerElement && other.spacing == spacing

    override fun hashCode(): Int = spacing.hashCode()
}

/**
 * Collects the glass below it into groups drawn as one. The first piece of each group draws the
 * group's material, and every piece draws its own content.
 */
internal class GlassContainerNode(spacing: Dp) : Modifier.Node(), TraversableNode {

    override val traverseKey: Any get() = GlassContainerNode

    var spacing by mutableStateOf(spacing)

    /** Joining needs the shader, so before Android 13 the glass inside draws itself as usual. */
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val isJoining: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    private val members = mutableStateListOf<GlassNode>()
    private val groups = ArrayList<GlassGroup>()

    fun join(node: GlassNode) {
        members += node
    }

    fun leave(node: GlassNode) {
        members -= node
    }

    override fun onDetach() {
        if (!isJoining) return
        val context = requireGraphicsContext()
        groups.forEach { it.release(context) }
        groups.clear()
    }

    /**
     * Draws the material of the group that [lead] is first in; does nothing for other pieces.
     * False when the group should be drawn once more to give back spare room.
     */
    @RequiresApi(33)
    fun ContentDrawScope.drawGroupLedBy(lead: GlassNode): Boolean {
        regroup(requireGraphicsContext())
        val group = groups.firstOrNull { it.members.firstOrNull() === lead } ?: return true
        val position = lead.pane?.origin ?: return true

        // Each piece keeps its own clip; the group shows as far as its pieces are visible.
        group.shapes.clear()
        var isCut = false
        var shown: Rect? = null
        var painted: Rect? = null
        for (member in group.members) {
            if (!member.arrange()) continue
            val shape = member.shape ?: continue
            group.shapes += shape
            val own = member.pane?.clip
            if (own != null) isCut = true
            val outline = if (own == null) shape.grown else shape.grown.intersect(own)
            val area = paintedArea(shape, member.glass).let { if (own == null) it else it.intersect(own) }
            if (!outline.isEmpty) shown = shown.joinedWith(outline)
            if (!area.isEmpty) painted = painted.joinedWith(area)
        }
        if (group.shapes.isEmpty()) return true

        val scene = group.scene
        val frame = group.frame()
        scene.backdrop = group.backdrop.layer
        scene.glass = lead.glass
        scene.shapes = group.shapes
        scene.spacing = spacing.toPx().coerceAtLeast(0f)
        scene.origin = frame.topLeft
        scene.extent = frame.size
        scene.revision = group.backdrop.generation
        scene.visible = if (isCut) (shown ?: Rect.Zero).translate(-frame.left, -frame.top) else null

        val pane = group.pane
        pane.origin = position
        pane.within = lead.pane?.within.orEmpty()
        pane.clip = if (isCut) painted ?: Rect.Zero else null
        val isSettled = recordMaterial(group.renderer, scene, pane, group.beneath, requireDensity())
        if (!lead.isInside) drawLayer(pane.surface)
        return isSettled
    }

    // Pieces of one backdrop and material form a group, split when it outgrows one pass.
    @RequiresApi(33)
    private fun regroup(context: GraphicsContext) {
        groups.forEach { it.members.clear() }
        for (member in members) {
            val pane = member.pane ?: continue
            if (member.glass.isIdentity) continue
            val group = groups.firstOrNull {
                it.backdrop === member.backdrop && it.material == member.material &&
                    it.members.size < GlassShaders.MaxShapes
            } ?: GlassGroup(member.backdrop, member.material, member.glass, context, before = pane, member.isInside)
                .also { groups += it }
            group.members += member
        }
        val emptied = groups.filter { it.members.isEmpty() }
        emptied.forEach { it.release(context) }
        groups.removeAll(emptied)
    }

    companion object
}
