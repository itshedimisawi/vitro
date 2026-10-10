package io.github.itshedimisawi.vitro.sample

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassAppearance
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.glassEffect

private val PatternImages = listOf(
    R.drawable.pattern_grey_steps,
    R.drawable.pattern_colour_steps,
    R.drawable.pattern_checker_small,
    R.drawable.pattern_checker_large,
    R.drawable.pattern_line_grid,
    R.drawable.pattern_horizontal_bands,
    R.drawable.pattern_vertical_bands,
)

private val PatternColours = listOf(
    Color(0xFFFFFFFF),
    Color(0xFF808080),
    Color(0xFFF09A37),
    Color(0xFFF7CE45),
    Color(0xFF2A611B),
    Color(0xFF2A63F3),
    Color(0xFF000000),
)

@Composable
fun BoxScope.PatternsDemo(backdrop: GlassBackdrop) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val count = PatternImages.size + PatternColours.size
    val advance = Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
    ) { index = (index + 1) % count }

    if (index < PatternImages.size) {
        // Patterns are drawn at three image pixels per dp so their features have a fixed size.
        Image(
            bitmap = ImageBitmap.imageResource(PatternImages[index]),
            contentDescription = null,
            alignment = Alignment.TopStart,
            contentScale = FixedScale(LocalDensity.current.density / 3f),
            filterQuality = FilterQuality.Low,
            modifier = Modifier.fillMaxSize().glassBackdrop(backdrop).then(advance),
        )
    } else {
        Box(
            Modifier
                .fillMaxSize()
                .glassBackdrop(backdrop)
                .background(PatternColours[index - PatternImages.size])
                .then(advance),
        )
    }

    val glass = Glass.Regular.copy(appearance = GlassAppearance.Light)
    Box(Modifier.offset(x = 16.dp, y = 59.dp).size(44.dp).glassEffect(backdrop, glass))
    Box(Modifier.offset(x = 333.33.dp, y = 59.dp).size(44.dp).glassEffect(backdrop, glass))
}
