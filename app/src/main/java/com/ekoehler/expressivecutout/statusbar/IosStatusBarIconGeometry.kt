package com.ekoehler.expressivecutout.statusbar

import com.ekoehler.expressivecutout.data.CustomStatusBarStyle
import java.util.Locale

internal data class IosFractionRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    fun signature(): String = listOf(x, y, width, height).joinToString(",") { fmt(it) }
}

internal data class IosMeasuredSignalBar(
    val rect: IosFractionRect,
    val radiusToWidth: Float,
)

internal data class IosMeasuredSignalGeometry(
    val sourceViewBoxWidthPx: Int,
    val sourceViewBoxHeightPx: Int,
    val bars: List<IosMeasuredSignalBar>,
) {
    val aspectRatio: Float = sourceViewBoxWidthPx.toFloat() / sourceViewBoxHeightPx.toFloat()

    fun signature(label: String): String = buildString {
        append("$label.signal=viewBox=${sourceViewBoxWidthPx}x$sourceViewBoxHeightPx bars=[")
        append(
            bars.joinToString(";") { bar ->
                "${bar.rect.signature()},r${fmt(bar.radiusToWidth)}"
            },
        )
        append("] baseline=aligned")
    }
}

internal data class IosMeasuredWifiGeometry(
    val sourceViewBoxWidthPx: Int,
    val sourceViewBoxHeightPx: Int,
    val outer: IosFractionRect,
    val outerStrokeToHeight: Float,
    val middle: IosFractionRect,
    val middleStrokeToHeight: Float,
    val dot: IosFractionRect,
    val dotShape: String,
) {
    val aspectRatio: Float = sourceViewBoxWidthPx.toFloat() / sourceViewBoxHeightPx.toFloat()

    fun signature(label: String): String =
        "$label.wifi=viewBox=${sourceViewBoxWidthPx}x$sourceViewBoxHeightPx " +
            "outer=${outer.signature()},stroke=${fmt(outerStrokeToHeight)} " +
            "middle=${middle.signature()},stroke=${fmt(middleStrokeToHeight)} " +
            "dot=${dot.signature()},shape=$dotShape"
}

internal data class IosMeasuredBatteryGeometry(
    val sourceViewBoxWidthPx: Int,
    val sourceViewBoxHeightPx: Int,
    val body: IosFractionRect,
    val bodyRadiusToHeight: Float,
    val terminal: IosFractionRect,
    val terminalRadiusToWidth: Float,
    val mode: String,
    val bodyColor: String,
    val terminalColor: String,
    val innerFill: IosFractionRect? = null,
    val innerRadiusToHeight: Float? = null,
    val outlineColor: String? = null,
) {
    val aspectRatio: Float = sourceViewBoxWidthPx.toFloat() / sourceViewBoxHeightPx.toFloat()

    fun signature(label: String): String = buildString {
        append("$label.battery=viewBox=${sourceViewBoxWidthPx}x$sourceViewBoxHeightPx ")
        append("body=${body.signature()},r${fmt(bodyRadiusToHeight)} ")
        innerFill?.let { fill ->
            append("inner=${fill.signature()},r${fmt(innerRadiusToHeight ?: 0f)} ")
        }
        append("terminal=${terminal.signature()},r${fmt(terminalRadiusToWidth)} ")
        append("mode=$mode bodyColor=$bodyColor terminalColor=$terminalColor")
        outlineColor?.let { append(" outlineColor=$it") }
    }
}

internal data class IosMeasuredIconSet(
    val label: String,
    val signal: IosMeasuredSignalGeometry,
    val wifi: IosMeasuredWifiGeometry,
    val battery: IosMeasuredBatteryGeometry,
) {
    fun signatureLines(): List<String> = listOf(
        signal.signature(label),
        wifi.signature(label),
        battery.signature(label),
    )
}

internal object IosStatusBarIconGeometry {
    val ios26 = IosMeasuredIconSet(
        label = "ios26",
        signal = IosMeasuredSignalGeometry(
            sourceViewBoxWidthPx = 243,
            sourceViewBoxHeightPx = 159,
            bars = listOf(
                IosMeasuredSignalBar(IosFractionRect(0.000f, 0.585f, 0.181f, 0.390f), 0.250f),
                IosMeasuredSignalBar(IosFractionRect(0.272f, 0.390f, 0.185f, 0.585f), 0.250f),
                IosMeasuredSignalBar(IosFractionRect(0.551f, 0.195f, 0.177f, 0.780f), 0.260f),
                IosMeasuredSignalBar(IosFractionRect(0.823f, 0.000f, 0.177f, 0.975f), 0.260f),
            ),
        ),
        wifi = IosMeasuredWifiGeometry(
            sourceViewBoxWidthPx = 217,
            sourceViewBoxHeightPx = 171,
            outer = IosFractionRect(0.000f, 0.000f, 1.000f, 0.392f),
            outerStrokeToHeight = 0.140f,
            middle = IosFractionRect(0.171f, 0.304f, 0.654f, 0.304f),
            middleStrokeToHeight = 0.143f,
            dot = IosFractionRect(0.346f, 0.591f, 0.309f, 0.310f),
            dotShape = "classic-teardrop",
        ),
        battery = IosMeasuredBatteryGeometry(
            sourceViewBoxWidthPx = 342,
            sourceViewBoxHeightPx = 164,
            body = IosFractionRect(0.000f, 0.000f, 0.915f, 1.000f),
            bodyRadiusToHeight = 0.255f,
            innerFill = IosFractionRect(0.070f, 0.146f, 0.775f, 0.707f),
            innerRadiusToHeight = 0.285f,
            terminal = IosFractionRect(0.947f, 0.366f, 0.053f, 0.299f),
            terminalRadiusToWidth = 0.500f,
            mode = "outline-fill",
            bodyColor = "#000000",
            terminalColor = "#989898",
            outlineColor = "#A0A0A0",
        ),
    )

    val ios27 = IosMeasuredIconSet(
        label = "ios27",
        signal = IosMeasuredSignalGeometry(
            sourceViewBoxWidthPx = 241,
            sourceViewBoxHeightPx = 156,
            bars = listOf(
                IosMeasuredSignalBar(IosFractionRect(0.000f, 0.596f, 0.178f, 0.404f), 0.485f),
                IosMeasuredSignalBar(IosFractionRect(0.270f, 0.417f, 0.187f, 0.583f), 0.489f),
                IosMeasuredSignalBar(IosFractionRect(0.539f, 0.218f, 0.187f, 0.782f), 0.489f),
                IosMeasuredSignalBar(IosFractionRect(0.817f, 0.000f, 0.183f, 0.994f), 0.500f),
            ),
        ),
        wifi = IosMeasuredWifiGeometry(
            sourceViewBoxWidthPx = 207,
            sourceViewBoxHeightPx = 161,
            outer = IosFractionRect(0.000f, 0.000f, 1.000f, 0.410f),
            outerStrokeToHeight = 0.158f,
            middle = IosFractionRect(0.174f, 0.329f, 0.652f, 0.317f),
            middleStrokeToHeight = 0.160f,
            dot = IosFractionRect(0.357f, 0.671f, 0.285f, 0.286f),
            dotShape = "soft-teardrop",
        ),
        battery = IosMeasuredBatteryGeometry(
            sourceViewBoxWidthPx = 342,
            sourceViewBoxHeightPx = 163,
            body = IosFractionRect(0.000f, 0.000f, 0.912f, 1.000f),
            bodyRadiusToHeight = 0.310f,
            terminal = IosFractionRect(0.942f, 0.325f, 0.058f, 0.319f),
            terminalRadiusToWidth = 0.500f,
            mode = "solid-capsule",
            bodyColor = "#000000",
            terminalColor = "#000000",
        ),
    )

    fun forStyle(style: CustomStatusBarStyle): IosMeasuredIconSet? = when (style) {
        CustomStatusBarStyle.IOS_26 -> ios26
        CustomStatusBarStyle.IOS_27 -> ios27
        else -> null
    }
}

private fun fmt(value: Float): String = String.format(Locale.US, "%.3f", value)
