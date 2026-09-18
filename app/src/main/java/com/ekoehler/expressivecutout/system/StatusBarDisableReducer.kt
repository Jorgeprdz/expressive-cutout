package com.ekoehler.expressivecutout.system

/** Independent reasons that may currently require native status-bar elements to stay disabled. */
internal enum class StatusBarDisableOwner {
    USER_PERSISTENT,
    CUSTOM_STATUS_BAR,
    DYNAMIC_ISLAND_TRANSIENT,
}

/**
 * Pure ownership reducer for SystemUI disable wishes.
 *
 * Every feature owns its own complete [StatusBarFlagState]. Releasing one owner therefore cannot
 * restore a flag that is still requested by another owner.
 */
internal object StatusBarDisableReducer {

    fun reduce(requests: Map<StatusBarDisableOwner, StatusBarFlagState>): StatusBarFlagState =
        requests.values.fold(StatusBarFlagState()) { effective, request ->
            StatusBarFlagState(
                hideNotificationIcons =
                    effective.hideNotificationIcons || request.hideNotificationIcons,
                hideSystemInfo = effective.hideSystemInfo || request.hideSystemInfo,
                hideClock = effective.hideClock || request.hideClock,
                silenceAlerts = effective.silenceAlerts || request.silenceAlerts,
            )
        }
}
