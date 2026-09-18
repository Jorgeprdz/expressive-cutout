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
    val clock: StatusBarClockStyle,
    val mobile: StatusBarMobileIconProfile,
    val wifi: StatusBarWifiIconProfile,
    val battery: StatusBarBatteryIconProfile,
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
            networkTypeLabel = state.cellular.networkType?.let(PixelStatusBarPresentation::networkTypeLabel),
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
        clock = StatusBarClockStyle(
            widthDp = 76f,
            heightDp = 24f,
            fontSizeSp = 14f,
            weight = StatusBarStyleTextWeight.MEDIUM,
        ),
        mobile = defaultMobile,
        wifi = defaultWifi,
        battery = defaultBattery,
        rightGroupHeightDp = 24f,
        edgeInsetDp = 8f,
        spacingMultiplier = 1f,
        networkTypeFontSizeSp = 9.25f,
        networkTypeWeight = StatusBarStyleTextWeight.MEDIUM,
    )

    private val pixel15 = StatusBarStyle(
        id = CustomStatusBarStyle.PIXEL_15,
        displayName = "Pixel 15",
        clock = StatusBarClockStyle(
            widthDp = 74f,
            heightDp = 24f,
            fontSizeSp = 14f,
            weight = StatusBarStyleTextWeight.MEDIUM,
        ),
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
        rightGroupHeightDp = 24f,
        edgeInsetDp = 8f,
        spacingMultiplier = 0.92f,
        networkTypeFontSizeSp = 9f,
        networkTypeWeight = StatusBarStyleTextWeight.MEDIUM,
        networkTypeWidthScale = 0.96f,
    )

    private val ios27 = StatusBarStyle(
        id = CustomStatusBarStyle.IOS_27,
        displayName = "iOS 27",
        clock = StatusBarClockStyle(
            widthDp = 80f,
            heightDp = 25f,
            fontSizeSp = 14.5f,
            weight = StatusBarStyleTextWeight.SEMIBOLD,
        ),
        mobile = defaultMobile.copy(
            widthDp = 16.2f,
            leftFraction = 0.10f,
            bottomFraction = 0.89f,
            availableWidthFraction = 0.80f,
            gapFraction = 0.075f,
            heightFractions = listOf(0.25f, 0.43f, 0.62f, 0.82f),
            cornerFraction = 0.50f,
            userBarStyleFallback = PixelMobileBarStyle.TALL,
        ),
        wifi = defaultWifi.copy(
            sizeDp = 16f,
            centerYFraction = 0.735f,
            radiiFractions = listOf(0.17f, 0.315f, 0.455f),
            strokeFraction = 0.088f,
            startAngle = 222f,
            sweepAngle = 96f,
            dotRadiusFraction = 0.092f,
        ),
        battery = defaultBattery.copy(
            widthDp = 23f,
            heightDp = 11.8f,
            outlineStrokeFraction = 0.105f,
            bodyCornerFraction = 0.44f,
            fillCornerHeightFraction = 0.24f,
            terminalHeightFraction = 0.32f,
        ),
        rightGroupHeightDp = 25f,
        edgeInsetDp = 8f,
        spacingMultiplier = 1.15f,
        networkTypeFontSizeSp = 9.5f,
        networkTypeWeight = StatusBarStyleTextWeight.SEMIBOLD,
        batteryPercentageFontSizeSp = 9.5f,
        batteryPercentageWeight = StatusBarStyleTextWeight.SEMIBOLD,
    )

    private val hyperOs = StatusBarStyle(
        id = CustomStatusBarStyle.HYPER_OS,
        displayName = "HyperOS",
        clock = StatusBarClockStyle(
            widthDp = 70f,
            heightDp = 23f,
            fontSizeSp = 13.5f,
            weight = StatusBarStyleTextWeight.REGULAR,
        ),
        mobile = defaultMobile.copy(
            widthDp = 13f,
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
            widthDp = 20.6f,
            heightDp = 10.8f,
            outlineStrokeFraction = 0.083f,
            bodyCornerFraction = 0.18f,
            terminalAlpha = 0.82f,
            fillCornerHeightFraction = 0.12f,
        ),
        rightGroupHeightDp = 23f,
        edgeInsetDp = 7f,
        spacingMultiplier = 0.78f,
        networkTypeFontSizeSp = 8.8f,
        networkTypeWeight = StatusBarStyleTextWeight.REGULAR,
        networkTypeWidthScale = 0.90f,
        batteryPercentageFontSizeSp = 8.8f,
        batteryPercentageWeight = StatusBarStyleTextWeight.REGULAR,
    )

    private val nothingOs = StatusBarStyle(
        id = CustomStatusBarStyle.NOTHING_OS,
        displayName = "Nothing OS",
        clock = StatusBarClockStyle(
            widthDp = 72f,
            heightDp = 24f,
            fontSizeSp = 13.8f,
            weight = StatusBarStyleTextWeight.MEDIUM,
        ),
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
            outlineStrokeFraction = 0.072f,
            bodyCornerFraction = 0.12f,
            terminalWidthFraction = 0.050f,
            terminalAlpha = 0.72f,
            fillCornerHeightFraction = 0.06f,
            chargingBolt = false,
        ),
        rightGroupHeightDp = 24f,
        edgeInsetDp = 8f,
        spacingMultiplier = 0.92f,
        networkTypeFontSizeSp = 8.9f,
        networkTypeWeight = StatusBarStyleTextWeight.MEDIUM,
        networkTypeWidthScale = 0.92f,
        batteryPercentageFontSizeSp = 8.9f,
        batteryPercentageWeight = StatusBarStyleTextWeight.MEDIUM,
    )

    val allStyles: List<StatusBarStyle> = listOf(
        default,
        pixel15,
        ios27,
        hyperOs,
        nothingOs,
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

internal fun StatusBarStyle.spacingDp(baseSpacingDp: Float): Float =
    (baseSpacingDp * spacingMultiplier).coerceAtLeast(0f)
