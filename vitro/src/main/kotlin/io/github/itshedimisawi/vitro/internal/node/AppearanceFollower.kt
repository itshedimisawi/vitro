package io.github.itshedimisawi.vitro.internal.node

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.State
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassAppearance
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.internal.render.GlassPane
import io.github.itshedimisawi.vitro.internal.tone.darknessOver
import io.github.itshedimisawi.vitro.internal.tone.followReadings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Keeps one piece of glass on the material its [Glass.appearance] asks for. Automatic glass follows
 * the content under it, easing between materials, and reports each switch to [onChanged].
 */
internal class AppearanceFollower(private val onChanged: (GlassAppearance) -> Unit) {

    private val darkness = Animatable(0f)
    private var following: Job? = null
    private var watched: GlassBackdrop? = null

    /** 0 for the light material, 1 for the dark one, in between while it changes over. */
    val value: Float get() = darkness.value

    /** Keeps the tone of [backdrop] read while this glass needs it; null stops. */
    fun watch(backdrop: GlassBackdrop?) {
        if (backdrop === watched) return
        watched?.unwatchTone()
        backdrop?.watchTone()
        watched = backdrop
    }

    fun follow(
        scope: CoroutineScope,
        backdrop: GlassBackdrop,
        pane: GlassPane,
        beneath: State<List<GlassPane>>?,
        glass: () -> Glass,
        isNight: () -> Boolean,
    ) {
        following?.cancel()
        following = scope.launch {
            var shown: GlassAppearance? = null
            val report = { level: Float ->
                val appearance = if (level >= 0.5f) GlassAppearance.Dark else GlassAppearance.Light
                if (appearance != shown) {
                    shown = appearance
                    onChanged(appearance)
                }
            }
            darkness.followReadings(ToneSpring, onTarget = report) {
                val appearance = glass().appearance
                val luminance = when (appearance) {
                    GlassAppearance.Automatic ->
                        backdrop.tone?.luminance(pane.outline, beneath?.value.orEmpty()) ?: Float.NaN
                    else -> Float.NaN
                }
                val wanted = when {
                    appearance == GlassAppearance.Light -> 0f
                    appearance == GlassAppearance.Dark -> 1f
                    luminance.isNaN() -> if (isNight()) 1f else 0f
                    else -> darknessOver(luminance, isNight())
                }
                wanted to !luminance.isNaN()
            }
        }
    }

    fun stop() {
        following?.cancel()
        following = null
        watch(null)
    }

    private companion object {
        val ToneSpring = spring<Float>(dampingRatio = 1f, stiffness = 120f)
    }
}
