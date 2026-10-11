package io.github.itshedimisawi.vitro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import io.github.itshedimisawi.vitro.internal.BackdropTone
import io.github.itshedimisawi.vitro.internal.GlassBackdropElement
import io.github.itshedimisawi.vitro.internal.GlassShade

/**
 * Handle to the content that glass refracts.
 *
 * Create one with [rememberGlassBackdrop], attach it to the content with [glassBackdrop] and pass
 * it to every [glassEffect] that should see that content.
 */
@Stable
public class GlassBackdrop internal constructor(internal val layer: GraphicsLayer) {
    internal var coordinates: LayoutCoordinates? = null

    // Glass drawn over this backdrop, in the order it was added. Each piece shows through the
    // ones added after it, and through any placed inside its own content.
    internal val panes = mutableStateListOf<GlassPane>()

    // Those of the panes that are part of the content itself. They are left out of what the
    // backdrop records, so they never see themselves, and are drawn over the content afterwards.
    internal val inside = mutableStateListOf<GlassPane>()
    internal var generation: Int by mutableIntStateOf(0)

    // Brightness of the content, kept up to date while any glass chooses its material from it.
    internal var tone: BackdropTone? by mutableStateOf(null)
    internal var toneWatchers: Int by mutableIntStateOf(0)
}

/**
 * One piece of glass over a backdrop. [surface] holds its shadow and material, and [layer] holds
 * those with its content on top. It also records where its corner sits, its [outline] and the
 * area of the backdrop it paints on with its shadow ([bounds]), the area it takes in to draw
 * itself, how its material changes the brightness of what shows through it, and the glass it
 * lies [within]: the pieces whose content it is part of, outermost first. A pane that is part of
 * the backdrop's own content ([isContent]) also carries the [clip] its surroundings put on it,
 * null when there is none.
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

/**
 * Creates and remembers a [GlassBackdrop] for the current composition.
 */
@Composable
public fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

/**
 * Marks this element's content as what glass sharing [backdrop] sees behind it.
 *
 * The content is drawn as usual. Glass is typically placed outside of it, as siblings layered on
 * top. Glass placed over other glass on the same backdrop sees it: each piece shows through the
 * ones added after it.
 *
 * Glass may also be part of the content, for example a button that scrolls with the page. Such a
 * piece sees the rest of the content without itself and is drawn over all of it, whatever its
 * place in drawing order. It follows its own position and is cut off where a scrolling or
 * clipping parent would cut it off, but it does not follow a `graphicsLayer` that scales, rotates
 * or fades a parent, and [scrollEdgeEffect] does not soften it.
 *
 * @param backdrop Handle that connects this content to the glass drawn over it.
 */
public fun Modifier.glassBackdrop(backdrop: GlassBackdrop): Modifier =
    this then GlassBackdropElement(backdrop)
