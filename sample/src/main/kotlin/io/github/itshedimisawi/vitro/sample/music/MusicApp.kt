package io.github.itshedimisawi.vitro.sample.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.ScrollEdgeEffectStyle
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.rememberGlassBackdrop
import io.github.itshedimisawi.vitro.scrollEdgeEffect
import io.github.itshedimisawi.vitro.sample.GlassSurface
import io.github.itshedimisawi.vitro.sample.GlassTab
import io.github.itshedimisawi.vitro.sample.rememberArtwork
import io.github.itshedimisawi.vitro.sample.tappable

/** The colours of the music screens in one theme. */
class MusicColors(
    val isDark: Boolean,
    val page: Color,
    val ink: Color,
    val quiet: Color,
    val divider: Color,
    val chevron: Color,
    val card: Color,
    val red: Color,
)

private val DayColors = MusicColors(
    isDark = false,
    page = Color.White,
    ink = Color.Black,
    quiet = Color(0xFF8A8A8E),
    divider = Color(0xFFE7E7E8),
    chevron = Color(0xFFC4C4C7),
    card = Color.White,
    red = Color(0xFFFA233B),
)

private val NightColors = MusicColors(
    isDark = true,
    page = Color.Black,
    ink = Color.White,
    quiet = Color(0xFF98989F),
    divider = Color(0xFF2A2A2B),
    chevron = Color(0xFF5A5A5E),
    card = Color(0xFF1C1C1D),
    red = Color(0xFFFA2D48),
)

val LocalMusicColors = staticCompositionLocalOf { DayColors }

val ScreenMargin = 20.dp

/** Room left at the end of every page for the controls floating over it. */
val ControlsRoom = 168.dp

private val Tabs = listOf(
    GlassTab("Home", Icons.Rounded.Home),
    GlassTab("Radio", Icons.Rounded.Sensors),
    GlassTab("Library", Icons.Rounded.LibraryMusic),
)

private const val SearchPage = 3
private const val LibraryTab = 2

@Composable
fun MusicApp() {
    val colors = if (isSystemInDarkTheme()) NightColors else DayColors
    val backdrop = rememberGlassBackdrop()
    val art = rememberArtwork()
    val focus = LocalFocusManager.current

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var isCollapsed by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val scrolls = remember { List(4) { ScrollState(0) } }

    fun closeSearch() {
        focus.clearFocus()
        isSearching = false
        query = ""
    }
    BackHandler(enabled = isSearching) { closeSearch() }

    // Scrolling on shrinks the tab bar out of the way; scrolling back restores it.
    val scrolling = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (consumed.y < -1f) isCollapsed = true else if (consumed.y > 1f) isCollapsed = false
                return Offset.Zero
            }
        }
    }

    CompositionLocalProvider(LocalMusicColors provides colors, LocalContentColor provides colors.ink) {
        Box(Modifier.fillMaxSize().background(colors.page)) {
            // The pages dissolve into the page colour under the status bar. Under the controls
            // the library does the same, and the pages full of artwork are shaded instead.
            val covered by animateDpAsState(
                targetValue = controlsHeight(isCollapsed && !isSearching),
                animationSpec = spring(dampingRatio = 1f, stiffness = 320f),
                label = "covered",
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .glassBackdrop(backdrop)
                    .scrollEdgeEffect(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(), color = colors.page)
                    .scrollEdgeEffect(
                        bottom = covered,
                        style = if (tab == LibraryTab && !isSearching) ScrollEdgeEffectStyle.Soft else ScrollEdgeEffectStyle.Shaded,
                        color = colors.page,
                    )
                    .background(colors.page)
                    .nestedScroll(scrolling),
            ) {
                Crossfade(
                    targetState = if (isSearching) SearchPage else tab,
                    animationSpec = tween(PageFadeMillis),
                    label = "page",
                ) { page ->
                    when (page) {
                        0 -> HomePage(scrolls[0])
                        1 -> RadioPage(scrolls[1])
                        LibraryTab -> LibraryPage(scrolls[2], art)
                        else -> SearchPage(scrolls[3], art, query)
                    }
                }
            }

            if (tab == LibraryTab && !isSearching) {
                GlassSurface(
                    backdrop = backdrop,
                    glass = Glass.Regular.interactive(),
                    modifier = Modifier.statusBarsPadding().padding(start = 16.dp).size(44.dp),
                ) {
                    Box(Modifier.fillMaxSize().tappable { tab = 0 }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                }
            }

            MusicControls(
                tabs = Tabs,
                tab = tab,
                onTab = {
                    tab = it
                    isCollapsed = false
                },
                isSearching = isSearching,
                onSearch = {
                    isSearching = true
                    isCollapsed = false
                },
                onRestore = { if (isSearching) closeSearch() else isCollapsed = false },
                isCollapsed = isCollapsed,
                query = query,
                onQueryChange = { query = it },
                artwork = art[0],
                backdrop = backdrop,
            )
        }
    }
}

internal const val PageFadeMillis = 280
