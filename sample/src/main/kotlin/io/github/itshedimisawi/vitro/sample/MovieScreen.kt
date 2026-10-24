package io.github.itshedimisawi.vitro.sample

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.glassEffectContainer
import io.github.itshedimisawi.vitro.rememberGlassBackdrop
import io.github.itshedimisawi.vitro.scrollEdgeEffect

private val PosterHeight = 430.dp
private val ActionTop = PosterHeight - 74.dp
private val SheetTop = ActionTop + ActionHeight + 16.dp
private val ToolbarHeight = ControlSize + 8.dp
private val TicketsWidth = 92.dp

/** The film: its poster, the way to book it, and what it is about. */
@Composable
fun MovieScreen(onSchedule: () -> Unit) {
    val backdrop = rememberGlassBackdrop()
    val scroll = rememberScrollState()
    val back = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    var isFavourite by rememberSaveable { mutableStateOf(false) }
    val page = MaterialTheme.colorScheme.background
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Scroll offsets at which the sheet meets the toolbar and the booking button slips under it.
    val density = LocalDensity.current
    val toolbarBottom = statusBar + ToolbarHeight
    val expanded = with(density) { (SheetTop - toolbarBottom).roundToPx() }
    val actionHidden = with(density) { (ActionTop + ActionHeight / 2 - toolbarBottom).roundToPx() }
    val showsTickets by remember(actionHidden) { derivedStateOf { scroll.value > actionHidden } }
    // The poster shows in full under the toolbar; the top edge softens the page once the sheet is up.
    val isSheetUp by remember(expanded) { derivedStateOf { scroll.value >= expanded } }
    SheetDetents(scroll, expanded)

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .glassBackdrop(backdrop)
                .background(page)
                .scrollEdgeEffect(top = statusBar, bottom = navigationBar, isTopHidden = !isSheetUp),
        ) {
            // The poster drifts at half the scroll speed, so the sheet slides up over it, and its
            // art fades into the page where the sheet begins.
            Image(
                painter = painterResource(R.drawable.severance),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PosterHeight)
                    .graphicsLayer { translationY = -scroll.value * 0.5f }
                    .drawWithContent {
                        drawContent()
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, page), startY = size.height - 110.dp.toPx()))
                    },
            )
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                Spacer(Modifier.height(ActionTop))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    // One container draws both buttons in a single pass; at no spacing they stay apart.
                    modifier = Modifier.glassEffectContainer(spacing = 0.dp).padding(horizontal = Margin),
                ) {
                    GlassIconButton(Icons.Rounded.PlayArrow, "Play trailer", {}, backdrop, size = ActionHeight)
                    GlassTextButton(
                        text = "Schedule and tickets",
                        onClick = onSchedule,
                        backdrop = backdrop,
                        modifier = Modifier.weight(1f).height(ActionHeight),
                    )
                }
                Spacer(Modifier.height(SheetTop - ActionTop - ActionHeight))
                Details()
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.statusBarsPadding().padding(horizontal = Margin, vertical = 4.dp),
        ) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBackIos, "Back", { back?.onBackPressed() }, backdrop)
            Spacer(Modifier.weight(1f))
            TicketsButton(showsTickets, onSchedule, backdrop)
            GlassButtonGroup(backdrop) {
                GlassGroupItem(Icons.Rounded.IosShare, "Share") {}
                GlassGroupItem(
                    icon = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    label = "Favourite",
                    onClick = { isFavourite = !isFavourite },
                )
            }
        }
    }
}

/**
 * Rests the sheet at the poster or fully raised under the toolbar, like an iOS sheet: a scroll
 * ending between the two settles on the nearer one. Past [expanded] it scrolls freely.
 */
@Composable
private fun SheetDetents(scroll: ScrollState, expanded: Int) {
    LaunchedEffect(scroll, expanded) {
        snapshotFlow { scroll.isScrollInProgress }.collect { isScrolling ->
            val offset = scroll.value
            if (!isScrolling && offset in 1 until expanded) {
                scroll.animateScrollTo(if (offset < expanded / 2) 0 else expanded)
            }
        }
    }
}

/** The booking button, grown out of the toolbar while the full-width one is scrolled out of sight. */
@Composable
private fun TicketsButton(isShown: Boolean, onClick: () -> Unit, backdrop: GlassBackdrop) {
    val width by animateDpAsState(
        targetValue = if (isShown) TicketsWidth else 0.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "tickets",
    )
    if (width <= 0.dp) return
    val shown = (width / TicketsWidth).coerceIn(0f, 1f)
    GlassTextButton(
        text = "Tickets",
        onClick = onClick,
        backdrop = backdrop,
        modifier = Modifier
            .padding(end = 8.dp * shown)
            .graphicsLayer { alpha = shown }
            .width(width)
            .height(ControlSize),
    )
}

@Composable
private fun Details() {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(colors.background)
            .navigationBarsPadding()
            .padding(bottom = 32.dp),
    ) {
        Box(
            Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
                .size(36.dp, 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.surfaceVariant),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = Margin, end = Margin, top = 20.dp),
        ) {
            Text(Movie.Title, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(Movie.Rating, fontSize = 18.sp)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.Star, contentDescription = "Rating", tint = colors.tertiary, modifier = Modifier.size(22.dp))
        }
        Text(
            text = Movie.Details,
            color = colors.onSurfaceVariant,
            fontSize = 15.sp,
            modifier = Modifier.padding(start = Margin, end = Margin, top = 6.dp),
        )
        Text(
            text = Movie.Synopsis,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(start = Margin, end = Margin, top = 22.dp),
        )

        SectionTitle("Cast")
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(PaddingValues(horizontal = Margin)),
        ) {
            Movie.Cast.forEach { CastChip(it) }
        }

        SectionTitle("Ratings & Reviews")
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(horizontal = Margin)) {
            Text(Movie.Rating, fontSize = 48.sp, fontWeight = FontWeight.Bold, lineHeight = 48.sp)
            Column(Modifier.padding(start = 10.dp, bottom = 6.dp)) {
                Text("out of 10", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(Movie.RatingCount, color = colors.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(start = Margin, end = Margin, top = 14.dp),
        ) {
            Movie.Reviews.forEach { ReviewCard(it) }
        }

        SectionTitle("Information")
        Column(
            Modifier
                .padding(horizontal = Margin)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surface),
        ) {
            Movie.Information.forEachIndexed { index, (label, value) ->
                if (index > 0) HorizontalDivider(color = colors.surfaceVariant, modifier = Modifier.padding(start = 16.dp))
                Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
                    Text(label, fontSize = 15.sp, maxLines = 1)
                    Text(
                        text = value,
                        color = colors.onSurfaceVariant,
                        fontSize = 15.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = Margin, end = Margin, top = 32.dp, bottom = 12.dp),
    )
}

@Composable
private fun CastChip(member: CastMember) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.surface)
            .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.surfaceVariant),
        ) {
            Text(member.name.split(" ").joinToString("") { it.take(1) }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(member.name, fontSize = 15.sp, maxLines = 1)
            Text(member.role, color = colors.onSurfaceVariant, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ReviewCard(review: Review) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .padding(16.dp),
    ) {
        Text(review.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(Modifier.padding(top = 4.dp, bottom = 8.dp)) {
            repeat(5) { star ->
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = if (star < review.stars) colors.tertiary else colors.surfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(review.text, fontSize = 14.sp, lineHeight = 19.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(57.dp))
        Text(review.author, color = colors.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
    }
}
