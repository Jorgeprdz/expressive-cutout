package com.ekoehler.expressivecutout.statusbar

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusBarRightGroupVisibilityPolicyTest {

    @Test
    fun `shows both mobile network and outside percentage on data when both fit`() {
        val result = StatusBarRightGroupVisibilityPolicy.resolve(
            wifiConnected = false,
            networkAvailable = true,
            outsidePercentageAvailable = true,
            bothFit = true,
            networkFitsWithoutPercentage = true,
            percentageFitsWithoutNetwork = true,
        )

        assertTrue(result.showNetwork)
        assertTrue(result.showPercentage)
    }

    @Test
    fun `hides mobile network while wifi is connected`() {
        val result = StatusBarRightGroupVisibilityPolicy.resolve(
            wifiConnected = true,
            networkAvailable = true,
            outsidePercentageAvailable = true,
            bothFit = true,
            networkFitsWithoutPercentage = true,
            percentageFitsWithoutNetwork = true,
        )

        assertFalse(result.showNetwork)
        assertTrue(result.showPercentage)
    }

    @Test
    fun `prioritizes mobile network over outside percentage on data when compact`() {
        val result = StatusBarRightGroupVisibilityPolicy.resolve(
            wifiConnected = false,
            networkAvailable = true,
            outsidePercentageAvailable = true,
            bothFit = false,
            networkFitsWithoutPercentage = true,
            percentageFitsWithoutNetwork = true,
        )

        assertTrue(result.showNetwork)
        assertFalse(result.showPercentage)
    }

    @Test
    fun `falls back to percentage only when network cannot fit`() {
        val result = StatusBarRightGroupVisibilityPolicy.resolve(
            wifiConnected = false,
            networkAvailable = true,
            outsidePercentageAvailable = true,
            bothFit = false,
            networkFitsWithoutPercentage = false,
            percentageFitsWithoutNetwork = true,
        )

        assertFalse(result.showNetwork)
        assertTrue(result.showPercentage)
    }
}
