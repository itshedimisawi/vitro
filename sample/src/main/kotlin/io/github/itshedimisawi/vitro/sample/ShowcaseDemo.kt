package io.github.itshedimisawi.vitro.sample

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassBackdrop

private val HeaderHeight = 430.dp
private val ScreenMargin = 20.dp
private val ToolbarMargin = 16.dp
private val AlertBlur = 24.dp
private val Destructive = Color(0xFFFF4245)

private class Track(val title: String, val artist: String, val art: Int)

private val Tracks = listOf(
    Track("Low Tide", "Marlow Reyes", 0),
    Track("Paper Lanterns", "The Sundials", 1),
    Track("Granite and Pine", "Ada Lindqvist", 2),
    Track("Northbound", "Kestrel", 3),
    Track("Morning Ferry", "Odile Marchetti", 4),
    Track("Slow Glass", "Marlow Reyes", 2),
    Track("Tidewater", "The Sundials", 0),
    Track("Harbour Lights", "Kestrel", 1),
)

/**
 * A playlist screen put together from the iPhone examples in Apple's interface guidelines: a
 * toolbar of grouped glass buttons, a search field, clear glass over a photo, an alert, a
 * tinted action and a player resting above the tab bar.
 */
@Composable
fun BoxScope.ShowcaseDemo(backdrop: GlassBackdrop) {
    val isDark = isSystemInDarkTheme()
    // Left automatic, so each piece picks its material from what is under it.
    val glass = Glass.Regular
    val ink = if (isDark) Color.White else Ink
    val focus = LocalFocusManager.current
    val scroll = rememberScrollState()
    val art = rememberArtwork()

    var query by rememberSaveable { mutableStateOf("") }
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var isAlertShown by rememberSaveable { mutableStateOf(false) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var playing by rememberSaveable { mutableIntStateOf(-1) }
    val dim by animateFloatAsState(if (isAlertShown) 0.25f else 0f, label = "dim")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .glassBackdrop(backdrop)
            .background(if (isDark) Color.Black else Color(0xFFF2F2F7))
            .drawWithContent {
                drawContent()
                drawRect(Color.Black, alpha = dim)
            }
            .verticalScroll(scroll),
    ) {
        Header()
        Column(
            modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            val shown = Tracks.withIndex().filter { (_, track) ->
                track.title.contains(query, ignoreCase = true) || track.artist.contains(query, ignoreCase = true)
            }
            SectionTitle(if (query.isEmpty()) "Songs" else "Results", ink)
            Card(isDark) {
                if (shown.isEmpty()) {
                    Text(
                        text = "No songs match “$query”.",
                        color = ink.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(18.dp),
                    )
                }
                shown.forEachIndexed { row, (index, track) ->
                    TrackRow(
                        track = track,
                        art = art[track.art],
                        ink = ink,
                        isCurrent = index == playing,
                        isEditing = isEditing,
                        hasDivider = row < shown.lastIndex,
                        onClick = {
                            playing = index
                            isPlaying = true
                        },
                    )
                }
            }
            SectionTitle("More like this", ink)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                art.forEachIndexed { index, picture ->
                    Column(Modifier.width(150.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Image(
                            bitmap = picture,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(150.dp).clip(RoundedCornerShape(20.dp)),
                        )
                        Text("Mix ${index + 1}", color = ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Card(isDark) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("About this playlist", color = ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Glass floats above this text and bends it at the rim while the page " +
                            "scrolls underneath, so sharp type makes the lensing easy to judge.",
                        color = ink.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                    )
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(190.dp))
        }
    }

    // Clear glass riding on the photo. It scrolls with the page from outside it, because glass
    // cannot be part of the content it looks through.
    val headerEnd = with(LocalDensity.current) { HeaderHeight.roundToPx() }
    if (scroll.value < headerEnd) {
        val clear = Glass.Clear.dimmed()
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .offset { IntOffset(0, -scroll.value) }
                .padding(start = ScreenMargin, top = HeaderHeight - ControlSize - 22.dp, end = ScreenMargin)
                .fillMaxWidth(),
        ) {
            GlassTextButton(
                text = "Play",
                icon = Icons.Rounded.PlayArrow,
                onClick = {
                    if (playing < 0) playing = 0
                    isPlaying = true
                },
                backdrop = backdrop,
                glass = clear,
                contentColor = Color.White,
            )
            GlassTextButton(
                text = "Shuffle",
                icon = Icons.Rounded.Shuffle,
                onClick = {
                    playing = (playing + 3).mod(Tracks.size)
                    isPlaying = true
                },
                backdrop = backdrop,
                glass = clear,
                contentColor = Color.White,
            )
            Spacer(Modifier.weight(1f))
            GlassIconButton(Icons.Rounded.FavoriteBorder, "Favourite", {}, backdrop, clear, contentColor = Color.White)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(horizontal = ToolbarMargin, vertical = 8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.Rounded.ArrowBackIosNew, "Back", {}, backdrop, glass)
            Spacer(Modifier.weight(1f))
            if (!isEditing) {
                GlassTextButton("Edit", { isEditing = true }, backdrop, glass)
            }
            GlassButtonGroup(backdrop, glass) {
                GlassGroupItem(Icons.Rounded.IosShare, "Share") {}
                GlassGroupItem(Icons.Rounded.MoreHoriz, "More") { isAlertShown = true }
            }
            if (isEditing) {
                GlassIconButton(
                    icon = Icons.Rounded.Check,
                    label = "Done",
                    onClick = { isEditing = false },
                    backdrop = backdrop,
                    glass = glass.tint(Accent),
                    contentColor = Color.White,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.Rounded.FilterList, "Filter", {}, backdrop, glass)
            GlassSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search",
                backdrop = backdrop,
                glass = glass,
                modifier = Modifier.weight(1f),
            )
            if (query.isNotEmpty()) {
                GlassIconButton(
                    icon = Icons.Rounded.Close,
                    label = "Clear",
                    onClick = {
                        query = ""
                        focus.clearFocus()
                    },
                    backdrop = backdrop,
                    glass = glass,
                )
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(start = ScreenMargin, end = ScreenMargin, bottom = 84.dp),
    ) {
        GlassTextButton(
            text = "Add Music",
            icon = Icons.Rounded.Add,
            onClick = {},
            backdrop = backdrop,
            glass = glass.tint(Accent),
            contentColor = Color.White,
        )
        Player(
            track = Tracks.getOrNull(playing),
            art = art,
            isPlaying = isPlaying,
            onToggle = {
                if (playing < 0) playing = 0
                isPlaying = !isPlaying
            },
            onNext = { playing = (playing + 1).mod(Tracks.size) },
            backdrop = backdrop,
            glass = glass,
        )
    }

    if (isAlertShown) {
        Box(Modifier.fillMaxSize().tappable { isAlertShown = false })
        Alert(
            onKeep = { isAlertShown = false },
            onRemove = {
                isAlertShown = false
                playing = -1
                isPlaying = false
            },
            backdrop = backdrop,
            glass = glass,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun Header() {
    Box(Modifier.fillMaxWidth().height(HeaderHeight)) {
        Image(
            painter = painterResource(Photos[3]),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f))),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = ScreenMargin, end = ScreenMargin, bottom = ControlSize + 40.dp),
        ) {
            Text("Slow Glass", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Playlist · ${Tracks.size} songs",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, ink: Color) {
    Text(text = text, color = ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun Card(isDark: Boolean, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(if (isDark) Color(0xFF1C1C1E) else Color.White),
    ) {
        content()
    }
}

@Composable
private fun TrackRow(
    track: Track,
    art: ImageBitmap,
    ink: Color,
    isCurrent: Boolean,
    isEditing: Boolean,
    hasDivider: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().tappable(onClick).padding(start = 14.dp),
    ) {
        Image(
            bitmap = art,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
        )
        Box(Modifier.padding(start = 12.dp).weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp, end = 14.dp, bottom = 12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = if (isCurrent) Accent else ink,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(track.artist, color = ink.copy(alpha = 0.6f), fontSize = 14.sp, maxLines = 1)
                }
                if (isEditing) {
                    Icon(Icons.Rounded.RemoveCircle, "Remove", tint = Destructive, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Rounded.MoreHoriz, null, tint = ink.copy(alpha = 0.45f), modifier = Modifier.size(22.dp))
                }
            }
            if (hasDivider) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(ink.copy(alpha = 0.14f)),
                )
            }
        }
    }
}

@Composable
private fun Player(
    track: Track?,
    art: List<ImageBitmap>,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    backdrop: GlassBackdrop,
    glass: Glass,
) = GlassSurface(backdrop, glass.interactive(), Modifier.fillMaxWidth().height(50.dp)) {
    val ink = LocalContentColor.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxSize().padding(start = 10.dp, end = 6.dp),
    ) {
        if (track == null) {
            Box(Modifier.size(32.dp).background(ink.copy(alpha = 0.1f), RoundedCornerShape(8.dp)))
        } else {
            Image(
                bitmap = art[track.art],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
            )
        }
        Text(
            text = track?.title ?: "Not Playing",
            color = ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp).weight(1f),
        )
        Box(Modifier.size(46.dp).tappable(onToggle), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = ink,
                modifier = Modifier.size(28.dp),
            )
        }
        Box(Modifier.size(46.dp).tappable(onNext), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.FastForward, "Next", tint = ink.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun Alert(
    onKeep: () -> Unit,
    onRemove: () -> Unit,
    backdrop: GlassBackdrop,
    glass: Glass,
    modifier: Modifier = Modifier,
) = GlassSurface(
    backdrop = backdrop,
    glass = glass.copy(blurRadius = AlertBlur),
    modifier = modifier.width(300.dp),
    shape = RoundedCornerShape(38.dp),
) {
    val ink = LocalContentColor.current
    Column(Modifier.tappable {}.padding(14.dp)) {
        Column(Modifier.padding(start = 8.dp, top = 10.dp, end = 8.dp, bottom = 22.dp)) {
            Text("Remove This Playlist?", color = ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "It will be removed from your library on all your devices.",
                color = ink.copy(alpha = 0.65f),
                fontSize = 16.sp,
                lineHeight = 21.sp,
            )
        }
        val quiet = ink.copy(alpha = if (ink == Color.White) 0.16f else 0.08f)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AlertButton("Keep Playlist", Accent, Color.White, onKeep)
            AlertButton("Remove", quiet, Destructive, onRemove)
            AlertButton("Cancel", quiet, ink, onKeep)
        }
    }
}

@Composable
private fun AlertButton(text: String, fill: Color, color: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().height(48.dp).background(fill, CircleShape).tappable(onClick),
    ) {
        Text(text = text, color = color, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}
