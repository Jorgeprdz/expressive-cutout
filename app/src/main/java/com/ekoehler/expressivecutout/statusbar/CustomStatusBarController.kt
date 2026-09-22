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
internal class CustomStatusBarController(
    context: Context,
    private val orientation: StateFlow<Int>,
    private val locked: StateFlow<Boolean>,
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

    fun start() {
        if (runtimeScope != null) return
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        runtimeScope = scope
        controlJob = scope.launch {
            combine(
                preferences.customStatusBarSettings,
                ShizukuState.status,
                orientation,
                locked,
            ) { settings, shizuku, orientation, locked ->
                Wish(settings, shizuku, orientation, locked)
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

    fun stop() {
        StatusBarIconController.clearOwnerRequest(StatusBarDisableOwner.CUSTOM_STATUS_BAR)
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
        val decision = CustomStatusBarActivationPolicy.decide(
            enabled = wish.settings.enabled && !wish.locked,
            shizukuReady = wish.shizuku == ShizukuStatus.READY,
            portraitSupported = portraitSupported,
        )

        if (wish.shizuku != ShizukuStatus.READY) {
            appearanceJob?.cancel()
            appearanceJob = null

            // We cannot issue disable(0) after Shizuku disappears. If a native suppression lease
            // was already applied, keep the matching renderer visible rather than leave a blank
            // status bar. The owner request is retained so StatusBarIconController re-applies it
            // when Shizuku reconnects. If the user turned the feature off while disconnected,
            // release the local owner now so reconnect clears the native lease before we hide.
            if (!wish.settings.enabled) {
                StatusBarIconController.clearOwnerRequest(StatusBarDisableOwner.CUSTOM_STATUS_BAR)
            }
            _render.value =
                nativeSuppressionApplied && wish.settings.enabled && !wish.locked && portraitSupported
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
    )
}
