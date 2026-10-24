package io.github.itshedimisawi.vitro.internal.node

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.internal.render.GlassPane

/**
 * Adds [pane] to the glass over this backdrop, on top of everything, or just under [before] when
 * that is given. A pane that is part of the content ([isContent]) is also drawn by the backdrop.
 */
internal fun GlassBackdrop.addPane(pane: GlassPane, isContent: Boolean, before: GlassPane? = null) {
    pane.isContent = isContent
    panes.insertBefore(pane, before)
    if (isContent) inside.insertBefore(pane, before)
}

internal fun GlassBackdrop.removePane(pane: GlassPane) {
    panes.remove(pane)
    inside.remove(pane)
}

/**
 * The glass under [pane] that paints inside its view: pieces added before it and pieces it is
 * inside of, but never one placed inside it, which is part of its own content. Derived, so a
 * reader is invalidated when glass in its view changes and not whenever any glass moves.
 */
internal fun GlassBackdrop.glassBeneath(pane: GlassPane): State<List<GlassPane>> = derivedStateOf {
    val index = panes.indexOf(pane)
    val within = pane.within
    panes.filterIndexed { position, other ->
        other !== pane &&
            (other in within || position < index && pane !in other.within) &&
            other.bounds.overlaps(pane.view)
    }
}

private fun MutableList<GlassPane>.insertBefore(pane: GlassPane, before: GlassPane?) {
    val index = if (before != null) indexOf(before) else -1
    if (index >= 0) add(index, pane) else add(pane)
}
