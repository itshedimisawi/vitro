package io.github.itshedimisawi.vitro

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import io.github.itshedimisawi.vitro.internal.ScrollEdgeElement

/**
 * How content is softened where it runs under bars at the top or bottom of a screen.
 */
public enum class ScrollEdgeEffectStyle {

    /**
     * Content dissolves into the colour of the page: toward white on a light page, toward black
     * on a dark one. Bars floating over it then sit on a calm surface whatever scrolls past.
     */
    Soft,

    /**
     * Content is darkened and keeps its colours. On a dark page this is the same as [Soft]; on a
     * light page it is a gentle shade that suits a screen full of saturated artwork.
     */
    Shaded,
}

/**
 * Softens this element's content toward its top and bottom edges, where bars float over it.
 *
 * The effect is at full strength under the bars and eases out a short way past them, so content
 * fades as it approaches a bar instead of meeting its edge. Place it after [glassBackdrop] on
 * the same element, so the glass over the content takes the softened content in:
 *
 * ```
 * Modifier.glassBackdrop(backdrop).scrollEdgeEffect(top = statusBarHeight, bottom = barsHeight)
 * ```
 *
 * @param top Height covered at the top, such as the status bar and any bar under it. Leave it
 * unspecified for no effect at the top.
 * @param bottom Height covered at the bottom, measured from the bottom edge to the top of the
 * highest bar. Leave it unspecified for no effect at the bottom.
 * @param style Whether content dissolves into the page or is shaded.
 * @param color Colour of the page, which [ScrollEdgeEffectStyle.Soft] dissolves content into.
 * Unspecified means white in a light interface and black in a dark one.
 */
public fun Modifier.scrollEdgeEffect(
    top: Dp = Dp.Unspecified,
    bottom: Dp = Dp.Unspecified,
    style: ScrollEdgeEffectStyle = ScrollEdgeEffectStyle.Soft,
    color: Color = Color.Unspecified,
): Modifier = this then ScrollEdgeElement(top, bottom, style, color)
