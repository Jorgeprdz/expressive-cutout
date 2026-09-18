package com.ekoehler.expressivecutout.statusbar

import java.util.Locale

internal data class Android16FractionRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    fun signature(): String = listOf(x, y, width, height).joinToString(",") { fmt(it) }
}

internal data class Android16MeasuredSignalBar(
    val rect: Android16FractionRect,
    val radiusToWidth: Float,
)

internal data class Android16MeasuredSignalGeometry(
    val sourceViewBoxWidthPx: Int,
    val sourceViewBoxHeightPx: Int,
    val bars: List<Android16MeasuredSignalBar>,
    val lowerIndicators: List<Android16MeasuredSignalBar>,
    val visualLanguage: String,
) {
    val aspectRatio: Float = sourceViewBoxWidthPx.toFloat() / sourceViewBoxHeightPx.toFloat()

    fun signature(label: String, level: Int?): String = buildString {
        val active = dynamicLevel(level, bars.size)
        append("$label.signal=viewBox=${sourceViewBoxWidthPx}x$sourceViewBoxHeightPx active=$active ")
        append("language=$visualLanguage bars=[")
        append(bars.joinToString(";") { "${it.rect.signature()},r${fmt(it.radiusToWidth)}" })
        append("] lower=[")
        append(lowerIndicators.joinToString(";") { "${it.rect.signature()},r${fmt(it.radiusToWidth)}" })
        append("] baseline=single optically-centered")
    }
}

internal data class Android16MeasuredWifiGeometry(
    val sourceViewBoxWidthPx: Int,
    val sourceViewBoxHeightPx: Int,
    val outer: Android16FractionRect,
    val outerStrokeToHeight: Float,
    val middle: Android16FractionRect,
    val middleStrokeToHeight: Float,
    val dot: Android16FractionRect,
    val visualLanguage: String,
) {
    val aspectRatio: Float = sourceViewBoxWidthPx.toFloat() / sourceViewBoxHeightPx.toFloat()

    fun signature(label: String, level: Int?): String =
        "$label.wifi=viewBox=${sourceViewBoxWidthPx}x$sourceViewBoxHeightPx " +
            "activeParts=${wifiActiveParts(level)} language=$visualLanguage " +
            "outer=${outer.signature()},stroke=${fmt(outerStrokeToHeight)} " +
            "middle=${middle.signature()},stroke=${fmt(middleStrokeToHeight)} " +
            "dot=${dot.signature()},shape=circle"
}

internal data class Android16MeasuredBatteryGeometry(
    val sourceViewBoxWidthPx: Int,
    val sourceViewBoxHeightPx: Int,
    val body: Android16FractionRect,
    val bodyRadiusToHeight: Float,
    val terminal: Android16FractionRect,
    val terminalRadiusToWidth: Float,
    val redThreshold: Int,
    val bodyColor: String,
    val activeColor: String,
    val inactiveColor: String,
    val textColor: String,
    val criticalColor: String,
    val visualLanguage: String,
) {
    val aspectRatio: Float = sourceViewBoxWidthPx.toFloat() / sourceViewBoxHeightPx.toFloat()

    fun signature(label: String, level: Int?, charging: Boolean): String {
        val safeLevel = (level ?: 0).coerceIn(0, 100)
        val colorMode = when {
            charging -> "charging-green"
            safeLevel < redThreshold -> "critical-red"
            else -> "pixel-fill"
        }
        return "$label.battery=viewBox=${sourceViewBoxWidthPx}x$sourceViewBoxHeightPx " +
            "level=$safeLevel fill=${fmt(safeLevel / 100f)} charging=$charging " +
            "body=${body.signature()},r${fmt(bodyRadiusToHeight)} " +
            "terminal=${terminal.signature()},r${fmt(terminalRadiusToWidth)} " +
            "mode=$visualLanguage colorMode=$colorMode bodyColor=$bodyColor activeColor=$activeColor " +
            "inactiveColor=$inactiveColor textColor=$textColor criticalColor=$criticalColor"
    }
}

internal data class Android16MeasuredIconSet(
    val label: String,
    val signal: Android16MeasuredSignalGeometry,
    val wifi: Android16MeasuredWifiGeometry,
    val battery: Android16MeasuredBatteryGeometry,
) {
    fun signatureLines(
        batteryLevel: Int?,
        charging: Boolean,
        wifiLevel: Int?,
        cellularLevel: Int?,
    ): List<String> = listOf(
        signal.signature(label, cellularLevel),
        wifi.signature(label, wifiLevel),
        battery.signature(label, batteryLevel, charging),
    )
}

internal object Android16StatusBarIconGeometry {
    val pixel1617 = Android16MeasuredIconSet(
        label = "pixel1617",
        signal = Android16MeasuredSignalGeometry(
            sourceViewBoxWidthPx = 132,
            sourceViewBoxHeightPx = 108,
            visualLanguage = "pixel-capsule-bars-v2",
            bars = listOf(
                Android16MeasuredSignalBar(Android16FractionRect(0.000f, 0.535f, 0.152f, 0.385f), 0.500f),
                Android16MeasuredSignalBar(Android16FractionRect(0.283f, 0.430f, 0.152f, 0.490f), 0.500f),
                Android16MeasuredSignalBar(Android16FractionRect(0.566f, 0.325f, 0.152f, 0.595f), 0.500f),
                Android16MeasuredSignalBar(Android16FractionRect(0.848f, 0.225f, 0.144f, 0.695f), 0.500f),
            ),
            lowerIndicators = emptyList(),
        ),
        wifi = Android16MeasuredWifiGeometry(
            sourceViewBoxWidthPx = 144,
            sourceViewBoxHeightPx = 112,
            visualLanguage = "pixel-bold-arcs-v2",
            outer = Android16FractionRect(0.030f, 0.040f, 0.940f, 0.392f),
            outerStrokeToHeight = 0.196f,
            middle = Android16FractionRect(0.235f, 0.445f, 0.530f, 0.286f),
            middleStrokeToHeight = 0.188f,
            dot = Android16FractionRect(0.408f, 0.742f, 0.184f, 0.237f),
        ),
        battery = Android16MeasuredBatteryGeometry(
            sourceViewBoxWidthPx = 224,
            sourceViewBoxHeightPx = 112,
            body = Android16FractionRect(0.000f, 0.040f, 0.897f, 0.920f),
            bodyRadiusToHeight = 0.240f,
            terminal = Android16FractionRect(0.942f, 0.330f, 0.049f, 0.340f),
            terminalRadiusToWidth = 0.500f,
            redThreshold = 20,
            bodyColor = "#99A1AA",
            activeColor = "#F0F4F5",
            inactiveColor = "#7F8D97",
            textColor = "#1C1D21",
            criticalColor = "#F50003",
            visualLanguage = "pixel-rounded-rect-v2",
        ),
    )
}

internal fun dynamicLevel(level: Int?, max: Int): Int = (level ?: max).coerceIn(0, max)

internal fun wifiActiveParts(level: Int?): Int = when ((level ?: 4).coerceIn(0, 4)) {
    0 -> 0
    1 -> 1
    2 -> 2
    else -> 3
}

private fun fmt(value: Float): String = String.format(Locale.US, "%.3f", value)
