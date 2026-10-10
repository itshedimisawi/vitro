package io.github.itshedimisawi.vitro

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.Modifier
import io.github.itshedimisawi.vitro.internal.GlassEffectElement

/**
 * Draws a glass material behind this element's content, refracting whatever [backdrop] captures.
 *
 * Apply it after modifiers that size and pad the element, so the glass covers the final bounds.
 * On Android 13 and later the material is rendered with a shader; earlier versions use a blurred
 * approximation with the same shape, lighting and interaction.
 *
 * @param backdrop Content seen through the glass, attached elsewhere with [glassBackdrop].
 * @param glass Material configuration, such as [Glass.Regular] or [Glass.Clear].
 * @param shape Outline of the glass. Defaults to a capsule.
 * @param interaction Receives the touch on this glass when it is [interactive][Glass.interactive],
 * so other elements can follow it. Leave it out when nothing else needs to know.
 */
public fun Modifier.glassEffect(
    backdrop: GlassBackdrop,
    glass: Glass = Glass.Regular,
    shape: CornerBasedShape = CircleShape,
    interaction: GlassInteractionState? = null,
): Modifier = this then GlassEffectElement(backdrop, glass, shape, interaction)
