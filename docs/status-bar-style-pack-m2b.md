# M2B.1 — Real Status Bar Style Redesign

## Why M2B needed a correction

The first M2B style pack added selectable styles, but the visual contract was too weak: unit tests only proved that styles existed and that geometry changed a little. That allowed multiple families to share nearly the same icon grammar.

M2B.1 treats the status bar styles as visual families. A style is not accepted just because an enum exists; it must expose a stable golden scene that captures its icon language, battery model, network-label behavior, spacing and text weights.

## Golden visual gate

The project now includes dependency-free JVM golden visual coverage through `StatusBarGoldenScene` and `StatusBarGoldenSceneTest`.

This pass intentionally uses deterministic visual scenes instead of shipping full Instagram/reference screenshots or proprietary assets. The original images are directional references only; goldens are clean project-owned fixtures under:

```text
app/src/test/resources/statusbar-goldens/
```

The default CI gate runs these tests as part of:

```text
./gradlew testDebugUnitTest --no-daemon
```

If any family accidentally reuses another renderer, changes numeric battery ownership, hides/shows network labels incorrectly, or drifts from its golden contract, the unit test job fails before `assembleDebug`.

## Families

### Default / One UI

Safe fallback. Preserves the existing custom status bar behavior as much as possible and remains the target for invalid persisted style values.

### iOS 26

- classic rounded signal bars
- three clean Wi-Fi arcs
- outline + fill battery
- no numeric battery
- lighter and more classic than iOS 27

### iOS 27

- bold pill-like signal bars
- heavier rounded Wi-Fi arcs
- solid capsule battery
- no numeric battery
- more rounded and visually heavier than iOS 26

### Pixel 15

- classic Android/Pixel bars
- classic Pixel Wi-Fi arcs
- traditional Android-style battery
- no dot matrix and no numeric capsule

### Pixel 16/17

- data-rich modern compact family
- dot-matrix mobile signal
- prominent `5G` / `LTE` / `4G+` network label behavior
- numeric battery capsule
- charging uses the prominent numeric capsule path and is covered by a separate golden

### HyperOS

- compact, balanced Android-style signal bars
- compact clean Wi-Fi
- compact numeric battery capsule
- less dense and less dominant than Pixel 16/17

### Nothing OS 5

- minimal monochrome compact family
- compact minimal bars, not Pixel 16/17 dot matrix
- hidden/discreet network label contract
- solid capsule battery without number

## Updating goldens

Only update a golden when the intended visual language changes. Do not update goldens to hide a regression.

Checklist before changing a golden:

1. Confirm the family still matches the visual brief above.
2. Confirm anti-regression tests still keep key pairs distinct:
   - iOS 26 vs iOS 27
   - Pixel 15 vs Pixel 16/17
   - Pixel 16/17 vs Nothing OS 5
   - Pixel 16/17 vs HyperOS
   - HyperOS vs Nothing OS 5
   - iOS 27 vs Nothing OS 5
3. Keep references clean and project-owned. Do not commit screenshots from Instagram or system asset dumps.

## Known bugs still outside this turn

M2A.2 remains **PARTIAL / KNOWN BUG**.

Still not fixed here:

- Mobile signal strength does not reliably reflect real intensity.
- Network type badge may not appear reliably in runtime real device data.
- Auto contrast / color switching does not respond correctly to the background.
- Wi-Fi connected can still hide mobile signal in some real states if the data source does not provide both cleanly.

M2B.1 only fixes the visual family contract and selector readability. It does not invent runtime network data or override the data-source bugs.
