package com.ekoehler.expressivecutout.statusbar

import com.ekoehler.expressivecutout.data.CustomStatusBarSettings
import com.ekoehler.expressivecutout.data.CustomStatusBarStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Pixel16CompactGeometryTest {

    private val pixel = StatusBarStyleRegistry.resolve(CustomStatusBarStyle.PIXEL_16_17)

    @Test
    fun `pixel16 compact wifi matches measured geometry`() {
        val wifi = Android16StatusBarIconGeometry.pixel1617.wifi

        assertEquals(39, wifi.sourceViewBoxWidthPx)
        assertEquals(29, wifi.sourceViewBoxHeightPx)
        assertEquals("compact-bold-arcs", wifi.visualLanguage)
        assertEquals(19f, pixel.wifi.sizeDp, 0.001f)
        assertTrue(wifi.outer.width > wifi.middle.width)
        assertTrue(wifi.outerStrokeToHeight in 0.15f..0.20f)
        assertTrue(wifi.dot.width in 0.18f..0.24f)
    }

    @Test
    fun `pixel16 compact signal matches measured geometry`() {
        val signal = Android16StatusBarIconGeometry.pixel1617.signal

        assertEquals(38, signal.sourceViewBoxWidthPx)
        assertEquals(30, signal.sourceViewBoxHeightPx)
        assertEquals("compact-measured-bars", signal.visualLanguage)
        assertEquals(StatusBarSignalVisualMode.PIXEL_COMPACT_BARS, pixel.signalVisualMode)
        assertEquals(18.5f, pixel.mobile.widthDp, 0.001f)
        assertEquals(14.5f, pixel.mobile.heightDp, 0.001f)
        assertEquals(4, signal.bars.size)
        assertTrue(signal.bars.zipWithNext().all { (left, right) -> right.rect.height > left.rect.height })
        assertTrue(signal.bars.all { kotlin.math.abs((it.rect.y + it.rect.height) - 1f) < 0.001f })
    }

    @Test
    fun `pixel16 compact battery matches measured geometry`() {
        val battery = Android16StatusBarIconGeometry.pixel1617.battery

        assertEquals(53, battery.sourceViewBoxWidthPx)
        assertEquals(29, battery.sourceViewBoxHeightPx)
        assertEquals("compact-solid-pill-v1", battery.visualLanguage)
        assertEquals(StatusBarBatteryVisualMode.COMPACT_SOLID_DYNAMIC, pixel.batteryVisualMode)
        assertEquals(24f, pixel.battery.widthDp, 0.001f)
        assertEquals(14f, pixel.battery.heightDp, 0.001f)
        assertFalse(pixel.batteryVisualMode.usesInternalPercentage)
        assertTrue(battery.body.width > 0.90f)
        assertTrue(battery.terminal.width < 0.09f)
    }

    @Test
    fun `pixel16 wifi level one differs from level four`() {
        val one = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_16_17, wifiLevel = 1)
        val four = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_16_17, wifiLevel = 4)

        assertNotEquals(one, four)
        assertTrue(one.contains("activeParts=1"))
        assertTrue(four.contains("activeParts=3"))
    }

    @Test
    fun `pixel16 signal level one differs from level four`() {
        val one = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_16_17, cellularLevel = 1)
        val four = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_16_17, cellularLevel = 4)

        assertNotEquals(one, four)
        assertTrue(one.contains("active=1"))
        assertTrue(four.contains("active=4"))
    }

    @Test
    fun `pixel16 battery nineteen differs from sixty seven`() {
        val nineteen = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_16_17, batteryLevel = 19)
        val sixtySeven = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_16_17, batteryLevel = 67)

        assertNotEquals(nineteen, sixtySeven)
        assertTrue(nineteen.contains("level=19 fill=0.190"))
        assertTrue(sixtySeven.contains("level=67 fill=0.670"))
    }

    @Test
    fun `pixel16 compact renderer differs from ios27`() {
        val pixel16 = StatusBarGoldenScene.render(CustomStatusBarStyle.PIXEL_16_17)
        val ios27 = StatusBarGoldenScene.render(CustomStatusBarStyle.IOS_27)

        assertNotEquals(ios27, pixel16)
        assertTrue(pixel16.contains("signature=pixel16-compact-measured"))
        assertTrue(ios27.contains("signature=ios27-solid-pill"))
    }

    @Test
    fun `only visible styles remain ios27 and pixel16`() {
        assertEquals(
            listOf(CustomStatusBarStyle.IOS_27, CustomStatusBarStyle.PIXEL_16_17),
            StatusBarStyleRegistry.allStyles.map { it.id },
        )
    }

    @Test
    fun `ios27 behavior still passes`() {
        val plan = StatusBarStyleRegistry.rendererFor(CustomStatusBarStyle.IOS_27).createRenderPlan(
            state = CustomStatusBarPreviewState.create(),
            settings = CustomStatusBarSettings.DEFAULT.copy(style = CustomStatusBarStyle.IOS_27),
        )

        assertEquals(CustomStatusBarStyle.IOS_27, plan.style.id)
        assertEquals(StatusBarBatteryVisualMode.IOS_SOLID_CAPSULE, plan.style.batteryVisualMode)
        assertTrue(plan.respectsIslandExclusion)
    }
}
