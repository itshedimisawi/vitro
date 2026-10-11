package io.github.itshedimisawi.vitro.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.Dp
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassPane
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

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
    // 0 for the light material, 1 for the dark one, in between while it changes over.
    var darkness: Float = 0f

    /** The outline at the size it is drawn, in the backdrop. */
    val grown: Rect
        get() {
            val halfWidth = size.width * scaleX / 2f
            val halfHeight = size.height * scaleY / 2f
            val centre = origin + Offset(size.width / 2f, size.height / 2f)
            return Rect(centre.x - halfWidth, centre.y - halfHeight, centre.x + halfWidth, centre.y + halfHeight)
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
    // Glass lying under this one that paints inside `view`, lowest first, and those among them
    // that this glass is inside of.
    var beneath: List<GlassPane> = emptyList()
    var enclosing: List<GlassPane> = emptyList()
    // What this glass takes in of the backdrop, measured from the corner of its frame.
    var view: Rect = Rect.Zero
    var revision: Int = 0
    // False while the renderer is holding more room than this scene needs, which it gives back
    // the next time it draws.
    var isSettled: Boolean = true
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

/** The size content is shown at through this glass, which never shrinks it below half. */
internal fun Glass.shownScale(): Float = magnification.coerceAtLeast(MinMagnification)

internal const val MinMagnification = 0.5f

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
        val isShadowed = shape.shadowOpacity() > 0f && radius > 0f
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

/** What pieces of glass must have in common to be drawn as one. */
internal data class GlassMaterial(
    val blurRadius: Dp,
    val refractionHeight: Dp,
    val refractionAmount: Dp,
    val dispersion: Float,
    val magnification: Float,
    val saturation: Float,
    val brightness: Float,
    val lightAngle: Float,
    val highlightIntensity: Float,
    val shadowRadius: Dp,
    val shadowOffset: Dp,
)

internal fun Glass.material(): GlassMaterial = GlassMaterial(
    blurRadius = blurRadius,
    refractionHeight = refractionHeight,
    refractionAmount = refractionAmount,
    dispersion = dispersion,
    magnification = magnification,
    saturation = saturation,
    brightness = brightness,
    lightAngle = lightAngle,
    highlightIntensity = highlightIntensity,
    shadowRadius = shadowRadius,
    shadowOffset = shadowOffset,
)

internal interface GlassRenderer {

    /** The part of the backdrop this renderer needs to draw [scene], measured from its corner. */
    fun DrawScope.view(scene: GlassScene): Rect

    fun DrawScope.render(scene: GlassScene)

    fun release() {}
}

internal const val LightVeil = 1.32f
internal const val LightFade = 1.34f
internal const val LightWash = 0.58f
internal const val DarkGain = 0.08f
internal const val DarkVivid = 0.18f
internal const val DarkLift = 0.19f
internal const val DarkShade = 1f
internal const val DarkKnee = 0.33f
internal const val DarkShadow = 0.15f
internal const val DarkRimGain = 0.35f
internal const val DarkRimLift = 0.12f
internal const val LightRim = 0.91f
internal const val LightRimVivid = 0.2f
internal const val DarkRimVivid = 0.58f
internal const val RimFloor = 0.11f
internal const val PressGlow = 0.05f
internal const val PressGlowPerGrowth = 0.35f

internal val LumaWeights = floatArrayOf(0.2126f, 0.7152f, 0.0722f)

/**
 * What the dark material adds to content of the given brightness, for a full veil: a lift near
 * black and a hold on bright content. The shader applies the same curve.
 */
internal fun darkBend(brightness: Float): Float =
    DarkLift * (1f - brightness).pow(16) - DarkShade * max(brightness - DarkKnee, 0f).pow(1.5f)

/**
 * How a piece of glass changes the colour of what shows through it: its saturation and veil, how
 * far it is toward the dark material, its tint and its dimming. It follows the shader step by
 * step; refraction, blur and the rim are left out.
 */
internal data class GlassShade(
    val saturation: Float,
    val veil: Float,
    val darkness: Float,
    val tint: Color,
    val tintStrength: Float,
    val dimming: Float,
) {
    /** Replaces the red, green and blue in [color] with what shows through the glass. */
    fun through(color: FloatArray) {
        val grey = color[0] * LumaWeights[0] + color[1] * LumaWeights[1] + color[2] * LumaWeights[2]
        var luma = 0f
        for (channel in 0..2) {
            color[channel] = max(grey + (color[channel] - grey) * saturation, 0f)
            luma += color[channel] * LumaWeights[channel]
        }
        luma = min(luma, 1f)
        for (channel in 0..2) {
            val value = color[channel]
            var pale = value * (1f + veil)
            var deep = pale
            if (veil >= 0f) {
                pale = value + (luma - value) * LightWash * veil + veil * (LightVeil - LightFade * luma)
                val vivid = max(luma + (value - luma) * (1f + DarkVivid * veil), 0f)
                deep = max(vivid * (1f + DarkGain * veil) + veil * darkBend(luma), 0f)
            }
            val veiled = pale + (deep - pale) * darkness
            val tinted = veiled + (tintChannel(channel) - veiled) * tintStrength
            color[channel] = (tinted * (1f - dimming)).coerceIn(0f, 1f)
        }
    }

    private fun tintChannel(channel: Int): Float = when (channel) {
        0 -> tint.red
        1 -> tint.green
        else -> tint.blue
    }

    companion object {
        val None = GlassShade(
            saturation = 1f,
            veil = 0f,
            darkness = 0f,
            tint = Color.Black,
            tintStrength = 0f,
            dimming = 0f,
        )
    }
}

internal fun GlassShape.shade(): GlassShade {
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

// A colour matrix can only follow a straight line, so the fallbacks use the line closest to the
// dark material's curve: content is lifted by `first` and loses `second` times its brightness.
private val DarkLine: Pair<Float, Float> by lazy {
    val steps = 64
    var sumBend = 0f
    var sumCross = 0f
    var sumSquare = 0f
    for (step in 0..steps) {
        val brightness = step / steps.toFloat()
        val bend = darkBend(brightness)
        sumBend += bend
        sumCross += (brightness - 0.5f) * bend
        sumSquare += (brightness - 0.5f) * (brightness - 0.5f)
    }
    val slope = sumCross / sumSquare
    (sumBend / (steps + 1) - slope * 0.5f) to -slope
}

internal fun GlassShape.shadowOpacity(): Float =
    glass.shadowOpacity * (1f + (DarkShadow - 1f) * darkness)

internal fun ColorMatrix.setToMaterial(scene: GlassScene) {
    val veil = scene.glass.brightness.coerceIn(0f, 1f)
    val dark = scene.shape.darkness
    val light = 1f - dark
    val saturation = scene.glass.saturation * (1f - LightWash * veil * light) * (1f + DarkVivid * veil * dark)
    val gain = 1f + DarkGain * veil * dark
    val lift = LightVeil * veil * light + DarkLine.first * veil * dark
    val fade = LightFade * veil * light + DarkLine.second * veil * dark
    reset()
    for (row in 0..2) {
        for (column in 0..2) {
            val kept = if (row == column) saturation else 0f
            val mixed = kept + LumaWeights[column] * (1f - saturation)
            this[row, column] = gain * mixed - fade * LumaWeights[column]
        }
        this[row, 4] = lift * 255f
    }
}

internal fun lightDirection(angle: Float): Offset {
    val radians = Math.toRadians(angle.toDouble())
    return Offset(kotlin.math.sin(radians).toFloat(), -kotlin.math.cos(radians).toFloat())
}
