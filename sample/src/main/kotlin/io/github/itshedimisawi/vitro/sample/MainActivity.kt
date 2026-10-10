package io.github.itshedimisawi.vitro.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.graphics.vector.ImageVector
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
val Accent = Color(0xFF0088FF)

fun appearanceFor(isDark: Boolean): GlassAppearance =
    if (isDark) GlassAppearance.Dark else GlassAppearance.Light

enum class Demo(val title: String, val icon: ImageVector) {
    Photos("Photos", Icons.Rounded.PhotoLibrary),
    Showcase("Showcase", Icons.Rounded.AutoAwesome),
    Solid("Solid", Icons.Rounded.Contrast),
    Playground("Tune", Icons.Rounded.Tune),
    Patterns("Test", Icons.Rounded.Science),
}

private val Tabs = Demo.entries.map { GlassTab(it.title, it.icon, badge = if (it == Demo.Patterns) 1 else 0) }

@Composable
fun SampleApp() {
    var demo by rememberSaveable { mutableStateOf(Demo.Photos) }
    var isSolidLight by rememberSaveable { mutableStateOf(false) }
    val backdrop = rememberGlassBackdrop()
    // The solid screen has its own light and dark switch; everywhere else the bar is left to
    // pick its material from the theme and what scrolls under it.
    val barGlass = if (demo == Demo.Solid) {
        Glass.Regular.copy(appearance = appearanceFor(!isSolidLight))
    } else {
        Glass.Regular
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            when (demo) {
                Demo.Photos -> PhotosDemo(backdrop)
                Demo.Showcase -> ShowcaseDemo(backdrop)
                Demo.Solid -> SolidDemo(backdrop, isSolidLight) { isSolidLight = !isSolidLight }
                Demo.Playground -> PlaygroundDemo(backdrop)
                Demo.Patterns -> PatternsDemo(backdrop)
            }
            GlassTabBar(
                tabs = Tabs,
                selected = demo.ordinal,
                onSelect = { demo = Demo.entries[it] },
                backdrop = backdrop,
                glass = barGlass,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
            )
        }
    }
}
