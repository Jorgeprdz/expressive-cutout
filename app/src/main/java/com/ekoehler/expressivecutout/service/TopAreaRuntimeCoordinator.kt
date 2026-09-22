package com.ekoehler.expressivecutout.service

import android.content.Context
import com.ekoehler.expressivecutout.data.BehaviourPreferences
import com.ekoehler.expressivecutout.data.StatusBarPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Desired top-area feature state, independent from transient availability such as Shizuku. */
internal data class TopAreaRuntimeState(
    val islandWanted: Boolean,
    val statusBarWanted: Boolean,
    val overlayWanted: Boolean,
)

/** Pure truth table for the two independently persisted top-area modules. */
internal object TopAreaRuntimePolicy {
    fun derive(
        islandWanted: Boolean,
        statusBarWanted: Boolean,
    ): TopAreaRuntimeState = TopAreaRuntimeState(
        islandWanted = islandWanted,
        statusBarWanted = statusBarWanted,
        overlayWanted = islandWanted || statusBarWanted,
    )
}

/** Suppresses duplicate lifecycle reconciliation for repeated identical preference emissions. */
internal class TopAreaRuntimeReconciler {
    private var current: TopAreaRuntimeState? = null

    fun accept(next: TopAreaRuntimeState): Boolean {
        if (current == next) return false
        current = next
        return true
    }

    fun reset() {
        current = null
    }
}

/**
 * Observes the existing Dynamic Island and Custom Status Bar preferences and emits one deterministic
 * desired runtime state. It owns no UI and performs no polling; Android-facing start/stop work stays
 * in the accessibility service.
 */
internal class TopAreaRuntimeCoordinator(
    context: Context,
    private val scope: CoroutineScope,
    private val onStateChanged: (TopAreaRuntimeState) -> Unit,
) {
    private val behaviourPreferences = BehaviourPreferences(context.applicationContext)
    private val statusBarPreferences = StatusBarPreferences(context.applicationContext)
    private val reconciler = TopAreaRuntimeReconciler()
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            combine(
                behaviourPreferences.settings
                    .map { it.cutoutEnabled }
                    .distinctUntilChanged(),
                statusBarPreferences.customStatusBarEnabled
                    .distinctUntilChanged(),
            ) { islandWanted, statusBarWanted ->
                TopAreaRuntimePolicy.derive(islandWanted, statusBarWanted)
            }
                .distinctUntilChanged()
                .collect { next ->
                    if (reconciler.accept(next)) onStateChanged(next)
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        reconciler.reset()
    }
}
