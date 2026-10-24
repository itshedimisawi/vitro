package io.github.itshedimisawi.vitro.internal.render

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
import androidx.compose.ui.unit.dp
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.internal.material.BlurReach
import io.github.itshedimisawi.vitro.internal.material.TintStrength
import io.github.itshedimisawi.vitro.internal.material.blurSigma
import io.github.itshedimisawi.vitro.internal.material.lightDirection
import io.github.itshedimisawi.vitro.internal.material.shownScale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Android 13 and later: the shadow and material shaders, with the blur split across two passes. */
@RequiresApi(33)
internal class ShaderRenderer : GlassRenderer {

    private val material = RuntimeShader(GlassShaders.Material)
    private val across = RuntimeShader(GlassShaders.BlurAcross)
    private val taps = BlurTaps()
    private val shadow = RuntimeShader(GlassShaders.Shadow)
    private val shadowBrush = ShaderBrush(shadow)

    private val frames = FloatArray(GlassShaders.MaxShapes * 4)
    private val corners = FloatArray(GlassShaders.MaxShapes * 4)
    private val grows = FloatArray(GlassShaders.MaxShapes * 4)
    private val traits = FloatArray(GlassShaders.MaxShapes * 4)
    private val tints = FloatArray(GlassShaders.MaxShapes * 4)
    private val clips = FloatArray(GlassShaders.MaxShapes * 4)

    private var wanted = IntSize.Zero
    private var held = IntSize.Zero

    override fun DrawScope.render(scene: GlassScene) {
        if (scene.visible?.isEmpty == true) return
        drawShadow(scene)
        drawMaterial(scene)
    }

    private fun DrawScope.drawShadow(scene: GlassScene) {
        val glass = scene.glass
        val radius = glass.shadowRadius.toPx()
        if (radius <= 0f || scene.shapes.none { it.shadowOpacity > 0f }) return
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

    // The view covers every outline at its grown size, so pressed glass shows what is really
    // behind it. Glass that shrinks what it shows reaches past its outline.
    override fun DrawScope.view(scene: GlassScene): Rect {
        val reach = 1f / scene.glass.shownScale().coerceAtMost(1f)
        val border = blurBorder(scene.glass)
        val wide = overhang(scene, reach, isWide = true) + border
        val tall = overhang(scene, reach, isWide = false) + border
        var area = Rect(
            -wide.toFloat(),
            -tall.toFloat(),
            (ceil(scene.extent.width).toInt() + wide).toFloat(),
            (ceil(scene.extent.height).toInt() + tall).toFloat(),
        )
        // Partly hidden glass only needs what its visible part pulls in, plus the blur around it.
        scene.visible?.let { visible ->
            val pull = ceil(scene.glass.refractionAmount.toPx() * (1f + 2f * abs(scene.glass.dispersion)))
            val margin = max(wide, tall) + pull + 1f
            val seen = area.intersect(visible.inflate(margin))
            area = if (seen.isEmpty) {
                Rect.Zero
            } else {
                Rect(floor(seen.left), floor(seen.top), ceil(seen.right), ceil(seen.bottom))
            }
        }
        val needed = IntSize(area.width.toInt(), area.height.toInt())
        // A layer gets a new buffer on every size change, so it only grows in steps while the
        // glass changes size, and takes its exact size once the glass rests.
        val room = GrowthRoom.roundToPx()
        held = when {
            needed == wanted -> needed
            else -> IntSize(roomFor(needed.width, held.width, room), roomFor(needed.height, held.height, room))
        }
        wanted = needed
        scene.isSettled = held == needed
        val left = area.left - (held.width - needed.width) / 2
        val top = area.top - (held.height - needed.height) / 2
        return Rect(left, top, left + held.width, top + held.height)
    }

    private fun roomFor(needed: Int, held: Int, room: Int): Int = if (needed <= held) held else needed + room

    /**
     * How far past the outline the view reaches so the blur has real content to draw on. The rim
     * shows content from further in, so only a heavy blur needs it.
     */
    private fun DrawScope.blurBorder(glass: Glass): Int {
        val blur = glass.blurRadius.toPx()
        if (blur <= 0f) return 0
        val spread = BlurReach * blurSigma(blur)
        return ceil(spread - nearestSample(glass)).toInt().coerceAtLeast(0)
    }

    /** The least distance inside the outline from which the rim takes content. */
    private fun DrawScope.nearestSample(glass: Glass): Float {
        if (glass.magnification < 1f) return 0f
        val height = glass.refractionHeight.toPx()
        val amount = glass.refractionAmount.toPx() * (1f - 2f * glass.dispersion.coerceAtLeast(0f))
        if (height <= 0f || amount <= 0f) return 0f
        if (amount <= height / 3f) return amount
        val slope = sqrt(height / (3f * amount))
        return height * (1f - slope) + amount * slope * slope * slope
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
        material.setFloatUniform("zoom", 1f / glass.shownScale())
        material.setFloatUniform("saturation", glass.saturation)
        material.setFloatUniform("brightness", glass.brightness.coerceIn(-1f, 1f))
        material.setFloatUniform("light", light.x, light.y)
        material.setFloatUniform("highlight", glass.highlightIntensity)
        material.setFloatUniform("shadow", glass.shadowRadius.toPx(), glass.shadowOffset.toPx())

        // The shaders blur across in one pass and down as the material reads the content. A blur
        // too strong for them uses the platform's.
        val blur = glass.blurRadius.toPx()
        val sigma = if (blur > 0f) blurSigma(blur) else 0f
        val isShaderBlur = sigma > 0f && sigma <= GlassShaders.MaxBlurSigma
        taps.prepare(if (isShaderBlur) sigma else 0f)
        material.setBlur(view)
        var effect = RenderEffect.createRuntimeShaderEffect(material, GlassShaders.Backdrop)
        if (isShaderBlur) {
            across.setBlur(view)
            effect = RenderEffect.createChainEffect(effect, RenderEffect.createRuntimeShaderEffect(across, GlassShaders.Content))
        } else if (blur > 0f) {
            val softened = RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP)
            effect = RenderEffect.createChainEffect(effect, softened)
        }
        layer.renderEffect = effect.asComposeRenderEffect()
        // Unclipped, the shader would run for every pixel of the screen.
        clipRect(view.left, view.top, view.right, view.bottom) { drawLayer(layer) }
    }

    private fun overhang(scene: GlassScene, reach: Float, isWide: Boolean): Int {
        var needed = 0f
        for (shape in scene.shapes) {
            val length = if (isWide) shape.size.width else shape.size.height
            val scale = (if (isWide) shape.scaleX else shape.scaleY) * reach
            needed = max(needed, length * (scale - 1f) / 2f)
        }
        return ceil(needed).toInt()
    }

    private fun RuntimeShader.setBlur(view: Rect) {
        setFloatUniform("blurCount", taps.count.toFloat())
        setFloatUniform("blurCentre", taps.centre)
        setFloatUniform("blurTaps", taps.taps)
        setFloatUniform("viewSize", view.width, view.height)
    }

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
            grows[at + 2] = shape.shadowOpacity
            grows[at + 3] = shape.glow
            val clip = shape.clip
            val shiftX = inset.x - scene.origin.x
            val shiftY = inset.y - scene.origin.y
            clips[at] = if (clip == null) -Open else clip.left + shiftX
            clips[at + 1] = if (clip == null) -Open else clip.top + shiftY
            clips[at + 2] = if (clip == null) Open else clip.right + shiftX
            clips[at + 3] = if (clip == null) Open else clip.bottom + shiftY
        }
        setFloatUniform("count", count.toFloat())
        setFloatUniform("blend", scene.spacing)
        setFloatUniform("frames", frames)
        setFloatUniform("corners", corners)
        setFloatUniform("grows", grows)
        setFloatUniform("clips", clips)
        setFloatUniform("density", density)
    }
}

private val GrowthRoom = 16.dp

// A side of a clip that nothing cuts.
private const val Open = 1_000_000f
