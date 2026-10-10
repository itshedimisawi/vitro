package io.github.itshedimisawi.vitro.internal

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import io.github.itshedimisawi.vitro.GlassBackdrop

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
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    override fun ContentDrawScope.draw() {
        backdrop.layer.record { this@draw.drawContent() }
        drawLayer(backdrop.layer)
        // Only the snapshot-based renderer needs to be told that the content changed.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) backdrop.generation++
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        backdrop.coordinates = coordinates
    }

    override fun onDetach() {
        backdrop.coordinates = null
    }
}
