package com.ekoehler.expressivecutout.statusbar

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ekoehler.expressivecutout.data.CustomStatusBarSettings
import com.ekoehler.expressivecutout.data.IosBatteryColorMode
import com.ekoehler.expressivecutout.data.PixelMobileBarStyle
import com.ekoehler.expressivecutout.data.PixelStatusBarScale
import kotlin.math.min

/**
 * Pixel-style visual layer. It consumes only normalized state/layout/settings and never queries
 * Android theme, connectivity, battery or Dynamic Island state on its own.
 */
@Composable
internal fun PixelStatusBarLayer(
    state: CustomStatusBarDeviceState,
    leftForeground: StatusBarForeground,
    rightForeground: StatusBarForeground,
    layout: StatusBarLayoutResult,
    settings: CustomStatusBarSettings = CustomStatusBarSettings.DEFAULT,
    modifier: Modifier = Modifier,
) {
    val safe = settings.sanitized()
    val renderPlan = runCatching {
        StatusBarStyleRegistry.rendererFor(safe.style).createRenderPlan(state, safe)
    }.getOrElse {
        StatusBarStyleRegistry.defaultRenderer.createRenderPlan(state, safe)
    }
    val styleSpec = renderPlan.style
    val scales = PixelStatusBarScale.resolve(safe)
    val leftTint by animateColorAsState(
        targetValue = if (leftForeground == StatusBarForeground.LIGHT) Color.White else Color.Black,
        animationSpec = tween(durationMillis = 160),
        label = "customStatusBarLeftTint",
    )
    val rightTint by animateColorAsState(
        targetValue = if (rightForeground == StatusBarForeground.LIGHT) Color.White else Color.Black,
        animationSpec = tween(durationMillis = 160),
        label = "customStatusBarRightTint",
    )

    val density = LocalDensity.current
    Box(modifier = modifier) {
        layout.leftContentRegion?.let { left ->
            val clockWidthDp = styleSpec.clock.widthDp * scales.clock
            val clockHeightDp = styleSpec.clock.heightDp * scales.clock
            val placed = StatusBarSafePlacement.placeLeft(
                region = left,
                contentWidth = with(density) { clockWidthDp.dp.roundToPx() },
                contentHeight = with(density) { clockHeightDp.dp.roundToPx() },
                requestedOffsetX = with(density) { safe.clockOffsetXDp.dp.roundToPx() },
                requestedOffsetY = with(density) { safe.clockOffsetYDp.dp.roundToPx() },
                globalOffsetY = with(density) { safe.statusBarOffsetYDp.dp.roundToPx() },
                edgeInset = with(density) { styleSpec.edgeInsetDp.dp.roundToPx() },
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(placed.left, placed.top) }
                    .width(with(density) { placed.width.toDp() })
                    .height(with(density) { placed.height.toDp() })
                    .clipToBounds(),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = state.timeText,
                    color = leftTint,
                    fontSize = (styleSpec.clock.fontSizeSp * scales.clock).sp,
                    fontWeight = styleSpec.clock.weight.toFontWeight(),
                    maxLines = 1,
                )
            }
        }

        layout.rightContentRegion?.let { right ->
            val outsidePercentage = if (styleSpec.batteryVisualMode.usesInternalPercentage) {
                null
            } else {
                PixelStatusBarPresentation.batteryPercentage(
                    state.battery.level,
                    safe.batteryPercentageMode,
                )
            }
            val edgeInsetPx = with(density) { styleSpec.edgeInsetDp.dp.roundToPx() }
            val availableDp = with(density) {
                (right.width - edgeInsetPx * 2).coerceAtLeast(0).toDp().value
            }
            val networkAvailable = styleSpec.networkLabelMode != StatusBarNetworkLabelMode.HIDDEN &&
                state.cellular.connected &&
                state.cellular.networkType != null
            val optical = PixelStatusBarOpticalMetrics.resolve(scales.systemIcons)
            val wifiOptical = PixelStatusBarOpticalMetrics.resolve(scales.wifi)
            val batteryOptical = PixelStatusBarOpticalMetrics.resolve(scales.battery)
            val spacingDp = renderPlan.spacingDp

            fun widthDp(includeNetwork: Boolean, includePercentage: Boolean): Float {
                val widths = buildList {
                    if (state.cellular.connected) {
                        add(styleSpec.mobile.widthDp * scales.systemIcons)
                    }
                    if (includeNetwork && networkAvailable) {
                        state.cellular.networkType?.let {
                            add(
                                PixelStatusBarPresentation.networkTypeWidthDp(
                                    it,
                                    scales.systemIcons,
                                ) * styleSpec.networkTypeWidthScale,
                            )
                        }
                    }
                    if (state.wifi.connected) {
                        add(styleSpec.wifi.sizeDp * scales.wifi)
                    }
                    add(styleSpec.battery.widthDp * scales.battery)
                    if (includePercentage && outsidePercentage != null) {
                        add(
                            PixelStatusBarPresentation.batteryPercentageWidthDp(
                                outsidePercentage,
                                scales.systemIcons,
                            ),
                        )
                    }
                }
                val gaps = (widths.size - 1).coerceAtLeast(0)
                return widths.sum() + gaps * spacingDp
            }

            val bothFit = widthDp(includeNetwork = networkAvailable, includePercentage = outsidePercentage != null) <= availableDp
            val percentageFitsWithoutNetwork =
                outsidePercentage != null && widthDp(includeNetwork = false, includePercentage = true) <= availableDp
            val networkFitsWithoutPercentage =
                networkAvailable && widthDp(includeNetwork = true, includePercentage = false) <= availableDp

            val showNetwork = when {
                bothFit -> networkAvailable
                percentageFitsWithoutNetwork -> false
                else -> networkFitsWithoutPercentage
            }
            val showPercentage = when {
                bothFit -> outsidePercentage != null
                percentageFitsWithoutNetwork -> true
                else -> false
            }

            val contentWidthDp = widthDp(showNetwork, showPercentage)
                .coerceAtMost(availableDp.coerceAtLeast(0f))
            val contentHeightDp =
                styleSpec.rightGroupHeightDp *
                    maxOf(scales.systemIcons, scales.wifi, scales.battery)
            val placed = StatusBarSafePlacement.placeRight(
                region = right,
                contentWidth = with(density) { contentWidthDp.dp.roundToPx() },
                contentHeight = with(density) { contentHeightDp.dp.roundToPx() },
                requestedOffsetX = with(density) { safe.systemIconsOffsetXDp.dp.roundToPx() },
                requestedOffsetY = with(density) { safe.systemIconsOffsetYDp.dp.roundToPx() },
                globalOffsetY = with(density) { safe.statusBarOffsetYDp.dp.roundToPx() },
                edgeInset = edgeInsetPx,
            )

            Row(
                modifier = Modifier
                    .offset { IntOffset(placed.left, placed.top) }
                    .width(with(density) { placed.width.toDp() })
                    .height(with(density) { placed.height.toDp() })
                    .clipToBounds(),
                horizontalArrangement = Arrangement.spacedBy(
                    spacingDp.dp,
                    Alignment.End,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.cellular.connected) {
                    if (showNetwork) {
                        state.cellular.networkType?.let {
                            Text(
                                text = PixelStatusBarPresentation.networkTypeLabel(it),
                                color = rightTint,
                                fontSize = (styleSpec.networkTypeFontSizeSp * scales.systemIcons).sp,
                                fontWeight = styleSpec.networkTypeWeight.toFontWeight(),
                                maxLines = 1,
                                modifier = Modifier.offset(y = optical.networkTypeYDp.dp),
                            )
                        }
                    }
                    PixelMobileGlyph(
                        level = state.cellular.level,
                        tint = rightTint,
                        scale = scales.systemIcons,
                        style = styleSpec.mobile.userBarStyleFallback,
                        profile = styleSpec.mobile,
                        visualMode = styleSpec.signalVisualMode,
                        modifier = Modifier.offset(y = optical.mobileYDp.dp),
                    )
                }
                if (state.wifi.connected) {
                    PixelWifiGlyph(
                        level = state.wifi.level,
                        tint = rightTint,
                        scale = scales.wifi,
                        profile = styleSpec.wifi,
                        visualMode = styleSpec.wifiVisualMode,
                        modifier = Modifier.offset(y = wifiOptical.wifiYDp.dp),
                    )
                }
                PixelBatteryGlyph(
                    level = state.battery.level,
                    charging = state.battery.charging,
                    tint = rightTint,
                    scale = scales.battery,
                    profile = styleSpec.battery,
                    visualMode = styleSpec.batteryVisualMode,
                    iosBatteryColorMode = safe.iosBatteryColorMode,
                    modifier = Modifier.offset(y = batteryOptical.batteryYDp.dp),
                )
                if (showPercentage && outsidePercentage != null) {
                    Text(
                        text = outsidePercentage,
                        color = rightTint,
                        fontSize = (styleSpec.batteryPercentageFontSizeSp * scales.systemIcons).sp,
                        fontWeight = styleSpec.batteryPercentageWeight.toFontWeight(),
                        maxLines = 1,
                        modifier = Modifier.offset(y = optical.percentageYDp.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun PixelWifiGlyph(
    level: Int?,
    tint: Color,
    scale: Float = 1f,
    modifier: Modifier = Modifier,
    profile: StatusBarWifiIconProfile? = null,
    visualMode: StatusBarWifiVisualMode = StatusBarWifiVisualMode.CLASSIC_ARCS,
) {
    val safeScale = scale.coerceAtLeast(0.1f)
    val glyphProfile = profile ?: StatusBarStyleRegistry.defaultStyle.wifi
    val measured = when (visualMode) {
        StatusBarWifiVisualMode.IOS_ARCS -> IosStatusBarIconGeometry.ios26.wifi
        StatusBarWifiVisualMode.IOS_BOLD_ARCS -> IosStatusBarIconGeometry.ios27.wifi
        else -> null
    }
    if (measured != null) {
        IosMeasuredWifiGlyph(
            geometry = measured,
            tint = tint,
            sizeDp = glyphProfile.sizeDp,
            scale = safeScale,
            modifier = modifier,
            level = level,
            inactiveAlpha = glyphProfile.inactiveAlpha,
        )
        return
    }
    if (visualMode == StatusBarWifiVisualMode.PIXEL_COMPACT_ARCS) {
        Android16MeasuredWifiGlyph(
            geometry = Android16StatusBarIconGeometry.pixel1617.wifi,
            tint = tint,
            sizeDp = glyphProfile.sizeDp,
            scale = safeScale,
            modifier = modifier,
            level = level,
            inactiveAlpha = glyphProfile.inactiveAlpha,
        )
        return
    }

    val strengths = PixelStatusBarGeometry.wifiStrengths(level)
    Canvas(
        modifier = modifier.size((glyphProfile.sizeDp * safeScale).dp),
    ) {
        val side = minOf(size.width, size.height)
        val geometry = PixelStatusBarGeometry.wifiGlyph(side, glyphProfile)
        val strokeMultiplier = when (visualMode) {
            StatusBarWifiVisualMode.IOS_BOLD_ARCS -> 1.12f
            StatusBarWifiVisualMode.NOTHING_MINIMAL_ARCS -> 0.86f
            StatusBarWifiVisualMode.HYPER_COMPACT_ARCS -> 0.92f
            else -> 1f
        }
        geometry.radii.forEachIndexed { index, radius ->
            drawArc(
                color = tint.copy(
                    alpha = if (strengths[index + 1] > 0f) {
                        1f
                    } else {
                        glyphProfile.inactiveAlpha
                    },
                ),
                startAngle = geometry.startAngle,
                sweepAngle = geometry.sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    geometry.centerX - radius,
                    geometry.centerY - radius,
                ),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(
                    width = geometry.strokeWidth * strokeMultiplier,
                    cap = StrokeCap.Round,
                ),
            )
        }
        drawCircle(
            color = tint.copy(
                alpha = if (strengths[0] > 0f) {
                    1f
                } else {
                    glyphProfile.inactiveAlpha
                },
            ),
            radius = geometry.dotRadius * if (visualMode == StatusBarWifiVisualMode.IOS_BOLD_ARCS) 1.15f else 1f,
            center = Offset(geometry.dotX, geometry.dotY),
        )
    }
}

@Composable
internal fun PixelMobileGlyph(
    level: Int?,
    tint: Color,
    scale: Float = 1f,
    style: PixelMobileBarStyle = PixelMobileBarStyle.CLASSIC,
    modifier: Modifier = Modifier,
    profile: StatusBarMobileIconProfile? = null,
    visualMode: StatusBarSignalVisualMode = StatusBarSignalVisualMode.CLASSIC_BARS,
) {
    val safeScale = scale.coerceAtLeast(0.1f)
    val glyphWidthDp = profile?.widthDp ?: PixelStatusBarPresentation.mobileSignalWidthDp(style, 1f)
    val glyphHeightDp = profile?.heightDp ?: PixelStatusBarGeometry.MOBILE_HEIGHT_DP
    val measured = when (visualMode) {
        StatusBarSignalVisualMode.IOS_ROUNDED_BARS -> IosStatusBarIconGeometry.ios26.signal
        StatusBarSignalVisualMode.IOS_BOLD_PILLS -> IosStatusBarIconGeometry.ios27.signal
        else -> null
    }
    if (measured != null) {
        IosMeasuredSignalGlyph(
            geometry = measured,
            tint = tint,
            widthDp = glyphWidthDp,
            heightDp = glyphHeightDp,
            scale = safeScale,
            modifier = modifier,
            level = level,
            inactiveAlpha = profile?.inactiveAlpha ?: PixelStatusBarGeometry.INACTIVE_ALPHA,
        )
        return
    }
    if (visualMode == StatusBarSignalVisualMode.DOT_MATRIX) {
        Android16MeasuredSignalGlyph(
            geometry = Android16StatusBarIconGeometry.pixel1617.signal,
            tint = tint,
            widthDp = glyphWidthDp,
            heightDp = glyphHeightDp,
            scale = safeScale,
            modifier = modifier,
            level = level,
            inactiveAlpha = profile?.inactiveAlpha ?: 0.34f,
        )
        return
    }

    Canvas(
        modifier = modifier
            .width((glyphWidthDp * safeScale).dp)
            .height((glyphHeightDp * safeScale).dp),
    ) {
        val strengths = PixelStatusBarGeometry.mobileStrengths(level)
        val geometry = if (profile != null) {
            PixelStatusBarGeometry.mobileGlyph(size.width, size.height, profile)
        } else {
            PixelStatusBarGeometry.mobileGlyph(size.width, size.height, style)
        }
        val inactiveAlpha = profile?.inactiveAlpha ?: PixelStatusBarGeometry.INACTIVE_ALPHA
        geometry.bars.forEachIndexed { index, bar ->
            val visualCorner = when (visualMode) {
                StatusBarSignalVisualMode.IOS_BOLD_PILLS -> bar.width
                StatusBarSignalVisualMode.IOS_ROUNDED_BARS -> bar.width * 0.70f
                StatusBarSignalVisualMode.COMPACT_MINIMAL_BARS -> bar.width * 0.22f
                else -> bar.cornerRadius
            }
            drawRoundRect(
                color = tint.copy(
                    alpha = if (strengths[index] > 0f) {
                        1f
                    } else {
                        inactiveAlpha
                    },
                ),
                topLeft = Offset(bar.left, bar.top),
                size = Size(bar.width, bar.height),
                cornerRadius = CornerRadius(min(visualCorner, bar.height / 2f)),
            )
        }
    }
}

@Composable
internal fun PixelBatteryGlyph(
    level: Int?,
    charging: Boolean,
    tint: Color,
    scale: Float = 1f,
    modifier: Modifier = Modifier,
    profile: StatusBarBatteryIconProfile? = null,
    visualMode: StatusBarBatteryVisualMode = StatusBarBatteryVisualMode.CLASSIC_ANDROID,
    iosBatteryColorMode: IosBatteryColorMode = IosBatteryColorMode.MONOCHROME,
) {
    val safeScale = scale.coerceAtLeast(0.1f)
    val glyphProfile = profile ?: StatusBarStyleRegistry.defaultStyle.battery
    val measured = when (visualMode) {
        StatusBarBatteryVisualMode.IOS_OUTLINE_FILL -> IosStatusBarIconGeometry.ios26.battery
        StatusBarBatteryVisualMode.IOS_SOLID_CAPSULE -> IosStatusBarIconGeometry.ios27.battery
        else -> null
    }
    if (measured != null) {
        val measuredTint = IosBatteryStatusColors.resolve(level, tint, iosBatteryColorMode)
        IosMeasuredBatteryGlyph(
            geometry = measured,
            tint = measuredTint,
            widthDp = glyphProfile.widthDp,
            heightDp = glyphProfile.heightDp,
            scale = safeScale,
            modifier = modifier,
            level = level,
            charging = charging,
        )
        return
    }
    if (visualMode == StatusBarBatteryVisualMode.NUMERIC_CAPSULE_PROMINENT) {
        Android16MeasuredBatteryGlyph(
            geometry = Android16StatusBarIconGeometry.pixel1617.battery,
            level = level,
            charging = charging,
            tint = tint,
            widthDp = glyphProfile.widthDp,
            heightDp = glyphProfile.heightDp,
            scale = safeScale,
            modifier = modifier,
        )
        return
    }

    val fraction = PixelStatusBarGeometry.batteryFraction(level)
    Canvas(
        modifier = modifier
            .width((glyphProfile.widthDp * safeScale).dp)
            .height((glyphProfile.heightDp * safeScale).dp),
    ) {
        val geometry = PixelStatusBarGeometry.batteryGlyph(
            width = size.width,
            height = size.height,
            level = level,
            profile = glyphProfile,
        )

        if (visualMode.usesInternalPercentage ||
            visualMode == StatusBarBatteryVisualMode.IOS_SOLID_CAPSULE ||
            visualMode == StatusBarBatteryVisualMode.SOLID_CAPSULE_MINIMAL
        ) {
            val capsuleColor = if (charging && visualMode == StatusBarBatteryVisualMode.NUMERIC_CAPSULE_PROMINENT) {
                Color(0xFF30D158)
            } else {
                tint
            }
            drawRoundRect(
                color = capsuleColor,
                topLeft = Offset(geometry.body.left, geometry.body.top),
                size = Size(geometry.body.width, geometry.body.height),
                cornerRadius = CornerRadius(geometry.bodyCornerRadius),
            )
            if (geometry.terminal.width > 0f && geometry.terminal.height > 0f) {
                drawRoundRect(
                    color = capsuleColor.copy(alpha = glyphProfile.terminalAlpha),
                    topLeft = Offset(geometry.terminal.left, geometry.terminal.top),
                    size = Size(geometry.terminal.width, geometry.terminal.height),
                    cornerRadius = CornerRadius(geometry.terminalCornerRadius),
                )
            }
            if (visualMode.usesInternalPercentage) {
                val textColor = if (charging) Color.Black else capsuleColor.contrastColor()
                val textSize = size.height * if (visualMode == StatusBarBatteryVisualMode.NUMERIC_CAPSULE_PROMINENT) 0.66f else 0.58f
                drawContext.canvas.nativeCanvas.drawText(
                    (level ?: 0).coerceIn(0, 100).toString(),
                    geometry.body.left + geometry.body.width / 2f,
                    geometry.body.top + geometry.body.height / 2f -
                        (Paint().ascent() + Paint().descent()) / 2f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = textColor.toArgb()
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.DEFAULT_BOLD
                        this.textSize = textSize
                    },
                )
                if (charging && visualMode == StatusBarBatteryVisualMode.NUMERIC_CAPSULE_PROMINENT) {
                    val bolt = Path().apply {
                        moveTo(size.width * 0.88f, size.height * 0.24f)
                        lineTo(size.width * 0.78f, size.height * 0.54f)
                        lineTo(size.width * 0.87f, size.height * 0.54f)
                        lineTo(size.width * 0.80f, size.height * 0.82f)
                        lineTo(size.width * 0.96f, size.height * 0.45f)
                        lineTo(size.width * 0.88f, size.height * 0.45f)
                        close()
                    }
                    drawPath(bolt, Color.Black)
                }
            }
            return@Canvas
        }

        drawRoundRect(
            color = tint,
            topLeft = Offset(geometry.body.left, geometry.body.top),
            size = Size(geometry.body.width, geometry.body.height),
            cornerRadius = CornerRadius(geometry.bodyCornerRadius),
            style = Stroke(width = geometry.outlineStroke),
        )
        if (geometry.terminal.width > 0f && geometry.terminal.height > 0f) {
            drawRoundRect(
                color = tint.copy(alpha = glyphProfile.terminalAlpha),
                topLeft = Offset(geometry.terminal.left, geometry.terminal.top),
                size = Size(geometry.terminal.width, geometry.terminal.height),
                cornerRadius = CornerRadius(geometry.terminalCornerRadius),
            )
        }

        if (geometry.fill.width > 0f && geometry.fill.height > 0f) {
            drawRoundRect(
                color = tint,
                topLeft = Offset(geometry.fill.left, geometry.fill.top),
                size = Size(geometry.fill.width, geometry.fill.height),
                cornerRadius = CornerRadius(
                    minOf(geometry.fillCornerRadius, geometry.fill.width / 2f),
                ),
            )
        }

        if (charging && glyphProfile.chargingBolt && geometry.bolt.size >= 3) {
            val bolt = Path().apply {
                moveTo(geometry.bolt.first().x, geometry.bolt.first().y)
                geometry.bolt.drop(1).forEach { point ->
                    lineTo(point.x, point.y)
                }
                close()
            }
            drawPath(
                path = bolt,
                color = if (fraction > 0.45f) {
                    tint.contrastColor()
                } else {
                    tint
                },
            )
        }
    }
}

private fun StatusBarStyleTextWeight.toFontWeight(): FontWeight = when (this) {
    StatusBarStyleTextWeight.REGULAR -> FontWeight.Normal
    StatusBarStyleTextWeight.MEDIUM -> FontWeight.Medium
    StatusBarStyleTextWeight.SEMIBOLD -> FontWeight.SemiBold
}
