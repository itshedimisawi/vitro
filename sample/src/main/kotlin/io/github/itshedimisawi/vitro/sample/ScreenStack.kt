package io.github.itshedimisawi.vitro.sample

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.delay

private val PushSpec = tween<Float>(380, easing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f))
private const val BelowDim = 0.12f
private const val Scrim = "scrim"

/**
 * The screens as an iOS-style stack: the deeper screen slides over the other, which shifts and
 * dims. Every screen stays composed, ahead of time while the app idles, so no move waits on
 * composition; only the screens in sight are placed.
 */
@Composable
fun ScreenStack(screen: Screen, content: @Composable (Screen) -> Unit) {
    val ready = remember { mutableStateListOf<Screen>() }
    val push = remember { Animatable(1f) }
    var from by remember { mutableStateOf(screen) }
    var to by remember { mutableStateOf(screen) }
    var isMoving by remember { mutableStateOf(false) }

    LaunchedEffect(screen) {
        if (screen == to) return@LaunchedEffect
        from = to
        to = screen
        isMoving = true
        try {
            push.snapTo(0f)
            push.animateTo(1f, PushSpec)
        } finally {
            isMoving = false
        }
    }
    LaunchedEffect(Unit) {
        delay(IdleAfterLaunch)
        for (next in Screen.entries) {
            withFrameNanos {}
            if (next !in ready) ready += next
        }
    }

    val forward = to > from
    val lower = minOf(from, to)
    val deeper = maxOf(from, to)
    val shown = Screen.entries.filter { it in ready || it == screen }
    Layout(
        content = {
            shown.forEach { each -> key(each) { Box(Modifier.layoutId(each)) { content(each) } } }
            Spacer(
                Modifier.layoutId(Scrim).drawBehind {
                    val dim = BelowDim * if (forward) push.value else 1f - push.value
                    drawRect(Color.Black.copy(alpha = dim))
                },
            )
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val whole = Constraints.fixed(width, constraints.maxHeight)
        val placeables = measurables.associate { it.layoutId to it.measure(whole) }
        layout(width, constraints.maxHeight) {
            // Each screen sits on its own layer, which a push only moves.
            if (!isMoving) {
                placeables[to]?.placeWithLayer(0, 0)
                return@layout
            }
            // The lower screen shifts a third of the way under the scrim; the deeper one slides over.
            placeables[lower]?.placeWithLayer(0, 0) {
                translationX = -width / 3f * if (forward) push.value else 1f - push.value
            }
            placeables[Scrim]?.place(0, 0)
            placeables[deeper]?.placeWithLayer(0, 0) {
                translationX = width * if (forward) 1f - push.value else push.value
            }
        }
    }
}

// Screens are composed ahead once the first frames are out of the way.
private const val IdleAfterLaunch = 600L
