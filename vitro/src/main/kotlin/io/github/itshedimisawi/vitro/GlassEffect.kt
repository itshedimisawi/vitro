package io.github.itshedimisawi.vitro

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.Modifier
import io.github.itshedimisawi.vitro.internal.node.GlassEffectElement

/**
 * Draws Liquid Glass behind this element's content, showing the content of [backdrop] through it.
 *
 * ```
 * Icon(
 *     Icons.Rounded.Add,
 *     contentDescription = "Add",
 *     modifier = Modifier.size(44.dp).glassEffect(backdrop, Glass.Regular.interactive()).padding(10.dp),
 * )
 * ```
 *
 * Apply it after the modifiers that size the element, so the glass covers its final bounds.
 * Android 13 and later get the full effect; earlier versions get a close approximation.
 *
 * @param backdrop The content seen through the glass, marked with [glassBackdrop].
 * @param glass The material, such as [Glass.Regular] or [Glass.Clear].
 * @param shape The outline of the glass. Defaults to a capsule. Corners are always round, so a
 * `CutCornerShape` is drawn with rounded corners of the same size.
 * @param state Exposes the material and the touch of this glass, see [GlassEffectState].
 * @param interactionSource Presses from this source, such as a keyboard or D-pad click, also
 * press [interactive][Glass.interactive] glass. Share it with the element's `clickable`:
 * ```
 * val source = remember { MutableInteractionSource() }
 * Modifier
 *     .glassEffect(backdrop, Glass.Regular.interactive(), interactionSource = source)
 *     .clickable(source, indication = null) { … }
 * ```
 */
public fun Modifier.glassEffect(
    backdrop: GlassBackdrop,
    glass: Glass = Glass.Regular,
    shape: CornerBasedShape = CircleShape,
    state: GlassEffectState? = null,
    interactionSource: InteractionSource? = null,
): Modifier = this then GlassEffectElement(backdrop, glass, shape, state, interactionSource)
