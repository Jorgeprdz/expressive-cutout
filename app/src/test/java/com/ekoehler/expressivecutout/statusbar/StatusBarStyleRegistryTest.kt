package com.ekoehler.expressivecutout.statusbar

import com.ekoehler.expressivecutout.data.CustomStatusBarSettings
import com.ekoehler.expressivecutout.data.CustomStatusBarStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusBarStyleRegistryTest {

    @Test
    fun `registry contains all M2B styles`() {
        val ids = StatusBarStyleRegistry.allStyles.map { it.id }.toSet()

        assertTrue(ids.contains(CustomStatusBarStyle.DEFAULT))
        assertTrue(ids.contains(CustomStatusBarStyle.PIXEL_15))
        assertTrue(ids.contains(CustomStatusBarStyle.IOS_27))
        assertTrue(ids.contains(CustomStatusBarStyle.HYPER_OS))
        assertTrue(ids.contains(CustomStatusBarStyle.NOTHING_OS))
    }

    @Test
    fun `invalid style id falls back to default`() {
        assertEquals(CustomStatusBarStyle.DEFAULT, StatusBarStyleRegistry.resolvePersisted("missing-style"))
        assertEquals(StatusBarStyleRegistry.defaultStyle, StatusBarStyleRegistry.resolve(null))
    }

    @Test
    fun `changing style does not mutate status model data`() {
        val state = CustomStatusBarPreviewState.create()

        StatusBarStyleRegistry.allStyles.forEach { style ->
            val plan = StatusBarStyleRegistry.rendererFor(style.id).createRenderPlan(
                state = state,
                settings = CustomStatusBarSettings.DEFAULT.copy(style = style.id),
            )

            assertEquals(state, plan.sourceState)
            assertEquals("9:41", plan.sourceState.timeText)
            assertEquals(82, plan.sourceState.battery.level)
            assertEquals(true, plan.sourceState.wifi.connected)
            assertEquals(true, plan.sourceState.cellular.connected)
        }
    }

    @Test
    fun `each renderer creates a plan from a minimal model`() {
        val minimal = CustomStatusBarDeviceState()

        StatusBarStyleRegistry.allStyles.forEach { style ->
            val plan = StatusBarStyleRegistry.rendererFor(style.id).createRenderPlan(
                state = minimal,
                settings = CustomStatusBarSettings.DEFAULT.copy(style = style.id),
            )

            assertEquals(style.id, plan.style.id)
            assertNotNull(plan.sanitizedSettings)
        }
    }

    @Test
    fun `styles preserve dynamic island exclusion contract`() {
        StatusBarStyleRegistry.allStyles.forEach { style ->
            val plan = StatusBarStyleRegistry.rendererFor(style.id).createRenderPlan(
                state = CustomStatusBarPreviewState.create(),
                settings = CustomStatusBarSettings.DEFAULT.copy(style = style.id),
            )

            assertTrue(plan.respectsIslandExclusion)
        }
    }
}
