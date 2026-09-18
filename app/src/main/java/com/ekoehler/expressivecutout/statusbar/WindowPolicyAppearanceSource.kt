package com.ekoehler.expressivecutout.statusbar

import android.content.Context
import android.util.Log
import com.ekoehler.expressivecutout.system.ShizukuState
import com.ekoehler.expressivecutout.system.ShizukuStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext

/**
 * Parses the stable policy fields that Android's WindowManager dumps for the status bar.
 *
 * Android 16 DisplayPolicy prints both mLastAppearance and mLastStatusBarAppearanceRegions from
 * the same state it reports to SystemUI. We intentionally consume only those fields instead of
 * depending on unrelated WindowManager text.
 */
internal object WindowPolicyAppearanceParser {

    fun parse(raw: String): SystemBarAppearanceSnapshot? {
        val regionPattern = Regex(
            """AppearanceRegion\{([^}]*)bounds=\[\s*(-?\d+)\s*,\s*(-?\d+)\s*\]\[\s*(-?\d+)\s*,\s*(-?\d+)\s*\]\}""",
        )
        val globalAppearance = raw.lineSequence()
            .map(String::trim)
            .firstOrNull { it.startsWith("mLastAppearance=") }
            ?.substringAfter('=')
            ?.let(::appearanceBits)

        val regions = raw.lineSequence().mapNotNull { line ->
            val match = regionPattern.find(line) ?: return@mapNotNull null
            val flags = match.groupValues[1]
            SystemBarAppearanceSnapshot.Region(
                left = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null,
                top = match.groupValues[3].toIntOrNull() ?: return@mapNotNull null,
                right = match.groupValues[4].toIntOrNull() ?: return@mapNotNull null,
                bottom = match.groupValues[5].toIntOrNull() ?: return@mapNotNull null,
                appearance = appearanceBits(flags),
            )
        }.toList()

        if (globalAppearance == null && regions.isEmpty()) return null
        return SystemBarAppearanceSnapshot(
            globalAppearance = globalAppearance,
            regions = regions,
        )
    }

    private fun appearanceBits(flags: String): Int =
        if ("LIGHT_STATUS_BARS" in flags) StatusBarAppearanceBits.LIGHT_STATUS_BARS else 0
}

/**
 * Real one-shot appearance source backed by WindowManager through Shizuku.
 *
 * There is deliberately no polling flow: foreground/window events owned by the accessibility
 * service call snapshot() to reconcile. The wrapped binder performs the privileged dump as the
 * Shizuku shell identity; any failure returns null so Auto mode falls back to the system theme.
 */
internal class ShizukuWindowAppearanceSource(
    context: Context,
    transport: WindowPolicyDumpTransport = ShizukuUserServiceWindowPolicyDumpTransport(context),
) : SystemBarAppearanceSource {

    private val reader = WindowPolicySnapshotReader(transport)

    override val changes: Flow<SystemBarAppearanceSnapshot> = emptyFlow()

    override suspend fun snapshot(): SystemBarAppearanceSnapshot? = withContext(Dispatchers.IO) {
        val shizuku = ShizukuState.status.value
        if (shizuku != ShizukuStatus.READY) {
            Log.d(
                TAG,
                "AUTO_APPEARANCE source=window_policy shizuku=$shizuku rawAvailable=false " +
                    "rawLength=0 parserSuccess=false reason=shizuku_not_ready",
            )
            return@withContext null
        }

        runCatching {
            val read = reader.read()
            val raw = read.raw
            val snapshot = read.snapshot
            Log.d(
                TAG,
                "AUTO_APPEARANCE source=window_policy shizuku=READY " +
                    "transport=user_service rawAvailable=${!raw.isNullOrBlank()} " +
                    "rawLength=${raw?.length ?: 0} " +
                    "globalAppearance=${snapshot?.globalAppearance} " +
                    "regions=${snapshot?.regions?.size ?: 0} parserSuccess=${snapshot != null} " +
                    "relevant=${raw?.let(::relevantLines) ?: "<none>"}",
            )
            snapshot
        }.onFailure { error ->
            Log.d(
                TAG,
                "AUTO_APPEARANCE source=window_policy shizuku=READY " +
                    "transport=user_service rawAvailable=false parserSuccess=false " +
                    "reason=${throwableSummary(error)}",
            )
        }.getOrNull()
    }

    private fun throwableSummary(error: Throwable): String =
        generateSequence<Throwable?>(error) { it.cause }
            .filterNotNull()
            .take(4)
            .joinToString(" <- ") { cause ->
                "${cause.javaClass.simpleName}:${cause.message}"
            }

    private fun relevantLines(raw: String): String = raw.lineSequence()
        .map(String::trim)
        .filter { line ->
            line.contains("appearance", ignoreCase = true) ||
                line.contains("statusbar", ignoreCase = true)
        }
        .filter(String::isNotBlank)
        .take(8)
        .joinToString(" | ")
        .take(1_200)
        .ifBlank { "<none>" }

    private companion object {
        const val TAG = "StatusBarAuto"
    }
}
