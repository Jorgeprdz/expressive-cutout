package com.ekoehler.expressivecutout.statusbar

import com.ekoehler.expressivecutout.data.CustomStatusBarSettings
import com.ekoehler.expressivecutout.data.CustomStatusBarStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusBarStyleRegistryTest {

    @Test
    fun `registry contains all M2B real visual families`() {
        val ids = StatusBarStyleRegistry.allStyles.map { it.id }

        assertEquals(
            listOf(
                CustomStatusBarStyle.DEFAULT,
                CustomStatusBarStyle.IOS_26,
                CustomStatusBarStyle.IOS_27,
                CustomStatusBarStyle.PIXEL_15,
                CustomStatusBarStyle.PIXEL_16_17,
                CustomStatusBarStyle.HYPER_OS,
                CustomStatusBarStyle.NOTHING_OS_5,
            ),
            ids,
        )
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

    @Test
    fun `battery ownership differs by family`() {
        val pixel1617 = StatusBarStyleRegistry.resolve(CustomStatusBarStyle.PIXEL_16_17)
        val hyperOs = StatusBarStyleRegistry.resolve(CustomStatusBarStyle.HYPER_OS)
        val nothing = StatusBarStyleRegistry.resolve(CustomStatusBarStyle.NOTHING_OS_5)
        val ios26 = StatusBarStyleRegistry.resolve(CustomStatusBarStyle.IOS_26)
        val ios27 = StatusBarStyleRegistry.resolve(CustomStatusBarStyle.IOS_27)

        assertEquals(StatusBarBatteryVisualMode.NUMERIC_CAPSULE_PROMINENT, pixel1617.batteryVisualMode)
        assertEquals(StatusBarBatteryVisualMode.NUMERIC_CAPSULE_COMPACT, hyperOs.batteryVisualMode)
        assertEquals(StatusBarBatteryVisualMode.SOLID_CAPSULE_MINIMAL, nothing.batteryVisualMode)
        assertEquals(StatusBarBatteryVisualMode.IOS_OUTLINE_FILL, ios26.batteryVisualMode)
        assertEquals(StatusBarBatteryVisualMode.IOS_SOLID_CAPSULE, ios27.batteryVisualMode)
        assertTrue(pixel1617.batteryVisualMode.usesInternalPercentage)
        assertTrue(hyperOs.batteryVisualMode.usesInternalPercentage)
        assertFalse(nothing.batteryVisualMode.usesInternalPercentage)
    }

    @Test
    fun `signal and network renderers keep families visually distinct`() {
        val ios26 = StatusBarStyleRegistry.resolve(CustomStatusBarStyle.IOS_26)
        val ios27 = StatusBarStyleRegistry.resolve(CustomStatusBarStyle.IOS_27)
        val pixel15 = StatusBarStyleRegistry.resolve(CustomStatusBarStyle.PIXEL_15)
        val pixel1617 = StatusBarStyleRegistry.resolve(CustomStatusBarStyle.PIXEL_16_17)
        val hyperOs = StatusBarStyleRegistry.resolve(CustomStatusBarStyle.HYPER_OS)
        val nothing = StatusBarStyleRegistry.resolve(CustomStatusBarStyle.NOTHING_OS_5)

        assertNotEquals(ios26.signalVisualMode, ios27.signalVisualMode)
        assertNotEquals(pixel15.signalVisualMode, pixel1617.signalVisualMode)
        assertNotEquals(pixel1617.signalVisualMode, nothing.signalVisualMode)
        assertNotEquals(pixel1617.signalVisualMode, hyperOs.signalVisualMode)
        assertEquals(StatusBarNetworkLabelMode.PROMINENT, pixel1617.networkLabelMode)
        assertEquals(StatusBarNetworkLabelMode.HIDDEN, nothing.networkLabelMode)
    }
}
