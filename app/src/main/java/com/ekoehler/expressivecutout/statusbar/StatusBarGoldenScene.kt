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
    ): String {
        val spec = StatusBarStyleRegistry.resolve(style)
        val networkText = if (spec.networkLabelMode == StatusBarNetworkLabelMode.HIDDEN) {
            "hidden"
        } else {
            PixelStatusBarPresentation.networkTypeLabel(StatusBarNetworkType.FIVE_G)
        }
        val batteryShape = batteryShape(spec.batteryVisualMode, charging)
        val measuredIos = IosStatusBarIconGeometry.forStyle(style)
        return buildString {
            appendLine("scene=status-bar-golden-v2")
            appendLine("canvas=${WIDTH}x$HEIGHT background=light")
            appendLine("style=${spec.id.name} display=${spec.displayName} signature=${spec.visualSignatureKey}")
            appendLine(
                "clock=9:41 weight=${spec.clock.weight.name} sizeSp=${fmt(spec.clock.fontSizeSp)}",
            )
            appendLine(
                "network=$networkText mode=${spec.networkLabelMode.name} " +
                    "weight=${spec.networkTypeWeight.name} sizeSp=${fmt(spec.networkTypeFontSizeSp)}",
            )
            if (measuredIos != null) {
                measuredIos.signatureLines().forEach { appendLine(it) }
            } else {
                appendLine(
                    "signal=${spec.signalVisualMode.name} level=4 active=4 geometry=${signalGeometry(spec)}",
                )
                appendLine(
                    "wifi=${spec.wifiVisualMode.name} level=4 arcs=3 stroke=${wifiStroke(spec.wifiVisualMode)}",
                )
                appendLine(
                    "battery=${spec.batteryVisualMode.name} level=${batteryLevel.coerceIn(0, 100)} " +
                        "charging=$charging numeric=${spec.batteryVisualMode.usesInternalPercentage} " +
                        "shape=$batteryShape",
                )
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
}
