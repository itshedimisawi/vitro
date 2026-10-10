package io.github.itshedimisawi.vitro.sample

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.glassEffect

private val Accents = listOf(
    Color(0xFFFF3B30),
    Color(0xFF007AFF),
    Color(0xFF34C759),
    Color(0xFFFF9500),
    Color(0xFFAF52DE),
)

private val Ink = Color(0xFF111111)

@Composable
fun BoxScope.FeedDemo(backdrop: GlassBackdrop) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .glassBackdrop(backdrop)
            .background(Color(0xFFF2F2F7)),
        contentPadding = PaddingValues(start = 16.dp, top = 120.dp, end = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(30) { index ->
            if (index % 3 == 0) PhotoCard(index) else StoryCard(index)
        }
    }

    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Search the feed",
            color = Ink.copy(alpha = 0.7f),
            fontSize = 16.sp,
            modifier = Modifier
                .weight(1f)
                .glassEffect(backdrop)
                .padding(horizontal = 20.dp, vertical = 15.dp),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .glassEffect(backdrop, Glass.Regular.interactive()),
        ) {
            Text("+", color = Ink, fontSize = 24.sp)
        }
    }

    Text(
        text = "Compose",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = 112.dp)
            .glassEffect(backdrop, Glass.Regular.tint(Accents[1]).interactive())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    )
}

@Composable
private fun PhotoCard(index: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp)),
    ) {
        Image(
            painter = painterResource(Photos[index / 3 % Photos.size]),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = "Photo ${index / 3 + 1}",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
        )
    }
}

@Composable
private fun StoryCard(index: Int) {
    val accent = Accents[index % Accents.size]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(width = 56.dp, height = 8.dp).clip(RoundedCornerShape(4.dp)).background(accent))
        Text(
            text = "Headline number ${index + 1}",
            color = Ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Glass floats above this text and bends it at the rim while the list " +
                "scrolls underneath, so sharp type makes the lensing easy to judge.",
            color = Ink.copy(alpha = 0.75f),
            fontSize = 15.sp,
            lineHeight = 21.sp,
        )
    }
}
