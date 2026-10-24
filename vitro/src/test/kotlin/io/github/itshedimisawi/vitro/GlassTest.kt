package io.github.itshedimisawi.vitro

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassTest {

    @Test
    fun regularKeepsCalibratedDefaults() {
        val glass = Glass.Regular
        assertEquals(2.5.dp, glass.blurRadius)
        assertEquals(12.dp, glass.refractionHeight)
        assertEquals(26.dp, glass.refractionAmount)
        assertEquals(1.1f, glass.saturation)
        assertEquals(0.5f, glass.brightness)
        assertEquals(GlassAppearance.Automatic, glass.appearance)
        assertEquals(Color.Unspecified, glass.tint)
        assertEquals(30.dp, glass.shadowRadius)
        assertEquals(8.dp, glass.shadowOffset)
        assertEquals(0.16f, glass.shadowOpacity)
        assertFalse(glass.isInteractive)
        assertFalse(glass.isIdentity)
    }

    @Test
    fun clearDiffersFromRegularOnlyInBlurSaturationAndBrightness() {
        val expected = Glass.Regular.copy(blurRadius = 2.dp, saturation = 1f, brightness = 0.1f)
        assertEquals(expected, Glass.Clear)
    }

    @Test
    fun buildersChangeOnlyTheirProperty() {
        assertEquals(Glass.Regular.copy(tint = Color.Red), Glass.Regular.tint(Color.Red))
        assertEquals(Glass.Regular.copy(isInteractive = true), Glass.Regular.interactive())
        assertEquals(Glass.Regular, Glass.Regular.interactive().interactive(false))
        assertEquals(Glass.Regular.copy(dimming = 0.35f), Glass.Regular.dimmed())
    }

    @Test
    fun equalGlassHasEqualHashCode() {
        val first = Glass.Clear.copy(dispersion = 0.05f)
        val second = Glass.Clear.copy(dispersion = 0.05f)
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, Glass.Clear)
    }

    @Test
    fun identityIsDistinctAndSurvivesBuilders() {
        assertTrue(Glass.Identity.isIdentity)
        assertNotEquals(Glass.Regular, Glass.Identity)
        assertTrue(Glass.Identity.tint(Color.Blue).isIdentity)
        assertTrue(Glass.Identity.interactive().isIdentity)
        assertTrue(Glass.Identity.copy(blurRadius = 10.dp).isIdentity)
        assertEquals("Glass.Identity", Glass.Identity.toString())
    }
}
