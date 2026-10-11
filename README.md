# Vitro — Liquid Glass for Jetpack Compose

Vitro brings a refractive glass material to Jetpack Compose as plain `Modifier` extensions. Apply it to any composable: there are no fixed components to adopt.

- Real lensing at the rim, colour dispersion, directional highlights and a soft shadow, rendered with an AGSL shader on Android 13+
- A blurred approximation with the same shape, lighting and interaction on Android 6–12
- `Regular` and `Clear` variants, tinting, dimming and touch-reactive glass
- Every optical property is configurable

## Setup

```kotlin
dependencies {
    implementation("io.github.itshedimisawi:vitro:0.1.0")
}
```

Requires `minSdk` 23.

## Usage

Glass needs to know what is behind it. Mark that content with `glassBackdrop`, then layer glass on top with `glassEffect`.

```kotlin
val backdrop = rememberGlassBackdrop()

Box {
    Image(
        painter = painterResource(R.drawable.photo),
        contentDescription = null,
        modifier = Modifier.fillMaxSize().glassBackdrop(backdrop),
    )

    Text(
        text = "Hello",
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .glassEffect(backdrop)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    )
}
```

`glassEffect` defaults to the regular material in a capsule. Pass a `Glass` and a shape to change that:

```kotlin
Modifier.glassEffect(
    backdrop = backdrop,
    glass = Glass.Clear.tint(Color.Blue).interactive(),
    shape = RoundedCornerShape(28.dp),
)
```

### Variants

| | Use it for |
|---|---|
| `Glass.Regular` | Most controls. Blurs and lifts the content behind it so text on top stays legible. |
| `Glass.Clear` | Controls floating over photos and video. Add `.dimmed()` over bright content. |

### Customising

| Function | Effect |
|---|---|
| `tint(color)` | Colours the material to give a control prominence. |
| `interactive()` | Glass grows under a press, lights up beneath the finger and stretches while it moves. |
| `dimmed(opacity)` | Adds a dark layer behind the glass, 35% by default. |
| `copy(...)` | Sets any optical property: `blurRadius`, `refractionHeight`, `refractionAmount`, `dispersion`, `magnification`, `saturation`, `brightness`, `appearance`, `lightAngle`, `highlightIntensity` and the shadow. |

### Following a touch

Interactive glass tracks its own touch. To let other elements react to it, create a `GlassInteractionState` and hand it to the glass:

```kotlin
val touch = rememberGlassInteractionState()

Box(Modifier.glassEffect(backdrop, Glass.Regular.interactive(), interaction = touch)) {
    // touch.isHeld, touch.pressure, touch.position and touch.drag are observable here
}
```

`pressure` runs from 0 at rest to 1 while held, with the same quick rise and rebound the glass itself uses, so anything driven by it moves in step with the glass. `scale` and `offset` are the swell, stretch and drift the glass applies to itself about its centre; apply them to anything layered on top that should stay in register with it.

### Glass on glass

Glass can float over other glass. Pieces that share a backdrop see each other in the order they are added: each one shows through those placed after it, with its outline, shadow and content.

```kotlin
Box(Modifier.glassEffect(backdrop)) { /* a bar */ }
Box(Modifier.glassEffect(backdrop, Glass.Clear.copy(magnification = 0.9f))) // shows the bar through it
```

`magnification` scales what is seen through the glass about its centre. Below 1 the glass takes in a wider area than it covers, which keeps the edges of whatever lies under it in view. Glass placed inside the content of another piece of glass sees that piece's material under it, without the content it is itself part of. Which piece counts as on top follows the order the glass is added, not `zIndex`.

### Glass that flows together

Put `glassEffectContainer` on the parent of controls that sit close together or move into one another. The glass below it is then drawn as one surface: pieces that come closer than `spacing` reach for each other and join, with the rim, the refraction and the shadow running unbroken across the join.

```kotlin
Row(Modifier.glassEffectContainer(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    IconButton(onClick = {}, Modifier.glassEffect(backdrop, Glass.Regular.interactive())) { /* ... */ }
    IconButton(onClick = {}, Modifier.glassEffect(backdrop, Glass.Regular.interactive())) { /* ... */ }
}
```

Only pieces of the same material join: they must share a backdrop and their optical properties. Tint, dimming, appearance and shadow opacity may differ and blend from one piece to the next. Pieces of another material in the same container are drawn alongside without joining. Joining needs Android 13; earlier versions draw each piece on its own.

### Light and dark

Glass follows the system dark theme by default and gives way to the content behind it at the far end: over near-black content it turns dark in a light interface, and over near-white content it turns light in a dark one, easing from one material to the other. What counts is the light the content gives off, so deep saturated colours and photos with bright spots keep light glass light. Light glass is veiled toward white; dark glass stays close to the colours behind it and only lifts the darkest tones. Force either one with `copy(appearance = GlassAppearance.Dark)`.

Content on automatic glass needs to change colour with it. `onAppearanceChanged` reports the material the glass settles on:

```kotlin
var isDark by remember { mutableStateOf(false) }
Text(
    text = "Edit",
    color = if (isDark) Color.White else Color.Black,
    modifier = Modifier.glassEffect(backdrop, onAppearanceChanged = { isDark = it == GlassAppearance.Dark }),
)
```

### Under bars

Where content scrolls under floating bars, soften it with `scrollEdgeEffect`. Put it after `glassBackdrop` on the same element so the glass takes the softened content in:

```kotlin
LazyColumn(
    Modifier
        .glassBackdrop(backdrop)
        .scrollEdgeEffect(top = statusBarHeight, bottom = navigationBarHeight)
) { /* content */ }
```

The effect finds the bars by itself: glass on the same backdrop that sits at the top or the bottom of the content. `top` and `bottom` are for what is not glass, such as the system bars; glass counts as a bar while the gap between it and the edge, or other bars there, stays under 46 dp and it does not run past the middle of the content.

There are two looks. `Soft` dissolves content into the page colour (pass `color` if the page is not white or black). `Shaded` darkens it and keeps its colours. The default, `Automatic`, chooses per edge from the content: glass rests on a shade unless what is under it is nearly as bright as a white page, and the edge eases from one look to the other as content scrolls by. An edge without glass is always soft. In a dark interface both looks are the same, and they are a large part of how glass bars look there: they sit on content that has already been taken down to about a seventh of its brightness.

### Rules of thumb

- Place glass outside the `glassBackdrop` content it uses where you can, as a sibling layered above it. Glass that has to move with the content, such as a button on a scrolling page, may be part of it: it then sees the rest of the content, is drawn over all of it and is cut off where a scrolling parent cuts it off. It does not follow a `graphicsLayer` that scales, rotates or fades a parent.
- Apply `glassEffect` after size and padding modifiers so it covers the final bounds.
- Give the backdrop an opaque background.
- Use glass sparingly, for controls and navigation that float above content.

## Sample

The `sample` module shows glass over photos, on a playlist screen with toolbars, search, an alert and a tab bar, over solid colours, a screen with a slider for every property, and a set of test patterns for judging blur and refraction. The Music button on the first tab opens a second screen: the main pages of a music app (home, radio, library, search) with a tab bar that shrinks to a button as you scroll, a player and a search button that stretches into a field, for comparing the glass with a real iOS app side by side.

```
./gradlew :sample:installDebug
```

Sample photos come from [Lorem Picsum](https://picsum.photos) and are provided under the Unsplash licence.

## Roadmap

See [ROADMAP.md](ROADMAP.md).

## Licence

Apache License 2.0. See [LICENSE](LICENSE).
