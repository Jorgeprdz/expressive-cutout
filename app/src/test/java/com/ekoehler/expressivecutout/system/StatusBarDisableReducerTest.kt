package com.ekoehler.expressivecutout.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusBarDisableReducerTest {

    @Test
    fun `single owner is the effective request`() {
        val request = StatusBarFlagState(hideClock = true)

        val effective = StatusBarDisableReducer.reduce(
            mapOf(StatusBarDisableOwner.USER_PERSISTENT to request),
        )

        assertEquals(request, effective)
    }

    @Test
    fun `multiple owners are unioned per flag`() {
        val effective = StatusBarDisableReducer.reduce(
            mapOf(
                StatusBarDisableOwner.USER_PERSISTENT to StatusBarFlagState(
                    hideClock = true,
                    silenceAlerts = true,
                ),
                StatusBarDisableOwner.CUSTOM_STATUS_BAR to StatusBarFlagState(
                    hideNotificationIcons = true,
                    hideSystemInfo = true,
                ),
            ),
        )

        assertTrue(effective.hideNotificationIcons)
        assertTrue(effective.hideSystemInfo)
        assertTrue(effective.hideClock)
        assertTrue(effective.silenceAlerts)
    }

    @Test
    fun `removing transient owner preserves custom status bar request`() {
        val custom = StatusBarFlagState(
            hideNotificationIcons = true,
            hideSystemInfo = true,
            hideClock = true,
        )
        val duringPulse = mapOf(
            StatusBarDisableOwner.CUSTOM_STATUS_BAR to custom,
            StatusBarDisableOwner.DYNAMIC_ISLAND_TRANSIENT to custom,
        )

        val afterPulse = duringPulse - StatusBarDisableOwner.DYNAMIC_ISLAND_TRANSIENT
        val effective = StatusBarDisableReducer.reduce(afterPulse)

        assertEquals(custom, effective)
    }

    @Test
    fun `manual persistent clock survives custom status bar release`() {
        val requests = mapOf(
            StatusBarDisableOwner.USER_PERSISTENT to StatusBarFlagState(hideClock = true),
            StatusBarDisableOwner.CUSTOM_STATUS_BAR to StatusBarFlagState(
                hideNotificationIcons = true,
                hideSystemInfo = true,
                hideClock = true,
            ),
        )

        val afterCustomRelease =
            StatusBarDisableReducer.reduce(requests - StatusBarDisableOwner.CUSTOM_STATUS_BAR)

        assertTrue(afterCustomRelease.hideClock)
        assertFalse(afterCustomRelease.hideNotificationIcons)
        assertFalse(afterCustomRelease.hideSystemInfo)
    }

    @Test
    fun `empty owners restore no disabled flags`() {
        assertEquals(StatusBarFlagState(), StatusBarDisableReducer.reduce(emptyMap()))
    }
}
