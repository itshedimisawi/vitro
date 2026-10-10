package io.github.itshedimisawi.vitro.sample

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassEffect
import io.github.itshedimisawi.vitro.rememberGlassInteractionState
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest

private val BarHeight = 62.dp
private val BarMargin = 20.dp
private val SelectionInset = 4.dp
private val LensGrowth = 21.dp
private val LensRoom = 14.dp
private const val LensZoom = 0.15f
private const val LensMagnification = 0.88f
private val Accent = Color(0xFF007AFF)

private val SettleSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 400f)

/**
 * A floating tab bar made of two pieces of glass on the same backdrop. The bar is ordinary
 * interactive glass, so it swells and leans toward a held finger. Its selection listens to the
 * same touch: while the bar is held it lifts into a clear lens that follows the finger and shows
 * the bar through it, and on release it settles on the nearest tab.
 */
@Composable
fun GlassTabBar(
    titles: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    backdrop: GlassBackdrop,
    glass: Glass,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val touch = rememberGlassInteractionState()
    val density = LocalDensity.current
    val select by rememberUpdatedState(onSelect)
    var barSize by remember { mutableStateOf(IntSize.Zero) }
    // Where the selection rests, counted in tabs: 0 is the first tab, 1.5 is between the second and third.
    val slot = remember { Animatable(selected.toFloat()) }
    // While held the selection sits under the finger, read straight from the touch so it never
    // trails behind. After a press on another tab it closes the remaining distance as `catchUp` runs out.
    val catchUp = remember { Animatable(1f) }
    val follow = remember { Follow() }

    fun slotUnder(x: Float): Float {
        val inset = with(density) { SelectionInset.toPx() }
        val tab = (barSize.width - inset * 2f) / titles.size
        if (tab <= 0f) return slot.value
        return ((x - inset) / tab - 0.5f).coerceIn(0f, titles.size - 1f)
    }

    fun currentSlot(): Float {
        if (touch.isHeld) {
            val under = slotUnder(touch.position.x)
            if (!follow.isActive) {
                follow.isActive = true
                follow.lead = slot.value - under
            }
            follow.last = under + follow.lead * catchUp.value
        }
        return if (follow.isActive) follow.last else slot.value
    }

    fun selection(): Rect =
        density.selectionBounds(barSize.toSize(), titles.size, currentSlot(), touch.pressure)

    // The lens is drawn outside the bar, so it applies the bar's swell and drift itself to stay on it.
    fun lensBounds(): Rect {
        val bounds = selection()
        val centre = barSize.toSize().center
        val scale = touch.scale
        val drift = touch.offset
        return Rect(
            left = centre.x + (bounds.left - centre.x) * scale.scaleX + drift.x,
            top = centre.y + (bounds.top - centre.y) * scale.scaleY + drift.y,
            right = centre.x + (bounds.right - centre.x) * scale.scaleX + drift.x,
            bottom = centre.y + (bounds.bottom - centre.y) * scale.scaleY + drift.y,
        )
    }

    LaunchedEffect(touch, titles.size) {
        // A quick tap can press and lift between two frames, so a release is recognised by the
        // finger having come to rest somewhere new, not by having watched it go down.
        var settledAt = touch.position
        snapshotFlow { touch.isHeld to touch.position }.collectLatest { (isHeld, at) ->
            if (isHeld) {
                if (catchUp.value != 0f) catchUp.animateTo(0f, SettleSpring)
            } else if (at != settledAt) {
                settledAt = at
                val nearest = slotUnder(at.x).roundToInt()
                if (follow.isActive) slot.snapTo(follow.last)
                follow.isActive = false
                catchUp.snapTo(1f)
                select(nearest)
                slot.animateTo(nearest.toFloat(), SettleSpring)
            }
        }
    }
    LaunchedEffect(selected) {
        if (!touch.isHeld) slot.animateTo(selected.toFloat(), SettleSpring)
    }

    Box(modifier.fillMaxWidth().height(BarHeight + LensRoom * 2)) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = BarMargin)
                    .fillMaxWidth()
                    .height(BarHeight)
                    .onSizeChanged { barSize = it }
                    .glassEffect(backdrop, glass.interactive(), interaction = touch)
                    .drawBehind {
                        val rest = 1f - touch.pressure.coerceIn(0f, 1f)
                        val bounds = selection()
                        drawRoundRect(
                            color = contentColor,
                            topLeft = bounds.topLeft,
                            size = bounds.size,
                            cornerRadius = CornerRadius(bounds.height / 2f),
                            alpha = 0.09f * rest,
                        )
                    },
            ) {
                // Outside the selection the labels are plain; inside it they take the accent
                // colour and are enlarged a little as the selection lifts.
                Labels(
                    titles = titles,
                    color = contentColor,
                    modifier = Modifier.drawWithContent {
                        clipPath(selection().capsule(), ClipOp.Difference) {
                            this@drawWithContent.drawContent()
                        }
                    },
                )
                Labels(
                    titles = titles,
                    color = Accent,
                    modifier = Modifier.drawWithContent {
                        val bounds = selection()
                        val zoom = 1f + LensZoom * touch.pressure.coerceIn(0f, 1f)
                        clipPath(bounds.capsule()) {
                            scale(zoom, bounds.center) { this@drawWithContent.drawContent() }
                        }
                    },
                )
            }
        }

        val isLifted by remember { derivedStateOf { touch.pressure > 0.01f } }
        if (isLifted) {
            val lens = remember(glass.appearance) {
                // Clear glass with a thin rim that takes in a little more than it covers, so
                // the edge of the bar beneath it stays visible inside it.
                Glass.Clear.copy(
                    appearance = glass.appearance,
                    blurRadius = 0.dp,
                    refractionHeight = 3.dp,
                    dispersion = 0.1f,
                    magnification = LensMagnification,
                )
            }
            Box(
                Modifier
                    .offset {
                        val bounds = lensBounds()
                        IntOffset(
                            (BarMargin.toPx() + bounds.left).roundToInt(),
                            (LensRoom.toPx() + bounds.top).roundToInt(),
                        )
                    }
                    .layout { measurable, _ ->
                        val bounds = lensBounds()
                        val placeable = measurable.measure(
                            Constraints.fixed(bounds.width.roundToInt(), bounds.height.roundToInt()),
                        )
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                    .graphicsLayer { alpha = touch.pressure.coerceIn(0f, 1f) }
                    .glassEffect(backdrop, lens),
            )
        }
    }
}

private fun Rect.capsule(): Path =
    Path().apply { addRoundRect(RoundRect(this@capsule, CornerRadius(height / 2f))) }

/** Bookkeeping for a selection that is tracking the finger. */
private class Follow {
    var isActive = false
    var lead = 0f
    var last = 0f
}

@Composable
private fun Labels(titles: List<String>, color: Color, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxSize().padding(horizontal = SelectionInset)) {
        titles.forEach { title ->
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text(text = title, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/**
 * Outline of the selection inside a bar of [bar] size: a capsule over one tab at rest that grows
 * on every side as it is lifted.
 */
private fun Density.selectionBounds(bar: Size, tabs: Int, slot: Float, lift: Float): Rect {
    val inset = SelectionInset.toPx()
    val tab = (bar.width - inset * 2f) / tabs
    val growth = LensGrowth.toPx() * lift.coerceIn(0f, 1.15f)
    val width = tab + growth
    val height = bar.height - inset * 2f + growth
    val centre = inset + tab * (slot + 0.5f)
    return Rect(
        left = centre - width / 2f,
        top = (bar.height - height) / 2f,
        right = centre + width / 2f,
        bottom = (bar.height + height) / 2f,
    )
}
