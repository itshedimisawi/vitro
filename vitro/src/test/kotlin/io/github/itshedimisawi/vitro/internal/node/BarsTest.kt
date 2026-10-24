package io.github.itshedimisawi.vitro.internal.node

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarsTest {

    private val extent = 1000f
    private val reach = 46f

    private fun bar(top: Float, bottom: Float) = Rect(0f, top, 100f, bottom)

    @Test
    fun glassNearAnEdgeIsABar() {
        val toolbar = bar(20f, 80f)
        val tabBar = bar(920f, 980f)
        val bars = findBars(listOf(toolbar, tabBar), extent, reach, top = 0f, bottom = 0f)
        assertEquals(listOf(toolbar), bars.top)
        assertEquals(listOf(tabBar), bars.bottom)
        assertEquals(80f, bars.covered(isTop = true, given = 0f, extent = extent))
        assertEquals(80f, bars.covered(isTop = false, given = 0f, extent = extent))
    }

    @Test
    fun glassStacksOnBarsAlreadyFound() {
        val tabBar = bar(920f, 980f)
        val player = bar(850f, 900f)
        val bars = findBars(listOf(player, tabBar), extent, reach, top = 0f, bottom = 0f)
        assertEquals(setOf(tabBar, player), bars.bottom.toSet())
        assertEquals(150f, bars.covered(isTop = false, given = 0f, extent = extent))
    }

    @Test
    fun glassTooFarFromTheEdgeIsNotABar() {
        val floating = bar(200f, 260f)
        assertTrue(findBars(listOf(floating), extent, reach, top = 0f, bottom = 0f).isEmpty)
    }

    @Test
    fun coveredHeightLetsGlassBelowTheStatusBarCount() {
        val toolbar = bar(140f, 200f)
        assertTrue(findBars(listOf(toolbar), extent, reach, top = 0f, bottom = 0f).isEmpty)
        val bars = findBars(listOf(toolbar), extent, reach, top = 100f, bottom = 0f)
        assertEquals(listOf(toolbar), bars.top)
    }

    @Test
    fun glassRunningPastTheMiddleIsNotABar() {
        val sheet = bar(0f, 600f)
        assertTrue(findBars(listOf(sheet), extent, reach, top = 0f, bottom = 0f).isEmpty)
    }
}
