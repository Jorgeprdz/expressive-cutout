package com.ekoehler.expressivecutout.statusbar

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal enum class StatusBarNetworkType {
    FOUR_G,
    FOUR_G_PLUS,
    LTE,
    FIVE_G,
}

internal data class StatusBarBatteryState(
    val level: Int? = null,
    val charging: Boolean = false,
    val full: Boolean = false,
)

internal data class StatusBarWifiState(
    val connected: Boolean = false,
    val level: Int? = null,
)

internal data class StatusBarCellularState(
    val connected: Boolean = false,
    val level: Int? = null,
    val networkType: StatusBarNetworkType? = null,
)

internal data class CustomStatusBarDeviceState(
    val timeText: String = "",
    val battery: StatusBarBatteryState = StatusBarBatteryState(),
    val wifi: StatusBarWifiState = StatusBarWifiState(),
    val cellular: StatusBarCellularState = StatusBarCellularState(),
)

/** Pure state transitions so platform callbacks never have to mutate unrelated status-bar fields. */
internal object CustomStatusBarDeviceReducer {

    fun withClock(state: CustomStatusBarDeviceState, timeText: String): CustomStatusBarDeviceState =
        state.copy(timeText = timeText)

    fun withBattery(
        state: CustomStatusBarDeviceState,
        level: Int?,
        charging: Boolean,
        full: Boolean,
    ): CustomStatusBarDeviceState = state.copy(
        battery = StatusBarBatteryState(
            level = level?.coerceIn(0, 100),
            charging = charging,
            full = full,
        ),
    )

    fun withWifi(
        state: CustomStatusBarDeviceState,
        connected: Boolean,
        level: Int?,
    ): CustomStatusBarDeviceState = state.copy(
        wifi = StatusBarWifiState(
            connected = connected,
            level = level.takeIf { connected }?.coerceIn(0, 4),
        ),
    )

    fun withCellularSignal(
        state: CustomStatusBarDeviceState,
        level: Int?,
    ): CustomStatusBarDeviceState = state.copy(
        cellular = state.cellular.copy(
            level = level?.coerceIn(0, 4),
        ),
    )

    fun withCellularPresence(
        state: CustomStatusBarDeviceState,
        connected: Boolean,
        level: Int?,
    ): CustomStatusBarDeviceState = state.copy(
        cellular = state.cellular.copy(
            connected = connected,
            level = level.takeIf { connected }?.coerceIn(0, 4),
            networkType = state.cellular.networkType.takeIf { connected },
        ),
    )

    fun withCellularNetworkType(
        state: CustomStatusBarDeviceState,
        networkType: StatusBarNetworkType?,
    ): CustomStatusBarDeviceState = state.copy(
        cellular = state.cellular.copy(
            connected = state.cellular.connected || networkType != null,
            networkType = networkType,
        ),
    )

    fun withCellular(
        state: CustomStatusBarDeviceState,
        connected: Boolean,
        level: Int?,
        networkType: StatusBarNetworkType?,
    ): CustomStatusBarDeviceState = state.copy(
        cellular = StatusBarCellularState(
            connected = connected,
            level = level.takeIf { connected }?.coerceIn(0, 4),
            networkType = networkType.takeIf { connected },
        ),
    )
}

/**
 * Conservative dBm buckets for the four-step Pixel-style glyphs.
 * Int.MIN_VALUE is NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED.
 */
internal object StatusBarSignalLevelMapper {

    fun wifiFromDbm(dbm: Int?): Int? = when {
        dbm == null || dbm == Int.MIN_VALUE -> null
        dbm >= -55 -> 4
        dbm >= -65 -> 3
        dbm >= -75 -> 2
        dbm >= -85 -> 1
        else -> 0
    }

    fun cellularFromPlatformLevel(level: Int?): Int? =
        level?.coerceIn(0, 4)

    fun cellularFromDbm(dbm: Int?): Int? = when {
        dbm == null || dbm == Int.MIN_VALUE -> null
        dbm >= -90 -> 4
        dbm >= -100 -> 3
        dbm >= -110 -> 2
        dbm >= -120 -> 1
        else -> 0
    }
}

/**
 * Process-local persistent device state fed by the already-existing SystemEventMonitor callbacks.
 * The renderer only observes this flow; it never registers duplicate battery/network listeners.
 */
internal object CustomStatusBarDeviceStateStore {
    private val _state = MutableStateFlow(CustomStatusBarDeviceState())
    val state: StateFlow<CustomStatusBarDeviceState> = _state.asStateFlow()

    fun updateClock(timeText: String) {
        _state.update { CustomStatusBarDeviceReducer.withClock(it, timeText) }
    }

    fun updateBattery(level: Int?, charging: Boolean, full: Boolean) {
        _state.update { CustomStatusBarDeviceReducer.withBattery(it, level, charging, full) }
    }

    fun updateWifi(connected: Boolean, level: Int?) {
        _state.update { CustomStatusBarDeviceReducer.withWifi(it, connected, level) }
    }

    fun updateCellularSignal(level: Int?) {
        _state.update {
            CustomStatusBarDeviceReducer.withCellularSignal(it, level)
        }
    }

    fun updateCellularPresence(connected: Boolean, level: Int?) {
        _state.update {
            CustomStatusBarDeviceReducer.withCellularPresence(it, connected, level)
        }
    }

    fun updateCellularNetworkType(networkType: StatusBarNetworkType?) {
        _state.update {
            CustomStatusBarDeviceReducer.withCellularNetworkType(it, networkType)
        }
    }

    fun updateCellular(
        connected: Boolean,
        level: Int?,
        networkType: StatusBarNetworkType?,
    ) {
        _state.update {
            CustomStatusBarDeviceReducer.withCellular(it, connected, level, networkType)
        }
    }
}
