package com.ekoehler.expressivecutout.statusbar

import org.junit.Assert.assertEquals
import org.junit.Test

class PixelStatusBarGeometryTest {

    @Test
    fun `wifi geometry preserves four pixel strength steps`() {
        assertEquals(listOf(0f, 0f, 0f, 0f), PixelStatusBarGeometry.wifiStrengths(null))
        assertEquals(listOf(1f, 0f, 0f, 0f), PixelStatusBarGeometry.wifiStrengths(1))
        assertEquals(listOf(1f, 1f, 1f, 1f), PixelStatusBarGeometry.wifiStrengths(4))
    }

    @Test
    fun `mobile geometry exposes exactly four bars`() {
        assertEquals(listOf(0f, 0f, 0f, 0f), PixelStatusBarGeometry.mobileStrengths(0))
        assertEquals(listOf(1f, 1f, 0f, 0f), PixelStatusBarGeometry.mobileStrengths(2))
        assertEquals(listOf(1f, 1f, 1f, 1f), PixelStatusBarGeometry.mobileStrengths(4))
    }

    @Test
    fun `battery fill clamps percentage safely`() {
        assertEquals(0f, PixelStatusBarGeometry.batteryFraction(null))
        assertEquals(0f, PixelStatusBarGeometry.batteryFraction(-10))
        assertEquals(0.5f, PixelStatusBarGeometry.batteryFraction(50))
        assertEquals(1f, PixelStatusBarGeometry.batteryFraction(150))
    }
}
