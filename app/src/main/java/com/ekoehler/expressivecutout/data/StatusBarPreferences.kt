package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/** Backing store for the status-bar hiding settings. */
private val Context.statusBarDataStore: DataStore<Preferences> by preferencesDataStore(name = "status_bar_prefs")

/**
 * What the user wants done to the system status bar, applied through Shizuku by
 * `StatusBarIconController`.
 *
 * This stores the *intent*, not the achieved state: the flags themselves live in system_server and
 * are lost whenever our process dies or the device reboots. Keeping the wish here is what lets the
 * controller re-apply it once Shizuku is reachable again.
 */
class StatusBarPreferences(private val context: Context) : JsonSerializable {

    /**
     * Notification icons default to hidden so the island is the primary visual notification surface.
     * A stored user choice still wins over this default.
     */
    val hideNotificationIcons: Flow<Boolean> = context.statusBarDataStore.data.map { prefs ->
        prefs[HIDE_NOTIFICATION_ICONS] ?: DEFAULT_HIDE_NOTIFICATION_ICONS
    }

    /** Whether system information icons should be hidden. */
    val hideSystemInfo: Flow<Boolean> = context.statusBarDataStore.data.map { prefs ->
        prefs[HIDE_SYSTEM_INFO] ?: false
    }

    /** Whether the status-bar clock should be hidden. */
    val hideClock: Flow<Boolean> = context.statusBarDataStore.data.map { prefs ->
        prefs[HIDE_CLOCK] ?: false
    }

    /**
     * System heads-up alerts default to silenced while Shizuku is available so a notification shown
     * by the island is not duplicated by Samsung's own pop-up. A stored user choice still wins.
     */
    val silenceAlerts: Flow<Boolean> = context.statusBarDataStore.data.map { prefs ->
        prefs[SILENCE_ALERTS] ?: DEFAULT_SILENCE_ALERTS
    }

    suspend fun setHideNotificationIcons(hide: Boolean) = context.statusBarDataStore.edit { prefs ->
        prefs[HIDE_NOTIFICATION_ICONS] = hide
    }

    suspend fun setHideSystemInfo(hide: Boolean) = context.statusBarDataStore.edit { prefs ->
        prefs[HIDE_SYSTEM_INFO] = hide
    }

    suspend fun setHideClock(hide: Boolean) = context.statusBarDataStore.edit { prefs ->
        prefs[HIDE_CLOCK] = hide
    }

    suspend fun setSilenceAlerts(silence: Boolean) = context.statusBarDataStore.edit { prefs ->
        prefs[SILENCE_ALERTS] = silence
    }

    private companion object {
        val HIDE_NOTIFICATION_ICONS = booleanPreferencesKey("hide_notification_icons")
        val HIDE_SYSTEM_INFO = booleanPreferencesKey("hide_system_info")
        val HIDE_CLOCK = booleanPreferencesKey("hide_clock")
        val SILENCE_ALERTS = booleanPreferencesKey("silence_alerts")
        const val DEFAULT_HIDE_NOTIFICATION_ICONS = true
        const val DEFAULT_SILENCE_ALERTS = true
    }

    /**
     * Exports the status-bar settings in a JSON string
     * { hideNotificationIcons: boolean, hideSystemInfo: boolean, hideClock: boolean,
     *   silenceAlerts: boolean }
     */
    override suspend fun toJson(): String {
        val hideIcons = hideNotificationIcons.first()
        val hideSystemInfo = hideSystemInfo.first()
        val hideClock = hideClock.first()
        val silence = silenceAlerts.first()
        return JSONObject().apply {
            put("hideNotificationIcons", hideIcons)
            put("hideSystemInfo", hideSystemInfo)
            put("hideClock", hideClock)
            put("silenceAlerts", silence)
        }.toString()
    }

    /**
     * Applies { hideNotificationIcons: boolean, hideSystemInfo: boolean, hideClock: boolean,
     * silenceAlerts: boolean } exported by [toJson]. Each missing field leaves its setting
     * untouched — importing a document from a build without this section shouldn't silently flip
     * any flag.
     */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        if (obj.has("hideNotificationIcons")) {
            setHideNotificationIcons(obj.optBoolean("hideNotificationIcons", DEFAULT_HIDE_NOTIFICATION_ICONS))
        }
        if (obj.has("hideSystemInfo")) {
            setHideSystemInfo(obj.optBoolean("hideSystemInfo", false))
        }
        if (obj.has("hideClock")) {
            setHideClock(obj.optBoolean("hideClock", false))
        }
        if (obj.has("silenceAlerts")) {
            setSilenceAlerts(obj.optBoolean("silenceAlerts", DEFAULT_SILENCE_ALERTS))
        }
    }
}
