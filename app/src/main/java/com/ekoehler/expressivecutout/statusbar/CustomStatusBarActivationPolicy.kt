package com.ekoehler.expressivecutout.statusbar

import com.ekoehler.expressivecutout.system.StatusBarFlagState

internal data class CustomStatusBarActivationDecision(
    val canRender: Boolean,
    val shouldAcquireNativeSuppression: Boolean,
    val nativeRequest: StatusBarFlagState?,
)

/**
 * Fail-safe activation rule: never draw a second complete status bar unless Shizuku can hold the
 * native clock/system/notification icons hidden. M1 keeps landscape on the proven island-only path.
 */
internal object CustomStatusBarActivationPolicy {
    fun decide(
        enabled: Boolean,
        shizukuReady: Boolean,
        portraitSupported: Boolean,
    ): CustomStatusBarActivationDecision {
        val active = enabled && shizukuReady && portraitSupported
        val request = if (active) {
            StatusBarFlagState(
                hideNotificationIcons = true,
                hideSystemInfo = true,
                hideClock = true,
                silenceAlerts = false,
            )
        } else {
            null
        }
        return CustomStatusBarActivationDecision(
            canRender = active,
            shouldAcquireNativeSuppression = active,
            nativeRequest = request,
        )
    }
}
