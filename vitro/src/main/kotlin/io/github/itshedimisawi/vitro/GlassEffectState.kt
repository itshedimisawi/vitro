package io.github.itshedimisawi.vitro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ScaleFactor
import io.github.itshedimisawi.vitro.internal.material.TintStrength
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The live state of one piece of glass: the material it shows and the touch on it.
 *
 * Pass it to [glassEffect] to read that state from the content or from other elements, for example
 * to colour a label with [contentColor]:
 *
 * ```
 * val state = rememberGlassEffectState()
 * Text(
 *     "Done",
 *     color = state.contentColor,
 *     modifier = Modifier.glassEffect(backdrop, state = state).padding(16.dp),
 * )
 * ```
 *
 * Use one state per piece of glass.
 */
@Stable
public class GlassEffectState internal constructor(appearance: GlassAppearance) {

    private val hold = Animatable(0f)
    private val settling = Animatable(Offset.Zero, Offset.VectorConverter)
    private var travel by mutableStateOf(Offset.Zero)
    private var origin = Offset.Unspecified
    private var extent by mutableStateOf(Size.Zero)
    private var density = 1f

    /**
     * The material the glass shows, [GlassAppearance.Light] or [GlassAppearance.Dark]. With
     * [GlassAppearance.Automatic] glass it follows the theme and the content behind the glass.
     */
    public var appearance: GlassAppearance by mutableStateOf(appearance)
        internal set

    /**
     * A legible colour for text and icons on the glass: white or black to suit [appearance], or
     * to contrast with a strong [tint][Glass.tint].
     */
    public val contentColor: Color
        get() {
            val tint = tint
            if (tint.isSpecified && tint.alpha * TintStrength >= StrongTint) {
                return if (tint.luminance() > BrightTint) Color.Black else Color.White
            }
            return if (appearance == GlassAppearance.Dark) Color.White else Color.Black
        }

    internal var tint: Color by mutableStateOf(Color.Unspecified)

    /** Whether a finger is down on [interactive][Glass.interactive] glass. */
    public var isHeld: Boolean by mutableStateOf(false)
        private set

    /** Where the finger is, in pixels relative to the glass. Keeps its last value after release. */
    public var position: Offset by mutableStateOf(Offset.Zero)
        private set

    /** How firmly the glass is pressed, from 0 at rest to 1 while held, overshooting slightly. */
    public val pressure: Float get() = hold.value

    /** How far the finger has moved since it went down, in pixels. Springs back after release. */
    public val drag: Offset get() = if (isHeld) travel else settling.value

    /**
     * How much the touch scales the glass on each axis, about its centre. The glass content follows
     * on its own; apply this and [offset] to other elements that should move with the glass.
     */
    public val scale: ScaleFactor
        get() {
            val reach = drag
            val distance = reach.getDistance()
            val swell = 1f + pressure * growth
            if (distance <= 0f || extent.maxDimension <= 0f) return ScaleFactor(swell, swell)
            val amount = (distance / density * PullStretch * sqrt(lightness)).coerceAtMost(MaxPull)
            val tug = amount * (reach.x * reach.x - reach.y * reach.y) / (distance * distance)
            return ScaleFactor(swell * (1f + tug), swell * (1f - tug))
        }

    /** How far, in pixels, the held glass has moved toward the finger. */
    public val offset: Offset get() = drag * (PullShift * lightness)

    /** Share of its longer side that the glass gains when fully pressed. */
    internal val growth: Float
        get() = if (extent.maxDimension > 0f) PressGrowth * density / extent.maxDimension else 0f

    // Small glass is easier to pull around than large glass.
    private val lightness: Float
        get() = if (extent.maxDimension > 0f) {
            (ReferenceSize * density / extent.maxDimension).coerceAtMost(1.5f)
        } else {
            1f
        }

    internal fun resize(size: Size, density: Float) {
        this.density = density
        extent = size
    }

    internal fun press(scope: CoroutineScope, at: Offset) {
        origin = at
        position = at
        travel = Offset.Zero
        isHeld = true
        scope.launch { hold.animateTo(1f, PressSpring) }
    }

    internal fun move(to: Offset) {
        position = to
        if (isHeld) travel = to - origin
    }

    internal fun release(scope: CoroutineScope) {
        if (!isHeld) return
        val reached = travel
        isHeld = false
        scope.launch { hold.animateTo(0f, ReleaseSpring) }
        scope.launch {
            settling.snapTo(reached)
            settling.animateTo(Offset.Zero, ReturnSpring)
        }
    }

    internal fun settle(scope: CoroutineScope) {
        isHeld = false
        travel = Offset.Zero
        scope.launch {
            hold.snapTo(0f)
            settling.snapTo(Offset.Zero)
        }
    }
}

// Pressing snaps up quickly; letting go rebounds past rest once.
private val PressSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 850f)
private val ReleaseSpring = spring<Float>(dampingRatio = 0.4f, stiffness = 300f)
private val ReturnSpring = spring<Offset>(dampingRatio = 0.4f, stiffness = 300f)

// A tint at least this strong sets the content colour: black on bright tints such as yellow or
// white, and white on the rest, as iOS labels its prominent glass.
private const val StrongTint = 0.5f
private const val BrightTint = 0.5f

// Sizes in dp.
private const val PressGrowth = 17f
private const val ReferenceSize = 44f
private const val PullShift = 0.036f
private const val PullStretch = 0.00033f
private const val MaxPull = 0.3f

/**
 * Creates and remembers a [GlassEffectState]. Until the glass first draws, its
 * [appearance][GlassEffectState.appearance] follows the system theme.
 */
@Composable
public fun rememberGlassEffectState(): GlassEffectState {
    val isDark = LocalGlassDarkTheme.current ?: isSystemInDarkTheme()
    return remember { GlassEffectState(if (isDark) GlassAppearance.Dark else GlassAppearance.Light) }
}
