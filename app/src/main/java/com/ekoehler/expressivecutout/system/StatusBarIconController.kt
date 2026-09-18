package com.ekoehler.expressivecutout.system

import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.ekoehler.expressivecutout.data.StatusBarPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

private const val TAG = "StatusBarIcons"

private const val DISABLE_NONE = 0x00000000
private const val DISABLE_NOTIFICATION_ICONS = 0x00020000
private const val DISABLE_NOTIFICATION_ALERTS = 0x00040000
private const val DISABLE_SYSTEM_INFO = 0x00100000
private const val DISABLE_CLOCK = 0x00800000

/**
 * Applies the union of independently-owned status-bar disable wishes through Shizuku.
 *
 * The hidden call is `IStatusBarService.disable`, which requires the privileged STATUS_BAR
 * permission. Shizuku lends the shell identity; failures are always fail-open and invalidate the
 * cached proxy so a later reconnect can rebuild it.
 *
 * [token] is intentionally process-lifetime. StatusBarManagerService ties disable records to it,
 * so process death restores SystemUI automatically.
 */
object StatusBarIconController {

    private val token = Binder()

    @Volatile
    private var service: Any? = null

    /** All live requests, keyed by feature ownership rather than by a shared boolean/ref-count. */
    private val ownerRequests = mutableMapOf<StatusBarDisableOwner, StatusBarFlagState>()

    @Volatile
    private var applicationPackageName: String? = null

    @Volatile
    private var applicationScope: CoroutineScope? = null

    @Volatile
    private var pulseDeadlineElapsedRealtimeMs: Long? = null

    @Volatile
    private var pulseExpiryJob: Job? = null

    private val transientStatusIconRequest = StatusBarFlagState(
        hideNotificationIcons = true,
        hideSystemInfo = true,
        hideClock = true,
    )

    fun start(context: Context, scope: CoroutineScope) {
        val preferences = StatusBarPreferences(context)
        applicationPackageName = context.packageName
        applicationScope = scope
        scope.launch {
            combine(
                preferences.hideNotificationIcons,
                preferences.hideSystemInfo,
                preferences.hideClock,
                preferences.silenceAlerts,
                ShizukuState.status,
            ) { hideIcons, hideSystemInfo, hideClock, silenceAlerts, status ->
                Wish(hideIcons, hideSystemInfo, hideClock, silenceAlerts, status)
            }
                .collect { wish ->
                    setOwnerRequestLocked(
                        StatusBarDisableOwner.USER_PERSISTENT,
                        StatusBarFlagState(
                            hideNotificationIcons = wish.hideIcons,
                            hideSystemInfo = wish.hideSystemInfo,
                            hideClock = wish.hideClock,
                            silenceAlerts = wish.silenceAlerts,
                        ),
                    )
                    if (wish.status != ShizukuStatus.READY) {
                        service = null
                        return@collect
                    }
                    applyEffectiveFlags(context.packageName)
                }
        }
    }

    /**
     * Existing API for callers that apply the user's persistent settings directly.
     * Other owners remain untouched.
     */
    @Synchronized
    fun apply(
        hideIcons: Boolean,
        hideSystemInfo: Boolean,
        hideClock: Boolean,
        silenceAlerts: Boolean,
        packageName: String,
    ): Boolean {
        applicationPackageName = packageName
        setOwnerRequestLocked(
            StatusBarDisableOwner.USER_PERSISTENT,
            StatusBarFlagState(
                hideNotificationIcons = hideIcons,
                hideSystemInfo = hideSystemInfo,
                hideClock = hideClock,
                silenceAlerts = silenceAlerts,
            ),
        )
        return applyEffectiveFlagsLocked(packageName)
    }

    /**
     * Stores or replaces one feature-owned request. This intentionally remembers the request while
     * Shizuku is unavailable; [start] will reapply the full union when the bridge becomes READY.
     */
    @Synchronized
    internal fun setOwnerRequest(
        owner: StatusBarDisableOwner,
        request: StatusBarFlagState,
    ): Boolean {
        setOwnerRequestLocked(owner, request)
        val packageName = applicationPackageName ?: return false
        if (ShizukuState.status.value != ShizukuStatus.READY) {
            service = null
            return false
        }
        return applyEffectiveFlagsLocked(packageName)
    }

    /** Releases only [owner]'s request and preserves every other owner's wishes. */
    @Synchronized
    internal fun clearOwnerRequest(owner: StatusBarDisableOwner): Boolean {
        ownerRequests.remove(owner)
        val packageName = applicationPackageName ?: return false
        if (ShizukuState.status.value != ShizukuStatus.READY) {
            service = null
            return false
        }
        return applyEffectiveFlagsLocked(packageName)
    }

    @Synchronized
    fun pulseStatusIcons(
        durationMs: Long = StatusBarPulseDeadline.DEFAULT_LIVE_ACTIVITY_PULSE_MS,
    ): Boolean {
        if (durationMs <= 0L) {
            clearPulseLocked()
            return false
        }
        if (ShizukuState.status.value != ShizukuStatus.READY) return false
        val scope = applicationScope ?: return false

        val now = SystemClock.elapsedRealtime()
        val deadline = StatusBarPulseDeadline.extend(
            currentDeadlineElapsedRealtimeMs = pulseDeadlineElapsedRealtimeMs,
            nowElapsedRealtimeMs = now,
            durationMs = durationMs,
        ) ?: return false
        pulseDeadlineElapsedRealtimeMs = deadline

        if (!setTransientStatusIconSuppression(active = true)) {
            pulseDeadlineElapsedRealtimeMs = null
            return false
        }

        if (pulseExpiryJob?.isActive != true) {
            pulseExpiryJob = scope.launch { awaitPulseExpiry() }
        }
        return true
    }

    /**
     * The Dynamic Island transient owner remains optional: if Shizuku is down we do not retain a
     * stale pulse request. Clearing always releases only that owner.
     */
    @Synchronized
    fun setTransientStatusIconSuppression(active: Boolean): Boolean {
        val packageName = applicationPackageName ?: return false
        if (active && ShizukuState.status.value != ShizukuStatus.READY) return false

        if (active) {
            setOwnerRequestLocked(
                StatusBarDisableOwner.DYNAMIC_ISLAND_TRANSIENT,
                transientStatusIconRequest,
            )
        } else {
            ownerRequests.remove(StatusBarDisableOwner.DYNAMIC_ISLAND_TRANSIENT)
        }

        if (ShizukuState.status.value != ShizukuStatus.READY) {
            service = null
            return false
        }

        val applied = applyEffectiveFlagsLocked(packageName)
        if (!applied && active) {
            ownerRequests.remove(StatusBarDisableOwner.DYNAMIC_ISLAND_TRANSIENT)
        }
        return applied
    }

    @Synchronized
    fun clearTransientStatusIconSuppression() {
        clearPulseLocked()
    }

    private suspend fun awaitPulseExpiry() {
        while (true) {
            val deadline = synchronized(this) { pulseDeadlineElapsedRealtimeMs } ?: return
            val remaining = StatusBarPulseDeadline.remainingMs(deadline, SystemClock.elapsedRealtime())
            if (remaining > 0L) {
                delay(remaining)
                continue
            }

            val cleared = synchronized(this) {
                val latest = pulseDeadlineElapsedRealtimeMs
                val now = SystemClock.elapsedRealtime()
                if (
                    StatusBarPulseDeadline.shouldExpire(
                        observedDeadlineElapsedRealtimeMs = deadline,
                        currentDeadlineElapsedRealtimeMs = latest,
                        nowElapsedRealtimeMs = now,
                    )
                ) {
                    pulseDeadlineElapsedRealtimeMs = null
                    pulseExpiryJob = null
                    setTransientStatusIconSuppression(active = false)
                    true
                } else {
                    false
                }
            }
            if (cleared) return
        }
    }

    private fun clearPulseLocked() {
        pulseDeadlineElapsedRealtimeMs = null
        pulseExpiryJob?.cancel()
        pulseExpiryJob = null
        setTransientStatusIconSuppression(active = false)
    }

    private fun setOwnerRequestLocked(
        owner: StatusBarDisableOwner,
        request: StatusBarFlagState,
    ) {
        ownerRequests[owner] = request
    }

    @Synchronized
    private fun applyEffectiveFlags(packageName: String): Boolean =
        applyEffectiveFlagsLocked(packageName)

    private fun applyEffectiveFlagsLocked(packageName: String): Boolean {
        val effective = StatusBarDisableReducer.reduce(ownerRequests)
        return applyFlagsLocked(effective, packageName)
    }

    private fun applyFlagsLocked(flags: StatusBarFlagState, packageName: String): Boolean = runCatching {
        var disableFlags = DISABLE_NONE
        if (flags.hideNotificationIcons) disableFlags = disableFlags or DISABLE_NOTIFICATION_ICONS
        if (flags.hideSystemInfo) disableFlags = disableFlags or DISABLE_SYSTEM_INFO
        if (flags.hideClock) disableFlags = disableFlags or DISABLE_CLOCK
        if (flags.silenceAlerts) disableFlags = disableFlags or DISABLE_NOTIFICATION_ALERTS
        val statusBar = service ?: buildService().also { service = it }
        statusBar.disable(disableFlags, packageName)
        true
    }.getOrElse { error ->
        Log.w(
            TAG,
            "Could not apply status-bar flags " +
                "(icons=${flags.hideNotificationIcons}, systemInfo=${flags.hideSystemInfo}, " +
                "clock=${flags.hideClock}, alerts=${flags.silenceAlerts})",
            error,
        )
        service = null
        false
    }

    private data class Wish(
        val hideIcons: Boolean,
        val hideSystemInfo: Boolean,
        val hideClock: Boolean,
        val silenceAlerts: Boolean,
        val status: ShizukuStatus,
    )

    /**
     * Hidden/non-SDK service access is necessary because third-party apps cannot hold STATUS_BAR.
     * Reflection targets the running framework's own stub instead of freezing AIDL transaction IDs.
     */
    private fun buildService(): Any {
        val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("statusbar"))
        return Class.forName("com.android.internal.statusbar.IStatusBarService\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, binder)
            ?: error("IStatusBarService.asInterface returned null")
    }

    private fun Any.disable(flags: Int, packageName: String) {
        val disable = runCatching {
            javaClass.getMethod(
                "disable",
                Int::class.javaPrimitiveType,
                IBinder::class.java,
                String::class.java,
            )
        }.getOrNull()
        if (disable != null) {
            disable.invoke(this, flags, token, packageName)
            return
        }
        javaClass.getMethod(
            "disableForUser",
            Int::class.javaPrimitiveType,
            IBinder::class.java,
            String::class.java,
            Int::class.javaPrimitiveType,
        ).invoke(this, flags, token, packageName, android.os.Process.myUserHandle().hashCode())
    }
}
