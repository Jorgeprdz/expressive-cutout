package com.ekoehler.expressivecutout.statusbar

import com.ekoehler.expressivecutout.data.BatteryPercentageMode
import com.ekoehler.expressivecutout.data.PixelMobileBarStyle

internal object PixelStatusBarPresentation {
    fun networkTypeLabel(type: StatusBarNetworkType): String = when (type) {
        StatusBarNetworkType.FOUR_G -> "4G"
        StatusBarNetworkType.FOUR_G_PLUS -> "4G+"
        StatusBarNetworkType.LTE -> "LTE"
        StatusBarNetworkType.FIVE_G -> "5G"
    }

    fun mobileSignalWidthDp(style: PixelMobileBarStyle, scale: Float): Float =
        PixelStatusBarGeometry.mobileWidthDp(style) * scale.coerceAtLeast(0.1f)

    fun batteryPercentage(level: Int?, mode: BatteryPercentageMode): String? {
        if (mode != BatteryPercentageMode.OUTSIDE) return null
        return level?.coerceIn(0, 100)?.let { "${it}%" }
    }

    fun networkTypeWidthDp(type: StatusBarNetworkType, scale: Float): Float {
        val safe = scale.coerceAtLeast(0.1f)
        val base = when (type) {
            StatusBarNetworkType.FOUR_G,
            StatusBarNetworkType.FIVE_G,
            -> 14.5f
            StatusBarNetworkType.FOUR_G_PLUS -> 19f
            StatusBarNetworkType.LTE -> 17f
        }
        return base * safe
    }

    fun batteryPercentageWidthDp(text: String, scale: Float): Float {
        val safe = scale.coerceAtLeast(0.1f)
        return (text.length * 5.2f + 1.5f) * safe
    }
}

internal object CustomStatusBarPreviewState {
    fun create(): CustomStatusBarDeviceState = CustomStatusBarDeviceState(
        timeText = "9:41",
        battery = StatusBarBatteryState(level = 82, charging = false, full = false),
        wifi = StatusBarWifiState(connected = true, level = 4),
        cellular = StatusBarCellularState(
            connected = true,
            level = 4,
            networkType = StatusBarNetworkType.FIVE_G,
        ),
    )
}
