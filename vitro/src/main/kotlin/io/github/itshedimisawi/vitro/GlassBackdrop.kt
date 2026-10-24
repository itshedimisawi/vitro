package io.github.itshedimisawi.vitro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
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
 * The content that glass shows through it.
 *
 * Create one with [rememberGlassBackdrop], mark the content with [glassBackdrop], and pass it to
 * each [glassEffect] drawn over that content.
 */
@Stable
public class GlassBackdrop internal constructor(internal val layer: GraphicsLayer) {
    internal var coordinates: LayoutCoordinates? = null

    // Glass over this backdrop, lowest first, and the part of it that sits inside the content.
    private val _panes = mutableStateListOf<GlassPane>()
    private val _inside = mutableStateListOf<GlassPane>()
    internal val panes: List<GlassPane> get() = _panes
    internal val inside: List<GlassPane> get() = _inside
    internal var generation: Int by mutableIntStateOf(0)

    // The brightness of the content, read only while some glass picks its material from it.
    internal var tone: BackdropTone? by mutableStateOf(null)
    internal val isToneWatched: Boolean get() = toneWatchers > 0
    private var toneWatchers by mutableIntStateOf(0)

    // The scroll edge effect on this content, which also softens the glass inside it.
    internal var edge: ScrollEdgeNode? = null

    internal fun watchTone() {
        toneWatchers++
    }

    internal fun unwatchTone() {
        toneWatchers--
    }

    /** Adds [pane] on top of the other glass, or just under [before]. */
    internal fun addPane(pane: GlassPane, isContent: Boolean, before: GlassPane? = null) {
        pane.isContent = isContent
        _panes.insertBefore(pane, before)
        if (isContent) _inside.insertBefore(pane, before)
    }

    internal fun removePane(pane: GlassPane) {
        _panes.remove(pane)
        _inside.remove(pane)
    }

    /**
     * The glass that [pane] sees in its view: pieces below it and pieces it sits inside, never one
     * inside it. Derived, so readers only update when that set changes.
     */
    internal fun glassBeneath(pane: GlassPane): State<List<GlassPane>> = derivedStateOf {
        val index = _panes.indexOf(pane)
        val within = pane.within
        _panes.filterIndexed { position, other ->
            other !== pane &&
                (other in within || position < index && pane !in other.within) &&
                other.bounds.overlaps(pane.view)
        }
    }

    private fun MutableList<GlassPane>.insertBefore(pane: GlassPane, before: GlassPane?) {
        val index = if (before != null) indexOf(before) else -1
        if (index >= 0) add(index, pane) else add(pane)
    }
}

/** Creates and remembers a [GlassBackdrop]. */
@Composable
public fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

/**
 * Marks this element's content as what the glass using [backdrop] shows through it.
 *
 * ```
 * Box {
 *     Image(…, Modifier.fillMaxSize().glassBackdrop(backdrop))
 *     Button(…, Modifier.glassEffect(backdrop))
 * }
 * ```
 *
 * The content is everything drawn after this modifier, so put a `background` after it, not
 * before. Glass usually sits on top of the content as a sibling, but it can also be inside it,
 * such as a button that scrolls with the page.
 *
 * Glass inside the content does not follow a parent `graphicsLayer` that scales, rotates or fades.
 *
 * @param backdrop The backdrop that glass drawn over this content refers to.
 */
public fun Modifier.glassBackdrop(backdrop: GlassBackdrop): Modifier =
    this then GlassBackdropElement(backdrop)
