package io.github.itshedimisawi.vitro.internal.node

import androidx.annotation.RequiresApi
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.GraphicsContext
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.internal.material.GlassMaterial
import io.github.itshedimisawi.vitro.internal.render.GlassPane
import io.github.itshedimisawi.vitro.internal.render.GlassScene
import io.github.itshedimisawi.vitro.internal.render.GlassShape
import io.github.itshedimisawi.vitro.internal.render.ShaderRenderer
import io.github.itshedimisawi.vitro.internal.render.joinedWith

/**
 * Glass of one material over one backdrop, drawn in a single pass. Its pane sits under the panes
 * of its pieces, which then hold only their content.
 */
@RequiresApi(33)
internal class GlassGroup(
    val backdrop: GlassBackdrop,
    val material: GlassMaterial,
    glass: Glass,
    context: GraphicsContext,
    before: GlassPane,
    isInside: Boolean,
) {
    val members = ArrayList<GlassNode>()
    val shapes = ArrayList<GlassShape>()
    val renderer = ShaderRenderer()
    private val picture = context.createGraphicsLayer()
    val pane = GlassPane(picture, picture)
    val scene = GlassScene(context.createGraphicsLayer(), backdrop.layer, glass)
    val beneath: State<List<GlassPane>> = backdrop.glassBeneath(pane)

    init {
        backdrop.addPane(pane, isInside, before)
    }

    /** The smallest rectangle around every piece at rest, in the backdrop. */
    fun frame(): Rect {
        var frame: Rect? = null
        for (shape in shapes) frame = frame.joinedWith(Rect(shape.origin, shape.size))
        return frame ?: Rect.Zero
    }

    fun release(context: GraphicsContext) {
        backdrop.removePane(pane)
        renderer.release()
        context.releaseGraphicsLayer(picture)
        context.releaseGraphicsLayer(scene.layer)
    }
}
