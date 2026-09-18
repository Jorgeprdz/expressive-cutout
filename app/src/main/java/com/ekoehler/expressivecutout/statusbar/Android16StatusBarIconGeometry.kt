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
) {
    val aspectRatio: Float = sourceViewBoxWidthPx.toFloat() / sourceViewBoxHeightPx.toFloat()

    fun signature(label: String, level: Int?): String = buildString {
        val active = dynamicLevel(level, bars.size)
        append("$label.signal=viewBox=${sourceViewBoxWidthPx}x$sourceViewBoxHeightPx active=$active bars=[")
        append(bars.joinToString(";") { "${it.rect.signature()},r${fmt(it.radiusToWidth)}" })
        append("] lower=[")
        append(lowerIndicators.joinToString(";") { "${it.rect.signature()},r${fmt(it.radiusToWidth)}" })
        append("] baseline=aligned")
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
) {
    val aspectRatio: Float = sourceViewBoxWidthPx.toFloat() / sourceViewBoxHeightPx.toFloat()

    fun signature(label: String, level: Int?): String =
        "$label.wifi=viewBox=${sourceViewBoxWidthPx}x$sourceViewBoxHeightPx " +
            "activeParts=${wifiActiveParts(level)} outer=${outer.signature()},stroke=${fmt(outerStrokeToHeight)} " +
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
) {
    val aspectRatio: Float = sourceViewBoxWidthPx.toFloat() / sourceViewBoxHeightPx.toFloat()

    fun signature(label: String, level: Int?, charging: Boolean): String {
        val safeLevel = (level ?: 0).coerceIn(0, 100)
        val colorMode = if (safeLevel < redThreshold) "critical-red" else "active-fill"
        return "$label.battery=viewBox=${sourceViewBoxWidthPx}x$sourceViewBoxHeightPx " +
            "level=$safeLevel fill=${fmt(safeLevel / 100f)} charging=$charging " +
            "body=${body.signature()},r${fmt(bodyRadiusToHeight)} " +
            "terminal=${terminal.signature()},r${fmt(terminalRadiusToWidth)} " +
            "mode=android16-rounded-rect colorMode=$colorMode bodyColor=$bodyColor activeColor=$activeColor " +
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
        label = "android16",
        signal = Android16MeasuredSignalGeometry(
            sourceViewBoxWidthPx = 149,
            sourceViewBoxHeightPx = 108,
            bars = listOf(
                Android16MeasuredSignalBar(Android16FractionRect(0.000f, 0.250f, 0.161f, 0.398f), 0.500f),
                Android16MeasuredSignalBar(Android16FractionRect(0.282f, 0.157f, 0.161f, 0.491f), 0.500f),
                Android16MeasuredSignalBar(Android16FractionRect(0.564f, 0.074f, 0.161f, 0.574f), 0.500f),
                Android16MeasuredSignalBar(Android16FractionRect(0.846f, 0.019f, 0.148f, 0.630f), 0.500f),
            ),
            lowerIndicators = listOf(
                Android16MeasuredSignalBar(Android16FractionRect(0.007f, 0.759f, 0.148f, 0.241f), 0.500f),
                Android16MeasuredSignalBar(Android16FractionRect(0.289f, 0.759f, 0.148f, 0.241f), 0.500f),
                Android16MeasuredSignalBar(Android16FractionRect(0.570f, 0.759f, 0.148f, 0.241f), 0.500f),
                Android16MeasuredSignalBar(Android16FractionRect(0.846f, 0.759f, 0.148f, 0.241f), 0.500f),
            ),
        ),
        wifi = Android16MeasuredWifiGeometry(
            sourceViewBoxWidthPx = 150,
            sourceViewBoxHeightPx = 113,
            outer = Android16FractionRect(0.000f, 0.000f, 1.000f, 0.407f),
            outerStrokeToHeight = 0.221f,
            middle = Android16FractionRect(0.190f, 0.442f, 0.620f, 0.292f),
            middleStrokeToHeight = 0.221f,
            dot = Android16FractionRect(0.397f, 0.735f, 0.207f, 0.274f),
        ),
        battery = Android16MeasuredBatteryGeometry(
            sourceViewBoxWidthPx = 243,
            sourceViewBoxHeightPx = 121,
            body = Android16FractionRect(0.000f, 0.000f, 0.909f, 1.000f),
            bodyRadiusToHeight = 0.250f,
            terminal = Android16FractionRect(0.951f, 0.298f, 0.049f, 0.405f),
            terminalRadiusToWidth = 0.500f,
            redThreshold = 20,
            bodyColor = "#99A1AA",
            activeColor = "#F0F4F5",
            inactiveColor = "#7F8D97",
            textColor = "#1C1D21",
            criticalColor = "#F50003",
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
