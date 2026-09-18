package com.ekoehler.expressivecutout.statusbar

import com.ekoehler.expressivecutout.data.BatteryPercentageMode

internal object PixelStatusBarPresentation {
    fun batteryPercentage(level: Int?, mode: BatteryPercentageMode): String? {
        if (mode != BatteryPercentageMode.OUTSIDE) return null
        return level?.coerceIn(0, 100)?.let { "${it}%" }
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
