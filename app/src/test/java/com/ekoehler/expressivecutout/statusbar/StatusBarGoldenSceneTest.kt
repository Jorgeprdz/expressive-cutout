package com.ekoehler.expressivecutout.statusbar

import androidx.compose.ui.graphics.Color
import com.ekoehler.expressivecutout.data.CustomStatusBarStyle
import com.ekoehler.expressivecutout.data.IosBatteryColorMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusBarGoldenSceneTest {

    @Test
    fun `ios27 light matches golden`() {
        assertGolden("ios27_light", CustomStatusBarStyle.IOS_27)
    }

    @Test
    fun `pixel16 light keeps compact measured visual contract`() {
        val rendered = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_16_17)

        assertTrue(rendered.contains("style=PIXEL_16_17 display=Pixel 16 signature=pixel16-compact-measured"))
        assertTrue(rendered.contains("network=5G mode=PROMINENT"))
        assertTrue(rendered.contains("pixel1617.signal=viewBox=38x30 active=4 language=compact-weighted-bars"))
        assertTrue(rendered.contains("pixel1617.wifi=viewBox=39x29 activeParts=3 language=compact-symmetric-arcs"))
        assertTrue(rendered.contains("mode=compact-solid-pill-v1 colorMode=pixel-fill"))
        assertTrue(rendered.contains("batteryText=hidden shape=compact-solid-dynamic"))
    }

    @Test
    fun `legacy families render through simplified migration targets`() {
        val ios27 = StatusBarGoldenScene.render(CustomStatusBarStyle.IOS_27)
        val pixel16 = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_16_17)

        assertEquals(ios27, StatusBarGoldenScene.render(CustomStatusBarStyle.IOS_26))
        assertEquals(pixel16, StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_15))
        assertEquals(pixel16, StatusBarGoldenScene.render(CustomStatusBarStyle.HYPER_OS))
        assertEquals(pixel16, StatusBarGoldenScene.render(CustomStatusBarStyle.NOTHING_OS_5))
        assertEquals(pixel16, StatusBarGoldenScene.render(CustomStatusBarStyle.DEFAULT))
    }

    @Test
    fun `measured ios27 icons reflect dynamic state`() {
        val ios27Full = StatusBarGoldenScene.render(
            CustomStatusBarStyle.IOS_27,
            batteryLevel = 67,
            wifiLevel = 4,
            cellularLevel = 4,
        )
        val ios27Low = StatusBarGoldenScene.render(
            CustomStatusBarStyle.IOS_27,
            batteryLevel = 19,
            wifiLevel = 1,
            cellularLevel = 1,
        )

        assertNotEquals(ios27Full, ios27Low)
        assertTrue(ios27Low.contains("ios27.signalState=level=1 active=1"))
        assertTrue(ios27Low.contains("ios27.wifiState=level=1 activeParts=1"))
        assertTrue(ios27Low.contains("ios27.batteryState=level=19 fill=0.190"))
    }

    @Test
    fun `ios battery status color thresholds still pass`() {
        assertEquals(Color.Black, IosBatteryStatusColors.resolve(20, Color.Black, IosBatteryColorMode.STATUS_COLOR))
        assertEquals(IosBatteryStatusColors.Yellow, IosBatteryStatusColors.resolve(19, Color.Black, IosBatteryColorMode.STATUS_COLOR))
        assertEquals(IosBatteryStatusColors.Yellow, IosBatteryStatusColors.resolve(10, Color.Black, IosBatteryColorMode.STATUS_COLOR))
        assertEquals(IosBatteryStatusColors.Red, IosBatteryStatusColors.resolve(9, Color.Black, IosBatteryColorMode.STATUS_COLOR))
        assertEquals(Color.Black, IosBatteryStatusColors.resolve(7, Color.Black, IosBatteryColorMode.MONOCHROME))
    }

    @Test
    fun `pixel16 redesigned icons reflect dynamic state`() {
        val full = StatusBarGoldenScene.render(
            CustomStatusBarStyle.PIXEL_16_17,
            batteryLevel = 67,
            wifiLevel = 4,
            cellularLevel = 4,
        )
        val low = StatusBarGoldenScene.render(
            CustomStatusBarStyle.PIXEL_16_17,
            batteryLevel = 19,
            wifiLevel = 1,
            cellularLevel = 1,
        )

        assertNotEquals(full, low)
        assertTrue(low.contains("pixel1617.wifi=viewBox=39x29 activeParts=1 language=compact-symmetric-arcs"))
        assertTrue(low.contains("pixel1617.signal=viewBox=38x30 active=1 language=compact-weighted-bars"))
        assertTrue(low.contains("level=19 fill=0.190"))
        assertTrue(low.contains("colorMode=critical-red"))
        assertTrue(full.contains("mode=compact-solid-pill-v1 colorMode=pixel-fill"))
    }

    @Test
    fun `pixel16 battery variants stay dynamic`() {
        val seven = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_16_17, batteryLevel = 7)
        val nineteen = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_16_17, batteryLevel = 19)
        val sixtySeven = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_16_17, batteryLevel = 67)
        val hundred = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_16_17, batteryLevel = 100)

        assertTrue(seven.contains("level=7 fill=0.070"))
        assertTrue(seven.contains("colorMode=critical-red"))
        assertTrue(nineteen.contains("level=19 fill=0.190"))
        assertTrue(sixtySeven.contains("level=67 fill=0.670"))
        assertTrue(sixtySeven.contains("colorMode=pixel-fill"))
        assertTrue(hundred.contains("level=100 fill=1.000"))
    }

    @Test
    fun `approved visible families stay visually distinct`() {
        val ios27 = StatusBarGoldenScene.render(CustomStatusBarStyle.IOS_27)
        val pixel16 = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_16_17)

        assertVisuallyDifferent(ios27, pixel16)
    }

    private fun assertGolden(
        name: String,
        style: CustomStatusBarStyle,
        batteryLevel: Int = 67,
        charging: Boolean = false,
    ) {
        assertEquals(
            golden(name),
            StatusBarGoldenScene.render(
                style = style,
                batteryLevel = batteryLevel,
                charging = charging,
            ),
        )
    }

    private fun assertVisuallyDifferent(left: String, right: String) {
        assertNotEquals(left, right)
        assertTrue(
            "Expected golden scenes to differ by at least four contract lines",
            StatusBarGoldenScene.visualDistance(left, right) >= 4,
        )
    }

    private fun golden(name: String): String =
        requireNotNull(javaClass.getResource("/statusbar-goldens/$name.golden")) {
            "Missing golden resource $name"
        }.readText().replace("\r\n", "\n").trim()
}
