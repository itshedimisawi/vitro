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

/** One piece of glass as drawn this frame: its outline in the backdrop and how touch changes it. */
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

    /** Where its parents let this glass show, in the backdrop; null when nothing cuts it. */
    var clip: Rect? = null

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
 * What a renderer draws in one go: one [shape], or several [shapes] of one material that blend
 * closer than [spacing]. Positions are relative to a frame of [extent] at [origin] in the backdrop.
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

    /** Where any of this glass can show, from the corner of its frame; null when nothing cuts it. */
    var visible: Rect? = null

    /** What this glass takes in of the backdrop, measured from the corner of its frame. */
    var view: Rect = Rect.Zero
    var revision: Int = 0

    /** False while the renderer holds more room than needed, which it gives back on the next draw. */
    var isSettled: Boolean = true
}

/** The area a scene paints on, from the corner of its frame: every outline and its shadow. */
internal fun DrawScope.paintedBounds(scene: GlassScene): Rect {
    var painted: Rect? = null
    for (shape in scene.shapes) painted = painted.joinedWith(paintedArea(shape, scene.glass))
    return (painted ?: Rect.Zero).translate(-scene.origin.x, -scene.origin.y)
}

/** The area [shape] paints on in the backdrop: its outline as drawn and the shadow of [glass]. */
internal fun DrawScope.paintedArea(shape: GlassShape, glass: Glass): Rect {
    val radius = glass.shadowRadius.toPx()
    val outline = shape.grown
    val isShadowed = shape.shadowOpacity > 0f && radius > 0f
    val grown = maxOf(shape.scaleX, shape.scaleY, 1f)
    val spread = if (isShadowed) radius * grown else 0f
    val drop = if (isShadowed) glass.shadowOffset.toPx() * grown else 0f
    return Rect(outline.left - spread, outline.top - spread, outline.right + spread, outline.bottom + spread + drop)
}

/** The smallest rectangle around this one, if any, and [other]. */
internal fun Rect?.joinedWith(other: Rect): Rect =
    if (this == null) other else Rect(min(left, other.left), min(top, other.top), max(right, other.right), max(bottom, other.bottom))
