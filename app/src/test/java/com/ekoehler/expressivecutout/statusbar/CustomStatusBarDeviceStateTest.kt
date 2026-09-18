package com.ekoehler.expressivecutout.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomStatusBarDeviceStateTest {

    @Test
    fun `platform cellular level maps directly and clamps to four bars`() {
        assertEquals(null, StatusBarSignalLevelMapper.cellularFromPlatformLevel(null))
        assertEquals(0, StatusBarSignalLevelMapper.cellularFromPlatformLevel(-1))
        assertEquals(0, StatusBarSignalLevelMapper.cellularFromPlatformLevel(0))
        assertEquals(2, StatusBarSignalLevelMapper.cellularFromPlatformLevel(2))
        assertEquals(4, StatusBarSignalLevelMapper.cellularFromPlatformLevel(4))
        assertEquals(4, StatusBarSignalLevelMapper.cellularFromPlatformLevel(7))
    }

    @Test
    fun `cellular signal update preserves connection metadata`() {
        val start = CustomStatusBarDeviceState(
            cellular = StatusBarCellularState(
                connected = true,
                level = null,
                networkType = StatusBarNetworkType.FIVE_G,
            ),
        )

        val updated = CustomStatusBarDeviceReducer.withCellularSignal(start, 3)

        assertEquals(true, updated.cellular.connected)
        assertEquals(3, updated.cellular.level)
        assertEquals(StatusBarNetworkType.FIVE_G, updated.cellular.networkType)
    }

    @Test
    fun `cellular presence updates preserve known network type`() {
        val start = CustomStatusBarDeviceState(
            cellular = StatusBarCellularState(
                connected = true,
                level = 2,
                networkType = StatusBarNetworkType.FIVE_G,
            ),
        )

        val updated = CustomStatusBarDeviceReducer.withCellularPresence(start, connected = true, level = 4)

        assertTrue(updated.cellular.connected)
        assertEquals(4, updated.cellular.level)
        assertEquals(StatusBarNetworkType.FIVE_G, updated.cellular.networkType)
    }

    @Test
    fun `network type update preserves live signal level`() {
        val start = CustomStatusBarDeviceState(
            cellular = StatusBarCellularState(
                connected = true,
                level = 3,
                networkType = null,
            ),
        )

        val updated = CustomStatusBarDeviceReducer.withCellularNetworkType(
            start,
            StatusBarNetworkType.LTE,
        )

        assertTrue(updated.cellular.connected)
        assertEquals(3, updated.cellular.level)
        assertEquals(StatusBarNetworkType.LTE, updated.cellular.networkType)
    }

    @Test
    fun `battery update preserves unrelated device state`() {
        val initial = CustomStatusBarDeviceState(
            timeText = "12:34",
            wifi = StatusBarWifiState(connected = true, level = 3),
        )

        val updated = CustomStatusBarDeviceReducer.withBattery(
            initial,
            level = 82,
            charging = true,
            full = false,
        )

        assertEquals("12:34", updated.timeText)
        assertEquals(StatusBarWifiState(connected = true, level = 3), updated.wifi)
        assertEquals(82, updated.battery.level)
        assertTrue(updated.battery.charging)
        assertFalse(updated.battery.full)
    }

    @Test
    fun `wifi disconnect clears signal instead of inventing bars`() {
        val connected = CustomStatusBarDeviceState(
            wifi = StatusBarWifiState(connected = true, level = 4),
        )

        val disconnected = CustomStatusBarDeviceReducer.withWifi(
            connected,
            connected = false,
            level = 4,
        )

        assertFalse(disconnected.wifi.connected)
        assertNull(disconnected.wifi.level)
    }

    @Test
    fun `wifi dbm maps to four pixel levels with unknown preserved`() {
        assertEquals(4, StatusBarSignalLevelMapper.wifiFromDbm(-50))
        assertEquals(3, StatusBarSignalLevelMapper.wifiFromDbm(-62))
        assertEquals(2, StatusBarSignalLevelMapper.wifiFromDbm(-72))
        assertEquals(1, StatusBarSignalLevelMapper.wifiFromDbm(-82))
        assertEquals(0, StatusBarSignalLevelMapper.wifiFromDbm(-92))
        assertNull(StatusBarSignalLevelMapper.wifiFromDbm(null))
        assertNull(StatusBarSignalLevelMapper.wifiFromDbm(Int.MIN_VALUE))
    }

    @Test
    fun `cellular dbm degrades unknown safely`() {
        assertEquals(4, StatusBarSignalLevelMapper.cellularFromDbm(-85))
        assertEquals(3, StatusBarSignalLevelMapper.cellularFromDbm(-95))
        assertEquals(2, StatusBarSignalLevelMapper.cellularFromDbm(-105))
        assertEquals(1, StatusBarSignalLevelMapper.cellularFromDbm(-115))
        assertEquals(0, StatusBarSignalLevelMapper.cellularFromDbm(-125))
        assertNull(StatusBarSignalLevelMapper.cellularFromDbm(null))
        assertNull(StatusBarSignalLevelMapper.cellularFromDbm(Int.MIN_VALUE))
    }

    @Test
    fun `network type remains absent when platform source cannot provide it`() {
        val updated = CustomStatusBarDeviceReducer.withCellular(
            CustomStatusBarDeviceState(),
            connected = true,
            level = 3,
            networkType = null,
        )

        assertTrue(updated.cellular.connected)
        assertEquals(3, updated.cellular.level)
        assertNull(updated.cellular.networkType)
    }
}
