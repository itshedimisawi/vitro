package io.github.itshedimisawi.vitro.sample

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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

@Composable
fun BoxScope.PlaygroundDemo(backdrop: GlassBackdrop) {
    var glass by remember { mutableStateOf(Glass.Regular.interactive()) }
    var corner by remember { mutableStateOf(36f) }

    Box(Modifier.fillMaxSize().glassBackdrop(backdrop)) {
        Image(
            painter = painterResource(Photos[2]),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = "The quick brown fox jumps over the lazy dog. ".repeat(6),
            color = Color.White,
            fontSize = 22.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 64.dp, end = 20.dp),
        )
    }

    val position = remember { mutableStateOf(Offset(0f, -420f)) }
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .draggable(position)
            .size(width = 230.dp, height = 140.dp)
            .glassEffect(backdrop, glass, RoundedCornerShape(corner.dp)),
    )

    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 80.dp)
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xE6161618))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Regular") { glass = Glass.Regular.interactive(glass.isInteractive) }
            Chip("Clear") { glass = Glass.Clear.interactive(glass.isInteractive) }
            Chip(if (glass.tint == Color.Unspecified) "Tint" else "Untint") {
                glass = glass.tint(
                    if (glass.tint == Color.Unspecified) Color(0xFF007AFF) else Color.Unspecified,
                )
            }
            Chip(if (glass.dimming > 0f) "Undim" else "Dim") {
                glass = glass.dimmed(if (glass.dimming > 0f) 0f else 0.35f)
            }
        }
        Control("Corner radius", corner, 0f..70f) { corner = it }
        Control("Blur radius", glass.blurRadius.value, 0f..40f) {
            glass = glass.copy(blurRadius = it.dp)
        }
        Control("Refraction height", glass.refractionHeight.value, 0f..48f) {
            glass = glass.copy(refractionHeight = it.dp)
        }
        Control("Refraction amount", glass.refractionAmount.value, 0f..80f) {
            glass = glass.copy(refractionAmount = it.dp)
        }
        Control("Dispersion", glass.dispersion, 0f..0.2f) { glass = glass.copy(dispersion = it) }
        Control("Saturation", glass.saturation, 0f..2.5f) { glass = glass.copy(saturation = it) }
        Control("Brightness", glass.brightness, -0.6f..0.6f) {
            glass = glass.copy(brightness = it)
        }
        Control("Light angle", glass.lightAngle, -180f..180f) {
            glass = glass.copy(lightAngle = it)
        }
        Control("Highlight", glass.highlightIntensity, 0f..2f) {
            glass = glass.copy(highlightIntensity = it)
        }
        Control("Shadow radius", glass.shadowRadius.value, 0f..40f) {
            glass = glass.copy(shadowRadius = it.dp)
        }
        Control("Shadow opacity", glass.shadowOpacity, 0f..0.6f) {
            glass = glass.copy(shadowOpacity = it)
        }
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun Control(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label  ${"%.2f".format(value)}",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.width(150.dp),
        )
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
