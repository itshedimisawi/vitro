package io.github.itshedimisawi.vitro.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.rememberGlassBackdrop

private val SeatPitch = 28.dp

/** Picking a show and seats, with the price of the selection and the way to buy it. */
@Composable
fun SeatsScreen(
    day: Int,
    onDay: (Int) -> Unit,
    time: Int,
    onTime: (Int) -> Unit,
    seats: List<Int>,
    onSeat: (Int) -> Unit,
    onBack: () -> Unit,
    onBuy: () -> Unit,
) {
    val backdrop = rememberGlassBackdrop()

    Box(Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .glassBackdrop(backdrop)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(top = 68.dp, bottom = 120.dp),
        ) {
            ChipRow(showDays(), day, onDay, backdrop)
            Spacer(Modifier.height(12.dp))
            ChipRow(ShowTimes, time, onTime, backdrop)
            Spacer(Modifier.height(96.dp))
            CinemaScreen()
            Spacer(Modifier.height(56.dp))
            SeatMap(seats, onSeat)
        }

        Box(Modifier.statusBarsPadding().fillMaxWidth().height(60.dp)) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                label = "Back",
                onClick = onBack,
                backdrop = backdrop,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = Margin),
            )
            Text(Movie.Title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = Margin, vertical = 8.dp),
        ) {
            Column(Modifier.padding(end = 20.dp)) {
                Text("$${seats.size * SeatPrice}", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (seats.isEmpty()) "Pick your seats" else ticketCount(seats.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
            GlassTextButton(
                text = "Buy",
                onClick = onBuy,
                backdrop = backdrop,
                enabled = seats.isNotEmpty(),
                modifier = Modifier.weight(1f).height(ActionHeight),
            )
        }
    }
}

@Composable
private fun ChipRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, backdrop: GlassBackdrop) {
    // The selection is a pill of glass in the text colour, as iOS marks a selection.
    val colors = MaterialTheme.colorScheme
    val selectedGlass = Glass.Regular.tint(colors.onBackground.copy(alpha = 0.9f)).interactive()
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Margin),
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            GlassSurface(
                backdrop = backdrop,
                glass = if (isSelected) selectedGlass else Glass.Regular.interactive(),
                contentColor = if (isSelected) colors.background else Color.Unspecified,
                modifier = Modifier.height(ControlSize),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(ControlSize)
                        .tappable { onSelect(index) }
                        .padding(horizontal = 16.dp),
                ) {
                    Text(label, fontSize = 15.sp, fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal)
                }
            }
        }
    }
}

/** The screen of the hall: a lit curve with its glow falling toward the seats. */
@Composable
private fun CinemaScreen() {
    val glow = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth(0.76f).height(64.dp)) {
        val edge = 18.dp.toPx()
        val curve = Path().apply {
            moveTo(0f, edge)
            quadraticTo(size.width / 2f, -edge / 2f, size.width, edge)
        }
        val below = Path().apply {
            addPath(curve)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        clipPath(below) {
            scale(scaleX = 1f, scaleY = 0.3f, pivot = Offset(size.width / 2f, edge / 2f)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(glow.copy(alpha = 0.6f), Color.Transparent),
                        center = Offset(size.width / 2f, edge / 2f),
                        radius = size.width / 2f,
                    ),
                    radius = size.width / 2f,
                    center = Offset(size.width / 2f, edge / 2f),
                )
            }
        }
        drawPath(curve, glow, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun SeatMap(seats: List<Int>, onSeat: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        val pitch = min(SeatPitch, maxWidth / SeatRows.max())
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SeatRows.forEachIndexed { index, count ->
                val row = index + 1
                Row {
                    for (number in 1..count) {
                        val id = seatId(row, number)
                        Seat(row, number, isTaken(row, number), id in seats, pitch) { onSeat(id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Seat(row: Int, number: Int, isTaken: Boolean, isSelected: Boolean, pitch: Dp, onClick: () -> Unit) {
    val tap = if (isTaken) Modifier else Modifier.tappable(onClick)
    val description = Modifier.clearAndSetSemantics {
        contentDescription = "Row $row, seat $number"
        selected = isSelected
        if (isTaken) disabled()
    }
    val colors = MaterialTheme.colorScheme
    val page = colors.background
    val (fill, outline) = when {
        isSelected -> colors.primary to colors.secondary
        isTaken -> colors.secondary.copy(alpha = 0.2f).compositeOver(page) to colors.secondary.copy(alpha = 0.35f).compositeOver(page)
        else -> Color.Transparent to colors.secondary
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(pitch, pitch + 1.dp).then(tap).then(description)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(pitch * 0.72f, pitch * 0.5f)
                .drawBehind {
                    val line = 1.5.dp.toPx()
                    val corner = CornerRadius(3.dp.toPx())
                    drawRoundRect(fill, cornerRadius = corner)
                    // The back and armrests: an outline open at the top.
                    val half = line / 2f
                    val arms = Path().apply {
                        moveTo(half, 0f)
                        lineTo(half, size.height - corner.x)
                        quadraticTo(half, size.height - half, corner.x, size.height - half)
                        lineTo(size.width - corner.x, size.height - half)
                        quadraticTo(size.width - half, size.height - half, size.width - half, size.height - corner.x)
                        lineTo(size.width - half, 0f)
                    }
                    drawPath(arms, outline, style = Stroke(line, cap = StrokeCap.Round))
                    if (!isSelected) {
                        drawRoundRect(
                            color = outline.copy(alpha = 0.55f),
                            topLeft = Offset(line * 2f, size.height * 0.45f),
                            size = Size(size.width - line * 4f, size.height * 0.3f),
                            cornerRadius = CornerRadius(1.dp.toPx()),
                        )
                    }
                },
        ) {
            if (isSelected) Text("$number", color = Color.White, fontSize = 8.sp, lineHeight = 8.sp)
        }
    }
}
