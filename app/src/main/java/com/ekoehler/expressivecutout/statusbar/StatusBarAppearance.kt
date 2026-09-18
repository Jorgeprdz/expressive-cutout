package com.ekoehler.expressivecutout.statusbar

/** Foreground tone the future custom status-bar glyphs should render with. */
internal enum class StatusBarForeground {
    LIGHT,
    DARK,
}

/** User policy; AUTO follows the foreground app's requested system-bar appearance when available. */
internal enum class StatusBarAppearanceMode {
    AUTO,
    FORCE_LIGHT_FOREGROUND,
    FORCE_DARK_FOREGROUND,
}

internal enum class StatusBarSystemTheme {
    LIGHT,
    DARK,
}

/**
 * Centralized semantic bit used by the Android-free reducer.
 *
 * 0x8 is the public WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS value on API 23+.
 * Keeping the numeric bridge here avoids spreading a platform constant through parsers/tests.
 */
internal object StatusBarAppearanceBits {
    const val LIGHT_STATUS_BARS = 0x8
}

internal data class StatusBarAppearanceRegion(
    val bounds: StatusBarRect,
    val appearance: Int,
)

internal data class StatusBarAppearanceState(
    val globalAppearance: Int? = null,
    val regions: List<StatusBarAppearanceRegion> = emptyList(),
)

/** Pure resolver from system-bar appearance semantics to our own foreground tone. */
internal object StatusBarAppearanceResolver {

    fun resolve(
        mode: StatusBarAppearanceMode,
        state: StatusBarAppearanceState?,
        x: Int,
        y: Int,
        systemTheme: StatusBarSystemTheme,
    ): StatusBarForeground = when (mode) {
        StatusBarAppearanceMode.FORCE_LIGHT_FOREGROUND -> StatusBarForeground.LIGHT
        StatusBarAppearanceMode.FORCE_DARK_FOREGROUND -> StatusBarForeground.DARK
        StatusBarAppearanceMode.AUTO -> {
            val appearance = state
                ?.regions
                ?.firstOrNull { it.bounds.contains(x, y) }
                ?.appearance
                ?: state?.globalAppearance

            if (appearance != null) {
                foregroundForAppearance(appearance)
            } else {
                fallbackForTheme(systemTheme)
            }
        }
    }

    fun foregroundForAppearance(appearance: Int): StatusBarForeground =
        if (appearance and StatusBarAppearanceBits.LIGHT_STATUS_BARS != 0) {
            // "LIGHT_STATUS_BARS" describes the background treatment: SystemUI uses dark glyphs.
            StatusBarForeground.DARK
        } else {
            StatusBarForeground.LIGHT
        }

    fun fallbackForTheme(theme: StatusBarSystemTheme): StatusBarForeground = when (theme) {
        StatusBarSystemTheme.LIGHT -> StatusBarForeground.DARK
        StatusBarSystemTheme.DARK -> StatusBarForeground.LIGHT
    }
}
