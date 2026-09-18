package com.ekoehler.expressivecutout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

enum class CustomStatusBarAppearancePreference {
    AUTO,
    LIGHT,
    DARK,
}

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

    val hideNotificationIcons: Flow<Boolean> = context.statusBarDataStore.data.map { prefs ->
        prefs[HIDE_NOTIFICATION_ICONS] ?: false
    }

    val hideSystemInfo: Flow<Boolean> = context.statusBarDataStore.data.map { prefs ->
        prefs[HIDE_SYSTEM_INFO] ?: false
    }

    val hideClock: Flow<Boolean> = context.statusBarDataStore.data.map { prefs ->
        prefs[HIDE_CLOCK] ?: false
    }

    val silenceAlerts: Flow<Boolean> = context.statusBarDataStore.data.map { prefs ->
        prefs[SILENCE_ALERTS] ?: false
    }

    val customStatusBarSettings: Flow<CustomStatusBarSettings> =
        context.statusBarDataStore.data.map { prefs ->
            CustomStatusBarSettings(
                enabled = prefs[CUSTOM_STATUS_BAR_ENABLED] ?: false,
                style = CustomStatusBarStyle.fromPersisted(prefs[CUSTOM_STATUS_BAR_STYLE]),
                appearance = prefs[CUSTOM_STATUS_BAR_APPEARANCE]
                    ?.let { runCatching { CustomStatusBarAppearancePreference.valueOf(it) }.getOrNull() }
                    ?: CustomStatusBarAppearancePreference.AUTO,
                masterScale = prefs[CUSTOM_STATUS_BAR_MASTER_SCALE]
                    ?: CustomStatusBarSettings.DEFAULT_MASTER_SCALE,
                clockScale = prefs[CUSTOM_STATUS_BAR_CLOCK_SCALE]
                    ?: CustomStatusBarSettings.DEFAULT_COMPONENT_SCALE,
                clockOffsetXDp = prefs[CUSTOM_STATUS_BAR_CLOCK_OFFSET_X] ?: 0f,
                clockOffsetYDp = prefs[CUSTOM_STATUS_BAR_CLOCK_OFFSET_Y] ?: 0f,
                systemIconsScale = prefs[CUSTOM_STATUS_BAR_SYSTEM_SCALE]
                    ?: CustomStatusBarSettings.DEFAULT_COMPONENT_SCALE,
                systemIconsSpacingDp = prefs[CUSTOM_STATUS_BAR_SYSTEM_SPACING]
                    ?: CustomStatusBarSettings.DEFAULT_SPACING_DP,
                systemIconsOffsetXDp = prefs[CUSTOM_STATUS_BAR_SYSTEM_OFFSET_X] ?: 0f,
                systemIconsOffsetYDp = prefs[CUSTOM_STATUS_BAR_SYSTEM_OFFSET_Y] ?: 0f,
                batteryScale = prefs[CUSTOM_STATUS_BAR_BATTERY_SCALE]
                    ?: CustomStatusBarSettings.DEFAULT_COMPONENT_SCALE,
                statusBarOffsetYDp = prefs[CUSTOM_STATUS_BAR_OFFSET_Y] ?: 0f,
                batteryPercentageMode =
                    BatteryPercentageMode.fromPersisted(prefs[CUSTOM_STATUS_BAR_BATTERY_PERCENTAGE]),
            ).sanitized()
        }

    val customStatusBarEnabled: Flow<Boolean> =
        customStatusBarSettings.map { it.enabled }

    val customStatusBarAppearance: Flow<CustomStatusBarAppearancePreference> =
        customStatusBarSettings.map { it.appearance }

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

    suspend fun setCustomStatusBarEnabled(enabled: Boolean) = context.statusBarDataStore.edit { prefs ->
        prefs[CUSTOM_STATUS_BAR_ENABLED] = enabled
    }

    suspend fun setCustomStatusBarAppearance(appearance: CustomStatusBarAppearancePreference) =
        context.statusBarDataStore.edit { prefs ->
            prefs[CUSTOM_STATUS_BAR_APPEARANCE] = appearance.name
        }

    suspend fun setCustomStatusBarSettings(settings: CustomStatusBarSettings) {
        val safe = settings.sanitized()
        context.statusBarDataStore.edit { prefs ->
            prefs[CUSTOM_STATUS_BAR_ENABLED] = safe.enabled
            prefs[CUSTOM_STATUS_BAR_STYLE] = safe.style.name
            prefs[CUSTOM_STATUS_BAR_APPEARANCE] = safe.appearance.name
            prefs[CUSTOM_STATUS_BAR_MASTER_SCALE] = safe.masterScale
            prefs[CUSTOM_STATUS_BAR_CLOCK_SCALE] = safe.clockScale
            prefs[CUSTOM_STATUS_BAR_CLOCK_OFFSET_X] = safe.clockOffsetXDp
            prefs[CUSTOM_STATUS_BAR_CLOCK_OFFSET_Y] = safe.clockOffsetYDp
            prefs[CUSTOM_STATUS_BAR_SYSTEM_SCALE] = safe.systemIconsScale
            prefs[CUSTOM_STATUS_BAR_SYSTEM_SPACING] = safe.systemIconsSpacingDp
            prefs[CUSTOM_STATUS_BAR_SYSTEM_OFFSET_X] = safe.systemIconsOffsetXDp
            prefs[CUSTOM_STATUS_BAR_SYSTEM_OFFSET_Y] = safe.systemIconsOffsetYDp
            prefs[CUSTOM_STATUS_BAR_BATTERY_SCALE] = safe.batteryScale
            prefs[CUSTOM_STATUS_BAR_OFFSET_Y] = safe.statusBarOffsetYDp
            prefs[CUSTOM_STATUS_BAR_BATTERY_PERCENTAGE] = safe.batteryPercentageMode.name
        }
    }

    private companion object {
        val HIDE_NOTIFICATION_ICONS = booleanPreferencesKey("hide_notification_icons")
        val HIDE_SYSTEM_INFO = booleanPreferencesKey("hide_system_info")
        val HIDE_CLOCK = booleanPreferencesKey("hide_clock")
        val SILENCE_ALERTS = booleanPreferencesKey("silence_alerts")
        val CUSTOM_STATUS_BAR_ENABLED = booleanPreferencesKey("custom_status_bar_enabled")
        val CUSTOM_STATUS_BAR_STYLE = stringPreferencesKey("custom_status_bar_style")
        val CUSTOM_STATUS_BAR_APPEARANCE = stringPreferencesKey("custom_status_bar_appearance")
        val CUSTOM_STATUS_BAR_MASTER_SCALE = floatPreferencesKey("custom_status_bar_master_scale")
        val CUSTOM_STATUS_BAR_CLOCK_SCALE = floatPreferencesKey("custom_status_bar_clock_scale")
        val CUSTOM_STATUS_BAR_CLOCK_OFFSET_X = floatPreferencesKey("custom_status_bar_clock_offset_x")
        val CUSTOM_STATUS_BAR_CLOCK_OFFSET_Y = floatPreferencesKey("custom_status_bar_clock_offset_y")
        val CUSTOM_STATUS_BAR_SYSTEM_SCALE = floatPreferencesKey("custom_status_bar_system_scale")
        val CUSTOM_STATUS_BAR_SYSTEM_SPACING = floatPreferencesKey("custom_status_bar_system_spacing")
        val CUSTOM_STATUS_BAR_SYSTEM_OFFSET_X = floatPreferencesKey("custom_status_bar_system_offset_x")
        val CUSTOM_STATUS_BAR_SYSTEM_OFFSET_Y = floatPreferencesKey("custom_status_bar_system_offset_y")
        val CUSTOM_STATUS_BAR_BATTERY_SCALE = floatPreferencesKey("custom_status_bar_battery_scale")
        val CUSTOM_STATUS_BAR_OFFSET_Y = floatPreferencesKey("custom_status_bar_offset_y")
        val CUSTOM_STATUS_BAR_BATTERY_PERCENTAGE =
            stringPreferencesKey("custom_status_bar_battery_percentage")
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
        val customSettings = customStatusBarSettings.first()
        return JSONObject().apply {
            put("hideNotificationIcons", hideIcons)
            put("hideSystemInfo", hideSystemInfo)
            put("hideClock", hideClock)
            put("silenceAlerts", silence)
            put("customStatusBarEnabled", customSettings.enabled)
            put("customStatusBarStyle", customSettings.style.name)
            put("customStatusBarAppearance", customSettings.appearance.name)
            put("customStatusBarMasterScale", customSettings.masterScale)
            put("customStatusBarClockScale", customSettings.clockScale)
            put("customStatusBarClockOffsetX", customSettings.clockOffsetXDp)
            put("customStatusBarClockOffsetY", customSettings.clockOffsetYDp)
            put("customStatusBarSystemScale", customSettings.systemIconsScale)
            put("customStatusBarSystemSpacing", customSettings.systemIconsSpacingDp)
            put("customStatusBarSystemOffsetX", customSettings.systemIconsOffsetXDp)
            put("customStatusBarSystemOffsetY", customSettings.systemIconsOffsetYDp)
            put("customStatusBarBatteryScale", customSettings.batteryScale)
            put("customStatusBarOffsetY", customSettings.statusBarOffsetYDp)
            put("customStatusBarBatteryPercentage", customSettings.batteryPercentageMode.name)
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
            setHideNotificationIcons(obj.optBoolean("hideNotificationIcons", false))
        }
        if (obj.has("hideSystemInfo")) {
            setHideSystemInfo(obj.optBoolean("hideSystemInfo", false))
        }
        if (obj.has("hideClock")) {
            setHideClock(obj.optBoolean("hideClock", false))
        }
        if (obj.has("silenceAlerts")) {
            setSilenceAlerts(obj.optBoolean("silenceAlerts", false))
        }
        if (
            obj.has("customStatusBarEnabled") ||
            obj.has("customStatusBarStyle") ||
            obj.has("customStatusBarAppearance") ||
            obj.has("customStatusBarMasterScale") ||
            obj.has("customStatusBarClockScale") ||
            obj.has("customStatusBarClockOffsetX") ||
            obj.has("customStatusBarClockOffsetY") ||
            obj.has("customStatusBarSystemScale") ||
            obj.has("customStatusBarSystemSpacing") ||
            obj.has("customStatusBarSystemOffsetX") ||
            obj.has("customStatusBarSystemOffsetY") ||
            obj.has("customStatusBarBatteryScale") ||
            obj.has("customStatusBarOffsetY") ||
            obj.has("customStatusBarBatteryPercentage")
        ) {
            var settings = customStatusBarSettings.first()
            if (obj.has("customStatusBarEnabled")) {
                settings = settings.copy(enabled = obj.optBoolean("customStatusBarEnabled", settings.enabled))
            }
            if (obj.has("customStatusBarStyle")) {
                settings = settings.copy(
                    style = CustomStatusBarStyle.fromPersisted(obj.optString("customStatusBarStyle")),
                )
            }
            if (obj.has("customStatusBarAppearance")) {
                val appearance = runCatching {
                    CustomStatusBarAppearancePreference.valueOf(obj.getString("customStatusBarAppearance"))
                }.getOrNull()
                if (appearance != null) settings = settings.copy(appearance = appearance)
            }
            if (obj.has("customStatusBarMasterScale")) {
                settings = settings.copy(masterScale = obj.optDouble("customStatusBarMasterScale").toFloat())
            }
            if (obj.has("customStatusBarClockScale")) {
                settings = settings.copy(clockScale = obj.optDouble("customStatusBarClockScale").toFloat())
            }
            if (obj.has("customStatusBarClockOffsetX")) {
                settings = settings.copy(clockOffsetXDp = obj.optDouble("customStatusBarClockOffsetX").toFloat())
            }
            if (obj.has("customStatusBarClockOffsetY")) {
                settings = settings.copy(clockOffsetYDp = obj.optDouble("customStatusBarClockOffsetY").toFloat())
            }
            if (obj.has("customStatusBarSystemScale")) {
                settings = settings.copy(systemIconsScale = obj.optDouble("customStatusBarSystemScale").toFloat())
            }
            if (obj.has("customStatusBarSystemSpacing")) {
                settings = settings.copy(systemIconsSpacingDp = obj.optDouble("customStatusBarSystemSpacing").toFloat())
            }
            if (obj.has("customStatusBarSystemOffsetX")) {
                settings = settings.copy(systemIconsOffsetXDp = obj.optDouble("customStatusBarSystemOffsetX").toFloat())
            }
            if (obj.has("customStatusBarSystemOffsetY")) {
                settings = settings.copy(systemIconsOffsetYDp = obj.optDouble("customStatusBarSystemOffsetY").toFloat())
            }
            if (obj.has("customStatusBarBatteryScale")) {
                settings = settings.copy(batteryScale = obj.optDouble("customStatusBarBatteryScale").toFloat())
            }
            if (obj.has("customStatusBarOffsetY")) {
                settings = settings.copy(statusBarOffsetYDp = obj.optDouble("customStatusBarOffsetY").toFloat())
            }
            if (obj.has("customStatusBarBatteryPercentage")) {
                settings = settings.copy(
                    batteryPercentageMode =
                        BatteryPercentageMode.fromPersisted(obj.optString("customStatusBarBatteryPercentage")),
                )
            }
            setCustomStatusBarSettings(settings)
        }
    }
}
