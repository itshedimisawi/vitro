package io.github.itshedimisawi.vitro.internal.node

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * How far moving glass stretches along each axis, as a share of its size. It grows with the speed
 * of the glass while it keeps moving and springs back once it stops.
 */
internal class MotionStretch {

    private val stretch = Animatable(Offset.Zero, Offset.VectorConverter)
    private var following: Job? = null

    val value: Offset get() = stretch.value

    /**
     * Follows glass that has just moved away from [from] within its parent; a scroll or a parent
     * that carries it does not count. [position] is read once a frame on the frame clock until two
     * frames pass without movement.
     */
    fun follow(scope: CoroutineScope, from: Offset, density: Float, position: () -> Offset) {
        if (following?.isActive == true) return
        following = scope.launch {
            val perSpeed = StretchPerVelocity / density
            var last = from
            var time = withFrameNanos { it }
            var stillFrames = 0
            while (stillFrames < 2) {
                val now = withFrameNanos { it }
                val at = position()
                val elapsed = (now - time) / 1_000_000f
                if (!at.isSpecified) break
                if (at == last || elapsed <= 0f) {
                    stillFrames++
                } else {
                    stillFrames = 0
                    val speed = (at - last) / elapsed
                    val target = Offset(
                        (abs(speed.x) * perSpeed).coerceAtMost(MaxStretch),
                        (abs(speed.y) * perSpeed).coerceAtMost(MaxStretch),
                    )
                    launch { stretch.animateTo(target, StretchSpring) }
                }
                last = at
                time = now
            }
            stretch.animateTo(Offset.Zero, ReboundSpring)
        }
    }

    fun reset(scope: CoroutineScope) {
        following?.cancel()
        scope.launch { stretch.snapTo(Offset.Zero) }
    }

    private companion object {
        val StretchSpring = spring<Offset>(dampingRatio = 1f, stiffness = Spring.StiffnessHigh)
        val ReboundSpring = spring<Offset>(dampingRatio = 0.4f, stiffness = 180f)
        const val StretchPerVelocity = 0.2f
        const val MaxStretch = 0.14f
    }
}
