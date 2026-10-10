package io.github.itshedimisawi.vitro

/**
 * Whether glass renders for a light or a dark interface.
 *
 * The appearance changes how [Glass.brightness] is applied: light glass is veiled toward white,
 * while dark glass keeps colours close to what is behind it and only raises the darkest tones.
 */
public enum class GlassAppearance {

    /** Follows the system dark theme setting. */
    Automatic,

    /** Always renders the light material. */
    Light,

    /** Always renders the dark material. */
    Dark,
}
