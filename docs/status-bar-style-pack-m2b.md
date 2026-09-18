# M2B.2 — Measured iOS Status Bar Icons

## Why this pass exists

M2B.1 compiled and produced an APK, but it still treated the reference image too abstractly. The result was a style pack that changed names, weights and spacing, yet the iOS families could still read as generic Android-like icons.

M2B.2 narrows the scope: only `iOS 26` and `iOS 27` are recreated in this pass. Pixel, HyperOS, Nothing OS and Default / One UI are preserved but not visually redesigned here.

## Measured-reference rule

The iOS reference image is now treated as measured geometry, not loose inspiration. The source image was measured at 1381 x 1536 px, then converted into normalized viewBox fractions so the runtime renderer can scale the icons without hardcoding screenshot pixels.

The measured geometry lives in:

```text
app/src/main/java/com/ekoehler/expressivecutout/statusbar/IosStatusBarIconGeometry.kt
```

The real Compose renderer lives in:

```text
app/src/main/java/com/ekoehler/expressivecutout/statusbar/IosStatusBarIconRenderer.kt
```

`PixelStatusBar.kt` routes the iOS 26/iOS 27 signal, Wi-Fi and battery paths to these measured iOS renderers instead of the generic Android-like renderer.

## iOS 26 measured contract

### Signal

- source viewBox: `243x159`
- four rounded-rect bars
- radius about `0.25w` to `0.26w`
- baseline aligned
- less pill-like than iOS 27

### Wi-Fi

- source viewBox: `217x171`
- outer arc: `0.000,0.000,1.000,0.392`, stroke `0.140`
- middle arc: `0.171,0.304,0.654,0.304`, stroke `0.143`
- dot: `0.346,0.591,0.309,0.310`, `classic-teardrop`

### Battery

- source viewBox: `342x164`
- mode: `outline-fill`
- gray outline color: `#A0A0A0`
- black inner fill: `0.070,0.146,0.775,0.707`
- gray terminal: `0.947,0.366,0.053,0.299`

## iOS 27 measured contract

### Signal

- source viewBox: `241x156`
- four pill-like bars
- radius about `0.485w` to `0.500w`
- baseline aligned
- visibly more rounded and bold than iOS 26

### Wi-Fi

- source viewBox: `207x161`
- outer arc: `0.000,0.000,1.000,0.410`, stroke `0.158`
- middle arc: `0.174,0.329,0.652,0.317`, stroke `0.160`
- dot: `0.357,0.671,0.285,0.286`, `soft-teardrop`

### Battery

- source viewBox: `342x163`
- mode: `solid-capsule`
- black body: `0.000,0.000,0.912,1.000`
- black terminal: `0.942,0.325,0.058,0.319`
- no numeric percentage

## M2B.4 — iOS battery color option

M2B.4 added the iOS battery color mode setting:

- `Black` / `MONOCHROME`: keeps the measured iOS battery using the current tint.
- `Status` / `STATUS_COLOR`: colors the measured iOS battery by level.

Thresholds:

- `<10%` = red
- `10–19%` = yellow
- `>=20%` = normal tint

This must stay protected whenever Pixel is redesigned.

## M2B.5 — Pixel status bar redesign

Pixel 16/17 was marked `NO PASS` after the first Android 16 measured pass. The previous implementation copied the measured reference too literally and produced a visually experimental result:

- the mobile signal included lower indicator capsules that read like a dot-matrix system instead of Pixel signal bars;
- the Wi-Fi arcs were too inflated;
- the numeric battery felt too pill-like / sticker-like;
- the group did not feel like one coherent Pixel status bar.

M2B.5 keeps runtime state dynamic but replaces the Pixel visual contract with `pixel1617-*` geometry:

```text
app/src/main/java/com/ekoehler/expressivecutout/statusbar/Android16StatusBarIconGeometry.kt
app/src/main/java/com/ekoehler/expressivecutout/statusbar/Android16StatusBarIconRenderer.kt
```

### Pixel signal v2

- source viewBox: `132x108`
- language: `pixel-capsule-bars-v2`
- four single-baseline capsule bars
- lower indicators removed for the visual redesign
- `cellular.level` still controls active bars

### Pixel Wi-Fi v2

- source viewBox: `144x112`
- language: `pixel-bold-arcs-v2`
- arcs are still thick, but less inflated than the previous measured pass
- dot remains circular
- `wifi.level` still controls active parts

### Pixel battery v2

- source viewBox: `224x112`
- language: `pixel-rounded-rect-v2`
- rounded rectangle, not full pill
- separated terminal
- numeric level remains centered
- levels `7`, `19`, `67`, and `100` are covered by dedicated goldens

## Golden visual gate

The JVM golden scene uses `scene=status-bar-golden-v3`.

Goldens include measured/dynamic geometry, not just style labels:

```text
ios27.signal=viewBox=241x156 bars=[...]
ios27.wifi=viewBox=207x161 outer=... middle=... dot=...
ios27.battery=viewBox=342x163 body=... terminal=... mode=solid-capsule
pixel1617.signal=viewBox=132x108 active=...
pixel1617.wifi=viewBox=144x112 activeParts=...
pixel1617.battery=viewBox=224x112 level=... mode=pixel-rounded-rect-v2
```

The golden resources live in:

```text
app/src/test/resources/statusbar-goldens/
```

The test entry point remains:

```text
./gradlew testDebugUnitTest --no-daemon
```

## Families preserved but not redesigned in M2B.5

- Default / One UI
- Pixel 15
- HyperOS
- Nothing OS 5
- iOS 26 / iOS 27 measured geometry

## Known bugs still outside this turn

Still not fixed here:

- Mobile signal strength does not reliably reflect real intensity if the data source does not deliver it.
- Network type badge may not appear reliably in runtime real device data.
- Auto contrast / color switching does not respond correctly to the background.
- Wi-Fi connected can still hide mobile signal in some real states if the data source does not provide both cleanly.

M2B.5 only redesigns the Pixel visual family. It does not invent runtime network data.
