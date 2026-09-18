package com.ekoehler.expressivecutout.statusbar

internal data class StatusBarRightGroupVisibility(
    val showNetwork: Boolean,
    val showPercentage: Boolean,
)

/**
 * Pure fit policy for the right status-bar group.
 *
 * Network identity is more important than an optional outside battery percentage. This keeps
 * 5G/LTE/4G+ visible on compact layouts, including when Wi-Fi is connected and both cannot fit.
 */
internal object StatusBarRightGroupVisibilityPolicy {
    fun resolve(
        networkAvailable: Boolean,
        outsidePercentageAvailable: Boolean,
        bothFit: Boolean,
        networkFitsWithoutPercentage: Boolean,
        percentageFitsWithoutNetwork: Boolean,
    ): StatusBarRightGroupVisibility = when {
        bothFit -> StatusBarRightGroupVisibility(
            showNetwork = networkAvailable,
            showPercentage = outsidePercentageAvailable,
        )
        networkFitsWithoutPercentage -> StatusBarRightGroupVisibility(
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
