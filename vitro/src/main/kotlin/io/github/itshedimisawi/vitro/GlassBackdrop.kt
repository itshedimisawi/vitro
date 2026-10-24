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
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import io.github.itshedimisawi.vitro.internal.node.GlassBackdropElement
import io.github.itshedimisawi.vitro.internal.node.ScrollEdgeNode
import io.github.itshedimisawi.vitro.internal.render.GlassPane
import io.github.itshedimisawi.vitro.internal.tone.BackdropTone

/**
 * Handle to the content that glass refracts.
 *
 * Create one with [rememberGlassBackdrop], attach it to the content with [glassBackdrop] and pass
 * it to every [glassEffect] that should see that content.
 */
@Stable
public class GlassBackdrop internal constructor(internal val layer: GraphicsLayer) {
    internal var coordinates: LayoutCoordinates? = null

    // Glass drawn over this backdrop, lowest first, and those of it that are part of the content.
    internal val panes = mutableStateListOf<GlassPane>()
    internal val inside = mutableStateListOf<GlassPane>()
    internal var generation: Int by mutableIntStateOf(0)

    // Brightness of the content, kept up to date while any glass chooses its material from it.
    internal var tone: BackdropTone? by mutableStateOf(null)
    internal var toneWatchers: Int by mutableIntStateOf(0)

    // The scroll edge effect on this content, whose veils also soften the glass in it.
    internal var edge: ScrollEdgeNode? = null
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
 * What counts as content is everything drawn by the modifiers after this one and by the
 * element's children, so place a `background` after it: `Modifier.glassBackdrop(backdrop).background(page)`.
 * Glass only refracts what the content draws. Areas it leaves transparent show as clear and
 * are left out when automatic glass chooses its appearance.
 *
 * Glass may also be part of the content, for example a button that scrolls with the page. Such a
 * piece sees the rest of the content without itself and is drawn over all of it, whatever its
 * place in drawing order. It follows its own position and is cut off where a scrolling or
 * clipping parent would cut it off, and [scrollEdgeEffect] softens it along with the rest of the
 * content. It does not follow a `graphicsLayer` that scales, rotates or fades a parent.
 *
 * @param backdrop Handle that connects this content to the glass drawn over it.
 */
public fun Modifier.glassBackdrop(backdrop: GlassBackdrop): Modifier =
    this then GlassBackdropElement(backdrop)
