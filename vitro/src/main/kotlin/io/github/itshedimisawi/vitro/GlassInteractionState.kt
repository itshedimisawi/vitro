package io.github.itshedimisawi.vitro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ScaleFactor
import kotlinx.coroutines.CoroutineScope
import kotlin.math.sqrt
import kotlinx.coroutines.launch

/**
 * The touch on a piece of interactive glass: whether it is held, how firmly, and where the finger
 * is.
 *
 * Interactive glass keeps one of these itself. Create your own with
 * [rememberGlassInteractionState] and pass it to [glassEffect] when other elements should react to
 * the same touch, such as a selection that swells and follows the finger while its bar is held.
 * Use one state per touched element.
 */
@Stable
public class GlassInteractionState {

    private val hold = Animatable(0f)
    private val settling = Animatable(Offset.Zero, Offset.VectorConverter)
    private var travel by mutableStateOf(Offset.Zero)
    private var origin = Offset.Unspecified
    private var extent by mutableStateOf(Size.Zero)
    private var density = 1f

    /** Whether a finger is down on the glass. */
    public var isHeld: Boolean by mutableStateOf(false)
        private set

    /**
     * Where the finger is, in the coordinates of the glass element. Keeps its last value after the
     * finger lifts.
     */
    public var position: Offset by mutableStateOf(Offset.Zero)
        private set

    /**
     * How firmly the glass is held, from 0 at rest to 1 while pressed. It rises quickly on a press
     * and rebounds once on release, so it briefly passes a little beyond either end.
     */
    public val pressure: Float get() = hold.value

    /**
     * How far the finger has travelled since it went down. Returns to zero with a rebound once the
     * finger lifts.
     */
    public val drag: Offset get() = if (isHeld) travel else settling.value

    /**
     * How much the touch enlarges the glass along each axis: it swells while held and stretches
     * along the direction the finger has pulled, thinning across it. 1 on both axes at rest.
     *
     * Together with [offset] this is the change the glass applies to itself and its content, about
     * its centre. Apply the same to anything layered on top that should stay in register with it.
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

    /**
     * How far the held glass has drifted toward the finger, in pixels. Larger glass drifts less.
     */
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

    private companion object {
        // Pressing snaps up quickly; letting go rebounds past rest once.
        val PressSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 850f)
        val ReleaseSpring = spring<Float>(dampingRatio = 0.4f, stiffness = 300f)
        val ReturnSpring = spring<Offset>(dampingRatio = 0.4f, stiffness = 300f)

        // Sizes in dp.
        const val PressGrowth = 17f
        const val ReferenceSize = 44f
        const val PullShift = 0.036f
        const val PullStretch = 0.00033f
        const val MaxPull = 0.3f
    }
}

/**
 * Creates and remembers a [GlassInteractionState] for the current composition.
 */
@Composable
public fun rememberGlassInteractionState(): GlassInteractionState =
    remember { GlassInteractionState() }
