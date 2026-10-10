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
    internal var generation: Int by mutableIntStateOf(0)

    // Brightness of the content, kept up to date while any glass chooses its material from it.
    internal var tone: BackdropTone? by mutableStateOf(null)
    internal var toneWatchers: Int by mutableIntStateOf(0)
}

/**
 * One piece of glass over a backdrop. [surface] holds its shadow and material, and [layer] holds
 * those with its content on top. It also records where its corner sits, the area of the backdrop
 * it paints on (shadow included), the area it takes in to draw itself, and the glass it lies
 * [within]: the pieces whose content it is part of, outermost first.
 */
internal class GlassPane(val surface: GraphicsLayer, val layer: GraphicsLayer) {
    var origin by mutableStateOf(Offset.Zero)
    var bounds by mutableStateOf(Rect.Zero)
    var view by mutableStateOf(Rect.Zero)
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
 * The content is drawn as usual. Elements using [glassEffect] must be placed outside of it,
 * typically as siblings layered on top, since glass cannot refract itself. Glass placed over
 * other glass on the same backdrop does see it: each piece shows through the ones added after it.
 *
 * @param backdrop Handle that connects this content to the glass drawn over it.
 */
public fun Modifier.glassBackdrop(backdrop: GlassBackdrop): Modifier =
    this then GlassBackdropElement(backdrop)
