package io.github.itshedimisawi.vitro

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The look of the glass drawn by [glassEffect].
 *
 * Start from [Glass.Regular] or [Glass.Clear] and adjust it with [tint], [interactive], [dimmed]
 * or [copy]. Use [Glass.Identity] to turn the glass off without changing the modifier chain.
 *
 * @property blurRadius How much the content behind the glass is blurred.
 * @property refractionHeight How far the rim reaches in from the edge. Content is bent within it.
 * @property refractionAmount How far the rim bends content. 0 turns bending off.
 * @property dispersion Colour fringing along the rim, from 0 for none to about 0.1 for strong.
 * @property magnification Zoom of the content seen through the glass. 1 is true size, below 1
 * shows a wider area, above 1 enlarges. Values below 0.5 are treated as 0.5.
 * @property saturation Colour intensity of the content seen through the glass. 1 leaves it as is.
 * @property brightness How strongly the glass adjusts content to keep what's on it legible, from
 * -1 to 1. Positive values veil it toward the material, negative values darken it.
 * @property appearance Whether the glass uses the light or the dark material, or picks one itself.
 * @property tint Colour mixed into the glass; its alpha sets the strength. Unspecified for none.
 * @property dimming Opacity of a dark layer behind the glass, from 0 to 1.
 * @property lightAngle Where the light comes from, in degrees clockwise from the top.
 * @property highlightIntensity Strength of the rim highlights. 0 removes them.
 * @property shadowRadius How far the shadow spreads.
 * @property shadowOffset How far the shadow falls below the glass.
 * @property shadowOpacity Darkness of the shadow, from 0 to 1.
 * @property isInteractive Whether the glass reacts to touch and movement.
 */
@Immutable
public class Glass private constructor(
    public val blurRadius: Dp,
    public val refractionHeight: Dp,
    public val refractionAmount: Dp,
    public val dispersion: Float,
    public val magnification: Float,
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
    internal val isIdentity: Boolean = false,
) {

    /** Returns a copy tinted with [color], to make a control stand out. Its alpha sets the strength. */
    public fun tint(color: Color): Glass = copy(tint = color)

    /** Returns a copy that swells, glows and stretches under the finger, when [isEnabled]. */
    public fun interactive(isEnabled: Boolean = true): Glass = copy(isInteractive = isEnabled)

    /**
     * Returns a copy darkened by [opacity], from 0 to 1, to keep content on [Clear] glass legible
     * over bright content.
     */
    public fun dimmed(opacity: Float = 0.35f): Glass = copy(dimming = opacity)

    /** Returns a copy with the given properties replaced. */
    public fun copy(
        blurRadius: Dp = this.blurRadius,
        refractionHeight: Dp = this.refractionHeight,
        refractionAmount: Dp = this.refractionAmount,
        dispersion: Float = this.dispersion,
        magnification: Float = this.magnification,
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
        magnification = magnification,
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
        isIdentity = isIdentity,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Glass) return false
        return blurRadius == other.blurRadius &&
            refractionHeight == other.refractionHeight &&
            refractionAmount == other.refractionAmount &&
            dispersion == other.dispersion &&
            magnification == other.magnification &&
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
            isInteractive == other.isInteractive &&
            isIdentity == other.isIdentity
    }

    override fun hashCode(): Int {
        var result = blurRadius.hashCode()
        result = 31 * result + refractionHeight.hashCode()
        result = 31 * result + refractionAmount.hashCode()
        result = 31 * result + dispersion.hashCode()
        result = 31 * result + magnification.hashCode()
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
        result = 31 * result + isIdentity.hashCode()
        return result
    }

    override fun toString(): String = if (isIdentity) "Glass.Identity" else
        "Glass(blurRadius=$blurRadius, refractionHeight=$refractionHeight, " +
            "refractionAmount=$refractionAmount, dispersion=$dispersion, " +
            "magnification=$magnification, " +
            "saturation=$saturation, brightness=$brightness, appearance=$appearance, " +
            "tint=$tint, dimming=$dimming, " +
            "lightAngle=$lightAngle, highlightIntensity=$highlightIntensity, " +
            "shadowRadius=$shadowRadius, shadowOffset=$shadowOffset, " +
            "shadowOpacity=$shadowOpacity, isInteractive=$isInteractive)"

    public companion object {

        /** The standard glass, for controls and bars. Content on it stays legible over anything. */
        public val Regular: Glass = Glass(
            blurRadius = 2.5.dp,
            refractionHeight = 12.dp,
            refractionAmount = 26.dp,
            dispersion = 0f,
            magnification = 1f,
            saturation = 1.1f,
            brightness = 0.5f,
            appearance = GlassAppearance.Automatic,
            tint = Color.Unspecified,
            dimming = 0f,
            lightAngle = -45f,
            highlightIntensity = 1f,
            shadowRadius = 30.dp,
            shadowOffset = 8.dp,
            shadowOpacity = 0.16f,
            isInteractive = false,
        )

        /**
         * Very see-through glass, for controls over photos and video. Pair it with [dimmed] over
         * bright content.
         */
        public val Clear: Glass = Regular.copy(
            blurRadius = 2.dp,
            saturation = 1f,
            brightness = 0.1f,
        )

        /** No glass at all: content draws as if [glassEffect] were not applied, whatever [copy] changes. */
        public val Identity: Glass = with(Regular) {
            Glass(
                blurRadius, refractionHeight, refractionAmount, dispersion, magnification, saturation,
                brightness, appearance, tint, dimming, lightAngle, highlightIntensity, shadowRadius,
                shadowOffset, shadowOpacity, isInteractive, isIdentity = true,
            )
        }
    }
}
