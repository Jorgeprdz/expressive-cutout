package com.ekoehler.expressivecutout.service

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.ekoehler.expressivecutout.permissions.Permissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps notification delivery attached while the island accessibility service is alive.
 *
 * Samsung and other OEM builds can leave a granted [CutoutNotificationListenerService]
 * disconnected without revoking notification access. The settings activity already repairs that
 * stale binding when it resumes; this watchdog does the same work from the island's always-on host
 * so recovery does not depend on the user reopening the app. Rebind requests use bounded backoff to
 * avoid hammering NotificationManager when the framework deliberately refuses a bind.
 */
class NotificationListenerWatchdog(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var watchJob: Job? = null
    private var retryAttempt = 0
    private var nextRetryAtMs = 0L

    /** Starts health checks immediately and keeps them running until [stop] is called. */
    fun start() {
        if (watchJob?.isActive == true) return
        watchJob = scope.launch {
            while (isActive) {
                healIfNeeded()
                delay(HEALTH_CHECK_INTERVAL_MS)
            }
        }
    }

    /** Stops health checks and releases the coroutine scope owned by this watchdog. */
    fun stop() {
        watchJob?.cancel()
        watchJob = null
        scope.cancel()
    }

    /**
     * Requests a framework rebind when notification access is granted but the listener is stale.
     * A healthy listener or a revoked permission resets the backoff so the next genuine disconnect
     * gets an immediate recovery attempt.
     */
    private fun healIfNeeded() {
        if (!Permissions.isNotificationAccessGranted(appContext)) {
            resetBackoff()
            return
        }
        if (CutoutNotificationListenerService.bound.value) {
            resetBackoff()
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (now < nextRetryAtMs) return

        Log.w(TAG, "Notification listener disconnected; requesting rebind (attempt ${retryAttempt + 1})")
        CutoutNotificationListenerService.requestRebind(appContext)
        nextRetryAtMs = now + retryDelayMs(retryAttempt)
        retryAttempt += 1
    }

    /** Clears retry state after the listener becomes healthy or its permission is removed. */
    private fun resetBackoff() {
        retryAttempt = 0
        nextRetryAtMs = 0L
    }

    /** Returns a bounded exponential retry delay for the current failed attempt count. */
    private fun retryDelayMs(attempt: Int): Long = when (attempt.coerceAtMost(MAX_BACKOFF_STEP)) {
        0 -> 5_000L
        1 -> 10_000L
        2 -> 20_000L
        3 -> 40_000L
        else -> 60_000L
    }

    private companion object {
        private const val TAG = "IslandListenerWatchdog"
        private const val HEALTH_CHECK_INTERVAL_MS = 5_000L
        private const val MAX_BACKOFF_STEP = 4
    }
}
