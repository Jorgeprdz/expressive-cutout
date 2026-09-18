# M2B — Status Bar Style Pack

## Added

- `Default / One UI` keeps the existing custom status bar visual behavior as the safe fallback.
- `Pixel 15` inspired style with compact Android-like icon proportions.
- `iOS 27` inspired style with rounded signal bars, softer Wi-Fi arcs and a capsule-like battery.
- `HyperOS` inspired style with denser spacing and flatter compact icons.
- `Nothing OS` inspired style with monochrome, minimal segmented geometry.
- `StatusBarStyleRegistry` / `StatusBarStyleRenderer` infrastructure so visual style selection does not duplicate status state logic.
- Persistent style selection through the existing custom status bar settings store.

All styles are drawn from project-owned Compose/Canvas primitives. No proprietary assets, logos, fonts, glyph files or platform resources are copied.

## Safe fallback

- Invalid persisted style values resolve to `Default / One UI`.
- The custom status bar renderer resolves styles through the registry and falls back to the default renderer if style creation fails.
- Styles change glyph proportions, spacing and text weights only. They do not invent unavailable network data.

## M2A.2 remains partial / known bug

M2A.2 — Pixel Mobile Signal Redesign remains **PARTIAL / KNOWN BUG**.

Known bugs still open:

- Mobile signal strength does not reliably reflect real intensity.
- Network type badge may not appear even when One UI reports 4G / 4G+ / 5G.
- Wi-Fi icon is still not Pixel-perfect.
- Wi-Fi connected should not hide mobile signal when showing both is enabled.
- Auto contrast / color switching does not respond correctly to the background.

## Suggested next milestone

M2A.3 — Signal + Contrast Data Audit

Focus areas:

- `TelephonyManager`
- `SignalStrength`
- `ServiceState`
- `SubscriptionManager`
- `NetworkCapabilities`
- `dumpsys telephony.registry`
- `LIGHT_STATUS_BARS` and launcher / wallpaper background state
