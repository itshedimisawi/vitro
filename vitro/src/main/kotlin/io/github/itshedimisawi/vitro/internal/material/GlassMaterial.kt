package io.github.itshedimisawi.vitro.internal.material

import androidx.compose.ui.unit.Dp
import io.github.itshedimisawi.vitro.Glass

/**
 * What pieces of glass must have in common to be drawn as one. The rest of [Glass] may differ
 * between them and blends from one piece to the next.
 */
internal data class GlassMaterial(
    val blurRadius: Dp,
    val refractionHeight: Dp,
    val refractionAmount: Dp,
    val dispersion: Float,
    val magnification: Float,
    val saturation: Float,
    val brightness: Float,
    val lightAngle: Float,
    val highlightIntensity: Float,
    val shadowRadius: Dp,
    val shadowOffset: Dp,
)

internal fun Glass.material(): GlassMaterial = GlassMaterial(
    blurRadius = blurRadius,
    refractionHeight = refractionHeight,
    refractionAmount = refractionAmount,
    dispersion = dispersion,
    magnification = magnification,
    saturation = saturation,
    brightness = brightness,
    lightAngle = lightAngle,
    highlightIntensity = highlightIntensity,
    shadowRadius = shadowRadius,
    shadowOffset = shadowOffset,
)
