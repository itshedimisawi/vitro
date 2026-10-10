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
| `copy(...)` | Sets any optical property: `blurRadius`, `refractionHeight`, `refractionAmount`, `dispersion`, `saturation`, `brightness`, `appearance`, `lightAngle`, `highlightIntensity` and the shadow. |

### Light and dark

Glass follows the system dark theme by default. Light glass is veiled toward white; dark glass stays close to the colours behind it and only lifts the darkest tones. Force either one with `copy(appearance = GlassAppearance.Dark)`.

### Rules of thumb

- Place glass outside the `glassBackdrop` content, as a sibling layered above it. Glass cannot refract itself.
- Apply `glassEffect` after size and padding modifiers so it covers the final bounds.
- Give the backdrop an opaque background.
- Use glass sparingly, for controls and navigation that float above content.

## Sample

The `sample` module shows glass over photos, over a scrolling feed, over solid colours, a screen with a slider for every property, and a set of test patterns for judging blur and refraction.

```
./gradlew :sample:installDebug
```

Sample photos come from [Lorem Picsum](https://picsum.photos) and are provided under the Unsplash licence.

## Roadmap

See [ROADMAP.md](ROADMAP.md).

## Licence

Apache License 2.0. See [LICENSE](LICENSE).
