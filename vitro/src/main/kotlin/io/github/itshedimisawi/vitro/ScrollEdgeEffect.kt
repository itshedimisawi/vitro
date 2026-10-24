package io.github.itshedimisawi.vitro

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import io.github.itshedimisawi.vitro.internal.node.ScrollEdgeElement

/** How [scrollEdgeEffect] softens content under bars. */
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
         * Each edge picks a style from the content under it: [Shaded] under glass over darker
         * content, [Soft] over bright content and where there is no glass.
         */
        public val Automatic: ScrollEdgeEffectStyle = ScrollEdgeEffectStyle(0)

        /** Content fades into the page colour, so bars sit on a calm surface whatever scrolls past. */
        public val Soft: ScrollEdgeEffectStyle = ScrollEdgeEffectStyle(1)

        /** Content is darkened but keeps its colours. Suits colourful artwork on a light page. */
        public val Shaded: ScrollEdgeEffectStyle = ScrollEdgeEffectStyle(2)
    }
}

/**
 * Softens content where it scrolls under bars at the top and bottom, as on iOS.
 *
 * ```
 * LazyColumn(
 *     Modifier
 *         .glassBackdrop(backdrop)
 *         .scrollEdgeEffect(top = statusBarHeight, bottom = navigationBarHeight),
 * ) { … }
 * ```
 *
 * Place it after [glassBackdrop] on the same element. Glass bars over the content are found on
 * their own, and the effect covers them; [top] and [bottom] add room for things that are not
 * glass, such as the system bars.
 *
 * @param top Height at the top covered by something that is not glass.
 * @param bottom Height at the bottom covered by something that is not glass.
 * @param style How the content is softened.
 * @param color The page colour that [ScrollEdgeEffectStyle.Soft] fades into. Defaults to white in
 * a light theme and black in a dark one.
 * @param isTopHidden Turns the top edge off, for example while artwork should show in full under
 * a toolbar. Changes fade in and out.
 * @param isBottomHidden Turns the bottom edge off, in the same way.
 */
public fun Modifier.scrollEdgeEffect(
    top: Dp = Dp.Unspecified,
    bottom: Dp = Dp.Unspecified,
    style: ScrollEdgeEffectStyle = ScrollEdgeEffectStyle.Automatic,
    color: Color = Color.Unspecified,
    isTopHidden: Boolean = false,
    isBottomHidden: Boolean = false,
): Modifier = this then ScrollEdgeElement(top, bottom, style, color, isTopHidden, isBottomHidden)
