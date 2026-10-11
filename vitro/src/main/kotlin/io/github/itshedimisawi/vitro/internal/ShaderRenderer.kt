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
import kotlin.math.min

@RequiresApi(33)
internal class ShaderRenderer : GlassRenderer {

    private val material = RuntimeShader(GlassShaders.Material)
    private val shadow = RuntimeShader(GlassShaders.Shadow)
    private val shadowBrush = ShaderBrush(shadow)

    private val frames = FloatArray(GlassShaders.MaxShapes * 4)
    private val corners = FloatArray(GlassShaders.MaxShapes * 4)
    private val grows = FloatArray(GlassShaders.MaxShapes * 4)
    private val traits = FloatArray(GlassShaders.MaxShapes * 4)
    private val tints = FloatArray(GlassShaders.MaxShapes * 4)

    override fun DrawScope.render(scene: GlassScene) {
        drawShadow(scene)
        drawMaterial(scene)
    }

    private fun DrawScope.drawShadow(scene: GlassScene) {
        val glass = scene.glass
        val radius = glass.shadowRadius.toPx()
        if (radius <= 0f || scene.shapes.none { it.shadowOpacity() > 0f }) return
        val offset = glass.shadowOffset.toPx()
        val spread = ceil(radius + offset + max(overhang(scene, 1f, isWide = true), overhang(scene, 1f, isWide = false)))
        shadow.setShapes(scene, Offset.Zero, density)
        shadow.setFloatUniform("shadow", radius, offset)
        drawRect(
            brush = shadowBrush,
            topLeft = Offset(-spread, -spread),
            size = Size(scene.extent.width + spread * 2f, scene.extent.height + spread * 2f),
        )
    }

    // The view covers every outline at its grown size, so a pressed element shows what is really
    // behind its larger footprint instead of a magnified copy. Glass that shrinks what it shows
    // also reaches past its own outline.
    override fun DrawScope.view(scene: GlassScene): Rect {
        val reach = 1f / scene.glass.magnification.coerceIn(MinMagnification, 1f)
        val marginX = overhang(scene, reach, isWide = true).toFloat()
        val marginY = overhang(scene, reach, isWide = false).toFloat()
        return Rect(-marginX, -marginY, ceil(scene.extent.width) + marginX, ceil(scene.extent.height) + marginY)
    }

    private fun DrawScope.drawMaterial(scene: GlassScene) {
        val glass = scene.glass
        val layer = scene.layer
        val view = scene.view
        layer.record(IntSize(view.width.toInt(), view.height.toInt())) { drawView(scene) }
        layer.topLeft = IntOffset(view.left.toInt(), view.top.toInt())

        val light = lightDirection(glass.lightAngle)
        material.setShapes(scene, -view.topLeft, density)
        scene.shapes.forEachIndexed { index, shape ->
            val tint = shape.glass.tint
            val at = index * 4
            traits[at] = shape.darkness
            traits[at + 1] = shape.glass.dimming.coerceIn(0f, 1f)
            traits[at + 2] = shape.touch.x - shape.size.width / 2f
            traits[at + 3] = shape.touch.y - shape.size.height / 2f
            tints[at] = if (tint.isSpecified) tint.red else 0f
            tints[at + 1] = if (tint.isSpecified) tint.green else 0f
            tints[at + 2] = if (tint.isSpecified) tint.blue else 0f
            tints[at + 3] = if (tint.isSpecified) tint.alpha * TintStrength else 0f
        }
        material.setFloatUniform("traits", traits)
        material.setFloatUniform("tints", tints)
        material.setFloatUniform("bevel", glass.refractionHeight.toPx())
        material.setFloatUniform("depth", glass.refractionAmount.toPx())
        material.setFloatUniform("dispersion", glass.dispersion)
        material.setFloatUniform("zoom", 1f / glass.magnification.coerceAtLeast(MinMagnification))
        material.setFloatUniform("saturation", glass.saturation)
        material.setFloatUniform("brightness", glass.brightness.coerceIn(-1f, 1f))
        material.setFloatUniform("light", light.x, light.y)
        material.setFloatUniform("highlight", glass.highlightIntensity)

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

    // How far the largest outline reaches past its place at rest along one axis. Rounded up in
    // coarse steps: a layer that changes size every frame is reallocated every frame.
    private fun overhang(scene: GlassScene, reach: Float, isWide: Boolean): Int {
        var needed = 0f
        for (shape in scene.shapes) {
            val length = if (isWide) shape.size.width else shape.size.height
            val scale = (if (isWide) shape.scaleX else shape.scaleY) * reach
            needed = max(needed, length * (scale - 1f) / 2f)
        }
        return ceil(needed / OverhangStep).toInt() * OverhangStep
    }

    // Shapes are given by their centres, measured from `inset` past the corner of the frame.
    private fun RuntimeShader.setShapes(scene: GlassScene, inset: Offset, density: Float) {
        val count = min(scene.shapes.size, GlassShaders.MaxShapes)
        for (index in 0 until count) {
            val shape = scene.shapes[index]
            val at = index * 4
            frames[at] = shape.origin.x - scene.origin.x + inset.x + shape.size.width / 2f
            frames[at + 1] = shape.origin.y - scene.origin.y + inset.y + shape.size.height / 2f
            frames[at + 2] = shape.size.width / 2f
            frames[at + 3] = shape.size.height / 2f
            shape.radii.copyInto(corners, at)
            grows[at] = shape.scaleX
            grows[at + 1] = shape.scaleY
            grows[at + 2] = shape.shadowOpacity()
            grows[at + 3] = shape.glow
        }
        setFloatUniform("count", count.toFloat())
        setFloatUniform("blend", scene.spacing)
        setFloatUniform("frames", frames)
        setFloatUniform("corners", corners)
        setFloatUniform("grows", grows)
        setFloatUniform("density", density)
    }
}

internal const val TintStrength = 0.85f
private const val OverhangStep = 48
private const val MinMagnification = 0.5f
