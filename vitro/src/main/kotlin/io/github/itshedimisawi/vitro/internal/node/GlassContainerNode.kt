package io.github.itshedimisawi.vitro.internal.node

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import io.github.itshedimisawi.vitro.internal.render.GlassShaders
import io.github.itshedimisawi.vitro.internal.render.paintedBounds

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
 * Collects the glass below it into groups that are drawn as one. It draws nothing itself: the
 * first piece of each group draws the material of the whole group where it stands, and every
 * piece then adds its own content.
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
     * Draws the material of the group that [lead] is the first piece of, in the scope [lead] is
     * drawing in. Does nothing for the other pieces of a group. False if the group should be
     * drawn once more, because its renderer has room to give back.
     */
    @RequiresApi(33)
    fun ContentDrawScope.drawGroupLedBy(lead: GlassNode): Boolean {
        regroup(requireGraphicsContext())
        val group = groups.firstOrNull { it.members.firstOrNull() === lead } ?: return true
        val position = lead.pane?.origin ?: return true

        group.shapes.clear()
        for (member in group.members) {
            if (member.arrange()) member.shape?.let { group.shapes += it }
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
        scene.view = with(group.renderer) { view(scene) }

        val pane = group.pane
        pane.origin = position
        pane.bounds = paintedBounds(scene).translate(scene.origin)
        pane.view = scene.view.translate(scene.origin)
        pane.within = lead.pane?.within.orEmpty()
        pane.clip = lead.pane?.clip
        scene.beneath = group.beneath.value
        scene.enclosing = pane.within

        val screen = requireDensity()
        val direction = layoutDirection
        val bounds = size
        val shift = scene.origin - position
        pane.surface.record {
            draw(screen, direction, drawContext.canvas, bounds, drawContext.graphicsLayer) {
                translate(shift.x, shift.y) { with(group.renderer) { render(scene) } }
            }
        }
        if (!lead.isInside) drawLayer(pane.surface)
        return scene.isSettled
    }

    // Pieces are drawn together when they share a backdrop and a material. A group that would
    // outgrow one pass is continued in another.
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
