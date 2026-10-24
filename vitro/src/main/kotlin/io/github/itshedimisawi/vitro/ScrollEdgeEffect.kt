package io.github.itshedimisawi.vitro

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import io.github.itshedimisawi.vitro.internal.node.ScrollEdgeElement

/**
 * How content is softened where it runs under bars at the top or bottom of a screen.
 */
@Immutable
@JvmInline
public value class ScrollEdgeEffectStyle private constructor(private val value: Int) {

    override fun toString(): String = when (this) {
        Automatic -> "Automatic"
        Soft -> "Soft"
        Shaded -> "Shaded"
        else -> "Unknown"
    }

    public companion object {

        /**
         * Each edge picks its look from what is there. Under glass, content that is darker than a
         * pale page is [Shaded], and the edge eases to [Soft] when the content under the glass
         * turns bright. An edge with no glass at it, such as one that only clears the status bar,
         * is [Soft].
         */
        public val Automatic: ScrollEdgeEffectStyle = ScrollEdgeEffectStyle(0)

        /**
         * Content dissolves into the colour of the page: toward white on a light page, toward
         * black on a dark one. Bars floating over it then sit on a calm surface whatever scrolls
         * past.
         */
        public val Soft: ScrollEdgeEffectStyle = ScrollEdgeEffectStyle(1)

        /**
         * Content is darkened and keeps its colours. On a dark page this is the same as [Soft]; on
         * a light page it is a gentle shade that suits a screen full of saturated artwork.
         */
        public val Shaded: ScrollEdgeEffectStyle = ScrollEdgeEffectStyle(2)
    }
}

/**
 * Softens this element's content toward its top and bottom edges, where bars float over it.
 *
 * The effect is at full strength under the bars and eases out a short way past them, so content
 * fades as it approaches a bar instead of meeting its edge. Place it after [glassBackdrop] on
 * the same element. The glass over the content then takes the softened content in, and the
 * effect finds the bars by itself: glass on that backdrop that sits at the top or the bottom of
 * the content, with a gap to the edge or to other glass there narrower than the effect's own
 * reach, and that does not run past the middle of the content.
 *
 * ```
 * Modifier.glassBackdrop(backdrop).scrollEdgeEffect(top = statusBarHeight)
 * ```
 *
 * @param top Height covered at the top by something that is not glass, such as the status bar.
 * Glass found at the top adds to it. Unspecified means only glass counts.
 * @param bottom Height covered at the bottom by something that is not glass, measured from the
 * bottom edge. Glass found at the bottom adds to it. Unspecified means only glass counts.
 * @param style Whether content dissolves into the page or is shaded, or [each edge chooses]
 * [ScrollEdgeEffectStyle.Automatic].
 * @param color Colour of the page, which [ScrollEdgeEffectStyle.Soft] dissolves content into.
 * Unspecified means white in a light interface and black in a dark one.
 * @param isTopHidden Whether the top edge is left unsoftened, for example while artwork meant to
 * show in full sits under the bars. The edge fades out and back in when this changes.
 * @param isBottomHidden Whether the bottom edge is left unsoftened, in the same way.
 */
public fun Modifier.scrollEdgeEffect(
    top: Dp = Dp.Unspecified,
    bottom: Dp = Dp.Unspecified,
    style: ScrollEdgeEffectStyle = ScrollEdgeEffectStyle.Automatic,
    color: Color = Color.Unspecified,
    isTopHidden: Boolean = false,
    isBottomHidden: Boolean = false,
): Modifier = this then ScrollEdgeElement(top, bottom, style, color, isTopHidden, isBottomHidden)
