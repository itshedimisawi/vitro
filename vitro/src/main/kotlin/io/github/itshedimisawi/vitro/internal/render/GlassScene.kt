package io.github.itshedimisawi.vitro.internal.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.internal.material.DarkShadow
import io.github.itshedimisawi.vitro.internal.material.GlassShade
import io.github.itshedimisawi.vitro.internal.material.TintStrength
import kotlin.math.max
import kotlin.math.min

/**
 * One piece of glass as it is drawn now: where its outline sits in the backdrop, its size at rest
 * and how the touch on it has changed it.
 */
internal class GlassShape(var glass: Glass) {
    var origin: Offset = Offset.Zero
    var size: Size = Size.Zero
    val radii: FloatArray = FloatArray(4)
    var scaleX: Float = 1f
    var scaleY: Float = 1f
    var touch: Offset = Offset.Zero
    var pressure: Float = 0f
    var glow: Float = 0f

    /** 0 for the light material, 1 for the dark one, in between while it changes over. */
    var darkness: Float = 0f

    /** The outline at the size it is drawn, in the backdrop. */
    val grown: Rect
        get() {
            val halfWidth = size.width * scaleX / 2f
            val halfHeight = size.height * scaleY / 2f
            val centre = origin + Offset(size.width / 2f, size.height / 2f)
            return Rect(centre.x - halfWidth, centre.y - halfHeight, centre.x + halfWidth, centre.y + halfHeight)
        }

    /** Opacity of the shadow, which dark glass mostly gives up. */
    val shadowOpacity: Float get() = glass.shadowOpacity * (1f + (DarkShadow - 1f) * darkness)

    /** How this glass changes the colour of what shows through it. */
    fun shade(): GlassShade {
        val tint = glass.tint
        val isTinted = tint.isSpecified
        return GlassShade(
            saturation = glass.saturation,
            veil = glass.brightness.coerceIn(-1f, 1f),
            darkness = darkness,
            tint = if (isTinted) tint else Color.Black,
            tintStrength = if (isTinted) tint.alpha * TintStrength else 0f,
            dimming = glass.dimming.coerceIn(0f, 1f),
        )
    }
}

/**
 * What a renderer draws in one go: a single [shape], or several [shapes] of the same material
 * that flow into one another where they come closer than [spacing]. Everything is laid out in a
 * frame of [extent] whose corner sits at [origin] in the backdrop.
 */
internal class GlassScene(
    val layer: GraphicsLayer,
    var backdrop: GraphicsLayer,
    var glass: Glass,
) {
    val shape = GlassShape(glass)
    var shapes: List<GlassShape> = listOf(shape)
    var spacing: Float = 0f
    var origin: Offset = Offset.Zero
    var extent: Size = Size.Zero

    /** Glass under this one that paints inside [view], lowest first. */
    var beneath: List<GlassPane> = emptyList()

    /** Those of [beneath] that this glass is inside of. */
    var enclosing: List<GlassPane> = emptyList()

    /** What this glass takes in of the backdrop, measured from the corner of its frame. */
    var view: Rect = Rect.Zero
    var revision: Int = 0

    /** False while the renderer holds more room than this scene needs; it gives it back on the next draw. */
    var isSettled: Boolean = true
}

/**
 * The area a scene paints on, measured from the corner of its frame: every outline at the size it
 * is drawn, and the shadow around and below it.
 */
internal fun DrawScope.paintedBounds(scene: GlassScene): Rect {
    val radius = scene.glass.shadowRadius.toPx()
    val offset = scene.glass.shadowOffset.toPx()
    var left = Float.POSITIVE_INFINITY
    var top = Float.POSITIVE_INFINITY
    var right = Float.NEGATIVE_INFINITY
    var bottom = Float.NEGATIVE_INFINITY
    for (shape in scene.shapes) {
        val outline = shape.grown
        val isShadowed = shape.shadowOpacity > 0f && radius > 0f
        val grown = maxOf(shape.scaleX, shape.scaleY, 1f)
        val spread = if (isShadowed) radius * grown else 0f
        val drop = if (isShadowed) offset * grown else 0f
        left = min(left, outline.left - spread)
        top = min(top, outline.top - spread)
        right = max(right, outline.right + spread)
        bottom = max(bottom, outline.bottom + spread + drop)
    }
    return Rect(left, top, right, bottom).translate(-scene.origin.x, -scene.origin.y)
}
