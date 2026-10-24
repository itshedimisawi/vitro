package io.github.itshedimisawi.vitro.internal.tone

import androidx.compose.ui.geometry.Rect
import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToneTest {

    // Two cells of 10 px side by side: white on the left, black on the right.
    private val split = BackdropTone(columns = 2, rows = 1, cell = 10f, colors = floatArrayOf(1f, 1f, 1f, 0f, 0f, 0f))

    @Test
    fun lightInterfaceTurnsDarkOnlyOverNearBlack() {
        assertEquals(1f, darknessOver(0f, isNight = false))
        assertEquals(1f, darknessOver(DayFlipStart, isNight = false))
        assertEquals(0f, darknessOver(DayFlipEnd, isNight = false))
        assertEquals(0.5f, darknessOver((DayFlipStart + DayFlipEnd) / 2f, isNight = false), 1e-5f)
    }

    @Test
    fun darkInterfaceTurnsLightOnlyOverNearWhite() {
        assertEquals(1f, darknessOver(0.5f, isNight = true))
        assertEquals(1f, darknessOver(NightFlipStart, isNight = true))
        assertEquals(0f, darknessOver(1f, isNight = true))
    }

    @Test
    fun barsRestOnShadeUnlessContentIsNearlyWhite() {
        assertEquals(1f, shadeOver(ShadeFull))
        assertEquals(0f, shadeOver(ShadeNone))
        assertEquals(1f, shadeOver(0f))
    }

    @Test
    fun luminanceAveragesTheCellsInTheArea() {
        assertEquals(1f, split.luminance(Rect(0f, 0f, 10f, 10f), emptyList()), 1e-6f)
        assertEquals(0f, split.luminance(Rect(10f, 0f, 20f, 10f), emptyList()), 1e-6f)
        assertEquals(0.5f, split.luminance(Rect(0f, 0f, 20f, 10f), emptyList()), 1e-6f)
    }

    @Test
    fun luminanceOutsideTheBackdropIsUnknown() {
        assertTrue(split.luminance(Rect(30f, 0f, 40f, 10f), emptyList()).isNaN())
        assertTrue(split.luminance(emptyList()).isNaN())
    }

    @Test
    fun luminanceOfSeveralAreasIsWeightedBySize() {
        val white = Rect(0f, 0f, 10f, 10f)
        val black = Rect(10f, 0f, 20f, 5f)
        assertEquals(2f / 3f, split.luminance(listOf(white, black)), 1e-5f)
    }

    @Test
    fun uncoveredContentDoesNotCountAsBlack() {
        // White on the left, nothing drawn on the right.
        val half = BackdropTone(2, 1, 10f, floatArrayOf(1f, 1f, 1f, 0f, 0f, 0f), floatArrayOf(1f, 0f))
        assertEquals(1f, half.luminance(Rect(0f, 0f, 20f, 10f), emptyList()), 1e-6f)
        assertTrue(half.luminance(Rect(10f, 0f, 20f, 10f), emptyList()).isNaN())
    }

    @Test
    fun partlyCoveredCellsCountByTheirCover() {
        val tone = BackdropTone(2, 1, 10f, floatArrayOf(1f, 1f, 1f, 0f, 0f, 0f), floatArrayOf(0.5f, 1f))
        assertEquals(1f / 3f, tone.luminance(Rect(0f, 0f, 20f, 10f), emptyList()), 1e-5f)
    }

    @Test
    fun readsPremultipliedPixelsRowByRow() {
        // Rows of two pixels, padded to 12 bytes: white at half cover then nothing, then black.
        val pixels = ByteBuffer.wrap(
            byteArrayOf(
                128.toByte(), 128.toByte(), 128.toByte(), 128.toByte(), 0, 0, 0, 0, 9, 9, 9, 9,
                0, 0, 0, 255.toByte(), 0, 0, 0, 255.toByte(), 9, 9, 9, 9,
            ),
        )
        val tone = BackdropTone.read(pixels, width = 2, height = 2, rowStride = 12, cell = 10f)
        assertEquals(1f, tone.luminance(Rect(0f, 0f, 20f, 10f), emptyList()), 1e-6f)
        assertEquals(0f, tone.luminance(Rect(0f, 10f, 20f, 20f), emptyList()), 1e-6f)
        assertEquals(1f / 3f, tone.luminance(Rect(0f, 0f, 10f, 20f), emptyList()), 1e-2f)
    }

    @Test
    fun midGreyGivesOffLessThanHalfTheLightOfWhite() {
        val grey = BackdropTone(columns = 1, rows = 1, cell = 1f, colors = floatArrayOf(0.5f, 0.5f, 0.5f))
        assertEquals(0.2176f, grey.luminance(Rect(0f, 0f, 1f, 1f), emptyList()), 1e-3f)
    }
}
