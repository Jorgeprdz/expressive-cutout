package com.ekoehler.expressivecutout.statusbar

import com.ekoehler.expressivecutout.data.CustomStatusBarStyle
import java.util.Locale

/**
 * Dependency-free visual golden scene for status-bar style families.
 *
 * This is intentionally deterministic and JVM-testable: it captures the rendered family's visual
 * contract (glyph language, numeric battery ownership, network-label behavior, spacing, and text
 * weight) without pulling emulator-only screenshot infrastructure into the default CI path.
 */
internal object StatusBarGoldenScene {
    private const val WIDTH = 240
    private const val HEIGHT = 36

    fun render(
        style: CustomStatusBarStyle,
        batteryLevel: Int = 67,
        charging: Boolean = false,
        wifiLevel: Int? = 4,
        cellularLevel: Int? = 4,
    ): String {
        val spec = StatusBarStyleRegistry.resolve(style)
        val resolvedStyle = spec.id
        val networkText = if (spec.networkLabelMode == StatusBarNetworkLabelMode.HIDDEN) {
            "hidden"
        } else {
            PixelStatusBarPresentation.networkTypeLabel(StatusBarNetworkType.FIVE_G)
        }
        val batteryShape = batteryShape(spec.batteryVisualMode, charging)
        val measuredIos = IosStatusBarIconGeometry.forStyle(resolvedStyle)
        val measuredAndroid16 = if (resolvedStyle == CustomStatusBarStyle.PIXEL_16_17) {
            Android16StatusBarIconGeometry.pixel1617
        } else {
            null
        }
        return buildString {
            appendLine("scene=status-bar-golden-v3")
            appendLine("canvas=${WIDTH}x$HEIGHT background=light")
            appendLine("style=${spec.id.name} display=${spec.displayName} signature=${spec.visualSignatureKey}")
            appendLine(
                "clock=9:41 weight=${spec.clock.weight.name} sizeSp=${fmt(spec.clock.fontSizeSp)}",
            )
            appendLine(
                "network=$networkText mode=${spec.networkLabelMode.name} " +
                    "weight=${spec.networkTypeWeight.name} sizeSp=${fmt(spec.networkTypeFontSizeSp)}",
            )
            when {
                measuredIos != null -> {
                    measuredIos.signatureLines().forEach { appendLine(it) }
                    appendLine("${measuredIos.label}.signalState=level=${cellularLevel ?: 4} active=${dynamicLevel(cellularLevel, 4)}")
                    appendLine("${measuredIos.label}.wifiState=level=${wifiLevel ?: 4} activeParts=${wifiActiveParts(wifiLevel)}")
                    appendLine(
                        "${measuredIos.label}.batteryState=level=${batteryLevel.coerceIn(0, 100)} " +
                            "fill=${fmt3(batteryLevel.coerceIn(0, 100) / 100f)} charging=$charging",
                    )
                }
                measuredAndroid16 != null -> {
                    measuredAndroid16.signatureLines(
                        batteryLevel = batteryLevel,
                        charging = charging,
                        wifiLevel = wifiLevel,
                        cellularLevel = cellularLevel,
                    ).forEach { appendLine(it) }
                }
                else -> {
                    appendLine(
                        "signal=${spec.signalVisualMode.name} level=${cellularLevel ?: 4} " +
                            "active=${dynamicLevel(cellularLevel, 4)} geometry=${signalGeometry(spec)}",
                    )
                    appendLine(
                        "wifi=${spec.wifiVisualMode.name} level=${wifiLevel ?: 4} " +
                            "activeParts=${wifiActiveParts(wifiLevel)} stroke=${wifiStroke(spec.wifiVisualMode)}",
                    )
                    appendLine(
                        "battery=${spec.batteryVisualMode.name} level=${batteryLevel.coerceIn(0, 100)} " +
                            "fill=${fmt3(batteryLevel.coerceIn(0, 100) / 100f)} " +
                            "charging=$charging numeric=${spec.batteryVisualMode.usesInternalPercentage} " +
                            "shape=$batteryShape",
                    )
                }
            }
            append(
                "spacingDp=${fmt(spec.spacingDp(4f))} edgeInsetDp=${fmt(spec.edgeInsetDp)}",
            )
        }
    }

    fun visualDistance(left: String, right: String): Int {
        val leftLines = left.lines()
        val rightLines = right.lines()
        val max = maxOf(leftLines.size, rightLines.size)
        return (0 until max).count { index -> leftLines.getOrNull(index) != rightLines.getOrNull(index) }
    }

    private fun signalGeometry(style: StatusBarStyle): String = when (style.signalVisualMode) {
        StatusBarSignalVisualMode.CLASSIC_BARS -> when (style.id) {
            CustomStatusBarStyle.PIXEL_15 -> "pixel-classic-bars"
            CustomStatusBarStyle.HYPER_OS -> "compact-balanced-bars"
            else -> "classic-rounded-bars"
        }
        StatusBarSignalVisualMode.IOS_ROUNDED_BARS -> "classic-rounded-bars"
        StatusBarSignalVisualMode.IOS_BOLD_PILLS -> "bold-pill-bars"
        StatusBarSignalVisualMode.DOT_MATRIX -> "active-dot-matrix"
        StatusBarSignalVisualMode.COMPACT_MINIMAL_BARS -> "minimal-monochrome-bars"
    }

    private fun wifiStroke(mode: StatusBarWifiVisualMode): String = when (mode) {
        StatusBarWifiVisualMode.CLASSIC_ARCS -> "balanced"
        StatusBarWifiVisualMode.IOS_ARCS -> "medium-rounded"
        StatusBarWifiVisualMode.IOS_BOLD_ARCS -> "bold-rounded"
        StatusBarWifiVisualMode.PIXEL_COMPACT_ARCS -> "compact-bold"
        StatusBarWifiVisualMode.HYPER_COMPACT_ARCS -> "compact-medium"
        StatusBarWifiVisualMode.NOTHING_MINIMAL_ARCS -> "minimal-thin"
    }

    private fun batteryShape(mode: StatusBarBatteryVisualMode, charging: Boolean): String = when (mode) {
        StatusBarBatteryVisualMode.CLASSIC_ANDROID -> "android-outline"
        StatusBarBatteryVisualMode.IOS_OUTLINE_FILL -> "outline-fill"
        StatusBarBatteryVisualMode.IOS_SOLID_CAPSULE -> "solid-capsule"
        StatusBarBatteryVisualMode.NUMERIC_CAPSULE_PROMINENT ->
            if (charging) "green-number-pill-bolt" else "large-number-pill"
        StatusBarBatteryVisualMode.NUMERIC_CAPSULE_COMPACT -> "compact-number-pill"
        StatusBarBatteryVisualMode.SOLID_CAPSULE_MINIMAL -> "minimal-solid-pill"
    }

    private fun fmt(value: Float): String = String.format(Locale.US, "%.2f", value)
    private fun fmt3(value: Float): String = String.format(Locale.US, "%.3f", value)
}
