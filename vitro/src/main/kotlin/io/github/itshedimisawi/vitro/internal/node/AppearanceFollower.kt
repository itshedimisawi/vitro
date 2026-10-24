package io.github.itshedimisawi.vitro.internal.node

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshotFlow
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassAppearance
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.internal.render.GlassPane
import io.github.itshedimisawi.vitro.internal.tone.darknessOver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Keeps the material of one piece of glass in step with the appearance asked for and, when that
 * is automatic, with what lies under it: the backdrop as seen through the glass beneath. A change
 * brought by the content is eased in; the rest is immediate.
 */
internal class AppearanceFollower(var onChanged: ((GlassAppearance) -> Unit)?) {

    private val darkness = Animatable(0f)
    private var following: Job? = null
    private var watched: GlassBackdrop? = null

    /** 0 for the light material, 1 for the dark one, in between while it changes over. */
    val value: Float get() = darkness.value

    /** Keeps the tone of [backdrop] read while glass needs it; null stops reading for this glass. */
    fun watch(backdrop: GlassBackdrop?) {
        if (backdrop === watched) return
        watched?.let { it.toneWatchers -= 1 }
        backdrop?.let { it.toneWatchers += 1 }
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
            var wasSampled = false
            snapshotFlow {
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
            }.collectLatest { (wanted, isSampled) ->
                val appearance = if (wanted >= 0.5f) GlassAppearance.Dark else GlassAppearance.Light
                if (appearance != shown) {
                    shown = appearance
                    onChanged?.invoke(appearance)
                }
                val isEased = isSampled && wasSampled
                wasSampled = isSampled
                if (isEased) darkness.animateTo(wanted, ToneSpring) else darkness.snapTo(wanted)
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
