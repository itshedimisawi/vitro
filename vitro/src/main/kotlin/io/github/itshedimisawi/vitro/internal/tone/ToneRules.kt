package io.github.itshedimisawi.vitro.internal.tone

/**
 * How dark glass should be over content of [luminance], from 0 (light) to 1 (dark). The theme
 * decides, except over near-black content in a light theme or near-white content in a dark one.
 */
internal fun darknessOver(luminance: Float, isNight: Boolean): Float {
    val start = if (isNight) NightFlipStart else DayFlipStart
    val end = if (isNight) NightFlipEnd else DayFlipEnd
    return 1f - ((luminance - start) / (end - start)).coerceIn(0f, 1f)
}

/**
 * How far content of [luminance] under bars is shaded instead of faded into the page, from 0 to 1.
 * Bars rest on a shade unless the content is nearly as bright as a white page.
 */
internal fun shadeOver(luminance: Float): Float =
    1f - ((luminance - ShadeFull) / (ShadeNone - ShadeFull)).coerceIn(0f, 1f)

internal const val ShadeFull = 0.67f
internal const val ShadeNone = 0.74f
internal const val DayFlipStart = 0.0263f
internal const val DayFlipEnd = 0.036f
internal const val NightFlipStart = 0.681f
internal const val NightFlipEnd = 0.793f
