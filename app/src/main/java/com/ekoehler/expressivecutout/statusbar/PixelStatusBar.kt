package com.ekoehler.expressivecutout.statusbar

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ekoehler.expressivecutout.data.CustomStatusBarSettings
import com.ekoehler.expressivecutout.data.PixelMobileBarStyle
import com.ekoehler.expressivecutout.data.PixelStatusBarScale

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
            val percentage = PixelStatusBarPresentation.batteryPercentage(
                state.battery.level,
                safe.batteryPercentageMode,
            )
            val edgeInsetPx = with(density) { styleSpec.edgeInsetDp.dp.roundToPx() }
            val availableDp = with(density) {
                (right.width - edgeInsetPx * 2).coerceAtLeast(0).toDp().value
            }
            val networkAvailable = state.cellular.connected && state.cellular.networkType != null
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
                    if (includePercentage && percentage != null) {
                        add(
                            PixelStatusBarPresentation.batteryPercentageWidthDp(
                                percentage,
                                scales.systemIcons,
                            ),
                        )
                    }
                }
                val gaps = (widths.size - 1).coerceAtLeast(0)
                return widths.sum() + gaps * spacingDp
            }

            val bothFit = widthDp(includeNetwork = networkAvailable, includePercentage = percentage != null) <= availableDp
            val percentageFitsWithoutNetwork =
                percentage != null && widthDp(includeNetwork = false, includePercentage = true) <= availableDp
            val networkFitsWithoutPercentage =
                networkAvailable && widthDp(includeNetwork = true, includePercentage = false) <= availableDp

            val showNetwork = when {
                bothFit -> networkAvailable
                percentageFitsWithoutNetwork -> false
                else -> networkFitsWithoutPercentage
            }
            val showPercentage = when {
                bothFit -> percentage != null
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
                        modifier = Modifier.offset(y = optical.mobileYDp.dp),
                    )
                }
                if (state.wifi.connected) {
                    PixelWifiGlyph(
                        level = state.wifi.level,
                        tint = rightTint,
                        scale = scales.wifi,
                        profile = styleSpec.wifi,
                        modifier = Modifier.offset(y = wifiOptical.wifiYDp.dp),
                    )
                }
                PixelBatteryGlyph(
                    level = state.battery.level,
                    charging = state.battery.charging,
                    tint = rightTint,
                    scale = scales.battery,
                    profile = styleSpec.battery,
                    modifier = Modifier.offset(y = batteryOptical.batteryYDp.dp),
                )
                if (showPercentage && percentage != null) {
                    Text(
                        text = percentage,
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
) {
    val safeScale = scale.coerceAtLeast(0.1f)
    val glyphProfile = profile ?: StatusBarStyleRegistry.defaultStyle.wifi
    val strengths = PixelStatusBarGeometry.wifiStrengths(level)
    Canvas(
        modifier = modifier.size((glyphProfile.sizeDp * safeScale).dp),
    ) {
        val side = minOf(size.width, size.height)
        val geometry = PixelStatusBarGeometry.wifiGlyph(side, glyphProfile)
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
                    width = geometry.strokeWidth,
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
            radius = geometry.dotRadius,
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
) {
    val safeScale = scale.coerceAtLeast(0.1f)
    val strengths = PixelStatusBarGeometry.mobileStrengths(level)
    val glyphWidthDp = profile?.widthDp ?: PixelStatusBarPresentation.mobileSignalWidthDp(style, 1f)
    val glyphHeightDp = profile?.heightDp ?: PixelStatusBarGeometry.MOBILE_HEIGHT_DP
    Canvas(
        modifier = modifier
            .width((glyphWidthDp * safeScale).dp)
            .height((glyphHeightDp * safeScale).dp),
    ) {
        val geometry = if (profile != null) {
            PixelStatusBarGeometry.mobileGlyph(size.width, size.height, profile)
        } else {
            PixelStatusBarGeometry.mobileGlyph(size.width, size.height, style)
        }
        val inactiveAlpha = profile?.inactiveAlpha ?: PixelStatusBarGeometry.INACTIVE_ALPHA
        geometry.bars.forEachIndexed { index, bar ->
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
                cornerRadius = CornerRadius(bar.cornerRadius),
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
) {
    val safeScale = scale.coerceAtLeast(0.1f)
    val glyphProfile = profile ?: StatusBarStyleRegistry.defaultStyle.battery
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
            val bolt = androidx.compose.ui.graphics.Path().apply {
                moveTo(geometry.bolt.first().x, geometry.bolt.first().y)
                geometry.bolt.drop(1).forEach { point ->
                    lineTo(point.x, point.y)
                }
                close()
            }
            drawPath(
                path = bolt,
                color = if (fraction > 0.45f) {
                    if (tint == Color.White) Color.Black else Color.White
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
