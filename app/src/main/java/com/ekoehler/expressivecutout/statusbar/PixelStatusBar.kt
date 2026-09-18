package com.ekoehler.expressivecutout.statusbar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pixel-style visual layer. It consumes only normalized state and layout; it never queries Android
 * theme, connectivity, battery or Dynamic Island state on its own.
 */
@Composable
internal fun PixelStatusBarLayer(
    state: CustomStatusBarDeviceState,
    foreground: StatusBarForeground,
    layout: StatusBarLayoutResult,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        targetValue = if (foreground == StatusBarForeground.LIGHT) Color.White else Color.Black,
        animationSpec = tween(durationMillis = 160),
        label = "customStatusBarTint",
    )

    Box(modifier = modifier) {
        layout.leftContentRegion?.let { left ->
            Box(
                modifier = Modifier
                    .offset { IntOffset(left.left, left.top) }
                    .width(left.width.dp)
                    .height(left.height.dp)
                    .padding(start = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = state.timeText,
                    color = tint,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }

        layout.rightContentRegion?.let { right ->
            Row(
                modifier = Modifier
                    .offset { IntOffset(right.left, right.top) }
                    .width(right.width.dp)
                    .height(right.height.dp)
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.cellular.connected) {
                    state.cellular.networkType?.let {
                        Text(
                            text = when (it) {
                                StatusBarNetworkType.FOUR_G -> "4G"
                                StatusBarNetworkType.LTE -> "LTE"
                                StatusBarNetworkType.FIVE_G -> "5G"
                            },
                            color = tint,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                    PixelMobileGlyph(level = state.cellular.level, tint = tint)
                }
                if (state.wifi.connected) {
                    PixelWifiGlyph(level = state.wifi.level, tint = tint)
                }
                PixelBatteryGlyph(
                    level = state.battery.level,
                    charging = state.battery.charging,
                    tint = tint,
                )
            }
        }
    }
}

@Composable
internal fun PixelWifiGlyph(
    level: Int?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val strengths = PixelStatusBarGeometry.wifiStrengths(level)
    Canvas(modifier = modifier.size(15.dp)) {
        val side = min(size.width, size.height)
        val cx = size.width / 2f
        val cy = size.height * 0.63f
        val stroke = maxOf(1.15.dp.toPx(), side * 0.085f)
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
    modifier: Modifier = Modifier,
) {
    val strengths = PixelStatusBarGeometry.mobileStrengths(level)
    Canvas(modifier = modifier.size(15.dp)) {
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
                cornerRadius = CornerRadius(min(barW * 0.36f, 1.8.dp.toPx())),
            )
        }
    }
}

@Composable
internal fun PixelBatteryGlyph(
    level: Int?,
    charging: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val fraction = PixelStatusBarGeometry.batteryFraction(level)
    Canvas(modifier = modifier.width(22.dp).height(12.dp)) {
        val terminalWidth = 1.5.dp.toPx()
        val bodyWidth = size.width - terminalWidth - 1.dp.toPx()
        val stroke = 1.2.dp.toPx()
        val radius = 2.2.dp.toPx()

        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, stroke / 2f),
            size = Size(bodyWidth, size.height - stroke),
            cornerRadius = CornerRadius(radius),
            style = Stroke(width = stroke),
        )
        drawRoundRect(
            color = tint.copy(alpha = 0.8f),
            topLeft = Offset(bodyWidth + 0.7.dp.toPx(), size.height * 0.32f),
            size = Size(terminalWidth, size.height * 0.36f),
            cornerRadius = CornerRadius(terminalWidth / 2f),
        )

        val inset = 2.2.dp.toPx()
        val fillWidth = (bodyWidth - inset * 2f).coerceAtLeast(0f) * fraction
        if (fillWidth > 0f) {
            drawRoundRect(
                color = tint,
                topLeft = Offset(inset, inset),
                size = Size(fillWidth, (size.height - inset * 2f).coerceAtLeast(0f)),
                cornerRadius = CornerRadius(1.2.dp.toPx()),
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
