package io.github.itshedimisawi.vitro.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.rememberGlassBackdrop
import kotlin.random.Random

private val Notch = 14.dp

/** The bought ticket, ready to show at the door. */
@Composable
fun TicketScreen(date: String, time: String, seats: String, onBack: () -> Unit) {
    val backdrop = rememberGlassBackdrop()

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .glassBackdrop(backdrop)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = Margin, end = Margin, top = 60.dp, bottom = 8.dp),
        ) {
            Ticket(date, time, seats)
            Spacer(Modifier.height(32.dp))
            GlassTextButton(
                text = "Add to Wallet",
                onClick = {},
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth().height(ActionHeight),
            )
        }

        Row(Modifier.statusBarsPadding().padding(horizontal = Margin, vertical = 4.dp)) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBackIos, "Back", onBack, backdrop)
            Spacer(Modifier.weight(1f))
            GlassIconButton(Icons.Rounded.IosShare, "Share", {}, backdrop)
        }
    }
}

@Composable
private fun Ticket(date: String, time: String, seats: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface)) {
        Image(
            painter = painterResource(R.drawable.severance),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(0f, 0.05f),
            modifier = Modifier.fillMaxWidth().height(128.dp),
        )
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 8.dp)) {
            Text(Movie.Title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(Movie.Details, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.padding(top = 24.dp)) {
                Field("Date", date)
                Field("Time", time)
            }
            Row(Modifier.padding(top = 16.dp)) { Field("Seats", seats) }
            Row(Modifier.padding(top = 24.dp)) {
                Field("Place", Movie.Place)
                Field("Order", Movie.Order)
            }
        }
        Perforation()
        Barcode(Movie.Order, Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 20.dp).height(88.dp))
    }
}

@Composable
private fun RowScope.Field(label: String, value: String) {
    Column(Modifier.weight(1f)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, fontSize = 19.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

/** The tear line: a dashed rule between two notches cut by the page behind the ticket. */
@Composable
private fun Perforation() {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .height(Notch * 2)
            .drawBehind {
                val middle = size.height / 2f
                val radius = Notch.toPx()
                drawLine(
                    color = colors.surfaceVariant,
                    start = Offset(radius + 6.dp.toPx(), middle),
                    end = Offset(size.width - radius - 6.dp.toPx(), middle),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                )
                drawCircle(colors.background, radius, Offset(0f, middle))
                drawCircle(colors.background, radius, Offset(size.width, middle))
            },
    )
}

/** Bars of one to four units, alternating with gaps, laid out the same way for the same [code]. */
@Composable
private fun Barcode(code: String, modifier: Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val random = Random(code.hashCode())
        val widths = generateSequence { 1 + random.nextInt(4) }.take(96).toList()
        val unit = size.width / widths.sum()
        var x = 0f
        widths.forEachIndexed { index, units ->
            val width = units * unit
            if (index % 2 == 0) drawRect(ink, Offset(x, 0f), Size(width, size.height))
            x += width
        }
    }
}
