package io.github.itshedimisawi.vitro.internal.render

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import io.github.itshedimisawi.vitro.internal.material.GlassShade

/**
 * One piece of glass over a backdrop, as the glass above it sees it. [surface] holds its shadow and
 * material, [layer] those with its content on top. Areas are in backdrop coordinates.
 */
internal class GlassPane(val surface: GraphicsLayer, val layer: GraphicsLayer) {
    /** Whether the pane is part of the backdrop's own content. */
    var isContent = false

    /** Where its parents let the pane show, when it is part of the content; null when uncut. */
    var clip by mutableStateOf<Rect?>(null)
    var origin by mutableStateOf(Offset.Zero)
    var outline by mutableStateOf(Rect.Zero)

    /** The area the pane paints on, shadow included. */
    var bounds by mutableStateOf(Rect.Zero)

    /** The area of the backdrop the pane takes in. */
    var view by mutableStateOf(Rect.Zero)

    /** How the material changes the colour of what shows through it. */
    var shade by mutableStateOf(GlassShade.None)

    /** The glass this pane lies inside, outermost first. */
    var within by mutableStateOf(emptyList<GlassPane>())
}
