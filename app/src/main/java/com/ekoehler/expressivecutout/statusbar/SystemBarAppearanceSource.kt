package com.ekoehler.expressivecutout.statusbar

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Structured transport boundary for the future Shizuku appearance source.
 *
 * M0 intentionally does not parse dumpsys or invent a Samsung text format. A concrete Android 16
 * source must translate verified platform/OEM evidence into this neutral snapshot.
 */
internal data class SystemBarAppearanceSnapshot(
    val globalAppearance: Int? = null,
    val regions: List<Region> = emptyList(),
) {
    data class Region(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val appearance: Int?,
    )
}

/** Normalizes source snapshots while failing closed to the theme fallback on malformed data. */
internal object SystemBarAppearanceNormalizer {

    fun normalize(snapshot: SystemBarAppearanceSnapshot): StatusBarAppearanceState? {
        val regions = snapshot.regions.mapNotNull { region ->
            val bounds = StatusBarRect(
                left = region.left,
                top = region.top,
                right = region.right,
                bottom = region.bottom,
            )
            val appearance = region.appearance
            if (!bounds.isValid || appearance == null) {
                null
            } else {
                StatusBarAppearanceRegion(bounds = bounds, appearance = appearance)
            }
        }

        if (snapshot.globalAppearance == null && regions.isEmpty()) return null
        return StatusBarAppearanceState(
            globalAppearance = snapshot.globalAppearance,
            regions = regions,
        )
    }
}

/**
 * Event-driven source contract plus a one-shot snapshot for foreground-window reconciliation.
 * No polling contract exists here by design.
 */
internal interface SystemBarAppearanceSource {
    val changes: Flow<SystemBarAppearanceSnapshot>
    suspend fun snapshot(): SystemBarAppearanceSnapshot?
}

/**
 * Small state holder shared by source events and TYPE_WINDOW_STATE_CHANGED reconciliation.
 *
 * The accessibility service will only need to call [reconcile] in M1; source-specific hidden API
 * work stays behind [SystemBarAppearanceSource].
 */
internal class StatusBarAppearanceController(
    private val source: SystemBarAppearanceSource,
) {
    private val _state = MutableStateFlow<StatusBarAppearanceState?>(null)
    val state: StateFlow<StatusBarAppearanceState?> = _state.asStateFlow()

    fun start(scope: CoroutineScope): Job = scope.launch {
        source.changes.collect { snapshot ->
            _state.value = SystemBarAppearanceNormalizer.normalize(snapshot)
        }
    }

    suspend fun reconcile() {
        _state.value = source.snapshot()?.let(SystemBarAppearanceNormalizer::normalize)
    }
}
