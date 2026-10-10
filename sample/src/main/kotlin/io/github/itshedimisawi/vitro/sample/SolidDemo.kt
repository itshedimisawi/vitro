package io.github.itshedimisawi.vitro.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.glassEffect

private val DarkBands = listOf(
    Color(0xFF000000),
    Color(0xFF2A611B),
    Color(0xFF2A63F3),
    Color(0xFFD32E21),
    Color(0xFF1C1C1E),
    Color(0xFF5E1A8A),
)

private val LightBands = listOf(
    Color(0xFFFFFFFF),
    Color(0xFFF2F2F7),
    Color(0xFFFFD60A),
    Color(0xFF64D2FF),
    Color(0xFFFF9F0A),
    Color(0xFF30D158),
)

private val Tints = listOf(
    Color(0xFF007AFF),
    Color(0xFF34C759),
    Color(0xFFFF3B30),
    Color(0xFFFF9500),
)

@Composable
fun BoxScope.SolidDemo(backdrop: GlassBackdrop, isLight: Boolean, onToggle: () -> Unit) {
    val bands = if (isLight) LightBands else DarkBands
    val ink = if (isLight) Ink else Color.White
    val regular = Glass.Regular.copy(appearance = appearanceFor(!isLight))

    Column(Modifier.fillMaxSize().glassBackdrop(backdrop)) {
        bands.forEachIndexed { index, color ->
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxWidth().weight(1f).background(color),
            ) {
                Text(
                    text = "BAND ${index + 1}  SOLID COLOUR",
                    color = ink.copy(alpha = 0.5f),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }

    Text(
        text = if (isLight) "Switch to dark" else "Switch to light",
        color = ink,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 12.dp)
            .glassEffect(backdrop, regular.interactive())
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    )

    val position = remember { mutableStateOf(Offset.Zero) }
    Column(
        modifier = Modifier.align(Alignment.Center).draggable(position),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(44.dp).glassEffect(backdrop, regular))
            Label("Regular", ink, Modifier.glassEffect(backdrop, regular))
            Box(Modifier.size(44.dp).glassEffect(backdrop, regular))
        }
        Label("Clear", ink, Modifier.glassEffect(backdrop, Glass.Clear))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 240.dp, height = 130.dp)
                .glassEffect(backdrop, regular, RoundedCornerShape(32.dp)),
        ) {
            Text("Rounded panel", color = ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Tints.forEach { tint ->
                Box(
                    Modifier
                        .size(56.dp)
                        .glassEffect(backdrop, Glass.Regular.tint(tint).interactive()),
                )
            }
        }
    }
}

@Composable
private fun Label(text: String, color: Color, modifier: Modifier) {
    Text(
        text = text,
        color = color,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(horizontal = 36.dp, vertical = 11.dp),
    )
}
