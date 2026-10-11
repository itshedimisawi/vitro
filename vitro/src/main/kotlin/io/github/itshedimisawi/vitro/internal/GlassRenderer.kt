package io.github.itshedimisawi.vitro.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassPane
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal class GlassScene(
    val layer: GraphicsLayer,
    var backdrop: GraphicsLayer,
    var glass: Glass,
) {
    // Glass lying under this one that paints inside `view`, lowest first, and those among them
    // that this glass is inside of.
    var beneath: List<GlassPane> = emptyList()
    var enclosing: List<GlassPane> = emptyList()
    var origin: Offset = Offset.Zero
    // What this glass takes in of the backdrop, measured from its own corner.
    var view: Rect = Rect.Zero
    var touch: Offset = Offset.Zero
    var pressure: Float = 0f
    var glow: Float = 0f
    var revision: Int = 0
    // 0 for the light material, 1 for the dark one, in between while it changes over.
    var darkness: Float = 0f
    var scaleX: Float = 1f
    var scaleY: Float = 1f
    val radii: FloatArray = FloatArray(4)
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

/** The glass itself and a border as wide as its blur, which softening the edge draws on. */
internal fun DrawScope.blurredView(scene: GlassScene): Rect {
    val margin = ceil(scene.glass.blurRadius.toPx())
    return Rect(-margin, -margin, ceil(size.width) + margin, ceil(size.height) + margin)
}

/**
 * The area a piece of glass paints on, measured from its own corner: its outline at the size it
 * is drawn, and the shadow around and below it.
 */
internal fun DrawScope.paintedBounds(scene: GlassScene): Rect {
    val halfWidth = size.width * scene.scaleX / 2f
    val halfHeight = size.height * scene.scaleY / 2f
    val outline = Rect(center.x - halfWidth, center.y - halfHeight, center.x + halfWidth, center.y + halfHeight)
    val radius = scene.glass.shadowRadius.toPx()
    if (scene.shadowOpacity() <= 0f || radius <= 0f) return outline
    val grown = maxOf(scene.scaleX, scene.scaleY, 1f)
    val spread = radius * grown
    val drop = scene.glass.shadowOffset.toPx() * grown
    return Rect(outline.left - spread, outline.top - spread, outline.right + spread, outline.bottom + spread + drop)
}

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

internal fun GlassScene.shade(): GlassShade {
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

internal fun GlassScene.shadowOpacity(): Float =
    glass.shadowOpacity * (1f + (DarkShadow - 1f) * darkness)

internal fun ColorMatrix.setToMaterial(scene: GlassScene) {
    val veil = scene.glass.brightness.coerceIn(0f, 1f)
    val dark = scene.darkness
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
