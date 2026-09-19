package com.ekoehler.expressivecutout.events

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicPauseTimeoutPolicyTest {

    @Test
    fun `paused music remains eligible before timeout`() {
        assertFalse(
            MusicPauseTimeoutPolicy.hasExpired(
                pausedAtElapsedRealtime = 1_000L,
                nowElapsedRealtime = 15_999L,
                timeoutMs = 15_000L,
            ),
        )
    }

    @Test
    fun `paused music expires at timeout boundary`() {
        assertTrue(
            MusicPauseTimeoutPolicy.hasExpired(
                pausedAtElapsedRealtime = 1_000L,
                nowElapsedRealtime = 16_000L,
                timeoutMs = 15_000L,
            ),
        )
    }
}
