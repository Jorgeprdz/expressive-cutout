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
            val clockWidthDp = 76f * scales.clock
            val clockHeightDp = 24f * scales.clock
            val placed = StatusBarSafePlacement.placeLeft(
                region = left,
                contentWidth = with(density) { clockWidthDp.dp.roundToPx() },
                contentHeight = with(density) { clockHeightDp.dp.roundToPx() },
                requestedOffsetX = with(density) { safe.clockOffsetXDp.dp.roundToPx() },
                requestedOffsetY = with(density) { safe.clockOffsetYDp.dp.roundToPx() },
                globalOffsetY = with(density) { safe.statusBarOffsetYDp.dp.roundToPx() },
                edgeInset = with(density) { 8.dp.roundToPx() },
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
                    fontSize = (14f * scales.clock).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }

        layout.rightContentRegion?.let { right ->
            val percentage = PixelStatusBarPresentation.batteryPercentage(
                state.battery.level,
                safe.batteryPercentageMode,
            )
            val availableDp = with(density) {
                (right.width - 16.dp.roundToPx()).coerceAtLeast(0).toDp().value
            }
            val networkAvailable = state.cellular.connected && state.cellular.networkType != null
            val optical = PixelStatusBarOpticalMetrics.resolve(scales.systemIcons)
            val batteryOptical = PixelStatusBarOpticalMetrics.resolve(scales.battery)

            fun widthDp(includeNetwork: Boolean, includePercentage: Boolean): Float {
                val widths = buildList {
                    if (state.cellular.connected) {
                        add(
                            PixelStatusBarPresentation.mobileSignalWidthDp(
                                safe.mobileBarStyle,
                                scales.systemIcons,
                            ),
                        )
                    }
                    if (includeNetwork && networkAvailable) {
                        state.cellular.networkType?.let {
                            add(PixelStatusBarPresentation.networkTypeWidthDp(it, scales.systemIcons))
                        }
                    }
                    if (state.wifi.connected) {
                        add(PixelStatusBarGeometry.WIFI_SIZE_DP * scales.systemIcons)
                    }
                    add(PixelStatusBarGeometry.BATTERY_WIDTH_DP * scales.battery)
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
                return widths.sum() + gaps * safe.systemIconsSpacingDp
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
                PixelStatusBarGeometry.RIGHT_GROUP_HEIGHT_DP * maxOf(scales.systemIcons, scales.battery)
            val placed = StatusBarSafePlacement.placeRight(
                region = right,
                contentWidth = with(density) { contentWidthDp.dp.roundToPx() },
                contentHeight = with(density) { contentHeightDp.dp.roundToPx() },
                requestedOffsetX = with(density) { safe.systemIconsOffsetXDp.dp.roundToPx() },
                requestedOffsetY = with(density) { safe.systemIconsOffsetYDp.dp.roundToPx() },
                globalOffsetY = with(density) { safe.statusBarOffsetYDp.dp.roundToPx() },
                edgeInset = with(density) { 8.dp.roundToPx() },
            )

            Row(
                modifier = Modifier
                    .offset { IntOffset(placed.left, placed.top) }
                    .width(with(density) { placed.width.toDp() })
                    .height(with(density) { placed.height.toDp() })
                    .clipToBounds(),
                horizontalArrangement = Arrangement.spacedBy(
                    safe.systemIconsSpacingDp.dp,
                    Alignment.End,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.cellular.connected) {
                    if (showNetwork) {
                        state.cellular.networkType?.let {
                            Text(
                                text = when (it) {
                                    StatusBarNetworkType.FOUR_G -> "4G"
                                    StatusBarNetworkType.LTE -> "LTE"
                                    StatusBarNetworkType.FIVE_G -> "5G"
                                },
                                color = rightTint,
                                fontSize = (9.25f * scales.systemIcons).sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                modifier = Modifier.offset(y = optical.networkTypeYDp.dp),
                            )
                        }
                    }
                    PixelMobileGlyph(
                        level = state.cellular.level,
                        tint = rightTint,
                        scale = scales.systemIcons,
                        style = safe.mobileBarStyle,
                        modifier = Modifier.offset(y = optical.mobileYDp.dp),
                    )
                }
                if (state.wifi.connected) {
                    PixelWifiGlyph(
                        level = state.wifi.level,
                        tint = rightTint,
                        scale = scales.systemIcons,
                        modifier = Modifier.offset(y = optical.wifiYDp.dp),
                    )
                }
                PixelBatteryGlyph(
                    level = state.battery.level,
                    charging = state.battery.charging,
                    tint = rightTint,
                    scale = scales.battery,
                    modifier = Modifier.offset(y = batteryOptical.batteryYDp.dp),
                )
                if (showPercentage && percentage != null) {
                    Text(
                        text = percentage,
                        color = rightTint,
                        fontSize = (9.25f * scales.systemIcons).sp,
                        fontWeight = FontWeight.Medium,
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
) {
    val safeScale = scale.coerceAtLeast(0.1f)
    val strengths = PixelStatusBarGeometry.wifiStrengths(level)
    Canvas(
        modifier = modifier.size((PixelStatusBarGeometry.WIFI_SIZE_DP * safeScale).dp),
    ) {
        val side = minOf(size.width, size.height)
        val geometry = PixelStatusBarGeometry.wifiGlyph(side)
        geometry.radii.forEachIndexed { index, radius ->
            drawArc(
                color = tint.copy(
                    alpha = if (strengths[index + 1] > 0f) {
                        1f
                    } else {
                        PixelStatusBarGeometry.INACTIVE_ALPHA
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
                    PixelStatusBarGeometry.INACTIVE_ALPHA
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
) {
    val safeScale = scale.coerceAtLeast(0.1f)
    val strengths = PixelStatusBarGeometry.mobileStrengths(level)
    Canvas(
        modifier = modifier
            .width(PixelStatusBarPresentation.mobileSignalWidthDp(style, safeScale).dp)
            .height((PixelStatusBarGeometry.MOBILE_HEIGHT_DP * safeScale).dp),
    ) {
        val geometry = PixelStatusBarGeometry.mobileGlyph(size.width, size.height, style)
        geometry.bars.forEachIndexed { index, bar ->
            drawRoundRect(
                color = tint.copy(
                    alpha = if (strengths[index] > 0f) {
                        1f
                    } else {
                        PixelStatusBarGeometry.INACTIVE_ALPHA
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
) {
    val safeScale = scale.coerceAtLeast(0.1f)
    val fraction = PixelStatusBarGeometry.batteryFraction(level)
    Canvas(
        modifier = modifier
            .width((PixelStatusBarGeometry.BATTERY_WIDTH_DP * safeScale).dp)
            .height((PixelStatusBarGeometry.BATTERY_HEIGHT_DP * safeScale).dp),
    ) {
        val geometry = PixelStatusBarGeometry.batteryGlyph(
            width = size.width,
            height = size.height,
            level = level,
        )

        drawRoundRect(
            color = tint,
            topLeft = Offset(geometry.body.left, geometry.body.top),
            size = Size(geometry.body.width, geometry.body.height),
            cornerRadius = CornerRadius(geometry.bodyCornerRadius),
            style = Stroke(width = geometry.outlineStroke),
        )
        drawRoundRect(
            color = tint.copy(alpha = 0.9f),
            topLeft = Offset(geometry.terminal.left, geometry.terminal.top),
            size = Size(geometry.terminal.width, geometry.terminal.height),
            cornerRadius = CornerRadius(geometry.terminalCornerRadius),
        )

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

        if (charging && geometry.bolt.size >= 3) {
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
