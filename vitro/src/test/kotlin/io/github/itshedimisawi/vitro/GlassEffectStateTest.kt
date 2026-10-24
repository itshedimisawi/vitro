package io.github.itshedimisawi.vitro

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class GlassEffectStateTest {

    private fun contentColor(appearance: GlassAppearance, tint: Color = Color.Unspecified): Color =
        GlassEffectState(appearance).apply { this.tint = tint }.contentColor

    @Test
    fun contentFollowsTheMaterial() {
        assertEquals(Color.White, contentColor(GlassAppearance.Dark))
        assertEquals(Color.Black, contentColor(GlassAppearance.Light))
    }

    @Test
    fun strongTintSetsTheContentColour() {
        assertEquals(Color.Black, contentColor(GlassAppearance.Dark, Color.White.copy(alpha = 0.9f)))
        assertEquals(Color.White, contentColor(GlassAppearance.Light, Color.Black.copy(alpha = 0.9f)))
        assertEquals(Color.White, contentColor(GlassAppearance.Light, Color(0xFF0A84FF)))
        assertEquals(Color.Black, contentColor(GlassAppearance.Dark, Color(0xFFFFD60A)))
    }

    @Test
    fun faintTintLeavesTheContentToTheMaterial() {
        assertEquals(Color.White, contentColor(GlassAppearance.Dark, Color.White.copy(alpha = 0.3f)))
        assertEquals(Color.Black, contentColor(GlassAppearance.Light, Color.Black.copy(alpha = 0.3f)))
    }
}
