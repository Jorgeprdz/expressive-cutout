package com.ekoehler.expressivecutout.statusbar

import com.ekoehler.expressivecutout.data.CustomStatusBarSettings
import com.ekoehler.expressivecutout.data.CustomStatusBarStyle
import com.ekoehler.expressivecutout.data.CustomStatusBarStyleUiPolicy
import com.ekoehler.expressivecutout.data.PixelMobileBarStyle

internal typealias StatusBarStyleId = CustomStatusBarStyle

internal enum class StatusBarStyleTextWeight {
    REGULAR,
    MEDIUM,
    SEMIBOLD,
}

internal enum class StatusBarSignalVisualMode {
    CLASSIC_BARS,
    IOS_ROUNDED_BARS,
    IOS_BOLD_PILLS,
    DOT_MATRIX,
    COMPACT_MINIMAL_BARS,
    PIXEL_COMPACT_BARS,
}

internal enum class StatusBarWifiVisualMode {
    CLASSIC_ARCS,
    IOS_ARCS,
    IOS_BOLD_ARCS,
    PIXEL_COMPACT_ARCS,
    HYPER_COMPACT_ARCS,
    NOTHING_MINIMAL_ARCS,
}

internal enum class StatusBarBatteryVisualMode {
    CLASSIC_ANDROID,
    IOS_OUTLINE_FILL,
    IOS_SOLID_CAPSULE,
    NUMERIC_CAPSULE_PROMINENT,
    NUMERIC_CAPSULE_COMPACT,
    SOLID_CAPSULE_MINIMAL,
    COMPACT_SOLID_DYNAMIC,
}

internal enum class StatusBarNetworkLabelMode {
    HIDDEN,
    DISCREET,
    PROMINENT,
}

internal data class StatusBarClockStyle(
    val widthDp: Float,
    val heightDp: Float,
    val fontSizeSp: Float,
    val weight: StatusBarStyleTextWeight,
)

internal data class StatusBarMobileIconProfile(
    val widthDp: Float,
    val heightDp: Float,
    val leftFraction: Float,
    val bottomFraction: Float,
    val availableWidthFraction: Float,
    val gapFraction: Float,
    val heightFractions: List<Float>,
    val cornerFraction: Float,
    val inactiveAlpha: Float = PixelStatusBarGeometry.INACTIVE_ALPHA,
    val userBarStyleFallback: PixelMobileBarStyle = PixelMobileBarStyle.CLASSIC,
)

internal data class StatusBarWifiIconProfile(
    val sizeDp: Float,
    val centerXFraction: Float,
    val centerYFraction: Float,
    val radiiFractions: List<Float>,
    val strokeFraction: Float,
    val startAngle: Float,
    val sweepAngle: Float,
    val dotYFraction: Float,
    val dotRadiusFraction: Float,
    val inactiveAlpha: Float = PixelStatusBarGeometry.INACTIVE_ALPHA,
)

internal data class StatusBarBatteryIconProfile(
    val widthDp: Float,
    val heightDp: Float,
    val outlineStrokeFraction: Float,
    val bodyWidthFraction: Float,
    val bodyCornerFraction: Float,
    val terminalWidthFraction: Float,
    val terminalGapFraction: Float,
    val terminalHeightFraction: Float,
    val terminalAlpha: Float,
    val fillInsetStrokeMultiplier: Float,
    val fillInsetHeightFraction: Float,
    val fillCornerHeightFraction: Float,
    val chargingBolt: Boolean = true,
)

internal data class StatusBarStyle(
    val id: StatusBarStyleId,
    val displayName: String,
    val visualSignatureKey: String,
    val clock: StatusBarClockStyle,
    val mobile: StatusBarMobileIconProfile,
    val wifi: StatusBarWifiIconProfile,
    val battery: StatusBarBatteryIconProfile,
    val signalVisualMode: StatusBarSignalVisualMode,
    val wifiVisualMode: StatusBarWifiVisualMode,
    val batteryVisualMode: StatusBarBatteryVisualMode,
    val networkLabelMode: StatusBarNetworkLabelMode,
    val rightGroupHeightDp: Float,
    val edgeInsetDp: Float,
    val spacingMultiplier: Float,
    val networkTypeFontSizeSp: Float,
    val networkTypeWeight: StatusBarStyleTextWeight,
    val networkTypeWidthScale: Float = 1f,
    val batteryPercentageFontSizeSp: Float = 9.25f,
    val batteryPercentageWeight: StatusBarStyleTextWeight = StatusBarStyleTextWeight.MEDIUM,
    val respectsIslandExclusion: Boolean = true,
)

internal data class StatusBarStyleRenderPlan(
    val style: StatusBarStyle,
    val sourceState: CustomStatusBarDeviceState,
    val sanitizedSettings: CustomStatusBarSettings,
    val spacingDp: Float,
    val networkTypeLabel: String?,
    val respectsIslandExclusion: Boolean,
)

internal interface StatusBarStyleRenderer {
    val style: StatusBarStyle

    fun createRenderPlan(
        state: CustomStatusBarDeviceState,
        settings: CustomStatusBarSettings,
    ): StatusBarStyleRenderPlan
}

private class DeclarativeStatusBarStyleRenderer(
    override val style: StatusBarStyle,
) : StatusBarStyleRenderer {
    override fun createRenderPlan(
        state: CustomStatusBarDeviceState,
        settings: CustomStatusBarSettings,
    ): StatusBarStyleRenderPlan {
        val safe = settings.sanitized()
        return StatusBarStyleRenderPlan(
            style = style,
            sourceState = state,
            sanitizedSettings = safe,
            spacingDp = style.spacingDp(safe.systemIconsSpacingDp),
            networkTypeLabel = state.cellular.networkType
                ?.takeUnless { style.networkLabelMode == StatusBarNetworkLabelMode.HIDDEN }
                ?.let(PixelStatusBarPresentation::networkTypeLabel),
            respectsIslandExclusion = style.respectsIslandExclusion,
        )
    }
}

internal object StatusBarStyleRegistry {
    private val defaultMobile = StatusBarMobileIconProfile(
        widthDp = 15f,
        heightDp = 15f,
        leftFraction = 0.14f,
        bottomFraction = 0.88f,
        availableWidthFraction = 0.72f,
        gapFraction = 0.11111111f,
        heightFractions = listOf(0.24f, 0.42f, 0.60f, 0.78f),
        cornerFraction = 0.50f,
        userBarStyleFallback = PixelMobileBarStyle.CLASSIC,
    )

    private val defaultWifi = StatusBarWifiIconProfile(
        sizeDp = 15f,
        centerXFraction = 0.50f,
        centerYFraction = 0.72f,
        radiiFractions = listOf(0.16f, 0.30f, 0.44f),
        strokeFraction = 0.082f,
        startAngle = 220f,
        sweepAngle = 100f,
        dotYFraction = 0.84f,
        dotRadiusFraction = 0.085f,
    )

    private val defaultBattery = StatusBarBatteryIconProfile(
        widthDp = 21.5f,
        heightDp = 11.5f,
        outlineStrokeFraction = 0.095f,
        bodyWidthFraction = 0.88f,
        bodyCornerFraction = 0.24f,
        terminalWidthFraction = 0.063f,
        terminalGapFraction = 0.021f,
        terminalHeightFraction = 0.34f,
        terminalAlpha = 0.90f,
        fillInsetStrokeMultiplier = 1.55f,
        fillInsetHeightFraction = 0.14f,
        fillCornerHeightFraction = 0.15f,
    )

    private val ios27 = StatusBarStyle(
        id = CustomStatusBarStyle.IOS_27,
        displayName = "iOS 27",
        visualSignatureKey = "ios27-solid-pill",
        clock = StatusBarClockStyle(80f, 25f, 14.5f, StatusBarStyleTextWeight.SEMIBOLD),
        mobile = defaultMobile.copy(
            widthDp = 16.4f,
            leftFraction = 0.10f,
            bottomFraction = 0.89f,
            availableWidthFraction = 0.82f,
            gapFraction = 0.060f,
            heightFractions = listOf(0.28f, 0.47f, 0.66f, 0.84f),
            cornerFraction = 0.80f,
            userBarStyleFallback = PixelMobileBarStyle.TALL,
        ),
        wifi = defaultWifi.copy(
            sizeDp = 16.3f,
            centerYFraction = 0.735f,
            radiiFractions = listOf(0.17f, 0.315f, 0.455f),
            strokeFraction = 0.102f,
            startAngle = 222f,
            sweepAngle = 96f,
            dotRadiusFraction = 0.105f,
        ),
        battery = defaultBattery.copy(
            widthDp = 23.4f,
            heightDp = 12.0f,
            outlineStrokeFraction = 0.105f,
            bodyWidthFraction = 0.90f,
            bodyCornerFraction = 0.50f,
            terminalWidthFraction = 0.052f,
            terminalHeightFraction = 0.30f,
            terminalAlpha = 1f,
            fillInsetStrokeMultiplier = 0f,
            fillInsetHeightFraction = 0f,
            fillCornerHeightFraction = 0.50f,
        ),
        signalVisualMode = StatusBarSignalVisualMode.IOS_BOLD_PILLS,
        wifiVisualMode = StatusBarWifiVisualMode.IOS_BOLD_ARCS,
        batteryVisualMode = StatusBarBatteryVisualMode.IOS_SOLID_CAPSULE,
        networkLabelMode = StatusBarNetworkLabelMode.DISCREET,
        rightGroupHeightDp = 25f,
        edgeInsetDp = 8f,
        spacingMultiplier = 1.15f,
        networkTypeFontSizeSp = 9.5f,
        networkTypeWeight = StatusBarStyleTextWeight.SEMIBOLD,
        batteryPercentageFontSizeSp = 9.5f,
        batteryPercentageWeight = StatusBarStyleTextWeight.SEMIBOLD,
    )

    private val pixel16 = StatusBarStyle(
        id = CustomStatusBarStyle.PIXEL_16_17,
        displayName = "Pixel 16",
        visualSignatureKey = "pixel16-compact-measured",
        clock = StatusBarClockStyle(76f, 24f, 14.2f, StatusBarStyleTextWeight.SEMIBOLD),
        mobile = defaultMobile.copy(
            widthDp = 18.5f,
            heightDp = 14.5f,
            leftFraction = 0.00f,
            bottomFraction = 1.00f,
            availableWidthFraction = 1.00f,
            gapFraction = 0.090f,
            heightFractions = listOf(0.50f, 0.633f, 0.833f, 0.967f),
            cornerFraction = 0.40f,
            inactiveAlpha = 0.20f,
            userBarStyleFallback = PixelMobileBarStyle.COMPACT,
        ),
        wifi = defaultWifi.copy(
            sizeDp = 19f,
            strokeFraction = 0.172f,
            dotRadiusFraction = 0.102f,
            inactiveAlpha = 0.20f,
        ),
        battery = defaultBattery.copy(
            widthDp = 24f,
            heightDp = 14f,
            outlineStrokeFraction = 0.0f,
            bodyWidthFraction = 0.925f,
            bodyCornerFraction = 0.215f,
            terminalWidthFraction = 0.075f,
            terminalGapFraction = 0.037f,
            terminalHeightFraction = 0.414f,
            terminalAlpha = 1f,
            fillInsetStrokeMultiplier = 0f,
            fillInsetHeightFraction = 0f,
            fillCornerHeightFraction = 0.215f,
            chargingBolt = false,
        ),
        signalVisualMode = StatusBarSignalVisualMode.PIXEL_COMPACT_BARS,
        wifiVisualMode = StatusBarWifiVisualMode.PIXEL_COMPACT_ARCS,
        batteryVisualMode = StatusBarBatteryVisualMode.COMPACT_SOLID_DYNAMIC,
        networkLabelMode = StatusBarNetworkLabelMode.PROMINENT,
        rightGroupHeightDp = 24f,
        edgeInsetDp = 7f,
        spacingMultiplier = 1.45f,
        networkTypeFontSizeSp = 10.2f,
        networkTypeWeight = StatusBarStyleTextWeight.SEMIBOLD,
        networkTypeWidthScale = 1.05f,
        batteryPercentageFontSizeSp = 10.2f,
        batteryPercentageWeight = StatusBarStyleTextWeight.SEMIBOLD,
    )

    val allStyles: List<StatusBarStyle> = listOf(ios27, pixel16)

    val defaultStyle: StatusBarStyle = ios27

    private val byId: Map<StatusBarStyleId, StatusBarStyle> = allStyles.associateBy { it.id }
    private val renderers: Map<StatusBarStyleId, StatusBarStyleRenderer> =
        allStyles.associate { it.id to DeclarativeStatusBarStyleRenderer(it) }

    val defaultRenderer: StatusBarStyleRenderer = renderers.getValue(defaultStyle.id)

    fun resolve(id: StatusBarStyleId?): StatusBarStyle =
        byId[CustomStatusBarStyleUiPolicy.migrateLegacy(id ?: defaultStyle.id)] ?: defaultStyle

    fun resolvePersisted(raw: String?): StatusBarStyleId =
        resolve(CustomStatusBarStyle.fromPersisted(raw)).id

    fun rendererFor(id: StatusBarStyleId?): StatusBarStyleRenderer =
        renderers[CustomStatusBarStyleUiPolicy.migrateLegacy(id ?: defaultStyle.id)] ?: defaultRenderer
}

internal val StatusBarBatteryVisualMode.usesInternalPercentage: Boolean
    get() = this == StatusBarBatteryVisualMode.NUMERIC_CAPSULE_PROMINENT ||
        this == StatusBarBatteryVisualMode.NUMERIC_CAPSULE_COMPACT

internal fun StatusBarStyle.spacingDp(baseSpacingDp: Float): Float =
    (baseSpacingDp * spacingMultiplier).coerceAtLeast(0f)
