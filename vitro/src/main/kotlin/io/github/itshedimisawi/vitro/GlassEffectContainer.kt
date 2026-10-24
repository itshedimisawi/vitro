package io.github.itshedimisawi.vitro

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.itshedimisawi.vitro.internal.node.GlassContainerElement

/**
 * Groups the glass inside this element so nearby pieces blend into one surface, and draws the
 * group at the cost of a single piece.
 *
 * ```
 * Row(Modifier.glassEffectContainer(spacing = 12.dp)) {
 *     Icon(…, Modifier.glassEffect(backdrop))
 *     Icon(…, Modifier.glassEffect(backdrop))
 * }
 * ```
 *
 * Use it for controls that sit together, such as a toolbar or a row of chips. With a [spacing] of
 * zero the pieces never blend and look as they do on their own, so the container only saves work.
 *
 * - Pieces blend only when they share a backdrop and a [Glass] that differs at most in tint,
 *   dimming, appearance, shadow opacity and interactivity. Other pieces are drawn separately.
 * - A `graphicsLayer` that fades or rotates a single piece does not apply to its glass.
 * - Blending needs Android 13. Earlier versions draw each piece separately.
 *
 * @param spacing How close two pieces must come before they start to blend.
 */
public fun Modifier.glassEffectContainer(spacing: Dp = 8.5.dp): Modifier =
    this then GlassContainerElement(spacing)
