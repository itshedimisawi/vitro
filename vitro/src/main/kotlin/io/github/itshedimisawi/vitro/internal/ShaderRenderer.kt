package io.github.itshedimisawi.vitro.internal

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.IntOffset
import kotlin.math.ceil

@RequiresApi(33)
internal class ShaderRenderer : GlassRenderer {

    private val material = RuntimeShader(GlassShaders.Material)
    private val shadow = RuntimeShader(GlassShaders.Shadow)
    private val shadowBrush = ShaderBrush(shadow)

    override fun DrawScope.render(scene: GlassScene) {
        scale(scene.scaleX, scene.scaleY) {
            drawShadow(scene)
            drawMaterial(scene)
        }
    }

    private fun DrawScope.drawShadow(scene: GlassScene) {
        val glass = scene.glass
        val radius = glass.shadowRadius.toPx()
        val opacity = scene.shadowOpacity()
        if (opacity <= 0f || radius <= 0f) return
        val offset = glass.shadowOffset.toPx()
        val spread = ceil(radius + offset)
        shadow.setOutline(this, scene)
        shadow.setFloatUniform("shadow", radius, offset, opacity)
        drawRect(
            brush = shadowBrush,
            topLeft = Offset(-spread, -spread),
            size = Size(size.width + spread * 2f, size.height + spread * 2f),
        )
    }

    private fun DrawScope.drawMaterial(scene: GlassScene) {
        val glass = scene.glass
        val layer = scene.layer
        layer.record {
            translate(-scene.origin.x, -scene.origin.y) { drawLayer(scene.backdrop) }
        }
        layer.topLeft = IntOffset.Zero

        val light = lightDirection(glass.lightAngle)
        val tint = glass.tint
        material.setOutline(this, scene)
        material.setFloatUniform("bevel", glass.refractionHeight.toPx())
        material.setFloatUniform("depth", glass.refractionAmount.toPx())
        material.setFloatUniform("dispersion", glass.dispersion)
        material.setFloatUniform("saturation", glass.saturation)
        material.setFloatUniform("brightness", glass.brightness.coerceIn(-1f, 1f))
        material.setFloatUniform("dark", if (scene.isDark) 1f else 0f)
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
            scene.pressure,
        )

        val blur = glass.blurRadius.toPx()
        var effect = RenderEffect.createRuntimeShaderEffect(material, GlassShaders.Backdrop)
        if (blur > 0f) {
            val softened = RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP)
            effect = RenderEffect.createChainEffect(effect, softened)
        }
        layer.renderEffect = effect.asComposeRenderEffect()
        drawLayer(layer)
    }

    private fun RuntimeShader.setOutline(scope: DrawScope, scene: GlassScene) {
        setFloatUniform("center", scope.size.width / 2f, scope.size.height / 2f)
        setFloatUniform("halfSize", scope.size.width / 2f, scope.size.height / 2f)
        setFloatUniform("radii", scene.radii)
        setFloatUniform("density", scope.density)
    }
}

internal const val TintStrength = 0.85f
