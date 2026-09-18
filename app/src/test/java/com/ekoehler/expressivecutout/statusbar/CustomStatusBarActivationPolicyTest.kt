package com.ekoehler.expressivecutout.statusbar

import com.ekoehler.expressivecutout.system.StatusBarFlagState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomStatusBarActivationPolicyTest {

    @Test
    fun `ready enabled status bar renders only after native suppression can be held`() {
        val decision = CustomStatusBarActivationPolicy.decide(
            enabled = true,
            shizukuReady = true,
            portraitSupported = true,
        )

        assertTrue(decision.shouldAcquireNativeSuppression)
        assertTrue(decision.canRender)
        assertEquals(
            StatusBarFlagState(
                hideNotificationIcons = true,
                hideSystemInfo = true,
                hideClock = true,
                silenceAlerts = false,
            ),
            decision.nativeRequest,
        )
    }

    @Test
    fun `shizuku loss hides custom renderer instead of duplicating system ui`() {
        val decision = CustomStatusBarActivationPolicy.decide(
            enabled = true,
            shizukuReady = false,
            portraitSupported = true,
        )

        assertFalse(decision.shouldAcquireNativeSuppression)
        assertFalse(decision.canRender)
        assertNull(decision.nativeRequest)
    }

    @Test
    fun `disabled feature releases only its owner`() {
        val decision = CustomStatusBarActivationPolicy.decide(
            enabled = false,
            shizukuReady = true,
            portraitSupported = true,
        )

        assertFalse(decision.shouldAcquireNativeSuppression)
        assertFalse(decision.canRender)
        assertNull(decision.nativeRequest)
    }

    @Test
    fun `landscape is fail safe for first vertical slice`() {
        val decision = CustomStatusBarActivationPolicy.decide(
            enabled = true,
            shizukuReady = true,
            portraitSupported = false,
        )

        assertFalse(decision.shouldAcquireNativeSuppression)
        assertFalse(decision.canRender)
    }
}
