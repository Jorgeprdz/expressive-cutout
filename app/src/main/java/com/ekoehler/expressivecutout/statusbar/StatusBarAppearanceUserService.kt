package com.ekoehler.expressivecutout.statusbar

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.ArrayDeque
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

/**
 * Shizuku-side appearance bridge. It observes WindowManager as the Shizuku shell identity and
 * forwards only the LIGHT_STATUS_BARS semantic needed by the custom status bar.
 */
@Keep
class StatusBarAppearanceUserService() : IStatusBarAppearanceUserService.Stub() {

    @Keep
    @Suppress("UNUSED_PARAMETER")
    constructor(context: Context) : this()

    private val running = AtomicBoolean(false)
    private val context = ArrayDeque<WindowManagerTraceLine>()
    private val settle = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "ExpressiveCutout-AutoColour").apply { isDaemon = true }
    }
    private val lock = Any()

    @Volatile
    private var process: Process? = null

    @Volatile
    private var callback: IStatusBarAppearanceCallback? = null

    @Volatile
    private var focusedWindow: String? = null

    @Volatile
    private var lastLightStatusBars: Boolean? = null

    private var pending: ScheduledFuture<*>? = null
    private var generation = 0L
    private var pendingLightStatusBars: Boolean? = null
    private var pendingWindow: String? = null

    override fun dumpWindowPolicy(): String = runCommand("/system/bin/dumpsys", "window", "policy")

    override fun dumpWindow(): String = runCommand("/system/bin/dumpsys", "window")

    override fun startMonitor(cb: IStatusBarAppearanceCallback?) {
        callback = cb
        Log.d(TAG, "AUTO_APPEARANCE monitor=start callback=${cb != null}")
        lastLightStatusBars?.let { value ->
            runCatching { cb?.onAppearanceChanged(value) }
        }
        if (running.compareAndSet(false, true)) {
            Thread(::runLogcat, "ExpressiveCutout-AutoColourEvents").apply { isDaemon = true }.start()
        }
        scheduleSnapshot(focusedWindow, reason = "start")
    }

    private fun runLogcat() {
        try {
            process = ProcessBuilder(
                "/system/bin/logcat",
                "-v",
                "threadtime",
                "WindowManager:D",
                "*:S",
            ).redirectErrorStream(true).start()
            BufferedReader(InputStreamReader(process!!.inputStream)).use { reader ->
                while (running.get()) {
                    val line = reader.readLine() ?: break
                    consume(line)
                }
            }
        } catch (error: Throwable) {
            Log.d(TAG, "AUTO_APPEARANCE monitor=logcat_failed reason=${error.javaClass.simpleName}:${error.message}")
            scheduleSnapshot(focusedWindow, reason = "logcat_failed")
        } finally {
            running.set(false)
            Log.d(TAG, "AUTO_APPEARANCE monitor=stopped")
        }
    }

    private fun consume(line: String) {
        val parsed = WindowManagerAppearanceEventParser.parseTraceLine(line) ?: return
        WindowManagerAppearanceEventParser.focusedWindowFromMessage(parsed.message)?.let { nextWindow ->
            if (nextWindow != focusedWindow) {
                focusedWindow = nextWindow
                Log.d(TAG, "AUTO_APPEARANCE focus=$nextWindow")
                synchronized(lock) {
                    if (pendingWindow != null && pendingWindow != nextWindow) cancelPendingLocked()
                }
                scheduleSnapshot(nextWindow, reason = "focus_changed")
            }
        }

        val prior = synchronized(context) {
            val copy = context.toList()
            context.addLast(parsed)
            while (context.size > MAX_CONTEXT_LINES) context.removeFirst()
            copy
        }

        val event = WindowManagerAppearanceEventParser.appearanceFromTrace(
            current = parsed,
            prior = prior,
            focusedWindow = focusedWindow,
        ) ?: return
        Log.d(
            TAG,
            "AUTO_APPEARANCE event focusedWindow=${event.sourceWindow} " +
                "lightStatusBars=${event.lightStatusBars}",
        )
        schedule(event.lightStatusBars, event.sourceWindow)
    }

    private fun scheduleSnapshot(window: String?, reason: String) {
        settle.schedule({
            val light = runCatching { WindowManagerAppearanceEventParser.lightStatusBarsFromDumpsysWindow(dumpWindow()) }
                .onFailure { error ->
                    Log.d(
                        TAG,
                        "AUTO_APPEARANCE snapshot source=dumpsys_window success=false " +
                            "reason=${error.javaClass.simpleName}:${error.message}",
                    )
                }
                .getOrNull()
            Log.d(
                TAG,
                "AUTO_APPEARANCE snapshot source=dumpsys_window reason=$reason " +
                    "focusedWindow=$window lightStatusBars=$light",
            )
            if (light != null) emitIfChanged(light)
        }, SNAPSHOT_SETTLE_MS, TimeUnit.MILLISECONDS)
    }

    private fun schedule(lightStatusBars: Boolean, window: String) = synchronized(lock) {
        pending?.cancel(false)
        generation++
        val currentGeneration = generation
        pendingLightStatusBars = lightStatusBars
        pendingWindow = window
        pending = settle.schedule({ commit(currentGeneration) }, EVENT_DEBOUNCE_MS, TimeUnit.MILLISECONDS)
    }

    private fun commit(expectedGeneration: Long) {
        val lightStatusBars: Boolean
        val window: String
        synchronized(lock) {
            if (expectedGeneration != generation) return
            lightStatusBars = pendingLightStatusBars ?: return
            window = pendingWindow ?: return
            pending = null
            pendingLightStatusBars = null
            pendingWindow = null
        }
        if (focusedWindow != window) return
        emitIfChanged(lightStatusBars)
    }

    private fun emitIfChanged(lightStatusBars: Boolean) {
        if (lastLightStatusBars == lightStatusBars) return
        lastLightStatusBars = lightStatusBars
        Log.d(TAG, "AUTO_APPEARANCE emit lightStatusBars=$lightStatusBars")
        runCatching { callback?.onAppearanceChanged(lightStatusBars) }
            .onFailure { error ->
                Log.d(TAG, "AUTO_APPEARANCE emit_failed reason=${error.javaClass.simpleName}:${error.message}")
            }
    }

    private fun cancelPendingLocked() {
        generation++
        pending?.cancel(false)
        pending = null
        pendingLightStatusBars = null
        pendingWindow = null
    }

    override fun stopMonitor() {
        Log.d(TAG, "AUTO_APPEARANCE monitor=stop")
        running.set(false)
        synchronized(lock) { cancelPendingLocked() }
        runCatching { process?.destroy() }
        process = null
        callback = null
    }

    override fun destroy() {
        stopMonitor()
        settle.shutdownNow()
        exitProcess(0)
    }

    private fun runCommand(vararg command: String): String {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        check(exitCode == 0) { "${command.joinToString(" ")} exited with code $exitCode" }
        return output
    }

    private companion object {
        const val TAG = "StatusBarAuto"
        const val MAX_CONTEXT_LINES = 40
        const val EVENT_DEBOUNCE_MS = 120L
        const val SNAPSHOT_SETTLE_MS = 120L
    }
}
