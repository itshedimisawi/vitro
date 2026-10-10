package io.github.itshedimisawi.vitro.internal

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.max

@RequiresApi(33)
internal class ShaderRenderer : GlassRenderer {

    private val material = RuntimeShader(GlassShaders.Material)
    private val shadow = RuntimeShader(GlassShaders.Shadow)
    private val shadowBrush = ShaderBrush(shadow)

    override fun DrawScope.render(scene: GlassScene) {
        drawShadow(scene)
        drawMaterial(scene)
    }

    private fun DrawScope.drawShadow(scene: GlassScene) {
        val glass = scene.glass
        val radius = glass.shadowRadius.toPx()
        val opacity = scene.shadowOpacity()
        if (opacity <= 0f || radius <= 0f) return
        val offset = glass.shadowOffset.toPx()
        val spread = ceil(radius + offset + max(overhang(size.width, scene.scaleX), overhang(size.height, scene.scaleY)))
        shadow.setOutline(this, scene, Offset.Zero)
        shadow.setFloatUniform("shadow", radius, offset, opacity)
        drawRect(
            brush = shadowBrush,
            topLeft = Offset(-spread, -spread),
            size = Size(size.width + spread * 2f, size.height + spread * 2f),
        )
    }

    // The view covers the outline at its grown size, so a pressed element shows what is really
    // behind its larger footprint instead of a magnified copy. Glass that shrinks what it shows
    // also reaches past its own outline.
    override fun DrawScope.view(scene: GlassScene): Rect {
        val reach = 1f / scene.glass.magnification.coerceIn(MinMagnification, 1f)
        val marginX = overhang(size.width, scene.scaleX * reach).toFloat()
        val marginY = overhang(size.height, scene.scaleY * reach).toFloat()
        return Rect(-marginX, -marginY, ceil(size.width) + marginX, ceil(size.height) + marginY)
    }

    private fun DrawScope.drawMaterial(scene: GlassScene) {
        val glass = scene.glass
        val layer = scene.layer
        val view = scene.view
        layer.record(IntSize(view.width.toInt(), view.height.toInt())) { drawView(scene) }
        layer.topLeft = IntOffset(view.left.toInt(), view.top.toInt())

        val light = lightDirection(glass.lightAngle)
        val tint = glass.tint
        material.setOutline(this, scene, -view.topLeft)
        material.setFloatUniform("bevel", glass.refractionHeight.toPx())
        material.setFloatUniform("depth", glass.refractionAmount.toPx())
        material.setFloatUniform("dispersion", glass.dispersion)
        material.setFloatUniform("zoom", 1f / glass.magnification.coerceAtLeast(MinMagnification))
        material.setFloatUniform("saturation", glass.saturation)
        material.setFloatUniform("brightness", glass.brightness.coerceIn(-1f, 1f))
        material.setFloatUniform("dark", scene.darkness)
        if (tint.isSpecified) {
            material.setFloatUniform(
                "tint",
                tint.red,
                tint.green,
                tint.blue,
                tint.alpha * TintStrength,
            )
        } else {
            material.setFloatUniform("tint", 0f, 0f, 0f, 0f)
        }
        material.setFloatUniform("dimming", glass.dimming.coerceIn(0f, 1f))
        material.setFloatUniform("light", light.x, light.y)
        material.setFloatUniform("highlight", glass.highlightIntensity)
        material.setFloatUniform(
            "touch",
            scene.touch.x - size.width / 2f,
            scene.touch.y - size.height / 2f,
        )
        material.setFloatUniform("glow", scene.glow)

        val blur = glass.blurRadius.toPx()
        var effect = RenderEffect.createRuntimeShaderEffect(material, GlassShaders.Backdrop)
        if (blur > 0f) {
            val softened = RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP)
            effect = RenderEffect.createChainEffect(effect, softened)
        }
        layer.renderEffect = effect.asComposeRenderEffect()
        // The shader has no edge of its own: left unclipped, the system works it out for every
        // pixel that could be drawn to, which is the whole screen for each piece of glass.
        clipRect(view.left, view.top, view.right, view.bottom) { drawLayer(layer) }
    }

    // Rounded up in coarse steps: a layer that changes size every frame is reallocated every frame.
    private fun overhang(length: Float, scale: Float): Int {
        val needed = max(length * (scale - 1f) / 2f, 0f)
        return ceil(needed / OverhangStep).toInt() * OverhangStep
    }

    private fun RuntimeShader.setOutline(scope: DrawScope, scene: GlassScene, inset: Offset) {
        setFloatUniform("center", inset.x + scope.size.width / 2f, inset.y + scope.size.height / 2f)
        setFloatUniform("grow", scene.scaleX, scene.scaleY)
        setFloatUniform("halfSize", scope.size.width / 2f, scope.size.height / 2f)
        setFloatUniform("radii", scene.radii)
        setFloatUniform("density", scope.density)
    }
}

internal const val TintStrength = 0.85f
private const val OverhangStep = 48
private const val MinMagnification = 0.5f
