package io.github.itshedimisawi.vitro.sample.music

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PromoShape = RoundedCornerShape(22.dp)
private val PromoRed = Brush.verticalGradient(listOf(Color(0xFFEC2A45), Color(0xFFE91E3F), Color(0xFFDD0B31)))
private val FamilyHues = Brush.horizontalGradient(
    0f to Color(0xFFC40A6E),
    0.22f to Color(0xFFE2188A),
    0.5f to Color(0xFFF2203C),
    0.72f to Color(0xFFEE2A1C),
    1f to Color(0xFF8E0808),
)
private val FamilyShade = Brush.verticalGradient(
    0f to Color(0x33200010),
    0.3f to Color.Transparent,
    0.6f to Color.Transparent,
    1f to Color(0x66300008),
)

/** The service's mark: a note beside the word, standing in for a logo. */
@Composable
fun Wordmark(size: TextUnit, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = color, modifier = Modifier.size((size.value * 0.95f).dp))
        Text(text = "Music", color = color, fontSize = size, lineHeight = size, fontWeight = FontWeight.Medium)
    }
}

/** A tall offer card: a headline, a picture in the middle and the terms at the bottom. */
@Composable
fun PromoCard(headline: String, terms: String, isFamily: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(0.801f)
            .clip(PromoShape)
            .background(if (isFamily) FamilyHues else PromoRed),
    ) {
        if (isFamily) {
            Box(Modifier.fillMaxSize().background(FamilyShade))
            Waveform(Modifier.align(BiasAlignment(0f, -0.09f)).fillMaxWidth(0.89f).aspectRatio(2.42f))
            Wordmark(39.sp, Color(0xFFFFD5E0), Modifier.align(BiasAlignment(0f, 0.37f)))
        } else {
            Wordmark(63.sp, Color(0xFFF7EEEA), Modifier.align(BiasAlignment(0f, -0.09f)))
        }
        Text(
            text = headline,
            color = Color.White,
            fontSize = 28.sp,
            lineHeight = 37.5.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 21.dp),
        ) {
            Text(text = "Accept Free Trial", color = Color.White, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(text = terms, color = Color.White, fontSize = 15.sp, lineHeight = 22.sp)
        }
    }
}

// Heights of the bars as a share of the tallest, outer ones first. Bars of the middle group are
// pale and carry a ball just above them.
private val OuterBars = floatArrayOf(0.14f, 0.34f, 0.28f, 0.5f)
private val MiddleBars = floatArrayOf(0.62f, 0.72f, 0.6f, 0.44f, 0.62f, 0.54f)

@Composable
private fun Waveform(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val count = OuterBars.size * 2 + MiddleBars.size
        val pitch = size.width / count
        val thick = pitch * 0.62f
        val base = size.height * 0.78f
        val pale = Brush.verticalGradient(listOf(Color(0xFFFFE9EE), Color(0xFFF58CA6)))
        val deep = Brush.verticalGradient(listOf(Color(0xFFFF3B30), Color(0xFFC00818)))
        fun bar(slot: Int, share: Float, brush: Brush, isCentred: Boolean) {
            val height = (size.height * share).coerceAtLeast(thick)
            val left = pitch * slot + (pitch - thick) / 2f
            val top = if (isCentred) base - size.height * 0.16f - height / 2f else base - height
            drawRoundRect(brush, Offset(left, top), Size(thick, height), CornerRadius(thick / 2f))
        }
        OuterBars.forEachIndexed { index, share ->
            bar(index, share, deep, isCentred = true)
            bar(count - 1 - index, share, deep, isCentred = true)
        }
        MiddleBars.forEachIndexed { index, share ->
            val slot = OuterBars.size + index
            bar(slot, share, pale, isCentred = false)
            val centre = Offset(pitch * (slot + 0.5f), base - size.height * share - thick * 1.05f)
            drawCircle(pale, radius = thick * 0.56f, center = centre)
        }
    }
}

/** A radio station: what its square tile says and in which colour. */
class Station(val name: String, val color: Color, val size: Int, val isSlanted: Boolean = false)

val Stations = listOf(
    Station("1", Color(0xFFE8281E), 84),
    Station("HITS\nHITS\nHITS", Color(0xFF1E6BF0), 25, isSlanted = true),
    Station("COUNTRY", Color(0xFF1838C8), 21),
    Station("MÚSICA\nUNO", Color(0xFFD03A8C), 25, isSlanted = true),
    Station("club", Color(0xFF1C1C1E), 36, isSlanted = true),
    Station("Chill", Color(0xFF2E8CF0), 33),
)

private val StationPaper = Color(0xFFF6F6F6)
private val StationInk = Color(0xFF3A3A3C)

@Composable
private fun RadioCaption(color: Color, size: TextUnit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = StationInk, modifier = Modifier.size((size.value * 1.05f).dp))
        Text(
            text = buildAnnotatedString {
                append("Music")
                withStyle(SpanStyle(color = color)) { append("Radio") }
            },
            color = StationInk,
            fontSize = size,
            lineHeight = size,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun StationName(station: Station, scale: Float) {
    Text(
        text = station.name,
        color = station.color,
        fontSize = (station.size * scale).sp,
        lineHeight = (station.size * scale * 0.94f).sp,
        fontWeight = FontWeight.Black,
        fontStyle = if (station.isSlanted) FontStyle.Italic else FontStyle.Normal,
        textAlign = TextAlign.Center,
        softWrap = false,
    )
}

/** The square tile of a [station]. It is pale in both themes, as printed artwork would be. */
@Composable
fun StationTile(station: Station, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(1f).clip(RoundedCornerShape(22.dp)).background(StationPaper)) {
        Box(Modifier.fillMaxSize().padding(bottom = 22.dp), contentAlignment = Alignment.Center) {
            StationName(station, scale = 1f)
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 9.dp)) { RadioCaption(station.color, 10.sp) }
    }
}

/** A wide card for a live station: its artwork above what is on air. */
@Composable
fun LiveCard(station: Station, onAir: String, show: String, blurb: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp))) {
        Box(Modifier.fillMaxWidth().weight(1f).background(StationPaper)) {
            Box(Modifier.fillMaxSize().padding(bottom = 40.dp), contentAlignment = Alignment.Center) {
                StationName(station, scale = 2.1f)
            }
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) { RadioCaption(station.color, 19.sp) }
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF8B8080)).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text = onAir, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = show, color = Color.White, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
            Text(
                text = blurb,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                minLines = 2,
                maxLines = 2,
            )
        }
    }
}

/** A browsing category: a colour, and the photo washed in it if it has one. */
class Category(val name: String, val color: Color, val photo: Int = -1)

val Categories = listOf(
    Category("Head Trip", Color(0xFFB8501C), 4),
    Category("Concerts", Color(0xFF6A1420), 0),
    Category("Spatial Audio", Color(0xFFE8304A), 3),
    Category("Pop", Color(0xFFE0608C), 2),
    Category("Hits", Color(0xFF8C8A1C)),
    Category("Electronic", Color(0xFFC86A14), 1),
    Category("Focus", Color(0xFF9A5604)),
    Category("Love", Color(0xFFA8A9A9), 4),
    Category("Kids", Color(0xFF21B646), 0),
    Category("Oldies", Color(0xFFDE9B01), 3),
    Category("Family", Color(0xFF22B747), 2),
    Category("Feeling Blue", Color(0xFF2F5491), 1),
    Category("Motivation", Color(0xFF980034)),
    Category("Feel Good", Color(0xFFE0842E), 4),
    Category("Punjabi", Color(0xFF904FA9), 0),
    Category("Original Music", Color(0xFF02782C), 2),
    Category("Alternative", Color(0xFFC6BC02), 3),
    Category("Soul/Funk", Color(0xFF8180EC), 1),
)

// Turns a photo into shades of one colour: black stays dark, white becomes the colour.
private fun wash(color: Color): ColorFilter {
    val red = 0.2126f
    val green = 0.7152f
    val blue = 0.0722f
    fun row(channel: Float) = floatArrayOf(red * channel, green * channel, blue * channel, 0f, 0f)
    return ColorFilter.colorMatrix(
        ColorMatrix(row(color.red * 1.25f) + row(color.green * 1.25f) + row(color.blue * 1.25f) + floatArrayOf(0f, 0f, 0f, 1f, 0f)),
    )
}

@Composable
fun CategoryTile(category: Category, art: List<ImageBitmap>, height: Dp, modifier: Modifier = Modifier) {
    val isDark = LocalMusicColors.current.isDark
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(height)
            .clip(shape)
            .background(category.color)
            .then(if (isDark) Modifier.border(0.5.dp, Color.White.copy(alpha = 0.14f), shape) else Modifier),
    ) {
        if (category.photo >= 0) {
            Image(
                bitmap = art[category.photo],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = remember(category) { wash(category.color) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.3f)),
            ),
        )
        Text(
            text = category.name,
            color = Color.White,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 12.dp, bottom = 11.dp),
        )
    }
}
