package io.github.itshedimisawi.vitro.sample

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

fun Modifier.draggable(position: MutableState<Offset>): Modifier = this
    .offset { IntOffset(position.value.x.roundToInt(), position.value.y.roundToInt()) }
    .pointerInput(position) {
        detectDragGestures { change, amount ->
            change.consume()
            position.value += amount
        }
    }
