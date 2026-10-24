package io.github.itshedimisawi.vitro.internal.render

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import io.github.itshedimisawi.vitro.internal.material.GlassShade

/**
 * One piece of glass over a backdrop, as glass placed above it sees it. [surface] holds its
 * shadow and material, and [layer] holds those with its content on top.
 *
 * It also records where its corner sits ([origin]), its [outline], the area of the backdrop it
 * paints on ([bounds]) and the area it takes in ([view]), how its material changes what shows
 * through it ([shade]), and the glass it lies [within], outermost first. A pane that is part of
 * the backdrop's own content ([isContent]) carries the [clip] its parents put on it, null when
 * there is none.
 */
internal class GlassPane(val surface: GraphicsLayer, val layer: GraphicsLayer) {
    var isContent = false
    var clip by mutableStateOf<Rect?>(null)
    var origin by mutableStateOf(Offset.Zero)
    var outline by mutableStateOf(Rect.Zero)
    var bounds by mutableStateOf(Rect.Zero)
    var view by mutableStateOf(Rect.Zero)
    var shade by mutableStateOf(GlassShade.None)
    var within by mutableStateOf(emptyList<GlassPane>())
}
