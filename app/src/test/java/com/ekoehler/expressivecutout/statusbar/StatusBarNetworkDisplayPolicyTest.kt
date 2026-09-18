package com.ekoehler.expressivecutout.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusBarNetworkDisplayPolicyTest {

    @Test
    fun `wifi connected hides mobile network label`() {
        val input = StatusBarNetworkDisplayInput(
            wifiConnected = true,
            cellularConnected = true,
            cellularDataActive = true,
            networkType = StatusBarNetworkType.FIVE_G,
        )

        assertFalse(StatusBarNetworkDisplayPolicy.shouldShowMobileNetworkLabel(input))
        assertEquals("wifi_active", StatusBarNetworkDisplayPolicy.hiddenReason(input))
    }

    @Test
    fun `mobile data active without wifi shows network label`() {
        val input = StatusBarNetworkDisplayInput(
            wifiConnected = false,
            cellularConnected = true,
            cellularDataActive = true,
            networkType = StatusBarNetworkType.FOUR_G_PLUS,
        )

        assertTrue(StatusBarNetworkDisplayPolicy.shouldShowMobileNetworkLabel(input))
        assertEquals(null, StatusBarNetworkDisplayPolicy.hiddenReason(input))
    }

    @Test
    fun `cellular available but wifi active does not show network label`() {
        val input = StatusBarNetworkDisplayInput(
            wifiConnected = true,
            cellularConnected = true,
            cellularDataActive = false,
            networkType = StatusBarNetworkType.LTE,
        )

        assertFalse(StatusBarNetworkDisplayPolicy.shouldShowMobileNetworkLabel(input))
    }

    @Test
    fun `missing network type does not invent label`() {
        val input = StatusBarNetworkDisplayInput(
            wifiConnected = false,
            cellularConnected = true,
            cellularDataActive = true,
            networkType = null,
        )

        assertFalse(StatusBarNetworkDisplayPolicy.shouldShowMobileNetworkLabel(input))
        assertEquals("network_type_missing", StatusBarNetworkDisplayPolicy.hiddenReason(input))
    }
}
