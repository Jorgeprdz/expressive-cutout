package com.ekoehler.expressivecutout.statusbar

/** Rendering state for the future custom status-bar layer. */
internal enum class StatusBarRenderMode {
    SHOW,
    HIDE,
    SUSPEND,
}

/**
 * Inputs are deliberately platform-neutral so Android/OEM detection can evolve without changing
 * the rendering policy itself.
 */
internal data class StatusBarVisibilityInput(
    val enabled: Boolean = true,
    val screenOn: Boolean = true,
    val locked: Boolean = false,
    val aod: Boolean = false,
    val fullscreen: Boolean = false,
    val immersive: Boolean = false,
    val shadeExpanded: Boolean = false,
    val quickSettingsExpanded: Boolean = false,
    val dex: Boolean = false,
    val multiWindow: Boolean = false,
)

internal data class StatusBarVisibilityDecision(
    val renderMode: StatusBarRenderMode,
    /** Desired CUSTOM_STATUS_BAR ownership; actual application still depends on Shizuku readiness. */
    val holdNativeSuppression: Boolean,
)

/** Pure lifecycle/visibility policy. Detection of these states is intentionally outside this class. */
internal object StatusBarVisibilityPolicy {

    fun decide(input: StatusBarVisibilityInput): StatusBarVisibilityDecision {
        if (!input.enabled) {
            return StatusBarVisibilityDecision(StatusBarRenderMode.HIDE, false)
        }

        if (!input.screenOn || input.locked || input.aod || input.dex) {
            return StatusBarVisibilityDecision(StatusBarRenderMode.HIDE, false)
        }

        // A fullscreen/immersive app owns whether the status-bar region is visible. We keep the
        // configured native-hide lease but never force our visual layer into an absent system bar.
        if (input.fullscreen || input.immersive) {
            return StatusBarVisibilityDecision(StatusBarRenderMode.HIDE, true)
        }

        // M1 will perform the visual handoff; keeping ownership here avoids native glyphs flashing
        // during partial shade/QS expansion.
        if (input.shadeExpanded || input.quickSettingsExpanded) {
            return StatusBarVisibilityDecision(StatusBarRenderMode.SUSPEND, true)
        }

        return StatusBarVisibilityDecision(StatusBarRenderMode.SHOW, true)
    }
}
