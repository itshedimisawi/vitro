package io.github.itshedimisawi.vitro

import org.junit.Assert.assertEquals
import org.junit.Test

class ValueClassesTest {

    @Test
    fun appearancesAreDistinctAndNamed() {
        val all = listOf(GlassAppearance.Automatic, GlassAppearance.Light, GlassAppearance.Dark)
        assertEquals(3, all.toSet().size)
        assertEquals(listOf("Automatic", "Light", "Dark"), all.map { it.toString() })
    }

    @Test
    fun scrollEdgeStylesAreDistinctAndNamed() {
        val all = listOf(ScrollEdgeEffectStyle.Automatic, ScrollEdgeEffectStyle.Soft, ScrollEdgeEffectStyle.Shaded)
        assertEquals(3, all.toSet().size)
        assertEquals(listOf("Automatic", "Soft", "Shaded"), all.map { it.toString() })
    }
}
