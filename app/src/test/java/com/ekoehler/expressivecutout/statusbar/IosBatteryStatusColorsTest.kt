package com.ekoehler.expressivecutout.statusbar

import androidx.compose.ui.graphics.Color
import com.ekoehler.expressivecutout.data.IosBatteryColorMode
import org.junit.Assert.assertEquals
import org.junit.Test

class IosBatteryStatusColorsTest {
    private val baseTint = Color.Black

    @Test
    fun `monochrome mode keeps base tint even at critical levels`() {
        assertEquals(
            baseTint,
            IosBatteryStatusColors.resolve(9, baseTint, IosBatteryColorMode.MONOCHROME),
        )
        assertEquals(
            baseTint,
            IosBatteryStatusColors.resolve(15, baseTint, IosBatteryColorMode.MONOCHROME),
        )
    }

    @Test
    fun `status mode turns below ten percent red`() {
        assertEquals(
            IosBatteryStatusColors.Red,
            IosBatteryStatusColors.resolve(9, baseTint, IosBatteryColorMode.STATUS_COLOR),
        )
        assertEquals(
            IosBatteryStatusColors.Red,
            IosBatteryStatusColors.resolve(0, baseTint, IosBatteryColorMode.STATUS_COLOR),
        )
    }

    @Test
    fun `status mode turns ten through nineteen percent yellow`() {
        assertEquals(
            IosBatteryStatusColors.Yellow,
            IosBatteryStatusColors.resolve(10, baseTint, IosBatteryColorMode.STATUS_COLOR),
        )
        assertEquals(
            IosBatteryStatusColors.Yellow,
            IosBatteryStatusColors.resolve(19, baseTint, IosBatteryColorMode.STATUS_COLOR),
        )
    }

    @Test
    fun `status mode keeps base tint at twenty percent and above`() {
        assertEquals(
            baseTint,
            IosBatteryStatusColors.resolve(20, baseTint, IosBatteryColorMode.STATUS_COLOR),
        )
        assertEquals(
            baseTint,
            IosBatteryStatusColors.resolve(100, baseTint, IosBatteryColorMode.STATUS_COLOR),
        )
    }
}
