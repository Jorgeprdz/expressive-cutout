package com.ekoehler.expressivecutout.statusbar

internal data class StatusBarNetworkDisplayInput(
    val wifiConnected: Boolean,
    val cellularConnected: Boolean,
    val cellularDataActive: Boolean,
    val networkType: StatusBarNetworkType?,
)

/** Pure policy for showing the textual mobile data badge. It never invents radio technology. */
internal object StatusBarNetworkDisplayPolicy {
    fun shouldShowMobileNetworkLabel(input: StatusBarNetworkDisplayInput): Boolean =
        !input.wifiConnected &&
            input.cellularConnected &&
            input.cellularDataActive &&
            input.networkType != null

    fun hiddenReason(input: StatusBarNetworkDisplayInput): String? = when {
        input.wifiConnected -> "wifi_active"
        !input.cellularConnected -> "cellular_not_connected"
        !input.cellularDataActive -> "cellular_data_not_active"
        input.networkType == null -> "network_type_missing"
        else -> null
    }
}
