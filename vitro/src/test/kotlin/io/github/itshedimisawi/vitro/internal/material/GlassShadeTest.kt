package io.github.itshedimisawi.vitro.internal.material

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GlassShadeTest {

    private val regular = GlassShade(
        saturation = 1.1f,
        veil = 0.5f,
        darkness = 0f,
        tint = Color.Black,
        tintStrength = 0f,
        dimming = 0f,
    )

    private fun GlassShade.over(vararg color: Float): FloatArray = color.copyOf().also { through(it) }

    @Test
    fun noneLeavesColourUnchanged() {
        val color = floatArrayOf(0.8f, 0.3f, 0.1f)
        assertArrayEquals(color, GlassShade.None.over(*color), 1e-6f)
    }

    @Test
    fun lightMaterialLiftsGreyAlongTheCalibratedLine() {
        // At the default brightness light glass maps grey g to 0.66 + 0.33 g.
        for (grey in listOf(0f, 0.25f, 0.5f, 0.75f)) {
            val seen = regular.over(grey, grey, grey)
            for (channel in seen) assertEquals(0.66f + 0.33f * grey, channel, 1e-4f)
        }
    }

    @Test
    fun darkMaterialLiftsBlackAndHoldsBrightContent() {
        val dark = regular.copy(darkness = 1f)
        assertEquals(0.5f * DarkLift, dark.over(0f, 0f, 0f)[0], 1e-6f)
        val bright = dark.over(0.9f, 0.9f, 0.9f)[0]
        assertTrue(bright < 0.9f)
    }

    @Test
    fun fullDimmingTurnsEverythingBlack() {
        val seen = regular.copy(dimming = 1f).over(1f, 0.5f, 0.2f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f), seen, 0f)
    }

    @Test
    fun tintMixesTowardItsColourByStrength() {
        val tinted = GlassShade.None.copy(tint = Color.Red, tintStrength = 1f).over(0.2f, 0.4f, 0.6f)
        assertArrayEquals(floatArrayOf(1f, 0f, 0f), tinted, 1e-6f)
    }

    @Test
    fun darkLineIsTheLeastSquaresFitOfTheDarkCurve() {
        val (lift, fade) = DarkLine
        var residual = 0f
        for (step in 0..64) {
            val brightness = step / 64f
            residual += darkBend(brightness) - (lift - fade * brightness)
        }
        assertTrue(abs(residual) < 1e-3f)
        assertTrue(fade > 0f)
    }
}
