package io.github.itshedimisawi.vitro.internal.node

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.UnplacedAwareModifierNode
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireView
import androidx.compose.ui.platform.InspectorInfo
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.internal.tone.ToneReader

internal class GlassBackdropElement(
    private val backdrop: GlassBackdrop,
) : ModifierNodeElement<BackdropNode>() {

    override fun create(): BackdropNode = BackdropNode(backdrop)

    override fun update(node: BackdropNode) {
        node.backdrop = backdrop
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glassBackdrop"
    }

    override fun equals(other: Any?): Boolean =
        other is GlassBackdropElement && other.backdrop === backdrop

    override fun hashCode(): Int = backdrop.hashCode()
}

internal class BackdropNode(
    var backdrop: GlassBackdrop,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode, UnplacedAwareModifierNode, TraversableNode {

    override val traverseKey: Any get() = BackdropNode

    private val reader = ToneReader { backdrop.tone = it }
    private var inside: GraphicsLayer? = null

    override fun ContentDrawScope.draw() {
        backdrop.layer.record { this@draw.drawContent() }
        drawLayer(backdrop.layer)
        // Glass inside the content stays out of the recording and is drawn on top here, softened
        // by the scroll edge effect's veils like the rest of the content.
        val edge = backdrop.edge
        when {
            backdrop.inside.isEmpty() -> Unit
            edge == null -> drawInsidePanes()
            else -> {
                val layer = inside ?: requireGraphicsContext().createGraphicsLayer().also { inside = it }
                layer.record {
                    drawInsidePanes()
                    with(edge) { drawVeils(BlendMode.SrcAtop) }
                }
                drawLayer(layer)
            }
        }
        // Only the snapshot renderer needs to know that the content changed.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) backdrop.generation++
        if (backdrop.isToneWatched) {
            with(reader) { read(backdrop.layer, requireGraphicsContext(), coroutineScope, requireView()) }
        } else {
            reader.pause()
        }
    }

    private fun DrawScope.drawInsidePanes() {
        for (pane in backdrop.inside) {
            val clip = pane.clip
            if (clip == null) {
                translate(pane.origin.x, pane.origin.y) { drawLayer(pane.layer) }
            } else if (!clip.isEmpty) {
                clipRect(clip.left, clip.top, clip.right, clip.bottom) {
                    translate(pane.origin.x, pane.origin.y) { drawLayer(pane.layer) }
                }
            }
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        backdrop.coordinates = coordinates
    }

    // Unplaced content is not drawn, so there is nothing to read until it is placed again.
    override fun onUnplaced() {
        reader.pause()
    }

    override fun onDetach() {
        backdrop.coordinates = null
        reader.release(requireGraphicsContext())
        inside?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        inside = null
    }

    companion object
}
