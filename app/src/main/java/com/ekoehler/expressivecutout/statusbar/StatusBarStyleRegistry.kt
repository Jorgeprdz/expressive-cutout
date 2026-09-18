package com.ekoehler.expressivecutout.statusbar

import com.ekoehler.expressivecutout.data.CustomStatusBarSettings
import com.ekoehler.expressivecutout.data.CustomStatusBarStyle
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

    private val default = StatusBarStyle(
        id = CustomStatusBarStyle.DEFAULT,
        displayName = "Default / One UI",
        visualSignatureKey = "one-ui-default",
        clock = StatusBarClockStyle(76f, 24f, 14f, StatusBarStyleTextWeight.MEDIUM),
        mobile = defaultMobile,
        wifi = defaultWifi,
        battery = defaultBattery,
        signalVisualMode = StatusBarSignalVisualMode.CLASSIC_BARS,
        wifiVisualMode = StatusBarWifiVisualMode.CLASSIC_ARCS,
        batteryVisualMode = StatusBarBatteryVisualMode.CLASSIC_ANDROID,
        networkLabelMode = StatusBarNetworkLabelMode.DISCREET,
        rightGroupHeightDp = 24f,
        edgeInsetDp = 8f,
        spacingMultiplier = 1f,
        networkTypeFontSizeSp = 9.25f,
        networkTypeWeight = StatusBarStyleTextWeight.MEDIUM,
    )

    private val ios26 = StatusBarStyle(
        id = CustomStatusBarStyle.IOS_26,
        displayName = "iOS 26",
        visualSignatureKey = "ios26-outline",
        clock = StatusBarClockStyle(78f, 24f, 14.2f, StatusBarStyleTextWeight.MEDIUM),
        mobile = defaultMobile.copy(
            widthDp = 15.8f,
            leftFraction = 0.12f,
            bottomFraction = 0.88f,
            availableWidthFraction = 0.76f,
            gapFraction = 0.090f,
            heightFractions = listOf(0.24f, 0.42f, 0.60f, 0.78f),
            cornerFraction = 0.62f,
            userBarStyleFallback = PixelMobileBarStyle.CLASSIC,
        ),
        wifi = defaultWifi.copy(
            sizeDp = 15.7f,
            centerYFraction = 0.72f,
            radiiFractions = listOf(0.17f, 0.31f, 0.45f),
            strokeFraction = 0.083f,
            startAngle = 222f,
            sweepAngle = 96f,
            dotRadiusFraction = 0.086f,
        ),
        battery = defaultBattery.copy(
            widthDp = 22.2f,
            heightDp = 11.6f,
            outlineStrokeFraction = 0.088f,
            bodyCornerFraction = 0.30f,
            terminalAlpha = 0.55f,
            fillCornerHeightFraction = 0.17f,
        ),
        signalVisualMode = StatusBarSignalVisualMode.IOS_ROUNDED_BARS,
        wifiVisualMode = StatusBarWifiVisualMode.IOS_ARCS,
        batteryVisualMode = StatusBarBatteryVisualMode.IOS_OUTLINE_FILL,
        networkLabelMode = StatusBarNetworkLabelMode.DISCREET,
        rightGroupHeightDp = 24f,
        edgeInsetDp = 8f,
        spacingMultiplier = 1.05f,
        networkTypeFontSizeSp = 9.2f,
        networkTypeWeight = StatusBarStyleTextWeight.MEDIUM,
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

    private val pixel15 = StatusBarStyle(
        id = CustomStatusBarStyle.PIXEL_15,
        displayName = "Pixel 15",
        visualSignatureKey = "pixel15-classic",
        clock = StatusBarClockStyle(74f, 24f, 14f, StatusBarStyleTextWeight.MEDIUM),
        mobile = defaultMobile.copy(
            widthDp = 13.8f,
            leftFraction = 0.08f,
            availableWidthFraction = 0.84f,
            gapFraction = 0.048f,
            heightFractions = listOf(0.23f, 0.39f, 0.56f, 0.72f),
            cornerFraction = 0.44f,
            userBarStyleFallback = PixelMobileBarStyle.COMPACT,
        ),
        wifi = defaultWifi.copy(
            strokeFraction = 0.078f,
            dotRadiusFraction = 0.078f,
        ),
        battery = defaultBattery.copy(
            widthDp = 21f,
            heightDp = 11.2f,
            bodyCornerFraction = 0.22f,
        ),
        signalVisualMode = StatusBarSignalVisualMode.CLASSIC_BARS,
        wifiVisualMode = StatusBarWifiVisualMode.CLASSIC_ARCS,
        batteryVisualMode = StatusBarBatteryVisualMode.CLASSIC_ANDROID,
        networkLabelMode = StatusBarNetworkLabelMode.DISCREET,
        rightGroupHeightDp = 24f,
        edgeInsetDp = 8f,
        spacingMultiplier = 0.92f,
        networkTypeFontSizeSp = 9f,
        networkTypeWeight = StatusBarStyleTextWeight.MEDIUM,
        networkTypeWidthScale = 0.96f,
    )

    private val pixel1617 = StatusBarStyle(
        id = CustomStatusBarStyle.PIXEL_16_17,
        displayName = "Pixel 16/17",
        visualSignatureKey = "pixel1617-data-rich",
        clock = StatusBarClockStyle(76f, 24f, 14.2f, StatusBarStyleTextWeight.SEMIBOLD),
        mobile = defaultMobile.copy(
            widthDp = 15.6f,
            heightDp = 15f,
            leftFraction = 0.06f,
            bottomFraction = 0.90f,
            availableWidthFraction = 0.88f,
            gapFraction = 0.030f,
            heightFractions = listOf(0.36f, 0.52f, 0.68f, 0.84f),
            cornerFraction = 0.50f,
            inactiveAlpha = 0.20f,
            userBarStyleFallback = PixelMobileBarStyle.COMPACT,
        ),
        wifi = defaultWifi.copy(
            sizeDp = 14.8f,
            centerYFraction = 0.715f,
            strokeFraction = 0.088f,
            dotRadiusFraction = 0.082f,
            startAngle = 222f,
            sweepAngle = 96f,
        ),
        battery = defaultBattery.copy(
            widthDp = 28f,
            heightDp = 13f,
            outlineStrokeFraction = 0.0f,
            bodyWidthFraction = 0.91f,
            bodyCornerFraction = 0.48f,
            terminalWidthFraction = 0.045f,
            terminalGapFraction = 0.014f,
            terminalHeightFraction = 0.30f,
            terminalAlpha = 0.35f,
            fillInsetStrokeMultiplier = 0f,
            fillInsetHeightFraction = 0f,
            fillCornerHeightFraction = 0.48f,
        ),
        signalVisualMode = StatusBarSignalVisualMode.DOT_MATRIX,
        wifiVisualMode = StatusBarWifiVisualMode.PIXEL_COMPACT_ARCS,
        batteryVisualMode = StatusBarBatteryVisualMode.NUMERIC_CAPSULE_PROMINENT,
        networkLabelMode = StatusBarNetworkLabelMode.PROMINENT,
        rightGroupHeightDp = 24f,
        edgeInsetDp = 7f,
        spacingMultiplier = 0.60f,
        networkTypeFontSizeSp = 10.2f,
        networkTypeWeight = StatusBarStyleTextWeight.SEMIBOLD,
        networkTypeWidthScale = 1.05f,
        batteryPercentageFontSizeSp = 10.2f,
        batteryPercentageWeight = StatusBarStyleTextWeight.SEMIBOLD,
    )

    private val hyperOs = StatusBarStyle(
        id = CustomStatusBarStyle.HYPER_OS,
        displayName = "HyperOS",
        visualSignatureKey = "hyperos-balanced-numeric",
        clock = StatusBarClockStyle(70f, 23f, 13.5f, StatusBarStyleTextWeight.REGULAR),
        mobile = defaultMobile.copy(
            widthDp = 13.2f,
            leftFraction = 0.10f,
            bottomFraction = 0.86f,
            availableWidthFraction = 0.82f,
            gapFraction = 0.040f,
            heightFractions = listOf(0.22f, 0.38f, 0.54f, 0.70f),
            cornerFraction = 0.32f,
            inactiveAlpha = 0.18f,
            userBarStyleFallback = PixelMobileBarStyle.COMPACT,
        ),
        wifi = defaultWifi.copy(
            sizeDp = 14.2f,
            centerYFraction = 0.71f,
            strokeFraction = 0.070f,
            startAngle = 222f,
            sweepAngle = 96f,
            inactiveAlpha = 0.18f,
        ),
        battery = defaultBattery.copy(
            widthDp = 24.5f,
            heightDp = 11.0f,
            outlineStrokeFraction = 0.0f,
            bodyWidthFraction = 0.90f,
            bodyCornerFraction = 0.22f,
            terminalWidthFraction = 0.048f,
            terminalAlpha = 0.78f,
            fillInsetStrokeMultiplier = 0f,
            fillInsetHeightFraction = 0f,
            fillCornerHeightFraction = 0.20f,
        ),
        signalVisualMode = StatusBarSignalVisualMode.CLASSIC_BARS,
        wifiVisualMode = StatusBarWifiVisualMode.HYPER_COMPACT_ARCS,
        batteryVisualMode = StatusBarBatteryVisualMode.NUMERIC_CAPSULE_COMPACT,
        networkLabelMode = StatusBarNetworkLabelMode.DISCREET,
        rightGroupHeightDp = 23f,
        edgeInsetDp = 7f,
        spacingMultiplier = 0.75f,
        networkTypeFontSizeSp = 8.8f,
        networkTypeWeight = StatusBarStyleTextWeight.REGULAR,
        networkTypeWidthScale = 0.90f,
        batteryPercentageFontSizeSp = 8.8f,
        batteryPercentageWeight = StatusBarStyleTextWeight.REGULAR,
    )

    private val nothingOs5 = StatusBarStyle(
        id = CustomStatusBarStyle.NOTHING_OS_5,
        displayName = "Nothing OS 5",
        visualSignatureKey = "nothingos5-minimal",
        clock = StatusBarClockStyle(72f, 24f, 13.8f, StatusBarStyleTextWeight.MEDIUM),
        mobile = defaultMobile.copy(
            widthDp = 14.4f,
            leftFraction = 0.09f,
            bottomFraction = 0.84f,
            availableWidthFraction = 0.82f,
            gapFraction = 0.070f,
            heightFractions = listOf(0.42f, 0.52f, 0.62f, 0.72f),
            cornerFraction = 0.22f,
            inactiveAlpha = 0.16f,
            userBarStyleFallback = PixelMobileBarStyle.COMPACT,
        ),
        wifi = defaultWifi.copy(
            sizeDp = 14.8f,
            centerYFraction = 0.72f,
            radiiFractions = listOf(0.15f, 0.30f, 0.45f),
            strokeFraction = 0.064f,
            startAngle = 226f,
            sweepAngle = 88f,
            dotRadiusFraction = 0.070f,
            inactiveAlpha = 0.15f,
        ),
        battery = defaultBattery.copy(
            widthDp = 21.2f,
            heightDp = 10.8f,
            outlineStrokeFraction = 0.0f,
            bodyCornerFraction = 0.44f,
            bodyWidthFraction = 0.90f,
            terminalWidthFraction = 0.050f,
            terminalAlpha = 0.72f,
            fillInsetStrokeMultiplier = 0f,
            fillInsetHeightFraction = 0f,
            fillCornerHeightFraction = 0.42f,
            chargingBolt = false,
        ),
        signalVisualMode = StatusBarSignalVisualMode.COMPACT_MINIMAL_BARS,
        wifiVisualMode = StatusBarWifiVisualMode.NOTHING_MINIMAL_ARCS,
        batteryVisualMode = StatusBarBatteryVisualMode.SOLID_CAPSULE_MINIMAL,
        networkLabelMode = StatusBarNetworkLabelMode.HIDDEN,
        rightGroupHeightDp = 24f,
        edgeInsetDp = 8f,
        spacingMultiplier = 0.85f,
        networkTypeFontSizeSp = 8.9f,
        networkTypeWeight = StatusBarStyleTextWeight.MEDIUM,
        networkTypeWidthScale = 0.92f,
        batteryPercentageFontSizeSp = 8.9f,
        batteryPercentageWeight = StatusBarStyleTextWeight.MEDIUM,
    )

    val allStyles: List<StatusBarStyle> = listOf(
        default,
        ios26,
        ios27,
        pixel15,
        pixel1617,
        hyperOs,
        nothingOs5,
    )

    val defaultStyle: StatusBarStyle = default

    private val byId: Map<StatusBarStyleId, StatusBarStyle> = allStyles.associateBy { it.id }
    private val renderers: Map<StatusBarStyleId, StatusBarStyleRenderer> =
        allStyles.associate { it.id to DeclarativeStatusBarStyleRenderer(it) }

    val defaultRenderer: StatusBarStyleRenderer = renderers.getValue(default.id)

    fun resolve(id: StatusBarStyleId?): StatusBarStyle = byId[id] ?: defaultStyle

    fun resolvePersisted(raw: String?): StatusBarStyleId =
        resolve(CustomStatusBarStyle.fromPersisted(raw)).id

    fun rendererFor(id: StatusBarStyleId?): StatusBarStyleRenderer =
        renderers[id] ?: defaultRenderer
}

internal val StatusBarBatteryVisualMode.usesInternalPercentage: Boolean
    get() = this == StatusBarBatteryVisualMode.NUMERIC_CAPSULE_PROMINENT ||
        this == StatusBarBatteryVisualMode.NUMERIC_CAPSULE_COMPACT

internal fun StatusBarStyle.spacingDp(baseSpacingDp: Float): Float =
    (baseSpacingDp * spacingMultiplier).coerceAtLeast(0f)
