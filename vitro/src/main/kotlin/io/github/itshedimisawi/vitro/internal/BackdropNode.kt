package io.github.itshedimisawi.vitro.internal

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.itshedimisawi.vitro.GlassBackdrop
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private var miniature: GraphicsLayer? = null
    private var reading: Job? = null
    private var isStale = false

    override fun ContentDrawScope.draw() {
        backdrop.layer.record { this@draw.drawContent() }
        drawLayer(backdrop.layer)
        // Only the snapshot-based renderer needs to be told that the content changed.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) backdrop.generation++
        if (backdrop.toneWatchers > 0) readTone()
    }

    // Draws the content very small and reads it back, at most a few times a second.
    private fun ContentDrawScope.readTone() {
        val small = miniature ?: requireGraphicsContext().createGraphicsLayer().also { miniature = it }
        val cell = ToneCell.toPx().coerceAtLeast(1f)
        small.record(size = IntSize(ceil(size.width / cell).toInt(), ceil(size.height / cell).toInt())) {
            scale(1f / cell, 1f / cell, pivot = Offset.Zero) { drawLayer(backdrop.layer) }
        }
        isStale = true
        if (reading?.isActive == true) return
        reading = coroutineScope.launch {
            while (isStale) {
                isStale = false
                val shot = small.toImageBitmap()
                backdrop.tone = withContext(Dispatchers.Default) { BackdropTone.read(shot, cell) }
                delay(ToneInterval)
            }
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        backdrop.coordinates = coordinates
    }

    override fun onDetach() {
        backdrop.coordinates = null
        reading?.cancel()
        miniature?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        miniature = null
    }

    private companion object {
        val ToneCell = 6.dp
        const val ToneInterval = 120L
    }
}
