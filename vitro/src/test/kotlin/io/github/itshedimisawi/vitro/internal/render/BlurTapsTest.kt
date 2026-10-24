package io.github.itshedimisawi.vitro.internal.render

import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlurTapsTest {

    @Test
    fun noBlurReadsThePixelAlone() {
        val taps = BlurTaps().apply { prepare(0f) }
        assertEquals(0, taps.count)
        assertEquals(1f, taps.centre)
    }

    @Test
    fun weightsAddUpToOne() {
        for (sigma in floatArrayOf(0.8f, 2.3f, 4.56f, 7f, GlassShaders.MaxBlurSigma)) {
            val taps = BlurTaps().apply { prepare(sigma) }
            var total = taps.centre
            for (tap in 0 until taps.count) total += 2f * taps.taps[tap * 2 + 1]
            assertEquals("sigma $sigma", 1f, total, 1e-5f)
        }
    }

    @Test
    fun theStrongestBlurFitsTheTaps() {
        val taps = BlurTaps().apply { prepare(GlassShaders.MaxBlurSigma) }
        assertTrue(taps.count <= GlassShaders.MaxBlurTaps)
        assertEquals(ceil(3f * GlassShaders.MaxBlurSigma).toInt(), lastPixel(taps))
    }

    // Spread back over pixels by the filtering of each read, the taps give the Gaussian itself.
    @Test
    fun filteredTapsGiveTheGaussian() {
        val sigma = 4.56f
        val taps = BlurTaps().apply { prepare(sigma) }
        val radius = ceil(3f * sigma).toInt()
        val spread = FloatArray(radius + 2)
        spread[0] = taps.centre
        for (tap in 0 until taps.count) {
            val offset = taps.taps[tap * 2]
            val weight = taps.taps[tap * 2 + 1]
            val near = floor(offset).toInt()
            val share = offset - near
            spread[near] += weight * (1f - share)
            spread[near + 1] += weight * share
        }
        val curve = FloatArray(radius + 1) { exp(-(it * it) / (2f * sigma * sigma)) }
        val total = curve[0] + 2f * curve.drop(1).sum()
        for (x in 0..radius) assertEquals("pixel $x", curve[x] / total, spread[x], 1e-5f)
        assertEquals(0f, spread[radius + 1], 1e-6f)
    }

    private fun lastPixel(taps: BlurTaps): Int {
        val offset = taps.taps[(taps.count - 1) * 2]
        return ceil(offset).toInt()
    }
}
