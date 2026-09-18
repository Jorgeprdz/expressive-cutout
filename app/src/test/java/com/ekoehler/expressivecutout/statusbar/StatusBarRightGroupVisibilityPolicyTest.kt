package com.ekoehler.expressivecutout.statusbar

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusBarRightGroupVisibilityPolicyTest {

    @Test
    fun `shows both network and outside percentage when both fit`() {
        val result = StatusBarRightGroupVisibilityPolicy.resolve(
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
    fun `prioritizes mobile network over outside percentage when compact`() {
        val result = StatusBarRightGroupVisibilityPolicy.resolve(
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
