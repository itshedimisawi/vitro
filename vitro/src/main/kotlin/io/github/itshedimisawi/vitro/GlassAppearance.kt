package io.github.itshedimisawi.vitro

import androidx.compose.runtime.Immutable

/**
 * Whether glass renders for a light or a dark interface.
 *
 * The appearance changes how [Glass.brightness] is applied: light glass is veiled toward white,
 * while dark glass keeps colours close to what is behind it and only raises the darkest tones.
 */
@Immutable
@JvmInline
public value class GlassAppearance private constructor(private val value: Int) {

    override fun toString(): String = when (this) {
        Automatic -> "Automatic"
        Light -> "Light"
        Dark -> "Dark"
        else -> "Unknown"
    }

    public companion object {

        /**
         * Follows the system dark theme, and gives way to the content behind the glass where that
         * is at the far end: glass over near-black content turns dark in a light interface, and
         * glass over near-white content turns light in a dark one.
         */
        public val Automatic: GlassAppearance = GlassAppearance(0)

        /** Always renders the light material. */
        public val Light: GlassAppearance = GlassAppearance(1)

        /** Always renders the dark material. */
        public val Dark: GlassAppearance = GlassAppearance(2)
    }
}
