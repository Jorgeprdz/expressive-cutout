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
    fun `ios26 light matches golden`() {
        assertGolden("ios26_light", CustomStatusBarStyle.IOS_26)
    }

    @Test
    fun `ios27 light matches golden`() {
        assertGolden("ios27_light", CustomStatusBarStyle.IOS_27)
    }

    @Test
    fun `pixel15 light matches golden`() {
        assertGolden("pixel15_light", CustomStatusBarStyle.PIXEL_15)
    }

    @Test
    fun `pixel1617 light matches golden`() {
        assertGolden("pixel1617_light", CustomStatusBarStyle.PIXEL_16_17)
    }

    @Test
    fun `pixel1617 charging light matches golden`() {
        assertGolden(
            name = "pixel1617_charging_light",
            style = CustomStatusBarStyle.PIXEL_16_17,
            charging = true,
        )
    }

    @Test
    fun `pixel1617 battery 7 matches golden`() {
        assertGolden(
            name = "pixel1617_battery_7_light",
            style = CustomStatusBarStyle.PIXEL_16_17,
            batteryLevel = 7,
        )
    }

    @Test
    fun `pixel1617 battery 19 matches golden`() {
        assertGolden(
            name = "pixel1617_battery_19_light",
            style = CustomStatusBarStyle.PIXEL_16_17,
            batteryLevel = 19,
        )
    }

    @Test
    fun `pixel1617 battery 67 matches golden`() {
        assertGolden(
            name = "pixel1617_battery_67_light",
            style = CustomStatusBarStyle.PIXEL_16_17,
            batteryLevel = 67,
        )
    }

    @Test
    fun `pixel1617 battery 100 matches golden`() {
        assertGolden(
            name = "pixel1617_battery_100_light",
            style = CustomStatusBarStyle.PIXEL_16_17,
            batteryLevel = 100,
        )
    }

    @Test
    fun `hyperos light matches golden`() {
        assertGolden("hyperos_light", CustomStatusBarStyle.HYPER_OS)
    }

    @Test
    fun `hyperos full battery light matches golden`() {
        assertGolden(
            name = "hyperos_100_light",
            style = CustomStatusBarStyle.HYPER_OS,
            batteryLevel = 100,
        )
    }

    @Test
    fun `nothingos5 light matches golden`() {
        assertGolden("nothingos5_light", CustomStatusBarStyle.NOTHING_OS_5)
    }

    @Test
    fun `default light matches golden`() {
        assertGolden("default_light", CustomStatusBarStyle.DEFAULT)
    }

    @Test
    fun `measured ios icons reflect dynamic state`() {
        val ios26Full = StatusBarGoldenScene.render(CustomStatusBarStyle.IOS_26, batteryLevel = 67, wifiLevel = 4, cellularLevel = 4)
        val ios26Low = StatusBarGoldenScene.render(CustomStatusBarStyle.IOS_26, batteryLevel = 19, wifiLevel = 1, cellularLevel = 1)
        val ios27Full = StatusBarGoldenScene.render(CustomStatusBarStyle.IOS_27, batteryLevel = 67, wifiLevel = 4, cellularLevel = 4)
        val ios27Low = StatusBarGoldenScene.render(CustomStatusBarStyle.IOS_27, batteryLevel = 19, wifiLevel = 1, cellularLevel = 1)

        assertNotEquals(ios26Full, ios26Low)
        assertNotEquals(ios27Full, ios27Low)
        assertTrue(ios26Low.contains("ios26.batteryState=level=19 fill=0.190"))
        assertTrue(ios26Low.contains("ios26.wifiState=level=1 activeParts=1"))
        assertTrue(ios27Low.contains("ios27.signalState=level=1 active=1"))
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
    fun `pixel1617 redesigned icons reflect dynamic state`() {
        val full = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_16_17, batteryLevel = 67, wifiLevel = 4, cellularLevel = 4)
        val low = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_16_17, batteryLevel = 19, wifiLevel = 1, cellularLevel = 1)

        assertNotEquals(full, low)
        assertTrue(low.contains("pixel1617.wifi=viewBox=144x112 activeParts=1 language=pixel-bold-arcs-v2"))
        assertTrue(low.contains("pixel1617.signal=viewBox=132x108 active=1 language=pixel-capsule-bars-v2"))
        assertTrue(low.contains("level=19 fill=0.190"))
        assertTrue(low.contains("colorMode=critical-red"))
        assertTrue(full.contains("mode=pixel-rounded-rect-v2 colorMode=pixel-fill"))
    }

    @Test
    fun `visual families stay distinct`() {
        val ios26 = StatusBarGoldenScene.render(CustomStatusBarStyle.IOS_26)
        val ios27 = StatusBarGoldenScene.render(CustomStatusBarStyle.IOS_27)
        val pixel15 = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_15)
        val pixel1617 = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_16_17)
        val hyperOs = StatusBarGoldenScene.render(CustomStatusBarStyle.HYPER_OS)
        val nothing = StatusBarGoldenScene.render(CustomStatusBarStyle.NOTHING_OS_5)

        assertVisuallyDifferent(ios26, ios27)
        assertVisuallyDifferent(pixel15, pixel1617)
        assertVisuallyDifferent(pixel1617, nothing)
        assertVisuallyDifferent(pixel1617, hyperOs)
        assertVisuallyDifferent(hyperOs, nothing)
        assertVisuallyDifferent(ios27, nothing)
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
