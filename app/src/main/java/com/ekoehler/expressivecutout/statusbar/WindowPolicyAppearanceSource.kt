package com.ekoehler.expressivecutout.statusbar

import android.content.Context
import com.ekoehler.expressivecutout.system.ShizukuState
import com.ekoehler.expressivecutout.system.ShizukuStatus
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * Parses the stable policy fields that Android's WindowManager dumps for the status bar.
 *
 * Android 16 DisplayPolicy prints both mLastAppearance and mLastStatusBarAppearanceRegions from
 * the same state it reports to SystemUI. We intentionally consume only those fields instead of
 * depending on unrelated WindowManager text.
 */
internal object WindowPolicyAppearanceParser {

    private val regionPattern = Regex(
        """AppearanceRegion\{([^}]]*?)bounds=\[\s*(-?\d+)\s*,\s*(-?\d+)\s*]\[\s*(-?\d+)\s*,\s*(-?\d+)\s*]}""",
    )

    fun parse(raw: String): SystemBarAppearanceSnapshot? {
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
    private val context: Context,
) : SystemBarAppearanceSource {

    override val changes: Flow<SystemBarAppearanceSnapshot> = emptyFlow()

    override suspend fun snapshot(): SystemBarAppearanceSnapshot? = withContext(Dispatchers.IO) {
        if (ShizukuState.status.value != ShizukuStatus.READY) return@withContext null

        runCatching {
            val target = SystemServiceHelper.getSystemService("window")
                ?: return@runCatching null
            val windowBinder = ShizukuBinderWrapper(target)
            val dumpFile = File.createTempFile("window-policy-", ".txt", context.cacheDir)
            try {
                FileOutputStream(dumpFile).use { stream ->
                    windowBinder.dump(stream.fd, arrayOf("policy"))
                    stream.flush()
                }
                WindowPolicyAppearanceParser.parse(dumpFile.readText())
            } finally {
                dumpFile.delete()
            }
        }.getOrNull()
    }
}
