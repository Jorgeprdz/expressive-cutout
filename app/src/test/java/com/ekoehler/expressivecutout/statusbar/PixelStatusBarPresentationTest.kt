package com.ekoehler.expressivecutout.statusbar

import com.ekoehler.expressivecutout.data.BatteryPercentageMode
import com.ekoehler.expressivecutout.data.PixelMobileBarStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PixelStatusBarPresentationTest {

    @Test
    fun `battery percentage off never renders text`() {
        assertNull(PixelStatusBarPresentation.batteryPercentage(82, BatteryPercentageMode.OFF))
    }

    @Test
    fun `outside battery percentage formats bounds`() {
        assertEquals("82%", PixelStatusBarPresentation.batteryPercentage(82, BatteryPercentageMode.OUTSIDE))
        assertEquals("0%", PixelStatusBarPresentation.batteryPercentage(0, BatteryPercentageMode.OUTSIDE))
        assertEquals("100%", PixelStatusBarPresentation.batteryPercentage(100, BatteryPercentageMode.OUTSIDE))
        assertNull(PixelStatusBarPresentation.batteryPercentage(null, BatteryPercentageMode.OUTSIDE))
    }

    @Test
    fun `mobile width estimate follows style and scales exactly once`() {
        assertEquals(
            15f,
            PixelStatusBarPresentation.mobileSignalWidthDp(PixelMobileBarStyle.CLASSIC, 1f),
            0.001f,
        )
        assertEquals(
            13.5f * 1.4f,
            PixelStatusBarPresentation.mobileSignalWidthDp(PixelMobileBarStyle.COMPACT, 1.4f),
            0.001f,
        )
        assertEquals(
            14.5f * 0.75f,
            PixelStatusBarPresentation.mobileSignalWidthDp(PixelMobileBarStyle.TALL, 0.75f),
            0.001f,
        )
    }

    @Test
    fun `preview factory is deterministic and callback free state`() {
        val preview = CustomStatusBarPreviewState.create()

        assertEquals("9:41", preview.timeText)
        assertEquals(82, preview.battery.level)
        assertEquals(4, preview.wifi.level)
        assertEquals(4, preview.cellular.level)
        assertEquals(StatusBarNetworkType.FIVE_G, preview.cellular.networkType)
    }
}
