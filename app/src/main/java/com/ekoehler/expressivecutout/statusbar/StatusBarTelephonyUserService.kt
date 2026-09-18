package com.ekoehler.expressivecutout.statusbar

import android.content.Context
import androidx.annotation.Keep
import kotlin.system.exitProcess

/**
 * Tiny Shizuku UserService used only for one-shot telephony registry snapshots.
 *
 * The custom status bar is already gated by Shizuku, so this avoids adding READ_PHONE_STATE just
 * to obtain the display network label. There is no polling or persistent observer in this process.
 */
@Keep
class StatusBarTelephonyUserService() : IStatusBarTelephonyUserService.Stub() {

    @Keep
    @Suppress("UNUSED_PARAMETER")
    constructor(context: Context) : this()

    override fun dumpTelephonyRegistry(): String {
        val process = ProcessBuilder("/system/bin/dumpsys", "telephony.registry")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        check(exitCode == 0) { "dumpsys telephony.registry exited with code $exitCode" }
        return output
    }

    override fun destroy() {
        exitProcess(0)
    }
}
