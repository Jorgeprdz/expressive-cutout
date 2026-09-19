package com.ekoehler.expressivecutout.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomStatusBarSettingsTest {

    @Test
    fun `defaults use the approved iOS 27 profile`() {
        val d = CustomStatusBarSettings.DEFAULT
        assertEquals(CustomStatusBarStyle.IOS_27, d.style)
        assertEquals(1f, d.masterScale)
        assertEquals(1f, d.clockScale)
        assertEquals(1f, d.systemIconsScale)
        assertEquals(1f, d.wifiScale)
        assertEquals(4f, d.systemIconsSpacingDp)
        assertEquals(1f, d.batteryScale)
        assertEquals(PixelMobileBarStyle.CLASSIC, d.mobileBarStyle)
        assertEquals(BatteryPercentageMode.OFF, d.batteryPercentageMode)
    }

    @Test
    fun `fine alignment range gives the clock more horizontal room`() {
        assertEquals(48f, CustomStatusBarSettings.MAX_OFFSET_DP)
        assertEquals(0f, CustomStatusBarSettings.DEFAULT.clockOffsetXDp)
    }

    @Test
    fun `sanitized settings clamp every tunable range`() {
        val s = CustomStatusBarSettings(
            style = CustomStatusBarStyle.HYPER_OS,
            masterScale = 9f,
            clockScale = 0f,
            clockOffsetXDp = 200f,
            clockOffsetYDp = -200f,
            systemIconsScale = 3f,
            wifiScale = 4f,
            systemIconsSpacingDp = -8f,
            systemIconsOffsetXDp = -200f,
            systemIconsOffsetYDp = 200f,
            batteryScale = 4f,
            statusBarOffsetYDp = 100f,
        ).sanitized()

        assertEquals(CustomStatusBarStyle.PIXEL_16_17, s.style)
        assertEquals(CustomStatusBarSettings.MAX_MASTER_SCALE, s.masterScale)
        assertEquals(CustomStatusBarSettings.MIN_COMPONENT_SCALE, s.clockScale)
        assertEquals(CustomStatusBarSettings.MAX_OFFSET_DP, s.clockOffsetXDp)
        assertEquals(-CustomStatusBarSettings.MAX_OFFSET_DP, s.clockOffsetYDp)
        assertEquals(CustomStatusBarSettings.MAX_COMPONENT_SCALE, s.systemIconsScale)
        assertEquals(CustomStatusBarSettings.MAX_COMPONENT_SCALE, s.wifiScale)
        assertEquals(CustomStatusBarSettings.MIN_SPACING_DP, s.systemIconsSpacingDp)
        assertEquals(-CustomStatusBarSettings.MAX_OFFSET_DP, s.systemIconsOffsetXDp)
        assertEquals(CustomStatusBarSettings.MAX_OFFSET_DP, s.systemIconsOffsetYDp)
        assertEquals(CustomStatusBarSettings.MAX_COMPONENT_SCALE, s.batteryScale)
        assertEquals(CustomStatusBarSettings.MAX_GLOBAL_Y_DP, s.statusBarOffsetYDp)
    }

    @Test
    fun `unknown persisted enums fall back safely`() {
        assertEquals(BatteryPercentageMode.OFF, BatteryPercentageMode.fromPersisted("wat"))
        assertEquals(PixelMobileBarStyle.CLASSIC, PixelMobileBarStyle.fromPersisted("future"))
        assertEquals(CustomStatusBarStyle.IOS_27, CustomStatusBarStyle.fromPersisted("future"))
    }

    @Test
    fun `legacy persisted style aliases migrate to the simplified pack`() {
        assertEquals(CustomStatusBarStyle.IOS_27, CustomStatusBarStyle.fromPersisted("IOS_26"))
        assertEquals(CustomStatusBarStyle.PIXEL_16_17, CustomStatusBarStyle.fromPersisted("DEFAULT"))
        assertEquals(CustomStatusBarStyle.PIXEL_16_17, CustomStatusBarStyle.fromPersisted("ONE_UI"))
        assertEquals(CustomStatusBarStyle.PIXEL_16_17, CustomStatusBarStyle.fromPersisted("PIXEL"))
        assertEquals(CustomStatusBarStyle.PIXEL_16_17, CustomStatusBarStyle.fromPersisted("PIXEL_15"))
        assertEquals(CustomStatusBarStyle.PIXEL_16_17, CustomStatusBarStyle.fromPersisted("PIXEL_16"))
        assertEquals(CustomStatusBarStyle.PIXEL_16_17, CustomStatusBarStyle.fromPersisted("PIXEL_17"))
        assertEquals(CustomStatusBarStyle.PIXEL_16_17, CustomStatusBarStyle.fromPersisted("HYPER_OS"))
        assertEquals(CustomStatusBarStyle.PIXEL_16_17, CustomStatusBarStyle.fromPersisted("NOTHING_OS"))
        assertEquals(CustomStatusBarStyle.PIXEL_16_17, CustomStatusBarStyle.fromPersisted("NOTHING_OS_5"))
    }

    @Test
    fun `only iOS 27 and Pixel 16 remain visible`() {
        assertEquals(
            listOf(CustomStatusBarStyle.IOS_27, CustomStatusBarStyle.PIXEL_16_17),
            CustomStatusBarStyleUiPolicy.visibleStyles,
        )
        assertEquals("iOS 27", CustomStatusBarStyleUiPolicy.displayName(CustomStatusBarStyle.IOS_27))
        assertEquals("Pixel 16", CustomStatusBarStyleUiPolicy.displayName(CustomStatusBarStyle.PIXEL_16_17))
    }

    @Test
    fun `iOS battery color setting is only visible for iOS 27`() {
        assertTrue(CustomStatusBarStyleUiPolicy.showsIosBatteryColor(CustomStatusBarStyle.IOS_27))
        assertTrue(CustomStatusBarStyleUiPolicy.showsIosBatteryColor(CustomStatusBarStyle.IOS_26))
        assertFalse(CustomStatusBarStyleUiPolicy.showsIosBatteryColor(CustomStatusBarStyle.PIXEL_16_17))
        assertFalse(CustomStatusBarStyleUiPolicy.showsIosBatteryColor(CustomStatusBarStyle.HYPER_OS))
        assertFalse(CustomStatusBarStyleUiPolicy.showsIosBatteryColor(CustomStatusBarStyle.NOTHING_OS_5))
    }

    @Test
    fun `effective scales apply master exactly once`() {
        val s = CustomStatusBarSettings(
            masterScale = 1.2f,
            clockScale = 0.9f,
            systemIconsScale = 1.1f,
            wifiScale = 1.25f,
            batteryScale = 0.8f,
        )
        val effective = PixelStatusBarScale.resolve(s)

        assertEquals(1.08f, effective.clock, 0.0001f)
        assertEquals(1.32f, effective.systemIcons, 0.0001f)
        assertEquals(1.65f, effective.wifi, 0.0001f)
        assertEquals(0.96f, effective.battery, 0.0001f)
    }

    @Test
    fun `pixel defaults restore all visual tuning`() {
        val changed = CustomStatusBarSettings(
            enabled = true,
            style = CustomStatusBarStyle.NOTHING_OS_5,
            appearance = CustomStatusBarAppearancePreference.DARK,
            masterScale = 1.3f,
            clockOffsetXDp = 12f,
            mobileBarStyle = PixelMobileBarStyle.TALL,
            batteryPercentageMode = BatteryPercentageMode.OUTSIDE,
        )

        val reset = changed.withPixelDefaults()

        assertEquals(true, reset.enabled)
        assertEquals(CustomStatusBarStyle.IOS_27, reset.style)
        assertEquals(CustomStatusBarAppearancePreference.DARK, reset.appearance)
        assertEquals(1f, reset.masterScale)
        assertEquals(0f, reset.clockOffsetXDp)
        assertEquals(PixelMobileBarStyle.CLASSIC, reset.mobileBarStyle)
        assertEquals(BatteryPercentageMode.OFF, reset.batteryPercentageMode)
    }
}
