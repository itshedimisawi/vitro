# Vitro

**Liquid Glass for Jetpack Compose.**

Vitro brings Apple's Liquid Glass material to Android as plain Compose modifiers, so any composable can become glass. The API follows SwiftUI's `glassEffect`, and the defaults are tuned to match iOS 26.

- Refraction at the rim, colour fringing, highlights and a soft shadow
- `Regular` and `Clear` glass, tinting, dimming and glass that reacts to touch
- Light and dark materials that adapt to the content behind them
- Nearby glass that blends into one surface
- A scroll edge effect for content passing under bars

## Installation

```kotlin
dependencies {
    implementation("io.github.itshedimisawi:vitro:0.1.0")
}
```

## Quick start

Glass shows a *backdrop*: the content you mark with `glassBackdrop`. Create one, mark the content, and pass it to each piece of glass.

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
            .glassEffect(backdrop, Glass.Regular.interactive())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    )
}
```

## Usage

### Glass variants

```kotlin
Modifier.glassEffect(backdrop)                                    // Glass.Regular in a capsule
Modifier.glassEffect(backdrop, Glass.Clear.dimmed())              // see-through, over photos
Modifier.glassEffect(backdrop, Glass.Regular.tint(Color.Blue))    // a prominent button
Modifier.glassEffect(backdrop, shape = RoundedCornerShape(16.dp))
Modifier.glassEffect(backdrop, if (reduceTransparency) Glass.Identity else Glass.Regular)
```

Every property (blur, refraction, magnification, saturation, light angle, shadow, …) can be changed with `Glass.copy(...)`.

### Content colour and touch

`GlassEffectState` tells you which material the glass shows and how it's being touched. Use it to colour content on glass that picks its appearance itself:

```kotlin
val state = rememberGlassEffectState()
Icon(
    Icons.Rounded.Share,
    contentDescription = "Share",
    tint = if (state.appearance == GlassAppearance.Dark) Color.White else Color.Black,
    modifier = Modifier.size(44.dp).glassEffect(backdrop, Glass.Regular.interactive(), state = state).padding(11.dp),
)
```

To force a material, use `Glass.Regular.copy(appearance = GlassAppearance.Dark)`.

### Grouping glass

Wrap controls that sit together in `glassEffectContainer`. Nearby pieces blend into one surface, and the whole group draws at the cost of one piece. With `spacing = 0.dp` nothing blends.

```kotlin
Row(Modifier.glassEffectContainer(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Icon(Icons.Rounded.Edit, "Edit", Modifier.size(44.dp).glassEffect(backdrop).padding(11.dp))
    Icon(Icons.Rounded.Delete, "Delete", Modifier.size(44.dp).glassEffect(backdrop).padding(11.dp))
}
```

### Scroll edge effect

Softens content where it scrolls under bars. Glass bars are found automatically; pass the system bar heights for the rest.

```kotlin
Column(
    Modifier
        .glassBackdrop(backdrop)
        .scrollEdgeEffect(top = statusBarHeight, bottom = navigationBarHeight)
        .verticalScroll(rememberScrollState()),
) { ... }
```

## API

| Vitro | SwiftUI |
|---|---|
| `Modifier.glassEffect(backdrop, glass, shape)` | `.glassEffect(_:in:)` |
| `Glass.Regular` / `Clear` / `Identity` | `.regular` / `.clear` / `.identity` |
| `.tint(color)`, `.interactive()`, `.dimmed()` | `.tint(_:)`, `.interactive()` |
| `Modifier.glassEffectContainer(spacing)` | `GlassEffectContainer(spacing:)` |
| `Modifier.scrollEdgeEffect(style, isTopHidden, …)` | `.scrollEdgeEffectStyle(_:for:)`, `.scrollEdgeEffectHidden(_:for:)` |
| `rememberGlassBackdrop()`, `Modifier.glassBackdrop(backdrop)` | (implicit) |
| `rememberGlassEffectState()` | |

## Tips

- Apply `glassEffect` after size and padding modifiers.
- Put a `background` after `glassBackdrop`, so the page colour is part of the backdrop.
- Use glass for controls and navigation that float above content, not for the content itself.

## Android versions

| Android | Rendering |
|---|---|
| 13+ (API 33) | Full effect, drawn with an AGSL shader |
| 12 (API 31–32) | Platform blur with an approximated lens |
| 6–11 (API 23–30) | Blurred snapshot with an approximated lens |

## Sample

The `sample` module is a three-screen cinema booking flow (film details, seat selection and ticket) with Liquid Glass toolbars, buttons and chips.

```
./gradlew :sample:installDebug
```

## Building

```
./gradlew :vitro:check
```

This runs the unit tests, lint and a public API check against `vitro/api/vitro.api`. After an intended API change, run `./gradlew :vitro:apiDump`.

## License

Apache License 2.0. See [LICENSE](LICENSE).

Vitro is an independent project and is not affiliated with Apple Inc.
