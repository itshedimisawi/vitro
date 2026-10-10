package io.github.itshedimisawi.vitro.sample

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.glassEffect
import io.github.itshedimisawi.vitro.sample.music.MusicActivity

val Photos = listOf(
    R.drawable.photo_1015,
    R.drawable.photo_1039,
    R.drawable.photo_29,
    R.drawable.photo_1047,
    R.drawable.photo_1062,
)

@Composable
fun BoxScope.PhotosDemo(backdrop: GlassBackdrop) {
    val pager = rememberPagerState { Photos.size }
    HorizontalPager(
        state = pager,
        modifier = Modifier.fillMaxSize().glassBackdrop(backdrop),
    ) { page ->
        Image(
            painter = painterResource(Photos[page]),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }

    Text(
        text = "Swipe photos, drag the glass",
        color = if (isSystemInDarkTheme()) Color.White else Ink,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 12.dp)
            .glassEffect(backdrop)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    )

    val context = LocalContext.current
    GlassTextButton(
        text = "Music",
        icon = Icons.Rounded.MusicNote,
        onClick = { context.startActivity(Intent(context, MusicActivity::class.java)) },
        backdrop = backdrop,
        glass = Glass.Regular,
        modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 76.dp),
    )

    val lens = remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .draggable(lens)
            .size(132.dp)
            .glassEffect(backdrop, Glass.Clear.interactive()),
    )

    val pill = remember { mutableStateOf(Offset(0f, 520f)) }
    Text(
        text = "Clear glass",
        color = Color.White,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .align(Alignment.Center)
            .draggable(pill)
            .glassEffect(backdrop, Glass.Clear.dimmed().interactive())
            .padding(horizontal = 28.dp, vertical = 18.dp),
    )
}
