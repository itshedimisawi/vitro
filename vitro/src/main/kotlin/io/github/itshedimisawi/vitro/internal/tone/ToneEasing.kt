package io.github.itshedimisawi.vitro.internal.tone

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest

/**
 * Follows [target]: a level, and whether it was read from the content. A change between two
 * readings eases in with [rise] or [fall]; any other change applies at once.
 */
internal suspend fun Animatable<Float, AnimationVector1D>.followReadings(
    rise: AnimationSpec<Float>,
    fall: AnimationSpec<Float> = rise,
    onTarget: (Float) -> Unit = {},
    target: () -> Pair<Float, Boolean>,
) {
    var wasRead = false
    snapshotFlow(target).collectLatest { (level, isRead) ->
        onTarget(level)
        val isEased = isRead && wasRead
        wasRead = isRead
        when {
            !isEased -> snapTo(level)
            level > value -> animateTo(level, rise)
            else -> animateTo(level, fall)
        }
    }
}
