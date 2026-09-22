package com.ekoehler.expressivecutout.data

enum class CustomStatusBarStyle {
    DEFAULT,
    IOS_26,
    IOS_27,
    PIXEL_15,
    PIXEL_16_17,
    HYPER_OS,
    NOTHING_OS_5;

    companion object {
        fun fromPersisted(raw: String?): CustomStatusBarStyle {
            val normalized = raw?.trim()?.uppercase()
            val legacy = when (normalized) {
                null,
                "" -> IOS_27
                "IOS_26" -> IOS_27
                "DEFAULT",
                "ONE_UI",
                "ONE_UI_EXISTING",
                "PIXEL",
                "PIXEL_15",
                "PIXEL_16",
                "PIXEL_17",
                "PIXEL_16_17",
                "HYPER_OS",
                "NOTHING_OS",
                "NOTHING_OS_5",
                -> PIXEL_16_17
                else -> entries.firstOrNull { it.name == normalized } ?: IOS_27
            }
            return CustomStatusBarStyleUiPolicy.migrateLegacy(legacy)
        }
    }
}

/** Product-facing style policy for the simplified M2C style pack. */
object CustomStatusBarStyleUiPolicy {
    val visibleStyles: List<CustomStatusBarStyle> = listOf(
        CustomStatusBarStyle.IOS_27,
        CustomStatusBarStyle.PIXEL_16_17,
    )

    fun migrateLegacy(style: CustomStatusBarStyle): CustomStatusBarStyle = when (style) {
        CustomStatusBarStyle.IOS_26,
        CustomStatusBarStyle.IOS_27,
        -> CustomStatusBarStyle.IOS_27

        CustomStatusBarStyle.DEFAULT,
        CustomStatusBarStyle.PIXEL_15,
        CustomStatusBarStyle.PIXEL_16_17,
        CustomStatusBarStyle.HYPER_OS,
        CustomStatusBarStyle.NOTHING_OS_5,
        -> CustomStatusBarStyle.PIXEL_16_17
    }

    fun displayName(style: CustomStatusBarStyle): String = when (migrateLegacy(style)) {
        CustomStatusBarStyle.IOS_27 -> "iOS 27"
        CustomStatusBarStyle.PIXEL_16_17 -> "Pixel 16"
        else -> "iOS 27"
    }

    fun description(style: CustomStatusBarStyle): String = when (migrateLegacy(style)) {
        CustomStatusBarStyle.IOS_27 -> "Rounded, polished and closest to the current approved look."
        CustomStatusBarStyle.PIXEL_16_17 -> "Modern Android spacing with Pixel-style signal, Wi-Fi and battery."
        else -> "Rounded, polished and closest to the current approved look."
    }

    fun showsIosBatteryColor(style: CustomStatusBarStyle): Boolean =
        migrateLegacy(style) == CustomStatusBarStyle.IOS_27
}

enum class PixelMobileBarStyle {
    CLASSIC,
    COMPACT,
    TALL;

    companion object {
        fun fromPersisted(raw: String?): PixelMobileBarStyle =
            entries.firstOrNull { it.name == raw } ?: CLASSIC
    }
}

enum class BatteryPercentageMode {
    OFF,
    INSIDE,
    OUTSIDE;

    companion object {
        fun fromPersisted(raw: String?): BatteryPercentageMode =
            entries.firstOrNull { it.name == raw } ?: OFF
    }
}

enum class StatusBarNotificationDisplayMode {
    DOT,
    ICONS,
    HIDDEN;

    companion object {
        fun fromPersisted(raw: String?): StatusBarNotificationDisplayMode =
            entries.firstOrNull { it.name == raw?.trim()?.uppercase() } ?: DOT
    }
}

enum class IosBatteryColorMode {
    MONOCHROME,
    STATUS_COLOR;

    companion object {
        fun fromPersisted(raw: String?): IosBatteryColorMode =
            entries.firstOrNull { it.name == raw } ?: MONOCHROME
    }
}

data class CustomStatusBarSettings(
    val enabled: Boolean = false,
    val style: CustomStatusBarStyle = CustomStatusBarStyle.IOS_27,
    val appearance: CustomStatusBarAppearancePreference = CustomStatusBarAppearancePreference.AUTO,
    val masterScale: Float = DEFAULT_MASTER_SCALE,
    val clockScale: Float = DEFAULT_COMPONENT_SCALE,
    val clockOffsetXDp: Float = 0f,
    val clockOffsetYDp: Float = 0f,
    val systemIconsScale: Float = DEFAULT_COMPONENT_SCALE,
    val wifiScale: Float = DEFAULT_COMPONENT_SCALE,
    val systemIconsSpacingDp: Float = DEFAULT_SPACING_DP,
    val systemIconsOffsetXDp: Float = 0f,
    val systemIconsOffsetYDp: Float = 0f,
    val mobileBarStyle: PixelMobileBarStyle = PixelMobileBarStyle.CLASSIC,
    val batteryScale: Float = DEFAULT_COMPONENT_SCALE,
    val statusBarOffsetYDp: Float = 0f,
    val batteryPercentageMode: BatteryPercentageMode = BatteryPercentageMode.OFF,
    val iosBatteryColorMode: IosBatteryColorMode = IosBatteryColorMode.MONOCHROME,
    val notificationDisplayMode: StatusBarNotificationDisplayMode = StatusBarNotificationDisplayMode.DOT,
) {
    fun sanitized(): CustomStatusBarSettings = copy(
        style = CustomStatusBarStyleUiPolicy.migrateLegacy(style),
        masterScale = masterScale.coerceIn(MIN_MASTER_SCALE, MAX_MASTER_SCALE),
        clockScale = clockScale.coerceIn(MIN_COMPONENT_SCALE, MAX_COMPONENT_SCALE),
        clockOffsetXDp = clockOffsetXDp.coerceIn(-MAX_OFFSET_DP, MAX_OFFSET_DP),
        clockOffsetYDp = clockOffsetYDp.coerceIn(-MAX_OFFSET_DP, MAX_OFFSET_DP),
        systemIconsScale = systemIconsScale.coerceIn(MIN_COMPONENT_SCALE, MAX_COMPONENT_SCALE),
        wifiScale = wifiScale.coerceIn(MIN_COMPONENT_SCALE, MAX_COMPONENT_SCALE),
        systemIconsSpacingDp = systemIconsSpacingDp.coerceIn(MIN_SPACING_DP, MAX_SPACING_DP),
        systemIconsOffsetXDp = systemIconsOffsetXDp.coerceIn(-MAX_OFFSET_DP, MAX_OFFSET_DP),
        systemIconsOffsetYDp = systemIconsOffsetYDp.coerceIn(-MAX_OFFSET_DP, MAX_OFFSET_DP),
        batteryScale = batteryScale.coerceIn(MIN_COMPONENT_SCALE, MAX_COMPONENT_SCALE),
        statusBarOffsetYDp = statusBarOffsetYDp.coerceIn(-MAX_GLOBAL_Y_DP, MAX_GLOBAL_Y_DP),
    )

    /** Restores the default simplified visual profile without unexpectedly disabling the live feature. */
    fun withPixelDefaults(): CustomStatusBarSettings = DEFAULT.copy(
        enabled = enabled,
        appearance = appearance,
        notificationDisplayMode = notificationDisplayMode,
    )

    companion object {
        const val MIN_MASTER_SCALE = 0.75f
        const val MAX_MASTER_SCALE = 1.35f
        const val MIN_COMPONENT_SCALE = 0.75f
        const val MAX_COMPONENT_SCALE = 1.40f
        const val MIN_SPACING_DP = 0f
        const val MAX_SPACING_DP = 12f
        const val MAX_OFFSET_DP = 48f
        const val MAX_GLOBAL_Y_DP = 12f
        const val DEFAULT_MASTER_SCALE = 1f
        const val DEFAULT_COMPONENT_SCALE = 1f
        const val DEFAULT_SPACING_DP = 4f

        val DEFAULT = CustomStatusBarSettings()
    }
}

internal data class EffectivePixelStatusBarScale(
    val clock: Float,
    val systemIcons: Float,
    val wifi: Float,
    val battery: Float,
)

internal object PixelStatusBarScale {
    fun resolve(settings: CustomStatusBarSettings): EffectivePixelStatusBarScale {
        val safe = settings.sanitized()
        return EffectivePixelStatusBarScale(
            clock = safe.masterScale * safe.clockScale,
            systemIcons = safe.masterScale * safe.systemIconsScale,
            wifi = safe.masterScale * safe.systemIconsScale * safe.wifiScale,
            battery = safe.masterScale * safe.batteryScale,
        )
    }
}
