package io.github.itshedimisawi.vitro

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/**
 * Whether glass uses the light material, the dark one, or picks one itself.
 *
 * Automatic glass follows the system dark theme. An app with its own theme setting should
 * provide [LocalGlassDarkTheme].
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

/**
 * Whether the app is in its dark theme, for glass with [GlassAppearance.Automatic] and for
 * [scrollEdgeEffect]. Null, the default, follows the system setting.
 *
 * ```
 * CompositionLocalProvider(LocalGlassDarkTheme provides isAppInDarkTheme) { … }
 * ```
 */
public val LocalGlassDarkTheme: ProvidableCompositionLocal<Boolean?> = compositionLocalOf { null }
