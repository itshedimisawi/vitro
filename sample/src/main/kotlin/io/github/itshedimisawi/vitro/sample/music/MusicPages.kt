package io.github.itshedimisawi.vitro.sample.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicVideo
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.itshedimisawi.vitro.sample.tappable

private val GridGap = 12.dp
private val RowHeight = 54.dp
private val RowTextStart = 61.dp

/** The frame every page shares: it scrolls, starts under the status bar and ends clear of the controls. */
@Composable
private fun Page(scroll: ScrollState, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = ControlsRoom),
        content = content,
    )
}

/** A page's large title with the account picture across from it. */
@Composable
private fun TitleRow(title: String) {
    val colors = LocalMusicColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = ScreenMargin),
    ) {
        LargeTitle(title, Modifier.weight(1f))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .background(
                    Brush.verticalGradient(
                        if (colors.isDark) {
                            listOf(Color(0xFF4A445C), Color(0xFF312747))
                        } else {
                            listOf(Color(0xFF96ACD0), Color(0xFF737EB3))
                        },
                    ),
                    CircleShape,
                ),
        ) {
            Text(text = "HM", color = Color.White.copy(alpha = 0.92f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LargeTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = modifier)
}

@Composable
private fun SectionTitle(text: String, hasMore: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = ScreenMargin)) {
        Text(text = text, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (hasMore) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = LocalMusicColors.current.quiet,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
fun HomePage(scroll: ScrollState) {
    Page(scroll) {
        TitleRow("Home")
        Spacer(Modifier.height(11.dp))
        PromoCard(
            headline = "Music just for you.\n1 month free.",
            terms = "1 month free, then US\$5,49/month.",
            isFamily = false,
            modifier = Modifier.padding(horizontal = ScreenMargin),
        )
        Spacer(Modifier.height(19.dp))
        PromoCard(
            headline = "Music for the whole\nfamily. 1 month free.",
            terms = "1 month free, then US\$8,99/month",
            isFamily = true,
            modifier = Modifier.padding(horizontal = ScreenMargin),
        )
    }
}

private class GenreStation(val name: String, val from: Color, val to: Color)

private val GenreStations = listOf(
    GenreStation("Pop", Color(0xFFF0308C), Color(0xFFB0185C)),
    GenreStation("Hip-Hop", Color(0xFF5A5CE0), Color(0xFF3A2CA8)),
    GenreStation("Dance", Color(0xFF22B45A), Color(0xFF0A7C3C)),
    GenreStation("Rock", Color(0xFFE8782A), Color(0xFFB03A14)),
)

@Composable
fun RadioPage(scroll: ScrollState) {
    val colors = LocalMusicColors.current
    Page(scroll) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = ScreenMargin)
                .fillMaxWidth()
                .height(55.dp)
                .background(Color(0xFFFA2C47), CircleShape)
                .padding(start = 18.dp, end = 12.dp),
        ) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(text = "Music", color = Color.White, fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "Get 1 month on us.", color = Color.White, fontSize = 15.sp, lineHeight = 19.sp)
            }
            Box(Modifier.size(30.dp).background(Color(0xFFCB5F7F), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(39.dp))
        TitleRow("Radio")
        Spacer(Modifier.height(20.dp))
        Stations.chunked(3).forEachIndexed { index, row ->
            if (index > 0) Spacer(Modifier.height(GridGap))
            Row(
                horizontalArrangement = Arrangement.spacedBy(GridGap),
                modifier = Modifier.padding(horizontal = ScreenMargin),
            ) {
                row.forEach { StationTile(it, Modifier.weight(1f)) }
            }
        }

        Spacer(Modifier.height(31.dp))
        SectionTitle("Listen to Live Radio for Free")
        Spacer(Modifier.height(15.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(GridGap),
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = ScreenMargin),
        ) {
            val card = Modifier.width(325.dp).height(321.dp)
            LiveCard(Stations[0], "MUSIC 1 · 10 – 11:30 PM", "Music 1", "New music from around the world, live all day.", card)
            LiveCard(Stations[1], "MUSIC HITS · 10 – 12 AM", "Music Hits", "Songs you know by heart, from the eighties to now.", card)
            LiveCard(Stations[2], "MUSIC COUNTRY · LIVE", "Music Country", "Where the story of country music is told.", card)
        }

        Spacer(Modifier.height(30.dp))
        SectionTitle("Stations by Genre", hasMore = true)
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(GridGap),
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = ScreenMargin),
        ) {
            GenreStations.forEach { station ->
                Column(Modifier.width(157.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.verticalGradient(listOf(station.from, station.to))),
                    ) {
                        Text(
                            text = station.name,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(text = "${station.name} Station", fontSize = 13.sp, lineHeight = 17.sp, maxLines = 1)
                    Text(text = "Music ${station.name}", color = colors.quiet, fontSize = 13.sp, lineHeight = 17.sp, maxLines = 1)
                }
            }
        }
    }
}

private class Shelf(val name: String, val icon: ImageVector)

private val Shelves = listOf(
    Shelf("Playlists", Icons.AutoMirrored.Rounded.QueueMusic),
    Shelf("Artists", Icons.Rounded.Mic),
    Shelf("Albums", Icons.Rounded.Album),
    Shelf("Songs", Icons.Rounded.MusicNote),
    Shelf("TV & Movies", Icons.Rounded.Tv),
    Shelf("Music Videos", Icons.Rounded.MusicVideo),
    Shelf("Genres", Icons.Rounded.GraphicEq),
    Shelf("Compilations", Icons.Rounded.Groups),
    Shelf("Composers", Icons.Rounded.Piano),
)

private class Record(val title: String, val artist: String)

private val Records = listOf(
    Record("Night Drive", "Aster Lane"),
    Record("Paper Moons", "Hollow Coast"),
    Record("Low Tide", "Marlo Vance"),
    Record("Afterglow (Deluxe)", "The Quiet Hours"),
    Record("Signal Fires", "North Arcade"),
    Record("Glasshouse", "Ines Maro"),
    Record("Slow Weather", "Fenwick"),
    Record("Half Light", "Juno Reyes"),
)

@Composable
fun LibraryPage(scroll: ScrollState, art: List<ImageBitmap>) {
    val colors = LocalMusicColors.current
    Page(scroll) {
        // The back button floats over this gap.
        Spacer(Modifier.height(59.dp))
        LargeTitle("Downloaded", Modifier.padding(horizontal = ScreenMargin))
        Spacer(Modifier.height(6.dp))
        Shelves.forEach { shelf ->
            Box(Modifier.fillMaxWidth().height(RowHeight).tappable {}) {
                Icon(
                    imageVector = shelf.icon,
                    contentDescription = null,
                    tint = colors.red,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp).size(28.dp),
                )
                Text(
                    text = shelf.name,
                    fontSize = 19.sp,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = RowTextStart),
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = colors.chevron,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 15.dp).size(22.dp),
                )
                Spacer(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = RowTextStart, end = 16.dp)
                        .fillMaxWidth()
                        .height(0.7.dp)
                        .background(colors.divider),
                )
            }
        }

        Spacer(Modifier.height(26.dp))
        SectionTitle("Recently Downloaded")
        Spacer(Modifier.height(14.dp))
        Records.chunked(2).forEachIndexed { row, pair ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(GridGap),
                modifier = Modifier.padding(horizontal = ScreenMargin),
            ) {
                pair.forEachIndexed { column, record ->
                    Column(Modifier.weight(1f)) {
                        Image(
                            bitmap = art[(row * 2 + column) % art.size],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(6.dp)),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(text = record.title, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = record.artist,
                            color = colors.quiet,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SearchPage(scroll: ScrollState, art: List<ImageBitmap>, query: String) {
    var hasOffer by rememberSaveable { mutableStateOf(true) }
    val found = Categories.filter { it.name.contains(query.trim(), ignoreCase = true) }
    Page(scroll) {
        TitleRow("Search")
        Spacer(Modifier.height(17.dp))
        if (hasOffer && query.isBlank()) {
            OfferCard(onClose = { hasOffer = false })
            Spacer(Modifier.height(14.dp))
        }
        found.chunked(2).forEachIndexed { index, pair ->
            if (index > 0) Spacer(Modifier.height(GridGap))
            Row(
                horizontalArrangement = Arrangement.spacedBy(GridGap),
                modifier = Modifier.padding(horizontal = ScreenMargin),
            ) {
                pair.forEach { CategoryTile(it, art, height = 96.dp, modifier = Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OfferCard(onClose: () -> Unit) {
    val colors = LocalMusicColors.current
    val shape = RoundedCornerShape(14.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = ScreenMargin)
            .fillMaxWidth()
            .shadow(if (colors.isDark) 0.dp else 10.dp, shape, ambientColor = Color(0x40000000), spotColor = Color(0x40000000))
            .clip(shape)
            .background(colors.card),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.6f).background(Color(0xFFF3152E))) {
            Wordmark(67.sp, Color(0xFFF7EEEA), Modifier.align(Alignment.Center))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(44.dp)
                    .tappable(onClose),
            ) {
                Box(Modifier.size(16.dp).background(Color.Black.copy(alpha = 0.28f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(11.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(text = "Get 1 month of free music.", fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Plus your entire music library on all your devices. 1 month free then US\$5,49/month.",
            fontSize = 14.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        // The button is 32 high in a 44 high touch target, so the gaps around it are 6 smaller.
        Spacer(Modifier.height(4.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(width = 120.dp, height = 44.dp).tappable {},
        ) {
            Box(Modifier.size(width = 120.dp, height = 32.dp).background(colors.red, CircleShape), contentAlignment = Alignment.Center) {
                Text(text = "Try It Free", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(text = "See All Plans", color = colors.red, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(15.dp))
    }
}
