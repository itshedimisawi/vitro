package io.github.itshedimisawi.vitro.internal.tone

import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Keeps a [BackdropTone] of some content up to date: the content is drawn very small and read
 * back, at most a few times a second, and each reading is handed to [onRead].
 */
internal class ToneReader(private val onRead: (BackdropTone) -> Unit) {

    private var miniature: GraphicsLayer? = null
    private var reading: Job? = null
    private var isStale = false
    private var cell = 1f
    private var scope: CoroutineScope? = null
    private var watched: View? = null

    // Content can change without being drawn again, when a layer inside it moves or scrolls. Any
    // frame of the window may therefore have changed what there is to read.
    private val onFrame = ViewTreeObserver.OnPreDrawListener {
        refresh()
        true
    }

    /** Call while drawing, whenever [source] has been recorded again. */
    fun DrawScope.read(source: GraphicsLayer, context: GraphicsContext, scope: CoroutineScope, view: View) {
        val small = miniature ?: context.createGraphicsLayer().also { miniature = it }
        cell = Cell.toPx().coerceAtLeast(1f)
        small.record(size = IntSize(ceil(size.width / cell).toInt(), ceil(size.height / cell).toInt())) {
            scale(1f / cell, 1f / cell, pivot = Offset.Zero) { drawLayer(source) }
        }
        this@ToneReader.scope = scope
        if (watched !== view) {
            unwatch()
            watched = view
            view.viewTreeObserver.addOnPreDrawListener(onFrame)
        }
        refresh()
    }

    /** Stops reading until [read] is called again. */
    fun pause() {
        unwatch()
        scope = null
    }

    fun release(context: GraphicsContext) {
        pause()
        reading?.cancel()
        reading = null
        miniature?.let { context.releaseGraphicsLayer(it) }
        miniature = null
    }

    private fun refresh() {
        val scope = scope ?: return
        val small = miniature ?: return
        isStale = true
        if (reading?.isActive == true) return
        val cell = cell
        reading = scope.launch {
            while (isStale) {
                isStale = false
                val shot = small.toImageBitmap()
                onRead(withContext(Dispatchers.Default) { BackdropTone.read(shot, cell) })
                delay(Interval)
            }
        }
    }

    private fun unwatch() {
        val observer = watched?.viewTreeObserver
        if (observer != null && observer.isAlive) observer.removeOnPreDrawListener(onFrame)
        watched = null
    }

    private companion object {
        val Cell = 6.dp
        const val Interval = 120L
    }
}
