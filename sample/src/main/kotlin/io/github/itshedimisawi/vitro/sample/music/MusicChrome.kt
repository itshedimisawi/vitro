package io.github.itshedimisawi.vitro.sample.music

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.sample.GlassSurface
import io.github.itshedimisawi.vitro.sample.GlassTab
import io.github.itshedimisawi.vitro.sample.GlassTabBar
import io.github.itshedimisawi.vitro.sample.tappable
import kotlin.math.max
import kotlin.math.roundToInt

// The controls at full size: a tab bar and a round search button side by side, with the player
// above them.
private val BarHeight = 62.dp
private val BarMargin = 21.dp
private val BarGap = 9.5.dp
private val PlayerHeight = 48.dp
private val PlayerGap = 8.dp

// The controls made small: one row of round buttons, set further in from the edges.
private val SmallSize = 48.dp
private val SmallMargin = 28.dp
private val SmallGap = 8.5.dp
private val FieldGap = 12.dp

// How far the bar stays above the bottom of the screen, and above the system bar if that is taller.
private val BottomMargin = 21.dp
private val SystemBarGap = 8.dp

private val Morph = spring<Float>(dampingRatio = 0.86f, stiffness = 320f)

/**
 * The floating controls of the music screens: tab bar, player and search. They are three pieces
 * of glass that keep their identity across three arrangements. At rest the tab bar and a round
 * search button share the bottom row with the player above. When [isCollapsed] the tab bar shrinks
 * to a round button and the player drops into the row. When [isSearching] the tab bar is a round
 * button again and the search button stretches into a field.
 */
@Composable
fun MusicControls(
    tabs: List<GlassTab>,
    tab: Int,
    onTab: (Int) -> Unit,
    isSearching: Boolean,
    onSearch: () -> Unit,
    onRestore: () -> Unit,
    isCollapsed: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    artwork: ImageBitmap,
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMusicColors.current
    val collapse = animateFloatAsState(if (isCollapsed && !isSearching) 1f else 0f, Morph, label = "collapse")
    val search = animateFloatAsState(if (isSearching) 1f else 0f, Morph, label = "search")
    val systemBar = WindowInsets.navigationBars
    val keyboard = WindowInsets.ime

    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            GlassTabBar(
                tabs = tabs,
                selected = tab,
                onSelect = onTab,
                backdrop = backdrop,
                glass = Glass.Regular,
                accent = colors.red,
                minimized = { max(collapse.value, search.value) },
                isMinimizedIconAccented = !isSearching,
                onRestore = onRestore,
            )
            Player(artwork, backdrop, compact = { collapse.value * (1f - search.value) })
            SearchControl(isSearching, search.value, onSearch, query, onQueryChange, backdrop)
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val collapsed = collapse.value
        val searching = search.value
        fun mix(full: Float, small: Float, field: Float) = lerp(lerp(full, small, collapsed), field, searching)

        val below = max(systemBar.getBottom(this), keyboard.getBottom(this))
        val floor = max(BottomMargin.toPx(), below + SystemBarGap.toPx())
        val row = floor + BarHeight.toPx() / 2f
        val small = SmallSize.toPx()
        val smallEdge = SmallMargin.toPx()

        // Each piece: left edge, width, height, and how far its centre is above the bottom.
        val barWidth = mix(width - BarMargin.toPx() * 2f - BarHeight.toPx() - BarGap.toPx(), small, small)
        val barHeight = mix(BarHeight.toPx(), small, small)
        val barLeft = mix(BarMargin.toPx(), smallEdge, smallEdge)

        val searchWidth = mix(BarHeight.toPx(), small, width - smallEdge * 2f - small - FieldGap.toPx())
        val searchHeight = mix(BarHeight.toPx(), small, small)
        val searchRight = mix(width - BarMargin.toPx(), width - smallEdge, width - smallEdge)

        val playerLeft = mix(BarMargin.toPx(), smallEdge + small + SmallGap.toPx(), smallEdge)
        val playerWidth = width - playerLeft * 2f
        val playerHeight = PlayerHeight.toPx()
        val playerRise = mix(
            BarHeight.toPx() / 2f + PlayerGap.toPx() + playerHeight / 2f,
            0f,
            small / 2f + SmallGap.toPx() + playerHeight / 2f,
        )

        fun fixed(width: Float, height: Float) =
            Constraints.fixed(width.roundToInt().coerceAtLeast(0), height.roundToInt().coerceAtLeast(0))

        val bar = measurables[0].measure(fixed(barWidth, barHeight))
        val player = measurables[1].measure(fixed(playerWidth, playerHeight))
        val field = measurables[2].measure(fixed(searchWidth, searchHeight))

        layout(width, constraints.maxHeight) {
            val centre = constraints.maxHeight - row
            bar.place(barLeft.roundToInt(), (centre - barHeight / 2f).roundToInt())
            field.place((searchRight - searchWidth).roundToInt(), (centre - searchHeight / 2f).roundToInt())
            player.place(playerLeft.roundToInt(), (centre - playerRise - playerHeight / 2f).roundToInt())
        }
    }
}

/** What is playing: artwork, title and artist, play and next. [compact] hides the next button. */
@Composable
private fun Player(artwork: ImageBitmap, backdrop: GlassBackdrop, compact: () -> Float) {
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    GlassSurface(backdrop, Glass.Regular.interactive()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 13.dp),
        ) {
            Image(
                bitmap = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)),
            )
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Night Drive",
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = "Aster Lane", fontSize = 13.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.width(44.dp).fillMaxHeight().tappable { isPlaying = !isPlaying },
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(30.dp),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer { alpha = (1f - compact() * 2f).coerceIn(0f, 1f) }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val shown = (placeable.width * (1f - compact())).roundToInt().coerceAtLeast(0)
                        layout(shown, placeable.height) { placeable.place(0, 0) }
                    }
                    .width(44.dp)
                    .fillMaxHeight()
                    .tappable {},
            ) {
                Icon(Icons.Rounded.FastForward, contentDescription = "Next", modifier = Modifier.size(30.dp))
            }
        }
    }
}

/**
 * The search control: a round button whose magnifier sits in the middle, or a field with the
 * magnifier at its start, as [open] goes from 0 to 1.
 */
@Composable
private fun SearchControl(
    isSearching: Boolean,
    open: Float,
    onSearch: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    backdrop: GlassBackdrop,
) {
    val spread = open.coerceIn(0f, 1f)
    GlassSurface(backdrop, if (isSearching) Glass.Regular else Glass.Regular.interactive()) {
        val color = LocalContentColor.current
        val quiet = color.copy(alpha = 0.6f)
        Box(
            Modifier
                .fillMaxSize()
                .then(if (isSearching) Modifier else Modifier.tappable(onSearch))
                .padding(horizontal = lerp(10.dp, 13.dp, spread)),
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                modifier = Modifier.align(BiasAlignment(-spread, 0f)).size(lerp(28.dp, 22.dp, spread)),
            )
            if (spread > 0f) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 29.dp)
                        .graphicsLayer { alpha = (spread * 2f - 1f).coerceIn(0f, 1f) },
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        enabled = isSearching,
                        singleLine = true,
                        textStyle = TextStyle(color = color, fontSize = 17.sp),
                        cursorBrush = SolidColor(LocalMusicColors.current.red),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.weight(1f),
                        decorationBox = { field ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Artists, Songs, Lyrics, and More",
                                    color = quiet,
                                    fontSize = 17.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            field()
                        },
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
