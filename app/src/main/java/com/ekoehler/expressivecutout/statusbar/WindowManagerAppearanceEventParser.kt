package com.ekoehler.expressivecutout.statusbar

import java.util.Locale

internal data class WindowManagerTraceLine(
    val at: Long,
    val pid: String,
    val tid: String,
    val message: String,
)

internal data class WindowManagerAppearanceEvent(
    val sourceWindow: String,
    val lightStatusBars: Boolean,
)

/**
 * Pure parser for WindowManager appearance events. Android's logcat prints the window header and
 * the matching status-bar regions as separate lines on the same pid/tid, so callers keep a short
 * context buffer and ask this parser to correlate the lines to the currently focused window.
 */
internal object WindowManagerAppearanceEventParser {

    private val traceLinePattern = Regex(
        """^(\d\d-\d\d)\s+(\d\d):(\d\d):(\d\d)\.(\d{3})\s+(\d+)\s+(\d+)\s+[VDIWEF]\s+WindowManager:\s?(.*)$""",
    )
    private val focusPattern = Regex("""(?i)\bto\s+Window\{([0-9a-f]+)\s+""")
    private val windowPattern = Regex("""(?i)\bwin=Window\{([0-9a-f]+)\b""")

    fun parseTraceLine(line: String): WindowManagerTraceLine? {
        val match = traceLinePattern.find(line) ?: return null
        val at = (((match.groupValues[2].toLong() * 60 + match.groupValues[3].toLong()) * 60 +
            match.groupValues[4].toLong()) * 1_000 + match.groupValues[5].toLong())
        return WindowManagerTraceLine(
            at = at,
            pid = match.groupValues[6],
            tid = match.groupValues[7],
            message = match.groupValues[8],
        )
    }

    fun focusedWindowFromMessage(message: String): String? {
        if (!message.contains("Changing focus", ignoreCase = true)) return null
        return focusPattern.find(message)
            ?.groupValues
            ?.getOrNull(1)
            ?.lowercase(Locale.ROOT)
    }

    fun appearanceFromTrace(
        current: WindowManagerTraceLine,
        prior: List<WindowManagerTraceLine>,
        focusedWindow: String?,
    ): WindowManagerAppearanceEvent? {
        val focus = focusedWindow?.lowercase(Locale.ROOT) ?: return null
        if (!current.message.contains("updateSystemBarAttributes", ignoreCase = true)) return null
        if (!current.message.contains("statusBarAprRegions=", ignoreCase = true)) return null

        val header = prior.asReversed().firstOrNull { line ->
            line.pid == current.pid &&
                line.tid == current.tid &&
                current.at - line.at in 0..2_000 &&
                line.message.contains("updateSystemBarAttributes", ignoreCase = true) &&
                line.message.contains("win=Window{", ignoreCase = true)
        } ?: return null

        val sourceWindow = windowPattern.find(header.message)
            ?.groupValues
            ?.getOrNull(1)
            ?.lowercase(Locale.ROOT)
            ?: return null
        if (sourceWindow != focus) return null

        return WindowManagerAppearanceEvent(
            sourceWindow = sourceWindow,
            lightStatusBars = current.message
                .substringAfter("statusBarAprRegions=", "")
                .contains("LIGHT_STATUS_BARS", ignoreCase = true),
        )
    }

    fun lightStatusBarsFromDumpsysWindow(raw: String): Boolean? {
        return raw.lineSequence()
            .map(String::trim)
            .firstOrNull { it.startsWith("mLastAppearance=", ignoreCase = true) }
            ?.substringAfter('=')
            ?.contains("LIGHT_STATUS_BARS", ignoreCase = true)
    }
}
