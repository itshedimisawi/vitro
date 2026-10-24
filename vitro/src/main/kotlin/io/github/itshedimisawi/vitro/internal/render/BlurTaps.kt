package io.github.itshedimisawi.vitro.internal.render

import kotlin.math.ceil
import kotlin.math.exp

/**
 * One axis of a Gaussian blur reaching three deviations out: the [centre] weight, then [count] taps
 * per side stored as offset and weight pairs in [taps]. Each tap reads between two pixels, so
 * filtering weighs them as the curve does.
 */
internal class BlurTaps {
    var count: Int = 0
        private set
    var centre: Float = 1f
        private set
    val taps: FloatArray = FloatArray(GlassShaders.MaxBlurTaps * 2)

    private var sigma = 0f

    /** Sets the taps for a deviation of [sigma] pixels, no more than [GlassShaders.MaxBlurSigma]; 0 for no blur. */
    fun prepare(sigma: Float) {
        if (sigma == this.sigma) return
        this.sigma = sigma
        taps.fill(0f)
        val radius = if (sigma > 0f) ceil(3f * sigma).toInt() else 0
        if (radius == 0) {
            count = 0
            centre = 1f
            return
        }
        val weight = { x: Int -> if (x > radius) 0f else exp(-(x * x) / (2f * sigma * sigma)) }
        var total = weight(0)
        for (x in 1..radius) total += 2f * weight(x)
        centre = weight(0) / total
        count = 0
        var x = 1
        while (x <= radius && count < GlassShaders.MaxBlurTaps) {
            val near = weight(x)
            val far = weight(x + 1)
            taps[count * 2] = (x * near + (x + 1) * far) / (near + far)
            taps[count * 2 + 1] = (near + far) / total
            count++
            x += 2
        }
    }
}
