# Roadmap

## Merging and morphing

`glassEffectContainer` joins nearby glass of one material into a single surface. Still to come:

- Stable identifiers per shape so a shape that appears, disappears or changes position morphs from and into its neighbours instead of fading
- A union option that forces several elements to share one glass shape at rest
- A materialise transition for shapes that are too far apart to morph

## Legibility

- Content colour that blends with what is behind the glass, as labels and symbols do on iOS, instead of the flat `GlassEffectState.contentColor`
- A progressive blur in the scroll edge effect, next to the fade it applies now

## Further ideas

- Honour system accessibility settings: reduced transparency, increased contrast and reduced motion
- Stacking order that follows `zIndex` rather than the order glass is added
- Continuous-curvature corners
- Verify the Android 12 rendering path on a device or emulator image
