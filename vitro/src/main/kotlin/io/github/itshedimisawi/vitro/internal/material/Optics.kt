package io.github.itshedimisawi.vitro.internal.material

import androidx.compose.ui.geometry.Offset
import io.github.itshedimisawi.vitro.Glass
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

// Calibrated constants of the material. The shader, the fallback colour matrix and the tone
// sampling all read them from here, so the three paths stay in step.
internal const val LightVeil = 1.32f
internal const val LightFade = 1.34f
internal const val LightWash = 0.58f
internal const val DarkGain = 0.08f
internal const val DarkVivid = 0.18f
internal const val DarkLift = 0.19f
internal const val DarkShade = 1f
internal const val DarkKnee = 0.33f
internal const val DarkShadow = 0.15f
internal const val DarkRimGain = 0.35f
internal const val DarkRimLift = 0.12f
internal const val LightRim = 0.91f
internal const val LightRimVivid = 0.2f
internal const val DarkRimVivid = 0.58f
internal const val RimFloor = 0.11f
internal const val PressGlow = 0.05f
internal const val PressGlowPerGrowth = 0.35f

/** Share of a tint colour's alpha that is mixed into the material. */
internal const val TintStrength = 0.85f

/** The platform blur is a Gaussian of this deviation per radius, cut off [BlurReach] deviations out. */
internal const val BlurSigmaPerRadius = 0.57735f
internal const val BlurSigmaBase = 0.5f
internal const val BlurReach = 3f

internal const val MinMagnification = 0.5f

internal val LumaWeights = floatArrayOf(0.2126f, 0.7152f, 0.0722f)

/** Standard deviation of the platform blur for a blur radius in pixels. */
internal fun blurSigma(radius: Float): Float = radius * BlurSigmaPerRadius + BlurSigmaBase

/**
 * What the dark material adds to content of the given brightness, for a full veil: a lift near
 * black and a hold on bright content. The shader applies the same curve.
 */
internal fun darkBend(brightness: Float): Float =
    DarkLift * (1f - brightness).pow(16) - DarkShade * max(brightness - DarkKnee, 0f).pow(1.5f)

/** The size content is shown at through this glass, which never shrinks it below half. */
internal fun Glass.shownScale(): Float = magnification.coerceAtLeast(MinMagnification)

/** Unit vector the light comes from, for an angle in degrees clockwise from straight up. */
internal fun lightDirection(angle: Float): Offset {
    val radians = Math.toRadians(angle.toDouble())
    return Offset(sin(radians).toFloat(), -cos(radians).toFloat())
}
