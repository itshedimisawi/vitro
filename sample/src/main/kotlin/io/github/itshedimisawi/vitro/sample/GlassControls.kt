package io.github.itshedimisawi.vitro.sample

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassEffect

val ControlSize = 48.dp
private val GroupItemWidth = 50.dp
private val ControlIconSize = 23.dp

/** A press that leaves the feedback to the glass under it. */
fun Modifier.tappable(onClick: () -> Unit): Modifier =
    clickable(interactionSource = null, indication = null, onClick = onClick)

/** A round glass button holding one icon. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    backdrop: GlassBackdrop,
    glass: Glass,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(ControlSize)
            .glassEffect(backdrop, glass.interactive())
            .tappable(onClick),
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(ControlIconSize))
    }
}

/** One capsule of glass shared by several related icons; fill it with [GlassGroupItem]s. */
@Composable
fun GlassButtonGroup(
    backdrop: GlassBackdrop,
    glass: Glass,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        content = content,
        modifier = modifier
            .height(ControlSize)
            .glassEffect(backdrop, glass.interactive())
            .padding(horizontal = 6.dp),
    )
}

@Composable
fun GlassGroupItem(icon: ImageVector, label: String, contentColor: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(GroupItemWidth, ControlSize).tappable(onClick),
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(ControlIconSize))
    }
}

/** A glass capsule with a text label and an optional icon before it. */
@Composable
fun GlassTextButton(
    text: String,
    onClick: () -> Unit,
    backdrop: GlassBackdrop,
    glass: Glass,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(ControlSize)
            .glassEffect(backdrop, glass.interactive())
            .tappable(onClick)
            .padding(horizontal = if (icon == null) 15.dp else 18.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text = text, color = contentColor, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** A glass search field: magnifier, the query or its placeholder, and a microphone. */
@Composable
fun GlassSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    backdrop: GlassBackdrop,
    glass: Glass,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val quiet = contentColor.copy(alpha = 0.5f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(ControlSize)
            .glassEffect(backdrop, glass)
            .padding(start = 15.dp, end = 14.dp),
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = quiet, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = contentColor, fontSize = 17.sp),
            cursorBrush = SolidColor(Accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.weight(1f),
            decorationBox = { field ->
                if (query.isEmpty()) Text(text = placeholder, color = quiet, fontSize = 17.sp, maxLines = 1)
                field()
            },
        )
        Icon(Icons.Rounded.Mic, contentDescription = null, tint = quiet, modifier = Modifier.size(20.dp))
    }
}
