package io.github.itshedimisawi.vitro

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.itshedimisawi.vitro.internal.GlassContainerElement

/**
 * Lets the glass inside this element flow together. Pieces that use [glassEffect] anywhere below
 * it are drawn as one surface: where two of them come closer than [spacing] their outlines reach
 * for each other and join, with the rim, the refraction and the shadow running unbroken across
 * the join. Pieces further apart look as they do on their own.
 *
 * Use it around controls that sit next to each other or that move into one another, such as a
 * bar that splits into buttons.
 *
 * Only pieces of the same material join: they must share a backdrop and every property of their
 * [Glass] except [Glass.tint], [Glass.dimming], [Glass.appearance], [Glass.shadowOpacity] and
 * [Glass.isInteractive], which blend from one piece to the next. Other pieces in the container
 * are drawn alongside without joining. Joined pieces are drawn together where the first of them
 * stands, so content placed between them in drawing order ends up over their glass. A piece
 * placed inside the content of another piece stays separate.
 *
 * Joining needs Android 13. On earlier versions this modifier changes nothing.
 *
 * @param spacing Distance between two outlines below which they begin to join. At half of it the
 * bridge between them is plain to see.
 */
public fun Modifier.glassEffectContainer(spacing: Dp = 8.5.dp): Modifier =
    this then GlassContainerElement(spacing)
