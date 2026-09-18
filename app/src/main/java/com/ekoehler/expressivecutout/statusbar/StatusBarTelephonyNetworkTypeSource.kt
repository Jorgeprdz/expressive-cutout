package com.ekoehler.expressivecutout.statusbar

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.ekoehler.expressivecutout.system.ShizukuState
import com.ekoehler.expressivecutout.system.ShizukuStatus
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

internal object StatusBarTelephonyNetworkTypeParser {

    private data class PhoneBlock(
        val id: Int,
        var displayInfo: String? = null,
        var dataEnabled: Boolean = false,
    )

    fun parse(raw: String): StatusBarNetworkType? {
        if (raw.isBlank()) return null

        val blocks = mutableListOf<PhoneBlock>()
        var current: PhoneBlock? = null
        var defaultPhoneId: Int? = null

        raw.lineSequence().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Phone Id=", ignoreCase = true) -> {
                    val id = trimmed.substringAfter('=').trim().toIntOrNull() ?: return@forEach
                    current = PhoneBlock(id).also(blocks::add)
                }
                trimmed.startsWith("mTelephonyDisplayInfo=", ignoreCase = true) -> {
                    current?.displayInfo = trimmed.substringAfter('=').trim()
                }
                trimmed.startsWith("mIsDataEnabled=", ignoreCase = true) -> {
                    current?.dataEnabled =
                        trimmed.substringAfter('=').trim().equals("true", ignoreCase = true)
                }
                trimmed.startsWith("mDefaultPhoneId=", ignoreCase = true) -> {
                    defaultPhoneId = trimmed.substringAfter('=').trim().toIntOrNull()
                }
            }
        }

        fun resolved(block: PhoneBlock): StatusBarNetworkType? =
            block.displayInfo?.let(::parseDisplayInfo)

        defaultPhoneId
            ?.let { id -> blocks.firstOrNull { it.id == id } }
            ?.let(::resolved)
            ?.let { return it }

        blocks.firstOrNull { it.dataEnabled && resolved(it) != null }
            ?.let(::resolved)
            ?.let { return it }

        return blocks.firstNotNullOfOrNull(::resolved)
    }

    private fun parseDisplayInfo(raw: String): StatusBarNetworkType? {
        if (raw.equals("null", ignoreCase = true)) return null

        val network = Regex("""(?i)\bnetwork=([^,}\s]+)""")
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.uppercase()
            ?: return null
        val override = Regex("""(?i)\boverride(?:Network)?=([^,}\s]+)""")
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.uppercase()
            .orEmpty()

        if ("NR" in override || network == "NR") return StatusBarNetworkType.FIVE_G
        if (override == "LTE_CA" || override == "LTE_ADV_PRO") {
            return StatusBarNetworkType.FOUR_G_PLUS
        }

        return when (network) {
            "LTE" -> StatusBarNetworkType.LTE
            "HSPAP", "HSPA", "HSDPA", "HSUPA", "UMTS", "TD_SCDMA" ->
                StatusBarNetworkType.FOUR_G
            else -> null
        }
    }
}

internal interface StatusBarTelephonyDumpTransport {
    suspend fun dumpTelephonyRegistry(): String?
}

internal class StatusBarTelephonyNetworkTypeSource(
    private val transport: StatusBarTelephonyDumpTransport,
) {
    suspend fun snapshot(): StatusBarNetworkType? =
        transport.dumpTelephonyRegistry()?.let(StatusBarTelephonyNetworkTypeParser::parse)
}

internal class ShizukuStatusBarTelephonyNetworkTypeSource(
    context: Context,
) {
    private val source = StatusBarTelephonyNetworkTypeSource(
        ShizukuUserServiceTelephonyDumpTransport(context),
    )

    suspend fun snapshot(): StatusBarNetworkType? {
        if (ShizukuState.status.value != ShizukuStatus.READY) return null
        return source.snapshot()
    }
}

internal class ShizukuUserServiceTelephonyDumpTransport(
    context: Context,
) : StatusBarTelephonyDumpTransport {

    private val appContext = context.applicationContext
    private val remote = AtomicReference<IStatusBarTelephonyUserService?>(null)
    private val lock = Any()

    @Volatile
    private var binding = false

    @Volatile
    private var pending: CompletableDeferred<IStatusBarTelephonyUserService?>? = null

    private val serviceArgs = Shizuku.UserServiceArgs(
        ComponentName(appContext, StatusBarTelephonyUserService::class.java),
    )
        .processNameSuffix("statusbar_telephony")
        .tag("statusbar-telephony")
        .version(USER_SERVICE_VERSION)
        .daemon(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = binder?.let(IStatusBarTelephonyUserService.Stub::asInterface)
            remote.set(service)
            val waiter = synchronized(lock) {
                binding = false
                pending.also { pending = null }
            }
            waiter?.complete(service)
            Log.d(TAG, "STATUS_BAR_SIGNAL telephonyServiceConnected=${service != null}")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            remote.set(null)
            val waiter = synchronized(lock) {
                binding = false
                pending.also { pending = null }
            }
            waiter?.complete(null)
            Log.d(TAG, "STATUS_BAR_SIGNAL telephonyServiceDisconnected=true")
        }
    }

    override suspend fun dumpTelephonyRegistry(): String? {
        val service = awaitService() ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { service.dumpTelephonyRegistry() }
                .onFailure { error ->
                    remote.compareAndSet(service, null)
                    Log.d(
                        TAG,
                        "STATUS_BAR_SIGNAL telephonyDumpFailure=" +
                            "${error.javaClass.simpleName}:${error.message}",
                    )
                }
                .getOrNull()
        }
    }

    private suspend fun awaitService(): IStatusBarTelephonyUserService? {
        remote.get()?.takeIf { it.asBinder().isBinderAlive }?.let { return it }

        val (waiter, shouldBind) = synchronized(lock) {
            remote.get()?.takeIf { it.asBinder().isBinderAlive }?.let {
                return@synchronized CompletableDeferred<IStatusBarTelephonyUserService?>().apply {
                    complete(it)
                } to false
            }

            pending?.let { return@synchronized it to false }

            val created = CompletableDeferred<IStatusBarTelephonyUserService?>()
            pending = created
            binding = true
            created to true
        }

        if (shouldBind) {
            val bindFailure = runCatching {
                withContext(Dispatchers.Main.immediate) {
                    Shizuku.bindUserService(serviceArgs, connection)
                }
            }.exceptionOrNull()

            if (bindFailure != null) {
                synchronized(lock) {
                    binding = false
                    if (pending === waiter) pending = null
                }
                waiter.complete(null)
                Log.d(
                    TAG,
                    "STATUS_BAR_SIGNAL telephonyBindFailure=" +
                        "${bindFailure.javaClass.simpleName}:${bindFailure.message}",
                )
            }
        }

        val result = withTimeoutOrNull(BIND_TIMEOUT_MS) { waiter.await() }
        if (result == null) {
            synchronized(lock) {
                binding = false
                if (pending === waiter) pending = null
            }
            Log.d(TAG, "STATUS_BAR_SIGNAL telephonyBindTimeout=true")
        }
        return result
    }

    private companion object {
        const val TAG = "StatusBarSignal"
        const val USER_SERVICE_VERSION = 1
        const val BIND_TIMEOUT_MS = 3_000L
    }
}
