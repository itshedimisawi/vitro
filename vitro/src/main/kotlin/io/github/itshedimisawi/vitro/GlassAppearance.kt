package io.github.itshedimisawi.vitro

/**
 * Whether glass renders for a light or a dark interface.
 *
 * The appearance changes how [Glass.brightness] is applied: light glass is veiled toward white,
 * while dark glass keeps colours close to what is behind it and only raises the darkest tones.
 */
public enum class GlassAppearance {

    /**
     * Follows the system dark theme, and gives way to the content behind the glass where that is
     * at the far end: glass over near-black content turns dark in a light interface, and glass
     * over near-white content turns light in a dark one.
     */
    Automatic,

    /** Always renders the light material. */
    Light,

    /** Always renders the dark material. */
    Dark,
}
