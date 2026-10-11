# Roadmap

Planned for the iteration after the core material is finalised.

## Merging and morphing

`glassEffectContainer` joins nearby glass of one material into a single surface, drawn in one pass from a smooth union of the shapes. Still to come:

- Stable identifiers per shape so a shape that appears, disappears or changes position morphs from and into its neighbours instead of fading
- A union option that forces several elements to share one glass shape at rest
- A materialise transition for shapes that are too far apart to morph

## Adaptive legibility

Glass should adapt to what is behind it instead of relying on fixed `brightness` and content colours.

- Sample the average luminance of the backdrop region under each glass element
- Switch the material between a light and a dark appearance with hysteresis, so it does not flicker over mixed content
- Expose the resolved appearance so content placed on the glass can pick a legible colour
- A scroll edge effect that fades and blurs content as it passes under a glass bar

## Further ideas

- Honour system accessibility settings: reduced transparency, increased contrast and reduced motion
- Continuous-curvature corners
- Verify the Android 12 rendering path on a device or emulator image; only Android 11 and Android 16 have been checked so far
- Publish to Maven Central with a checked public API dump
