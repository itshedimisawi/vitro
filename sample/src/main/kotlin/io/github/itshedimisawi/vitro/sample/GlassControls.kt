package io.github.itshedimisawi.vitro.sample

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassAppearance
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.GlassInteractionState
import io.github.itshedimisawi.vitro.glassEffect

val ControlSize = 44.dp
val ActionHeight = 50.dp

/** Space between the screen edges and the content and controls next to them. */
val Margin = 20.dp

/** A press that leaves the feedback to the glass under it. */
fun Modifier.tappable(onClick: () -> Unit): Modifier =
    clickable(interactionSource = null, indication = null, onClick = onClick)

/** The colour for content on a piece of glass: white on the dark material, ink on the light one. */
@Stable
class GlassInk(isDark: Boolean) {
    var isDark by mutableStateOf(isDark)
        private set

    val color: Color get() = if (isDark) Color.White else Ink

    fun follow(appearance: GlassAppearance) {
        isDark = appearance == GlassAppearance.Dark
    }
}

/** Remembers a [GlassInk] for [glass]; pass its [GlassInk.follow] to the glass to keep it right. */
@Composable
fun rememberGlassInk(glass: Glass): GlassInk {
    val isNight = isSystemInDarkTheme()
    return remember {
        GlassInk(glass.appearance == GlassAppearance.Dark || glass.appearance == GlassAppearance.Automatic && isNight)
    }
}

/**
 * A piece of glass that tells its content which colour to draw with. Unless [contentColor] is
 * given, the colour follows the material the glass settles on.
 */
@Composable
fun GlassSurface(
    backdrop: GlassBackdrop,
    glass: Glass,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = CircleShape,
    contentColor: Color = Color.Unspecified,
    interaction: GlassInteractionState? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val ink = rememberGlassInk(glass)
    Box(modifier.glassEffect(backdrop, glass, shape, interaction, ink::follow)) {
        CompositionLocalProvider(LocalContentColor provides contentColor.takeOrElse { ink.color }) {
            content()
        }
    }
}

/** A round glass button holding one icon. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
    glass: Glass = Glass.Regular,
    shape: CornerBasedShape = CircleShape,
    size: Dp = ControlSize,
    contentColor: Color = Color.Unspecified,
) {
    GlassSurface(backdrop, glass.interactive(), modifier.size(size), shape, contentColor) {
        Box(Modifier.fillMaxSize().tappable(onClick), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/** One glass capsule shared by related icon actions, as on an iOS toolbar; fill it with [GlassGroupItem]s. */
@Composable
fun GlassButtonGroup(
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
    glass: Glass = Glass.Regular,
    content: @Composable RowScope.() -> Unit,
) {
    GlassSurface(backdrop, glass.interactive(), modifier.height(ControlSize)) {
        Row(verticalAlignment = Alignment.CenterVertically, content = content, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
fun GlassGroupItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp, ControlSize).tappable(onClick)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(ControlSize * 0.5f))
    }
}

/** A glass button with a centred label, sized by [modifier]. A disabled button is dimmed and ignores taps. */
@Composable
fun GlassTextButton(
    text: String,
    onClick: () -> Unit,
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
    glass: Glass = Glass.Regular,
    shape: CornerBasedShape = CircleShape,
    enabled: Boolean = true,
    contentColor: Color = Color.Unspecified,
) {
    GlassSurface(backdrop, glass.interactive(enabled), modifier, shape, contentColor) {
        val tap = if (enabled) Modifier.tappable(onClick) else Modifier
        Box(Modifier.fillMaxSize().then(tap), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                color = LocalContentColor.current.copy(alpha = if (enabled) 1f else 0.4f),
            )
        }
    }
}
