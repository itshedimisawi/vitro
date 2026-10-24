# Vitro

**Liquid Glass for Jetpack Compose.**

Vitro brings Apple's Liquid Glass material to Android. It is written from scratch as an AGSL shader and exposed as plain Compose `Modifier`s, so any composable can become glass. The API follows SwiftUI's `glassEffect`, and the defaults are calibrated against iOS 26 captures.

- Real refraction at the rim, colour dispersion, specular highlights and a soft shadow
- `Regular` and `Clear` variants, tinting, dimming and touch-reactive glass
- Light and dark materials that adapt to the content behind them
- Nearby glass that merges into one surface
- A scroll edge effect for content passing under bars

## How it works

Content marked with `glassBackdrop` is recorded into a `GraphicsLayer`. Each piece of glass draws the part of that layer behind it through a `RuntimeShader`. The shader computes a signed-distance outline, bends content inside the rim band, and applies blur, saturation, the light or dark veil, tint and the rim light in a single pass. Glass in a `glassEffectContainer` is drawn as a smooth union of up to eight shapes.

| Android | Rendering |
|---|---|
| 13+ (API 33) | AGSL shader with the full effect |
| 12 (API 31–32) | Platform blur, colour matrix and a stepped lens approximation |
| 6–11 (API 23–30) | Downscaled snapshot blurred on the CPU, with the same approximation |

## Installation

```kotlin
dependencies {
    implementation("io.github.itshedimisawi:vitro:0.1.0")
}
```

## Quick start

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

## API

| | SwiftUI equivalent |
|---|---|
| `Modifier.glassBackdrop(backdrop)` | (implicit) |
| `Modifier.glassEffect(backdrop, glass, shape)` | `.glassEffect(_:in:)` |
| `Glass.Regular` / `Clear` / `Identity` | `.regular` / `.clear` / `.identity` |
| `.tint(color)`, `.interactive()`, `.dimmed()` | `.tint(_:)`, `.interactive()` |
| `Modifier.glassEffectContainer(spacing)` | `GlassEffectContainer(spacing:)` |
| `Modifier.scrollEdgeEffect(top, bottom, style)` | `.scrollEdgeEffectStyle(_:for:)` |

Every optical property (blur, refraction, dispersion, magnification, saturation, brightness, light angle, shadow) can be set through `Glass.copy(...)`. Use `GlassAppearance` to force the light or dark material, `onAppearanceChanged` to colour content on automatic glass, and `GlassInteractionState` to let other elements follow the touch on interactive glass.

## Tips

- Apply `glassEffect` after size and padding modifiers.
- Place glass as a sibling above the backdrop content where you can.
- Give the backdrop an opaque background.
- Use glass sparingly, for controls and navigation that float above content.

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
