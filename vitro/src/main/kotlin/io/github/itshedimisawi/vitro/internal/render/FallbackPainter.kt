package io.github.itshedimisawi.vitro.internal.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp
import io.github.itshedimisawi.vitro.internal.material.LightRim
import io.github.itshedimisawi.vitro.internal.material.RimFloor
import io.github.itshedimisawi.vitro.internal.material.TintStrength
import io.github.itshedimisawi.vitro.internal.material.lightDirection
import io.github.itshedimisawi.vitro.internal.material.shownScale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Shadow, stepped lens, rim and press glow drawn with paths, for renderers without the shader. */
internal class FallbackPainter {

    private val outline = Path()
    private val ring = Path()

    // The outline is drawn at the size the touch has given it, while what shows through keeps
    // its own size, set by the magnification alone: pressed glass uncovers more of the backdrop.
    fun DrawScope.paint(scene: GlassScene, drawBackdrop: DrawScope.() -> Unit) {
        val shape = scene.shape
        val shown = scene.glass.shownScale()
        scale(shape.scaleX, shape.scaleY) {
            paintMaterial(scene) { scale(shown / shape.scaleX, shown / shape.scaleY, block = drawBackdrop) }
        }
    }

    private fun DrawScope.paintMaterial(scene: GlassScene, drawBackdrop: DrawScope.() -> Unit) {
        val glass = scene.glass
        drawShadow(scene)

        outline.trace(this, scene.shape.radii, 0f)
        clipPath(outline) {
            val bevel = min(glass.refractionHeight.toPx(), size.minDimension / 2f)
            val depth = glass.refractionAmount.toPx()
            if (bevel > 0f && depth > 0f) {
                // Each ring redraws the backdrop stretched outward, stepping the lens curve.
                for (step in 0 until Rings) {
                    val slope = 1f - (step + 0.5f) / Rings
                    val shift = depth * slope * slope * slope
                    ring.trace(this, scene.shape.radii, bevel * step / Rings)
                    clipPath(ring) {
                        scale(
                            scaleX = 1f / max(1f - shift * 2f / size.width, 0.35f),
                            scaleY = 1f / max(1f - shift * 2f / size.height, 0.35f),
                            block = drawBackdrop,
                        )
                    }
                }
                ring.trace(this, scene.shape.radii, bevel)
                clipPath(ring, block = drawBackdrop)
            } else {
                drawBackdrop()
            }

            if (glass.brightness < 0f) {
                drawRect(Color.Black, alpha = -glass.brightness.coerceAtLeast(-1f))
            }
            if (glass.tint.isSpecified) {
                drawRect(glass.tint, alpha = glass.tint.alpha * TintStrength)
            }
            if (glass.dimming > 0f) {
                drawRect(Color.Black, alpha = glass.dimming.coerceAtMost(1f))
            }

            drawHighlights(scene, bevel)
        }
    }

    private fun DrawScope.drawHighlights(scene: GlassScene, bevel: Float) {
        val strength = scene.glass.highlightIntensity
        if (strength > 0f) {
            val light = lightDirection(scene.glass.lightAngle)
            val reach = (abs(light.x) * size.width + abs(light.y) * size.height) / 2f
            val lit = center + light * reach
            val far = center - light * reach
            // The rim follows the shader's: full toward and away from the light, faint across it.
            // Light glass is pushed toward white by the same share; dark glass, whose glint a
            // plain stroke cannot follow, keeps a fixed one.
            val dark = scene.shape.darkness
            val peak = strength * (LightRim * (1f - dark) + DarkRimStroke * dark)
            drawPath(
                path = outline,
                brush = Brush.linearGradient(
                    0f to glow(peak),
                    0.5f to glow(peak * RimFloor),
                    1f to glow(peak),
                    start = lit,
                    end = far,
                ),
                style = Stroke(width = RimWidth.toPx() * 2f),
            )
            if (bevel > 0f) {
                drawPath(
                    path = outline,
                    brush = Brush.linearGradient(
                        0f to glow(0.08f * strength),
                        0.45f to Color.Transparent,
                        start = lit,
                        end = far,
                    ),
                    style = Stroke(width = bevel),
                )
            }
        }

        val pressure = scene.shape.pressure
        if (pressure > 0f) {
            drawRect(
                Brush.radialGradient(
                    0f to glow(0.2f * pressure),
                    1f to Color.Transparent,
                    center = scene.shape.touch,
                    radius = max(size.height * 2f, 1f),
                ),
            )
            drawRect(Color.White, alpha = 0.05f * pressure.coerceAtMost(1f))
        }
    }

    private fun DrawScope.drawShadow(scene: GlassScene) {
        val glass = scene.glass
        val radius = glass.shadowRadius.toPx()
        val opacity = scene.shape.shadowOpacity
        if (opacity <= 0f || radius <= 0f) return
        translate(top = glass.shadowOffset.toPx()) {
            for (step in ShadowSteps downTo 1) {
                val outer = radius * step / ShadowSteps
                val inner = radius * (step - 1) / ShadowSteps
                val alpha = opacity * (falloff(inner, radius) - falloff(outer, radius))
                ring.trace(this, scene.shape.radii, -outer)
                drawPath(ring, Color.Black, alpha = alpha.coerceIn(0f, 1f))
            }
        }
    }

    private fun Path.trace(scope: DrawScope, radii: FloatArray, inset: Float) {
        rewind()
        addRoundRect(
            RoundRect(
                left = inset,
                top = inset,
                right = scope.size.width - inset,
                bottom = scope.size.height - inset,
                topLeftCornerRadius = corner(radii[0], inset),
                topRightCornerRadius = corner(radii[1], inset),
                bottomRightCornerRadius = corner(radii[2], inset),
                bottomLeftCornerRadius = corner(radii[3], inset),
            ),
        )
    }

    private fun corner(radius: Float, inset: Float) = CornerRadius(max(radius - inset, 0f))

    private fun glow(alpha: Float) = Color.White.copy(alpha = alpha.coerceIn(0f, 1f))

    private fun falloff(distance: Float, radius: Float): Float {
        val fraction = ((distance + radius) / (radius * 2f)).coerceIn(0f, 1f)
        return 1f - fraction * fraction * (3f - 2f * fraction)
    }

    private companion object {
        const val Rings = 5
        const val DarkRimStroke = 0.4f
        val RimWidth = 1.dp
        const val ShadowSteps = 10
    }
}
