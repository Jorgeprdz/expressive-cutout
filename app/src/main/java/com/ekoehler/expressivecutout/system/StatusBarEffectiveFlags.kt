package com.ekoehler.expressivecutout.system

/**
 * Compatibility facade for the original persistent + transient status-bar composition.
 *
 * The actual rule now lives in [StatusBarDisableReducer], so future owners such as the custom
 * status bar cannot be accidentally cleared when a Dynamic Island pulse ends.
 */
internal object StatusBarEffectiveFlags {

    fun compose(
        persistent: StatusBarFlagState,
        transientHideStatusIcons: Boolean,
    ): StatusBarFlagState {
        val requests = buildMap {
            put(StatusBarDisableOwner.USER_PERSISTENT, persistent)
            if (transientHideStatusIcons) {
                put(
                    StatusBarDisableOwner.DYNAMIC_ISLAND_TRANSIENT,
                    StatusBarFlagState(
                        hideNotificationIcons = true,
                        hideSystemInfo = true,
                        hideClock = true,
                    ),
                )
            }
        }
        return StatusBarDisableReducer.reduce(requests)
    }
}
