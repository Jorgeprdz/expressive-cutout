# Status bar style pack notes

## M2B.2 — Measured iOS Status Bar Icons

M2B.2 made the iOS reference image a measured geometry source instead of loose inspiration. The source image was measured, normalized into viewBox fractions, and routed through measured renderers so runtime scaling does not hardcode screenshot pixels.

Measured geometry:

```text
app/src/main/java/com/ekoehler/expressivecutout/statusbar/IosStatusBarIconGeometry.kt
```

Measured renderer:

```text
app/src/main/java/com/ekoehler/expressivecutout/statusbar/IosStatusBarIconRenderer.kt
```

`PixelStatusBar.kt` routes iOS 26/iOS 27 signal, Wi-Fi and battery paths to the measured iOS renderers instead of the generic Android-like renderer.

### iOS 26 measured contract

- Signal source viewBox: `243x159`.
- Wi-Fi source viewBox: `217x171`.
- Battery source viewBox: `342x164`.
- Battery mode: `outline-fill`.

### iOS 27 measured contract

- Signal source viewBox: `241x156`.
- Wi-Fi source viewBox: `207x161`.
- Battery source viewBox: `342x163`.
- Battery mode: `solid-capsule`.
- No numeric percentage inside the battery.

## M2B.4 — iOS battery color option

M2B.4 added the iOS battery color mode setting:

- `Black` / `MONOCHROME`: keeps the measured iOS battery using the current tint.
- `Status` / `STATUS_COLOR`: colors the measured iOS battery by level.

Thresholds:

- `<10%` = red.
- `10–19%` = yellow.
- `>=20%` = normal tint.

This remains protected whenever Pixel is redesigned.

## M2B.5 — Pixel status bar redesign history

Pixel 16/17 was marked `NO PASS` after the first Android 16 measured pass. The previous implementation copied the measured reference too literally and produced a visually experimental result:

- the mobile signal included lower indicator capsules that read like a dot-matrix system instead of compact signal bars;
- the Wi-Fi arcs were too inflated;
- the numeric battery felt too pill-like / sticker-like;
- the group did not feel like one coherent status bar.

M2B.5 kept runtime state dynamic but used the older `pixel1617-*` geometry:

```text
app/src/main/java/com/ekoehler/expressivecutout/statusbar/Android16StatusBarIconGeometry.kt
app/src/main/java/com/ekoehler/expressivecutout/statusbar/Android16StatusBarIconRenderer.kt
```

That old Pixel contract was:

- signal source viewBox: `132x108`, language `pixel-capsule-bars-v2`;
- Wi-Fi source viewBox: `144x112`, language `pixel-bold-arcs-v2`;
- battery source viewBox: `224x112`, language `pixel-rounded-rect-v2`;
- battery showed centered numeric level.

M2C.3 supersedes that Pixel visual contract.

## M2B.7 — Real status sources

M2B.7 fixes the actual data-source problem behind the failed quick fixes. The renderer remains fail-closed and the runtime state owns real inputs.

### Auto color

The Auto color path follows the same architecture class as O.status without copying visuals or assets:

- Shizuku owns a `StatusBarAppearanceUserService`.
- The user service starts `/system/bin/logcat -v threadtime WindowManager:D *:S`.
- It parses `updateSystemBarAttributes` and `statusBarAprRegions=`.
- It correlates the appearance event with the current focused `Window{...}`.
- It debounces duplicate events.
- It falls back to `/system/bin/dumpsys window` and `mLastAppearance=`.
- It emits only a normalized `LIGHT_STATUS_BARS` boolean back to the app process.

The app process converts that boolean into `SystemBarAppearanceSnapshot(globalAppearance = 0x8 or 0)`, and the existing resolver keeps Android semantics intact:

- `LIGHT_STATUS_BARS` = dark icons.
- missing flag = light icons.

### Mobile network label

- Wi-Fi connected hides the mobile text label.
- Mobile data without Wi-Fi can show the label if a real network type exists.
- Missing network type does not invent `5G`, `LTE`, or `4G+`.

The Shizuku telephony user service resolves display info from the default data subscription before falling back to the base `TelephonyManager`, which avoids reading the wrong SIM on dual-SIM devices.

## M2C — App visual redesign + style simplification

M2C turns the status-bar pack from an experimental catalogue into a focused product surface.

### Product decision

Only two styles are visible and selectable:

- `iOS 27`
- `Pixel 16`

The old families are no longer visible in the app UI:

- `Default / One UI`
- `iOS 26`
- `Pixel 15`
- `HyperOS`
- `Nothing OS 5`

The internal enum values remain for safe preference migration and backward compatibility, but `StatusBarStyleRegistry.allStyles` exposes only the two approved styles.

### Legacy preference migration

Persisted legacy styles migrate silently:

- `IOS_26` -> `IOS_27`.
- `PIXEL_15` -> `PIXEL_16_17`.
- `PIXEL_16` / `PIXEL_17` -> `PIXEL_16_17`.
- `HYPER_OS` -> `PIXEL_16_17`.
- `NOTHING_OS` / `NOTHING_OS_5` -> `PIXEL_16_17`.
- `DEFAULT` / `ONE_UI` / `ONE_UI_EXISTING` -> `PIXEL_16_17`.
- unknown values -> `IOS_27`.

`CustomStatusBarSettings.sanitized()` also normalizes any in-memory legacy value before rendering or persistence.

### UI changes

`CustomStatusBarScreen` was redesigned around a simpler hierarchy:

- premium preview card at the top;
- two-style segmented selector;
- compact icon appearance control;
- single master scale control;
- fine alignment controls for clock/right indicators;
- iOS battery color card shown only for `iOS 27`;
- compact icon rhythm controls for scale, spacing and battery scale.

M2C does not change Dynamic Island, Live Activities, Shizuku ownership, Auto color, network label source or iOS battery thresholds.

## M2C.3 — Pixel icon correction using compact geometry

M2C.3 replaces the previous Pixel 16 renderer contract because the old `pixel16-data-rich` look still read as visually wrong in product review.

The new decision is:

```text
Visible style: Pixel 16
Internal visual base: compact measured geometry derived from the Nothing-like reference
```

`Nothing OS 5` is not reintroduced as a visible style. Its measured compact language is reused internally only as the corrected Pixel 16 geometry.

### Files involved

```text
app/src/main/java/com/ekoehler/expressivecutout/statusbar/Android16StatusBarIconGeometry.kt
app/src/main/java/com/ekoehler/expressivecutout/statusbar/Android16StatusBarIconRenderer.kt
app/src/main/java/com/ekoehler/expressivecutout/statusbar/StatusBarStyleRegistry.kt
app/src/main/java/com/ekoehler/expressivecutout/statusbar/PixelStatusBar.kt
```

### Pixel Wi-Fi compact geometry

- source viewBox: `39x29`.
- language: `compact-bold-arcs`.
- runtime size: `19dp`.
- two thick rounded arcs plus a round dot.
- `wifi.level` remains dynamic:
  - `0`: inactive;
  - `1`: dot;
  - `2`: dot + middle arc;
  - `3/4`: dot + middle arc + outer arc.

### Pixel signal compact geometry

- source viewBox: `38x30`.
- language: `compact-measured-bars`.
- runtime size: `18.5dp x 14.5dp`.
- four narrow vertical bars on one baseline.
- `cellular.level` remains dynamic from 0 to 4 active bars.

### Pixel battery compact geometry

- source viewBox: `55x29`.
- language: `compact-solid-pill-v1`.
- runtime total size: `27dp x 14dp`.
- body is roughly `24dp x 14dp`.
- terminal is roughly `2dp x 6dp` with a small gap.
- internal numeric percentage is intentionally hidden because it broke the compact measured look.
- `battery.level` remains dynamic through fill width.
- low battery still uses the critical color path.
- charging still has a charging visual path.

### Preserved behaviour

M2C.3 does not change:

- visible style list (`iOS 27`, `Pixel 16` only);
- iOS 27 geometry;
- iOS battery color thresholds;
- Auto color;
- mobile network type source;
- network label policy;
- fine alignment controls;
- Dynamic Island;
- Live Activity backend;
- music, calls, timers or notifications;
- debug signing.

### Golden visual gate

The JVM golden scene continues to use:

```text
scene=status-bar-golden-v3
```

M2C.3 updates the Pixel contract to:

```text
style=PIXEL_16_17 display=Pixel 16 signature=pixel16-compact-measured
pixel1617.signal=viewBox=38x30 active=... language=compact-measured-bars
pixel1617.wifi=viewBox=39x29 activeParts=... language=compact-bold-arcs
pixel1617.battery=viewBox=55x29 level=... mode=compact-solid-pill-v1
batteryText=hidden shape=compact-solid-dynamic
```

The test entry point remains:

```text
./gradlew testDebugUnitTest --no-daemon
```

## Families preserved but not redesigned

The old visual families remain only as migration inputs. They are not selectable from the UI.

## Known limitations outside this turn

Still outside this turn:

- Mobile signal strength still depends on platform/OEM signal data quality.
- If Shizuku is not ready, Auto color falls back safely instead of guessing.
- If TelephonyDisplayInfo and dumpsys both fail, the mobile text label remains hidden instead of inventing a label.
