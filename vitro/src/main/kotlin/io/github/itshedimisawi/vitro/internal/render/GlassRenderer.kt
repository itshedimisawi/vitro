package io.github.itshedimisawi.vitro.internal.render

import android.os.Build
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import io.github.itshedimisawi.vitro.internal.material.shownScale
import kotlin.math.ceil
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope

/** Draws the shadow and material of a [GlassScene]; the content on top is drawn by the caller. */
internal interface GlassRenderer {

    /** The part of the backdrop this renderer needs to draw [scene], measured from its corner. */
    fun DrawScope.view(scene: GlassScene): Rect

    fun DrawScope.render(scene: GlassScene)

    fun release() {}
}

/** The best renderer this device supports: the shader on Android 13, blurred approximations below. */
internal fun createGlassRenderer(context: GraphicsContext, scope: CoroutineScope): GlassRenderer = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> ShaderRenderer()
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> BlurRenderer()
    else -> SnapshotRenderer(context, scope)
}

/**
 * Draws what this glass takes in, with the corner of its view at the top left: the backdrop, then
 * the glass beneath that reaches into the view. Glass this piece is inside of shows as its bare
 * material, since its content includes this piece.
 */
internal fun DrawScope.drawView(scene: GlassScene) {
    translate(-scene.origin.x - scene.view.left, -scene.origin.y - scene.view.top) {
        drawLayer(scene.backdrop)
        for (pane in scene.beneath) {
            translate(pane.origin.x, pane.origin.y) {
                drawLayer(if (pane in scene.enclosing) pane.surface else pane.layer)
            }
        }
    }
}

/**
 * What the fallback renderers take in: the outline at the size it is drawn, widened for glass
 * that shrinks what it shows, and a border as wide as the blur, which softening the edge draws on.
 */
internal fun DrawScope.blurredView(scene: GlassScene): Rect {
    val margin = ceil(scene.glass.blurRadius.toPx())
    val reach = 1f / scene.glass.shownScale().coerceAtMost(1f)
    val marginX = margin + ceil(max(scene.extent.width * (scene.shape.scaleX * reach - 1f) / 2f, 0f))
    val marginY = margin + ceil(max(scene.extent.height * (scene.shape.scaleY * reach - 1f) / 2f, 0f))
    return Rect(-marginX, -marginY, ceil(scene.extent.width) + marginX, ceil(scene.extent.height) + marginY)
}
