package com.ekoehler.expressivecutout.statusbar

import com.ekoehler.expressivecutout.data.CustomStatusBarSettings
import com.ekoehler.expressivecutout.data.CustomStatusBarStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusBarStyleRegistryTest {

    @Test
    fun `registry exposes only the simplified M2C visual pack`() {
        val ids = StatusBarStyleRegistry.allStyles.map { it.id }

        assertEquals(
            listOf(
                CustomStatusBarStyle.IOS_27,
                CustomStatusBarStyle.PIXEL_16_17,
            ),
            ids,
        )
        assertEquals(listOf("iOS 27", "Pixel 16"), StatusBarStyleRegistry.allStyles.map { it.displayName })
    }

    @Test
    fun `invalid style id falls back to iOS 27`() {
        assertEquals(CustomStatusBarStyle.IOS_27, StatusBarStyleRegistry.resolvePersisted("missing-style"))
        assertEquals(StatusBarStyleRegistry.defaultStyle, StatusBarStyleRegistry.resolve(null))
        assertEquals(CustomStatusBarStyle.IOS_27, StatusBarStyleRegistry.defaultStyle.id)
    }

    @Test
    fun `legacy style ids migrate to simplified renderers`() {
        assertEquals(CustomStatusBarStyle.IOS_27, StatusBarStyleRegistry.resolve(CustomStatusBarStyle.IOS_26).id)
        assertEquals(CustomStatusBarStyle.PIXEL_16_17, StatusBarStyleRegistry.resolve(CustomStatusBarStyle.DEFAULT).id)
        assertEquals(CustomStatusBarStyle.PIXEL_16_17, StatusBarStyleRegistry.resolve(CustomStatusBarStyle.PIXEL_15).id)
        assertEquals(CustomStatusBarStyle.PIXEL_16_17, StatusBarStyleRegistry.resolve(CustomStatusBarStyle.HYPER_OS).id)
        assertEquals(CustomStatusBarStyle.PIXEL_16_17, StatusBarStyleRegistry.resolve(CustomStatusBarStyle.NOTHING_OS_5).id)
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

    @Test
    fun `battery ownership differs only across the two approved families`() {
        val pixel16 = StatusBarStyleRegistry.resolve(CustomStatusBarStyle.PIXEL_16_17)
        val ios27 = StatusBarStyleRegistry.resolve(CustomStatusBarStyle.IOS_27)

        assertEquals(StatusBarBatteryVisualMode.COMPACT_SOLID_DYNAMIC, pixel16.batteryVisualMode)
        assertEquals(StatusBarBatteryVisualMode.IOS_SOLID_CAPSULE, ios27.batteryVisualMode)
        assertFalse(pixel16.batteryVisualMode.usesInternalPercentage)
    }

    @Test
    fun `signal and network renderers keep approved families visually distinct`() {
        val ios27 = StatusBarStyleRegistry.resolve(CustomStatusBarStyle.IOS_27)
        val pixel16 = StatusBarStyleRegistry.resolve(CustomStatusBarStyle.PIXEL_16_17)

        assertEquals(StatusBarSignalVisualMode.IOS_BOLD_PILLS, ios27.signalVisualMode)
        assertEquals(StatusBarSignalVisualMode.PIXEL_COMPACT_BARS, pixel16.signalVisualMode)
        assertEquals(StatusBarNetworkLabelMode.PROMINENT, pixel16.networkLabelMode)
    }
}
