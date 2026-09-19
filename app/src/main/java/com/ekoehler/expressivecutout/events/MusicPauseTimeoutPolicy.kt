package com.ekoehler.expressivecutout.events

/**
 * Defines how long a paused media session may keep the island music pill alive.
 *
 * A short grace period keeps intentional pause/resume usable, while preventing Spotify and other
 * players from pinning a stale paused pill indefinitely when the session remains published.
 */
internal object MusicPauseTimeoutPolicy {
    const val DEFAULT_TIMEOUT_MS: Long = 15_000L

    fun hasExpired(
        pausedAtElapsedRealtime: Long,
        nowElapsedRealtime: Long,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ): Boolean = nowElapsedRealtime - pausedAtElapsedRealtime >= timeoutMs
}
