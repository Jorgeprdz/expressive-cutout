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
import com.ekoehler.expressivecutout.data.PixelStatusBarScale
import kotlin.math.min
import kotlin.math.roundToInt

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

            fun widthDp(includeNetwork: Boolean, includePercentage: Boolean): Float {
                val widths = buildList {
                    if (state.cellular.connected) add(15f * scales.systemIcons)
                    if (includeNetwork && networkAvailable) add(24f * scales.systemIcons)
                    if (state.wifi.connected) add(15f * scales.systemIcons)
                    add(22f * scales.battery)
                    if (includePercentage && percentage != null) add(30f * scales.systemIcons)
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
            val contentHeightDp = 26f * maxOf(scales.systemIcons, scales.battery)
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
                                fontSize = (10f * scales.systemIcons).sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                            )
                        }
                    }
                    PixelMobileGlyph(
                        level = state.cellular.level,
                        tint = rightTint,
                        scale = scales.systemIcons,
                    )
                }
                if (state.wifi.connected) {
                    PixelWifiGlyph(
                        level = state.wifi.level,
                        tint = rightTint,
                        scale = scales.systemIcons,
                    )
                }
                PixelBatteryGlyph(
                    level = state.battery.level,
                    charging = state.battery.charging,
                    tint = rightTint,
                    scale = scales.battery,
                )
                if (showPercentage && percentage != null) {
                    Text(
                        text = percentage,
                        color = rightTint,
                        fontSize = (9.5f * scales.systemIcons).sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
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
    Canvas(modifier = modifier.size((15f * safeScale).dp)) {
        val side = min(size.width, size.height)
        val cx = size.width / 2f
        val cy = size.height * 0.63f
        val stroke = maxOf((1.15f * safeScale).dp.toPx(), side * 0.085f)
        val radii = listOf(0.20f, 0.36f, 0.52f)
        radii.forEachIndexed { index, fraction ->
            val radius = side * fraction
            drawArc(
                color = tint.copy(alpha = if (strengths[index + 1] > 0f) 1f else 0.22f),
                startAngle = 220f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        drawCircle(
            color = tint.copy(alpha = if (strengths[0] > 0f) 1f else 0.22f),
            radius = maxOf(stroke * 0.62f, side * 0.055f),
            center = Offset(cx, size.height * 0.79f),
        )
    }
}

@Composable
internal fun PixelMobileGlyph(
    level: Int?,
    tint: Color,
    scale: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val safeScale = scale.coerceAtLeast(0.1f)
    val strengths = PixelStatusBarGeometry.mobileStrengths(level)
    Canvas(modifier = modifier.size((15f * safeScale).dp)) {
        val w = size.width
        val h = size.height
        val left = w * 0.13f
        val bottom = h * 0.86f
        val availableW = w * 0.74f
        val gap = availableW * 0.055f
        val barW = (availableW - gap * 3f) / 4f

        repeat(4) { index ->
            val barH = h * (0.20f + index * 0.16f)
            val x = left + index * (barW + gap)
            drawRoundRect(
                color = tint.copy(alpha = if (strengths[index] > 0f) 1f else 0.22f),
                topLeft = Offset(x, bottom - barH),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(min(barW * 0.36f, (1.8f * safeScale).dp.toPx())),
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
            .width((22f * safeScale).dp)
            .height((12f * safeScale).dp),
    ) {
        val terminalWidth = (1.5f * safeScale).dp.toPx()
        val bodyWidth = size.width - terminalWidth - (1f * safeScale).dp.toPx()
        val stroke = (1.2f * safeScale).dp.toPx()
        val radius = (2.2f * safeScale).dp.toPx()

        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, stroke / 2f),
            size = Size(bodyWidth, size.height - stroke),
            cornerRadius = CornerRadius(radius),
            style = Stroke(width = stroke),
        )
        drawRoundRect(
            color = tint.copy(alpha = 0.8f),
            topLeft = Offset(bodyWidth + (0.7f * safeScale).dp.toPx(), size.height * 0.32f),
            size = Size(terminalWidth, size.height * 0.36f),
            cornerRadius = CornerRadius(terminalWidth / 2f),
        )

        val inset = (2.2f * safeScale).dp.toPx()
        val fillWidth = (bodyWidth - inset * 2f).coerceAtLeast(0f) * fraction
        if (fillWidth > 0f) {
            drawRoundRect(
                color = tint,
                topLeft = Offset(inset, inset),
                size = Size(fillWidth, (size.height - inset * 2f).coerceAtLeast(0f)),
                cornerRadius = CornerRadius((1.2f * safeScale).dp.toPx()),
            )
        }

        if (charging) {
            val bolt = androidx.compose.ui.graphics.Path().apply {
                moveTo(bodyWidth * 0.55f, size.height * 0.18f)
                lineTo(bodyWidth * 0.38f, size.height * 0.54f)
                lineTo(bodyWidth * 0.52f, size.height * 0.54f)
                lineTo(bodyWidth * 0.42f, size.height * 0.84f)
                lineTo(bodyWidth * 0.68f, size.height * 0.44f)
                lineTo(bodyWidth * 0.54f, size.height * 0.44f)
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
