package com.ekoehler.expressivecutout.statusbar

import android.content.Context
import androidx.annotation.Keep
import kotlin.system.exitProcess

/**
 * Minimal Shizuku UserService used only to execute the one-shot WindowManager policy dump as the
 * shell/root identity owned by Shizuku. It does not observe anything and has no recurring work.
 */
@Keep
class StatusBarAppearanceUserService() : IStatusBarAppearanceUserService.Stub() {

    @Keep
    @Suppress("UNUSED_PARAMETER")
    constructor(context: Context) : this()

    override fun dumpWindowPolicy(): String {
        val process = ProcessBuilder("/system/bin/dumpsys", "window", "policy")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        check(exitCode == 0) { "dumpsys window policy exited with code $exitCode" }
        return output
    }

    override fun destroy() {
        exitProcess(0)
    }
}
