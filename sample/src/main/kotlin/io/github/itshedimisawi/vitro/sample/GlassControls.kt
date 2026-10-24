package io.github.itshedimisawi.vitro.sample

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
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
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.GlassEffectState
import io.github.itshedimisawi.vitro.glassEffect
import io.github.itshedimisawi.vitro.rememberGlassEffectState

val ControlSize = 44.dp
val ActionHeight = 50.dp

/** Space between the screen edges and the content and controls next to them. */
val Margin = 20.dp

/** A press that leaves the feedback to the glass under it, which [interactionSource] tells about it. */
fun Modifier.tappable(interactionSource: MutableInteractionSource? = null, onClick: () -> Unit): Modifier =
    clickable(interactionSource = interactionSource, indication = null, onClick = onClick)

/** A piece of glass that colours its content to suit the material, unless [contentColor] is given. */
@Composable
fun GlassSurface(
    backdrop: GlassBackdrop,
    glass: Glass,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = CircleShape,
    contentColor: Color = Color.Unspecified,
    state: GlassEffectState = rememberGlassEffectState(),
    interactionSource: InteractionSource? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.glassEffect(backdrop, glass, shape, state, interactionSource)) {
        CompositionLocalProvider(LocalContentColor provides contentColor.takeOrElse { state.contentColor }) {
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
    val source = remember { MutableInteractionSource() }
    GlassSurface(backdrop, glass.interactive(), modifier.size(size), shape, contentColor, interactionSource = source) {
        Box(Modifier.fillMaxSize().tappable(source, onClick), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/** One glass capsule holding related icon actions, as on an iOS toolbar; fill it with [GlassGroupItem]s. */
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
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp, ControlSize).tappable(onClick = onClick)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(ControlSize * 0.5f))
    }
}

/** A glass button with a centred label, sized by [modifier]. When disabled it is dimmed and ignores taps. */
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
    val source = remember { MutableInteractionSource() }
    GlassSurface(backdrop, glass.interactive(enabled), modifier, shape, contentColor, interactionSource = source) {
        val tap = if (enabled) Modifier.tappable(source, onClick) else Modifier
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
