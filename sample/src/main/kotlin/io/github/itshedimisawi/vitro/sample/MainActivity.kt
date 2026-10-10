package io.github.itshedimisawi.vitro.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassEffect
import io.github.itshedimisawi.vitro.rememberGlassBackdrop

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { SampleApp() }
    }
}

val Ink = Color(0xFF111111)

val DarkRegular = Glass.Regular.copy(brightness = -0.8f)

enum class Demo(val title: String) {
    Photos("Photos"),
    Feed("Feed"),
    Solid("Solid"),
    Playground("Tune"),
}

@Composable
fun SampleApp() {
    var demo by rememberSaveable { mutableStateOf(Demo.Photos) }
    var isSolidLight by rememberSaveable { mutableStateOf(false) }
    val backdrop = rememberGlassBackdrop()
    val isDark = demo == Demo.Solid && !isSolidLight
    val contentColor = if (isDark) Color.White else Ink

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            when (demo) {
                Demo.Photos -> PhotosDemo(backdrop)
                Demo.Feed -> FeedDemo(backdrop)
                Demo.Solid -> SolidDemo(backdrop, isSolidLight) { isSolidLight = !isSolidLight }
                Demo.Playground -> PlaygroundDemo(backdrop)
            }
            TabBar(
                backdrop = backdrop,
                glass = if (isDark) DarkRegular else Glass.Regular,
                selected = demo,
                contentColor = contentColor,
                onSelect = { demo = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun TabBar(
    backdrop: GlassBackdrop,
    glass: Glass,
    selected: Demo,
    contentColor: Color,
    onSelect: (Demo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .glassEffect(backdrop, glass)
            .padding(4.dp),
    ) {
        Demo.entries.forEach { demo ->
            val isSelected = demo == selected
            Text(
                text = demo.title,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) contentColor.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(demo) }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            )
        }
    }
}
