package com.ekoehler.expressivecutout.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusBarVisibilityPolicyTest {

    @Test
    fun `normal app shows renderer and holds native suppression`() {
        val decision = StatusBarVisibilityPolicy.decide(StatusBarVisibilityInput())

        assertEquals(StatusBarRenderMode.SHOW, decision.renderMode)
        assertTrue(decision.holdNativeSuppression)
    }

    @Test
    fun `fullscreen and immersive hide renderer without releasing configured suppression`() {
        val fullscreen = StatusBarVisibilityPolicy.decide(
            StatusBarVisibilityInput(fullscreen = true),
        )
        val immersive = StatusBarVisibilityPolicy.decide(
            StatusBarVisibilityInput(immersive = true),
        )

        assertEquals(StatusBarRenderMode.HIDE, fullscreen.renderMode)
        assertTrue(fullscreen.holdNativeSuppression)
        assertEquals(StatusBarRenderMode.HIDE, immersive.renderMode)
        assertTrue(immersive.holdNativeSuppression)
    }

    @Test
    fun `lockscreen aod and screen off hide and release custom lease`() {
        val decisions = listOf(
            StatusBarVisibilityPolicy.decide(StatusBarVisibilityInput(locked = true)),
            StatusBarVisibilityPolicy.decide(StatusBarVisibilityInput(aod = true)),
            StatusBarVisibilityPolicy.decide(StatusBarVisibilityInput(screenOn = false)),
        )

        decisions.forEach { decision ->
            assertEquals(StatusBarRenderMode.HIDE, decision.renderMode)
            assertFalse(decision.holdNativeSuppression)
        }
    }

    @Test
    fun `shade and quick settings suspend visual layer while retaining native suppression`() {
        val shade = StatusBarVisibilityPolicy.decide(
            StatusBarVisibilityInput(shadeExpanded = true),
        )
        val quickSettings = StatusBarVisibilityPolicy.decide(
            StatusBarVisibilityInput(quickSettingsExpanded = true),
        )

        assertEquals(StatusBarRenderMode.SUSPEND, shade.renderMode)
        assertTrue(shade.holdNativeSuppression)
        assertEquals(StatusBarRenderMode.SUSPEND, quickSettings.renderMode)
        assertTrue(quickSettings.holdNativeSuppression)
    }

    @Test
    fun `dex remains unsupported in first vertical slice and releases custom lease`() {
        val decision = StatusBarVisibilityPolicy.decide(StatusBarVisibilityInput(dex = true))

        assertEquals(StatusBarRenderMode.HIDE, decision.renderMode)
        assertFalse(decision.holdNativeSuppression)
    }

    @Test
    fun `disabled feature never owns native suppression`() {
        val decision = StatusBarVisibilityPolicy.decide(
            StatusBarVisibilityInput(enabled = false),
        )

        assertEquals(StatusBarRenderMode.HIDE, decision.renderMode)
        assertFalse(decision.holdNativeSuppression)
    }

    @Test
    fun `multi window remains eligible for rendering`() {
        val decision = StatusBarVisibilityPolicy.decide(
            StatusBarVisibilityInput(multiWindow = true),
        )

        assertEquals(StatusBarRenderMode.SHOW, decision.renderMode)
        assertTrue(decision.holdNativeSuppression)
    }
}
