package com.ekoehler.expressivecutout.statusbar

internal data class StatusBarRightGroupVisibility(
    val showNetwork: Boolean,
    val showPercentage: Boolean,
)

/**
 * Pure fit policy for the right status-bar group.
 *
 * Mobile network identity is only shown while the device is effectively on mobile data. When Wi-Fi
 * is connected, the custom status bar should not show 5G/LTE/4G+ next to Wi-Fi.
 */
internal object StatusBarRightGroupVisibilityPolicy {
    fun resolve(
        wifiConnected: Boolean,
        networkAvailable: Boolean,
        outsidePercentageAvailable: Boolean,
        bothFit: Boolean,
        networkFitsWithoutPercentage: Boolean,
        percentageFitsWithoutNetwork: Boolean,
    ): StatusBarRightGroupVisibility {
        val networkEligible = networkAvailable && !wifiConnected
        return when {
            bothFit -> StatusBarRightGroupVisibility(
                showNetwork = networkEligible,
                showPercentage = outsidePercentageAvailable,
            )
            networkEligible && networkFitsWithoutPercentage -> StatusBarRightGroupVisibility(
                showNetwork = true,
                showPercentage = false,
            )
            percentageFitsWithoutNetwork -> StatusBarRightGroupVisibility(
                showNetwork = false,
                showPercentage = true,
            )
            else -> StatusBarRightGroupVisibility(
                showNetwork = false,
                showPercentage = false,
            )
        }
    }
}
