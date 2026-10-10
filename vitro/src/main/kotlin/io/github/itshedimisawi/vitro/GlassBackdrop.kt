package io.github.itshedimisawi.vitro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
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
    internal var generation: Int by mutableIntStateOf(0)
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
 * typically as siblings layered on top, since glass cannot refract itself.
 *
 * @param backdrop Handle that connects this content to the glass drawn over it.
 */
public fun Modifier.glassBackdrop(backdrop: GlassBackdrop): Modifier =
    this then GlassBackdropElement(backdrop)
