package io.github.itshedimisawi.vitro

import androidx.compose.runtime.Immutable

/** Whether glass uses the light material, the dark one, or picks one itself. */
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
         * Follows the system theme, except over content at the far end: glass turns dark over
         * near-black content in a light theme, and light over near-white content in a dark one.
         */
        public val Automatic: GlassAppearance = GlassAppearance(0)

        /** Always the light material. */
        public val Light: GlassAppearance = GlassAppearance(1)

        /** Always the dark material. */
        public val Dark: GlassAppearance = GlassAppearance(2)
    }
}
