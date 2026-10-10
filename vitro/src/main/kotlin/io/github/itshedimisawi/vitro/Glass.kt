package io.github.itshedimisawi.vitro

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Configuration of the glass material rendered by [glassEffect].
 *
 * Start from [Glass.Regular] or [Glass.Clear] and refine with [tint], [interactive], [dimmed] or
 * [copy]. Instances are immutable and safe to share.
 *
 * @property blurRadius How strongly content behind the glass is blurred.
 * @property refractionHeight Width of the curved rim, measured inward from the edge, where content
 * is bent. The area further inside stays flat and undistorted.
 * @property refractionAmount How far the rim displaces the content it bends. Zero disables lensing.
 * @property dispersion Colour separation along the rim, where 0 is none and around 0.1 is a strong
 * prism-like fringe.
 * @property saturation Colour intensity of content seen through the glass, where 1 leaves it
 * unchanged.
 * @property brightness How strongly the luminosity of content seen through the glass is adjusted
 * for legibility, from 0 to 1. Its effect depends on [appearance]. Negative values darken toward
 * black in either appearance.
 * @property appearance Whether the material renders for a light or a dark interface.
 * @property tint Colour blended into the material, with its alpha setting the strength.
 * [Color.Unspecified] leaves the glass untinted.
 * @property dimming Opacity of the dark layer placed behind the glass, from 0 to 1.
 * @property lightAngle Direction the light comes from, in degrees clockwise from straight up.
 * @property highlightIntensity Strength of the rim and specular highlights, where 0 removes them.
 * @property shadowRadius Distance over which the shadow cast beneath the glass fades out.
 * @property shadowOffset Distance the shadow is shifted downward.
 * @property shadowOpacity Darkness of the shadow at its densest, from 0 to 1.
 * @property isInteractive Whether the glass reacts to touch and to its own movement.
 */
@Immutable
public class Glass private constructor(
    public val blurRadius: Dp,
    public val refractionHeight: Dp,
    public val refractionAmount: Dp,
    public val dispersion: Float,
    public val saturation: Float,
    public val brightness: Float,
    public val appearance: GlassAppearance,
    public val tint: Color,
    public val dimming: Float,
    public val lightAngle: Float,
    public val highlightIntensity: Float,
    public val shadowRadius: Dp,
    public val shadowOffset: Dp,
    public val shadowOpacity: Float,
    public val isInteractive: Boolean,
) {

    /**
     * Returns a copy tinted with [color], used to give a control prominence.
     *
     * @param color Colour blended into the material; its alpha sets the strength.
     */
    public fun tint(color: Color): Glass = copy(tint = color)

    /**
     * Returns a copy that responds to touch with scale, a highlight under the finger and an
     * elastic stretch while it moves.
     *
     * @param isEnabled Whether the response is active.
     */
    public fun interactive(isEnabled: Boolean = true): Glass = copy(isInteractive = isEnabled)

    /**
     * Returns a copy with a dark layer behind the glass, for keeping content on [Clear] glass
     * legible over bright backgrounds.
     *
     * @param opacity Opacity of the dark layer, from 0 to 1.
     */
    public fun dimmed(opacity: Float = 0.35f): Glass = copy(dimming = opacity)

    /**
     * Returns a copy with the given properties replaced. See [Glass] for what each one controls.
     */
    public fun copy(
        blurRadius: Dp = this.blurRadius,
        refractionHeight: Dp = this.refractionHeight,
        refractionAmount: Dp = this.refractionAmount,
        dispersion: Float = this.dispersion,
        saturation: Float = this.saturation,
        brightness: Float = this.brightness,
        appearance: GlassAppearance = this.appearance,
        tint: Color = this.tint,
        dimming: Float = this.dimming,
        lightAngle: Float = this.lightAngle,
        highlightIntensity: Float = this.highlightIntensity,
        shadowRadius: Dp = this.shadowRadius,
        shadowOffset: Dp = this.shadowOffset,
        shadowOpacity: Float = this.shadowOpacity,
        isInteractive: Boolean = this.isInteractive,
    ): Glass = Glass(
        blurRadius = blurRadius,
        refractionHeight = refractionHeight,
        refractionAmount = refractionAmount,
        dispersion = dispersion,
        saturation = saturation,
        brightness = brightness,
        appearance = appearance,
        tint = tint,
        dimming = dimming,
        lightAngle = lightAngle,
        highlightIntensity = highlightIntensity,
        shadowRadius = shadowRadius,
        shadowOffset = shadowOffset,
        shadowOpacity = shadowOpacity,
        isInteractive = isInteractive,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Glass) return false
        return blurRadius == other.blurRadius &&
            refractionHeight == other.refractionHeight &&
            refractionAmount == other.refractionAmount &&
            dispersion == other.dispersion &&
            saturation == other.saturation &&
            brightness == other.brightness &&
            appearance == other.appearance &&
            tint == other.tint &&
            dimming == other.dimming &&
            lightAngle == other.lightAngle &&
            highlightIntensity == other.highlightIntensity &&
            shadowRadius == other.shadowRadius &&
            shadowOffset == other.shadowOffset &&
            shadowOpacity == other.shadowOpacity &&
            isInteractive == other.isInteractive
    }

    override fun hashCode(): Int {
        var result = blurRadius.hashCode()
        result = 31 * result + refractionHeight.hashCode()
        result = 31 * result + refractionAmount.hashCode()
        result = 31 * result + dispersion.hashCode()
        result = 31 * result + saturation.hashCode()
        result = 31 * result + brightness.hashCode()
        result = 31 * result + appearance.hashCode()
        result = 31 * result + tint.hashCode()
        result = 31 * result + dimming.hashCode()
        result = 31 * result + lightAngle.hashCode()
        result = 31 * result + highlightIntensity.hashCode()
        result = 31 * result + shadowRadius.hashCode()
        result = 31 * result + shadowOffset.hashCode()
        result = 31 * result + shadowOpacity.hashCode()
        result = 31 * result + isInteractive.hashCode()
        return result
    }

    override fun toString(): String =
        "Glass(blurRadius=$blurRadius, refractionHeight=$refractionHeight, " +
            "refractionAmount=$refractionAmount, dispersion=$dispersion, " +
            "saturation=$saturation, brightness=$brightness, appearance=$appearance, " +
            "tint=$tint, dimming=$dimming, " +
            "lightAngle=$lightAngle, highlightIntensity=$highlightIntensity, " +
            "shadowRadius=$shadowRadius, shadowOffset=$shadowOffset, " +
            "shadowOpacity=$shadowOpacity, isInteractive=$isInteractive)"

    public companion object {

        /**
         * The standard material. Blurs and adjusts the luminosity of what is behind it so text and
         * symbols placed on top stay legible over any content.
         */
        public val Regular: Glass = Glass(
            blurRadius = 12.dp,
            refractionHeight = 14.dp,
            refractionAmount = 22.dp,
            dispersion = 0.03f,
            saturation = 1.4f,
            brightness = 0.5f,
            appearance = GlassAppearance.Automatic,
            tint = Color.Unspecified,
            dimming = 0f,
            lightAngle = -45f,
            highlightIntensity = 1f,
            shadowRadius = 20.dp,
            shadowOffset = 6.dp,
            shadowOpacity = 0.06f,
            isInteractive = false,
        )

        /**
         * A highly translucent material that keeps what is behind it visible. Intended for
         * controls floating over photos and video; combine with [dimmed] over bright content.
         */
        public val Clear: Glass = Regular.copy(
            blurRadius = 4.dp,
            saturation = 1.1f,
            brightness = 0.1f,
        )
    }
}
