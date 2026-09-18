package com.ekoehler.expressivecutout.statusbar

import android.content.Context
import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.annotation.Keep
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

/**
 * Shizuku-side telephony bridge. The app process never asks for READ_PHONE_STATE; instead this
 * service executes with Shizuku's privileged identity and returns only display-oriented radio
 * information needed by the custom status bar.
 */
@Keep
class StatusBarTelephonyUserService() : IStatusBarTelephonyUserService.Stub() {

    @Volatile
    private var appContext: Context? = null

    @Keep
    constructor(context: Context) : this() {
        appContext = context.applicationContext
    }

    override fun readDisplayInfo(): IntArray {
        val context = appContext ?: return intArrayOf(UNKNOWN, OVERRIDE_NONE)
        val manager = context.getSystemService(TelephonyManager::class.java)
            ?: return intArrayOf(UNKNOWN, OVERRIDE_NONE)

        val fallbackNetwork = runCatching { manager.dataNetworkType }.getOrDefault(UNKNOWN)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return intArrayOf(fallbackNetwork, OVERRIDE_NONE)
        }

        val network = AtomicInteger(fallbackNetwork)
        val override = AtomicInteger(OVERRIDE_NONE)
        val latch = CountDownLatch(1)

        val callback = object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
            override fun onDisplayInfoChanged(info: TelephonyDisplayInfo) {
                network.set(info.networkType)
                override.set(info.overrideNetworkType)
                latch.countDown()
            }
        }

        val registered = runCatching {
            manager.registerTelephonyCallback(DIRECT_EXECUTOR, callback)
            true
        }.getOrDefault(false)

        if (!registered) return intArrayOf(fallbackNetwork, OVERRIDE_NONE)

        try {
            latch.await(DISPLAY_INFO_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } finally {
            runCatching { manager.unregisterTelephonyCallback(callback) }
        }

        return intArrayOf(network.get(), override.get())
    }

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

    private companion object {
        const val UNKNOWN = 0
        const val OVERRIDE_NONE = 0
        const val DISPLAY_INFO_TIMEOUT_MS = 900L
        val DIRECT_EXECUTOR = java.util.concurrent.Executor { command -> command.run() }
    }
}
