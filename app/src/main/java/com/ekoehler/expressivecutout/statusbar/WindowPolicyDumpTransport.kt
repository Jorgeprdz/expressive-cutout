package com.ekoehler.expressivecutout.statusbar

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

internal interface WindowPolicyDumpTransport {
    suspend fun dumpWindowPolicy(): String?
}

internal data class WindowPolicySnapshotRead(
    val raw: String?,
    val snapshot: SystemBarAppearanceSnapshot?,
)

internal class WindowPolicySnapshotReader(
    private val transport: WindowPolicyDumpTransport,
) {
    suspend fun read(): WindowPolicySnapshotRead {
        val raw = transport.dumpWindowPolicy()
        return WindowPolicySnapshotRead(
            raw = raw,
            snapshot = raw?.let(WindowPolicyAppearanceParser::parse),
        )
    }

    suspend fun snapshot(): SystemBarAppearanceSnapshot? = read().snapshot
}

/**
 * Binds a tiny non-daemon Shizuku UserService so the dump command runs as shell/root rather than
 * through the app process. ShizukuBinderWrapper only proxies transact(); its dump() delegates to
 * the original binder and therefore is not suitable for this privileged dumpsys path.
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
                        "AUTO_APPEARANCE transport=user_service dumpFailure=" +
                            "${error.javaClass.simpleName}:${error.message}",
                    )
                }
                .getOrNull()
        }
    }

    private suspend fun awaitService(): IStatusBarAppearanceUserService? {
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
        const val USER_SERVICE_VERSION = 2
        const val BIND_TIMEOUT_MS = 3_000L
    }
}
