package com.ekoehler.expressivecutout.statusbar

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomStatusBarDisconnectPolicyTest {

    @Test
    fun `applied native lease keeps matching renderer during Shizuku loss`() {
        assertTrue(
            CustomStatusBarDisconnectPolicy.keepRenderer(
                nativeSuppressionApplied = true,
                enabled = true,
                locked = false,
                portraitSupported = true,
            ),
        )
    }

    @Test
    fun `no known native lease never invents a custom renderer`() {
        assertFalse(
            CustomStatusBarDisconnectPolicy.keepRenderer(
                nativeSuppressionApplied = false,
                enabled = true,
                locked = false,
                portraitSupported = true,
            ),
        )
    }

    @Test
    fun `lock and unsupported orientation hide fallback renderer`() {
        assertFalse(
            CustomStatusBarDisconnectPolicy.keepRenderer(
                nativeSuppressionApplied = true,
                enabled = true,
                locked = true,
                portraitSupported = true,
            ),
        )
        assertFalse(
            CustomStatusBarDisconnectPolicy.keepRenderer(
                nativeSuppressionApplied = true,
                enabled = true,
                locked = false,
                portraitSupported = false,
            ),
        )
    }


    @Test
    fun `shutdown defers only while an applied lease cannot be released`() {
        assertTrue(
            CustomStatusBarShutdownPolicy.shouldDefer(
                shizukuReady = false,
                nativeSuppressionApplied = true,
            ),
        )
        assertFalse(
            CustomStatusBarShutdownPolicy.shouldDefer(
                shizukuReady = true,
                nativeSuppressionApplied = true,
            ),
        )
        assertFalse(
            CustomStatusBarShutdownPolicy.shouldDefer(
                shizukuReady = false,
                nativeSuppressionApplied = false,
            ),
        )
    }
}
