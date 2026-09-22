package com.ekoehler.expressivecutout.statusbar

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

internal interface WindowPolicyDumpTransport {
    suspend fun dumpWindowPolicy(): String?
    suspend fun dumpWindow(): String? = dumpWindowPolicy()
    fun changes(): Flow<SystemBarAppearanceSnapshot> = emptyFlow()
    fun close() = Unit
}

internal data class WindowPolicySnapshotRead(
    val raw: String?,
    val snapshot: SystemBarAppearanceSnapshot?,
)

internal class WindowPolicySnapshotReader(
    private val transport: WindowPolicyDumpTransport,
) {
    suspend fun read(): WindowPolicySnapshotRead {
        val raw = transport.dumpWindow() ?: transport.dumpWindowPolicy()
        return WindowPolicySnapshotRead(
            raw = raw,
            snapshot = raw?.let(WindowPolicyAppearanceParser::parse),
        )
    }

    suspend fun snapshot(): SystemBarAppearanceSnapshot? = read().snapshot
}

/**
 * Binds a tiny non-daemon Shizuku UserService so the dump/logcat commands run as shell/root rather
 * than through the app process. It also exposes a callback flow for live WindowManager appearance
 * events so Auto tint is not limited to one-shot foreground reconciliation.
 */
internal class ShizukuUserServiceWindowPolicyDumpTransport(
    context: Context,
) : WindowPolicyDumpTransport {

    private val appContext = context.applicationContext
    private val remote = AtomicReference<IStatusBarAppearanceUserService?>(null)
    private val lock = Any()

    @Volatile
    private var binding = false

    @Volatile
    private var bindingWanted = false

    @Volatile
    private var pending: CompletableDeferred<IStatusBarAppearanceUserService?>? = null

    private val serviceArgs = Shizuku.UserServiceArgs(
        ComponentName(appContext, StatusBarAppearanceUserService::class.java),
    )
        .processNameSuffix("statusbar_auto")
        .tag("statusbar-auto")
        .version(USER_SERVICE_VERSION)
        .daemon(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = binder?.let(IStatusBarAppearanceUserService.Stub::asInterface)
            if (!bindingWanted) {
                remote.set(null)
                val waiter = synchronized(lock) {
                    binding = false
                    pending.also { pending = null }
                }
                waiter?.complete(null)
                runCatching { Shizuku.unbindUserService(serviceArgs, this, true) }
                return
            }
            remote.set(service)
            val waiter = synchronized(lock) {
                binding = false
                pending.also { pending = null }
            }
            waiter?.complete(service)
            Log.d(TAG, "AUTO_APPEARANCE transport=user_service connected=${service != null}")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            remote.set(null)
            val waiter = synchronized(lock) {
                binding = false
                pending.also { pending = null }
            }
            waiter?.complete(null)
            Log.d(TAG, "AUTO_APPEARANCE transport=user_service disconnected=true")
        }
    }

    override suspend fun dumpWindowPolicy(): String? {
        val service = awaitService() ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { service.dumpWindowPolicy() }
                .onFailure { error ->
                    remote.compareAndSet(service, null)
                    Log.d(
                        TAG,
                        "AUTO_APPEARANCE transport=user_service policyDumpFailure=" +
                            "${error.javaClass.simpleName}:${error.message}",
                    )
                }
                .getOrNull()
        }
    }

    override suspend fun dumpWindow(): String? {
        val service = awaitService() ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { service.dumpWindow() }
                .onFailure { error ->
                    remote.compareAndSet(service, null)
                    Log.d(
                        TAG,
                        "AUTO_APPEARANCE transport=user_service windowDumpFailure=" +
                            "${error.javaClass.simpleName}:${error.message}",
                    )
                }
                .getOrNull()
        }
    }

    override fun changes(): Flow<SystemBarAppearanceSnapshot> = callbackFlow {
        val service = awaitService()
        if (service == null) {
            Log.d(TAG, "AUTO_APPEARANCE monitor=unavailable reason=bind_failed")
            close()
            return@callbackFlow
        }

        val callback = object : IStatusBarAppearanceCallback.Stub() {
            override fun onAppearanceChanged(lightStatusBars: Boolean) {
                val appearance = if (lightStatusBars) {
                    StatusBarAppearanceBits.LIGHT_STATUS_BARS
                } else {
                    0
                }
                Log.d(
                    TAG,
                    "AUTO_APPEARANCE callback lightStatusBars=$lightStatusBars " +
                        "appearance=$appearance",
                )
                trySend(SystemBarAppearanceSnapshot(globalAppearance = appearance))
            }
        }

        runCatching { service.startMonitor(callback) }
            .onFailure { error ->
                Log.d(
                    TAG,
                    "AUTO_APPEARANCE monitor=start_failed reason=" +
                        "${error.javaClass.simpleName}:${error.message}",
                )
                close(error)
            }
        awaitClose {
            runCatching { service.stopMonitor() }
                .onFailure { error ->
                    Log.d(
                        TAG,
                        "AUTO_APPEARANCE monitor=stop_failed reason=" +
                            "${error.javaClass.simpleName}:${error.message}",
                    )
                }
        }
    }

    override fun close() {
        bindingWanted = false
        val waiter = synchronized(lock) {
            binding = false
            pending.also { pending = null }
        }
        waiter?.complete(null)
        remote.set(null)
        runCatching {
            Shizuku.unbindUserService(serviceArgs, connection, true)
        }.onFailure { error ->
            Log.d(
                TAG,
                "AUTO_APPEARANCE transport=user_service unbindFailure=" +
                    "${error.javaClass.simpleName}:${error.message}",
            )
        }
    }

    private suspend fun awaitService(): IStatusBarAppearanceUserService? {
        bindingWanted = true
        remote.get()?.takeIf { it.asBinder().isBinderAlive }?.let { return it }

        val (waiter, shouldBind) = synchronized(lock) {
            remote.get()?.takeIf { it.asBinder().isBinderAlive }?.let {
                return@synchronized CompletableDeferred<IStatusBarAppearanceUserService?>().apply {
                    complete(it)
                } to false
            }

            val existing = pending
            if (existing != null) {
                existing to false
            } else {
                val created = CompletableDeferred<IStatusBarAppearanceUserService?>()
                pending = created
                val bindNow = !binding
                binding = true
                created to bindNow
            }
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
                    "AUTO_APPEARANCE transport=user_service bindFailure=" +
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
            Log.d(TAG, "AUTO_APPEARANCE transport=user_service bindTimeout=true")
        }
        return result
    }

    private companion object {
        const val TAG = "StatusBarAuto"
        const val USER_SERVICE_VERSION = 3
        const val BIND_TIMEOUT_MS = 3_000L
    }
}
