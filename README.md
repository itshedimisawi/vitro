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

`magnification` scales what is seen through the glass about its centre. Below 1 the glass takes in a wider area than it covers, which keeps the edges of whatever lies under it in view. Glass placed inside the content of another piece of glass does not see that piece.

### Light and dark

Glass follows the system dark theme by default. Light glass is veiled toward white; dark glass stays close to the colours behind it and only lifts the darkest tones. Force either one with `copy(appearance = GlassAppearance.Dark)`.

### Rules of thumb

- Place glass outside the `glassBackdrop` content it uses, as a sibling layered above it. Glass cannot refract itself.
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
