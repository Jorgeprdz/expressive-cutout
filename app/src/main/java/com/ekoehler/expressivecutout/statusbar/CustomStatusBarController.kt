package com.ekoehler.expressivecutout.statusbar

import android.content.Context
import android.content.res.Configuration
import com.ekoehler.expressivecutout.data.CustomStatusBarAppearancePreference
import com.ekoehler.expressivecutout.data.CustomStatusBarSettings
import com.ekoehler.expressivecutout.data.StatusBarPreferences
import com.ekoehler.expressivecutout.system.ShizukuState
import com.ekoehler.expressivecutout.system.ShizukuStatus
import com.ekoehler.expressivecutout.system.StatusBarDisableOwner
import com.ekoehler.expressivecutout.system.StatusBarIconController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Owns the Custom Status Bar lifecycle independently from the Dynamic Island.
 *
 * The shared accessibility overlay remains the visual host, but native SystemUI suppression,
 * Shizuku readiness, appearance monitoring and persisted status-bar settings are owned here.
 */
internal object CustomStatusBarDisconnectPolicy {
    fun keepRenderer(
        nativeSuppressionApplied: Boolean,
        enabled: Boolean,
        locked: Boolean,
        portraitSupported: Boolean,
    ): Boolean =
        nativeSuppressionApplied && enabled && !locked && portraitSupported
}

internal class CustomStatusBarController(
    context: Context,
    private val orientation: StateFlow<Int>,
    private val locked: StateFlow<Boolean>,
    private val screenOn: StateFlow<Boolean>,
) {
    private val appContext = context.applicationContext
    private val preferences = StatusBarPreferences(appContext)
    private val appearanceController = StatusBarAppearanceController(
        ShizukuWindowAppearanceSource(appContext),
    )

    private val _render = MutableStateFlow(false)
    val render: StateFlow<Boolean> = _render.asStateFlow()

    private val _settings = MutableStateFlow(CustomStatusBarSettings.DEFAULT)
    val settings: StateFlow<CustomStatusBarSettings> = _settings.asStateFlow()

    private val _appearanceMode = MutableStateFlow(StatusBarAppearanceMode.AUTO)
    val appearanceMode: StateFlow<StatusBarAppearanceMode> = _appearanceMode.asStateFlow()

    val systemAppearance: StateFlow<StatusBarAppearanceState?> = appearanceController.state

    private var runtimeScope: CoroutineScope? = null
    private var controlJob: Job? = null
    private var appearanceJob: Job? = null
    private var nativeSuppressionApplied = false
    private var stopRequested = false

    fun start() {
        stopRequested = false
        if (runtimeScope != null) return
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        runtimeScope = scope
        controlJob = scope.launch {
            combine(
                preferences.customStatusBarSettings,
                ShizukuState.status,
                orientation,
                locked,
                screenOn,
            ) { settings, shizuku, orientation, locked, screenOn ->
                Wish(settings, shizuku, orientation, locked, screenOn)
            }
                .distinctUntilChanged()
                .collect { wish -> reconcile(scope, wish) }
        }
    }

    fun reconcileAppearance() {
        val scope = runtimeScope ?: return
        if (!_render.value) return
        scope.launch { appearanceController.reconcile() }
    }

    /**
     * Requests a normal feature shutdown. If Shizuku is unavailable while a native suppression
     * lease is known to be active, the visual renderer must stay alive until that lease can be
     * released; otherwise the user would see an empty status-bar region.
     *
     * @return true when all Custom Status Bar runtime resources are already stopped.
     */
    fun requestStop(): Boolean {
        stopRequested = true
        StatusBarIconController.clearOwnerRequest(StatusBarDisableOwner.CUSTOM_STATUS_BAR)
        if (ShizukuState.status.value != ShizukuStatus.READY && nativeSuppressionApplied) {
            return false
        }
        finishStop()
        return true
    }

    /** Forced teardown used when the accessibility host itself is going away. */
    fun stop() {
        stopRequested = true
        StatusBarIconController.clearOwnerRequest(StatusBarDisableOwner.CUSTOM_STATUS_BAR)
        finishStop()
    }

    private fun finishStop() {
        nativeSuppressionApplied = false
        _render.value = false
        appearanceJob?.cancel()
        appearanceJob = null
        controlJob?.cancel()
        controlJob = null
        runtimeScope?.cancel()
        runtimeScope = null
    }

    private fun reconcile(scope: CoroutineScope, wish: Wish) {
        _settings.value = wish.settings
        _appearanceMode.value = when (wish.settings.appearance) {
            CustomStatusBarAppearancePreference.AUTO -> StatusBarAppearanceMode.AUTO
            CustomStatusBarAppearancePreference.LIGHT ->
                StatusBarAppearanceMode.FORCE_LIGHT_FOREGROUND
            CustomStatusBarAppearancePreference.DARK ->
                StatusBarAppearanceMode.FORCE_DARK_FOREGROUND
        }

        val portraitSupported = wish.orientation == Configuration.ORIENTATION_PORTRAIT
        val visibility = StatusBarVisibilityPolicy.decide(
            StatusBarVisibilityInput(
                enabled = wish.settings.enabled,
                screenOn = wish.screenOn,
                locked = wish.locked,
            ),
        )
        val visibleWanted = visibility.renderMode == StatusBarRenderMode.SHOW
        val decision = CustomStatusBarActivationPolicy.decide(
            enabled = visibleWanted,
            shizukuReady = wish.shizuku == ShizukuStatus.READY,
            portraitSupported = portraitSupported,
        )

        if (wish.shizuku != ShizukuStatus.READY) {
            appearanceJob?.cancel()
            appearanceJob = null

            if (nativeSuppressionApplied) {
                if (wish.settings.enabled && !stopRequested) {
                    // requestStop may have removed this owner while disconnected. Re-add the
                    // desired lease if the user turns the feature back on before Shizuku returns;
                    // setOwnerRequest remembers it even though it cannot transact yet.
                    CustomStatusBarActivationPolicy.decide(
                        enabled = true,
                        shizukuReady = true,
                        portraitSupported = true,
                    ).nativeRequest?.let { request ->
                        StatusBarIconController.setOwnerRequest(
                            StatusBarDisableOwner.CUSTOM_STATUS_BAR,
                            request,
                        )
                    }
                } else {
                    // Forget the local owner now so StatusBarIconController clears the real native
                    // lease as soon as Shizuku reconnects.
                    StatusBarIconController.clearOwnerRequest(StatusBarDisableOwner.CUSTOM_STATUS_BAR)
                }
            }

            // Safety wins over the requested OFF state until native SystemUI can be restored.
            _render.value = CustomStatusBarDisconnectPolicy.keepRenderer(
                nativeSuppressionApplied = nativeSuppressionApplied,
                enabled = nativeSuppressionApplied,
                locked = wish.locked || !wish.screenOn,
                portraitSupported = portraitSupported,
            )
            return
        }

        if (stopRequested) {
            StatusBarIconController.clearOwnerRequest(StatusBarDisableOwner.CUSTOM_STATUS_BAR)
            finishStop()
            return
        }

        if (decision.canRender) {
            ensureAppearanceMonitoring(scope)
        } else {
            appearanceJob?.cancel()
            appearanceJob = null
        }

        val applied = decision.nativeRequest?.let { request ->
            StatusBarIconController.setOwnerRequest(
                StatusBarDisableOwner.CUSTOM_STATUS_BAR,
                request,
            )
        } ?: run {
            StatusBarIconController.clearOwnerRequest(StatusBarDisableOwner.CUSTOM_STATUS_BAR)
            false
        }

        nativeSuppressionApplied = decision.nativeRequest != null && applied
        _render.value = decision.canRender && applied
        if (_render.value) {
            scope.launch { appearanceController.reconcile() }
        }
    }

    private fun ensureAppearanceMonitoring(scope: CoroutineScope) {
        if (appearanceJob?.isActive == true) return
        appearanceJob = appearanceController.start(scope)
    }

    private data class Wish(
        val settings: CustomStatusBarSettings,
        val shizuku: ShizukuStatus,
        val orientation: Int,
        val locked: Boolean,
        val screenOn: Boolean,
    )
}
