package io.github.itshedimisawi.vitro.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassAppearance
import io.github.itshedimisawi.vitro.rememberGlassBackdrop

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { SampleApp() }
    }
}

val Ink = Color(0xFF111111)

fun appearanceFor(isDark: Boolean): GlassAppearance =
    if (isDark) GlassAppearance.Dark else GlassAppearance.Light

enum class Demo(val title: String) {
    Photos("Photos"),
    Feed("Feed"),
    Solid("Solid"),
    Playground("Tune"),
    Patterns("Test"),
}

@Composable
fun SampleApp() {
    var demo by rememberSaveable { mutableStateOf(Demo.Photos) }
    var isSolidLight by rememberSaveable { mutableStateOf(false) }
    val backdrop = rememberGlassBackdrop()
    val isDark = if (demo == Demo.Solid) !isSolidLight else isSystemInDarkTheme()
    val contentColor = if (isDark) Color.White else Ink

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            when (demo) {
                Demo.Photos -> PhotosDemo(backdrop)
                Demo.Feed -> FeedDemo(backdrop)
                Demo.Solid -> SolidDemo(backdrop, isSolidLight) { isSolidLight = !isSolidLight }
                Demo.Playground -> PlaygroundDemo(backdrop)
                Demo.Patterns -> PatternsDemo(backdrop)
            }
            GlassTabBar(
                titles = Demo.entries.map { it.title },
                selected = demo.ordinal,
                onSelect = { demo = Demo.entries[it] },
                backdrop = backdrop,
                glass = Glass.Regular.copy(appearance = appearanceFor(isDark)),
                contentColor = contentColor,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
            )
        }
    }
}
