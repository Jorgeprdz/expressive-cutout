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
    /**
     * Pixel 16 uses the compact measured geometry approved in M2C.3. M2C.4 adds optical polish:
     * wider mobile bars and symmetric rounded Wi-Fi curves so the tiny runtime glyphs do not read
     * as square or under-weighted on a real launcher screen.
     */
    val pixel1617 = Android16MeasuredIconSet(
        label = "pixel1617",
        signal = Android16MeasuredSignalGeometry(
            sourceViewBoxWidthPx = 38,
            sourceViewBoxHeightPx = 30,
            visualLanguage = "compact-weighted-bars",
            bars = listOf(
                Android16MeasuredSignalBar(Android16FractionRect(0.000f, 0.500f, 0.205f, 0.500f), 0.360f),
                Android16MeasuredSignalBar(Android16FractionRect(0.265f, 0.367f, 0.205f, 0.633f), 0.360f),
                Android16MeasuredSignalBar(Android16FractionRect(0.530f, 0.167f, 0.205f, 0.833f), 0.360f),
                Android16MeasuredSignalBar(Android16FractionRect(0.795f, 0.033f, 0.205f, 0.967f), 0.360f),
            ),
            lowerIndicators = emptyList(),
        ),
        wifi = Android16MeasuredWifiGeometry(
            sourceViewBoxWidthPx = 39,
            sourceViewBoxHeightPx = 29,
            visualLanguage = "compact-symmetric-arcs",
            outer = Android16FractionRect(0.040f, 0.069f, 0.920f, 0.414f),
            outerStrokeToHeight = 0.158f,
            middle = Android16FractionRect(0.240f, 0.448f, 0.520f, 0.310f),
            middleStrokeToHeight = 0.160f,
            dot = Android16FractionRect(0.405f, 0.724f, 0.190f, 0.276f),
        ),
        battery = Android16MeasuredBatteryGeometry(
            sourceViewBoxWidthPx = 55,
            sourceViewBoxHeightPx = 29,
            body = Android16FractionRect(0.000f, 0.000f, 0.891f, 1.000f),
            bodyRadiusToHeight = 0.215f,
            terminal = Android16FractionRect(0.927f, 0.293f, 0.073f, 0.414f),
            terminalRadiusToWidth = 0.500f,
            redThreshold = 20,
            bodyColor = "#99A1AA",
            activeColor = "#151515",
            inactiveColor = "#15151533",
            textColor = "none",
            criticalColor = "#F50003",
            visualLanguage = "compact-solid-pill-v1",
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
