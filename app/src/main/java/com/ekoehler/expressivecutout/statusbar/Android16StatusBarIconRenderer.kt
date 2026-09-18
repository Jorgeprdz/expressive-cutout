package com.ekoehler.expressivecutout.statusbar

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
internal fun Android16MeasuredSignalGlyph(
    geometry: Android16MeasuredSignalGeometry,
    tint: Color,
    widthDp: Float,
    heightDp: Float,
    scale: Float,
    modifier: Modifier = Modifier,
    level: Int? = 4,
    inactiveAlpha: Float = 0.34f,
) {
    val safeScale = scale.coerceAtLeast(0.1f)
    Canvas(
        modifier = modifier
            .width((widthDp * safeScale).dp)
            .height((heightDp * safeScale).dp),
    ) {
        val active = dynamicLevel(level, geometry.bars.size)
        val activeColor = pixelActiveColor(tint)
        val inactiveColor = pixelInactiveColor(tint, inactiveAlpha)
        val box = fitAndroid16Box(geometry.aspectRatio)
        geometry.bars.forEachIndexed { index, bar ->
            drawAndroid16SignalPart(
                bar = bar,
                box = box,
                color = if (index < active) activeColor else inactiveColor,
            )
        }
        geometry.lowerIndicators.forEachIndexed { index, bar ->
            drawAndroid16SignalPart(
                bar = bar,
                box = box,
                color = if (index < active) activeColor.copy(alpha = 0.62f) else inactiveColor.copy(alpha = inactiveColor.alpha * 0.70f),
            )
        }
    }
}

@Composable
internal fun Android16MeasuredWifiGlyph(
    geometry: Android16MeasuredWifiGeometry,
    tint: Color,
    sizeDp: Float,
    scale: Float,
    modifier: Modifier = Modifier,
    level: Int? = 4,
    inactiveAlpha: Float = 0.42f,
) {
    val safeScale = scale.coerceAtLeast(0.1f)
    Canvas(modifier = modifier.size((sizeDp * safeScale).dp)) {
        val activeParts = wifiActiveParts(level)
        val active = pixelActiveColor(tint)
        val inactive = pixelInactiveColor(tint, inactiveAlpha)
        val box = fitAndroid16Box(geometry.aspectRatio)
        drawAndroid16Arc(
            visibleRect = geometry.outer.toRect(box),
            stroke = box.height * geometry.outerStrokeToHeight,
            color = if (activeParts >= 3) active.copy(alpha = 0.94f) else inactive,
            startAngle = 207f,
            sweepAngle = 126f,
        )
        drawAndroid16Arc(
            visibleRect = geometry.middle.toRect(box),
            stroke = box.height * geometry.middleStrokeToHeight,
            color = if (activeParts >= 2) active else inactive,
            startAngle = 210f,
            sweepAngle = 120f,
        )
        val dot = geometry.dot.toRect(box)
        drawCircle(
            color = if (activeParts >= 1) active else inactive,
            radius = min(dot.width, dot.height) / 2f,
            center = Offset(dot.left + dot.width / 2f, dot.top + dot.height / 2f),
        )
    }
}

@Composable
internal fun Android16MeasuredBatteryGlyph(
    geometry: Android16MeasuredBatteryGeometry,
    level: Int?,
    charging: Boolean,
    tint: Color,
    widthDp: Float,
    heightDp: Float,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val safeScale = scale.coerceAtLeast(0.1f)
    Canvas(
        modifier = modifier
            .width((widthDp * safeScale).dp)
            .height((heightDp * safeScale).dp),
    ) {
        val safeLevel = (level ?: 0).coerceIn(0, 100)
        val fraction = safeLevel / 100f
        val box = fitAndroid16Box(geometry.aspectRatio)
        val body = geometry.body.toRect(box)
        val terminal = geometry.terminal.toRect(box)
        val bodyRadius = body.height * geometry.bodyRadiusToHeight
        val terminalRadius = terminal.width * geometry.terminalRadiusToWidth
        val shell = pixelBatteryShell(tint)
        val fillColor = when {
            charging -> Color(0xFF30D158)
            safeLevel < geometry.redThreshold -> Color(0xFFF50003)
            tint == Color.White -> Color(0xFFF0F4F5)
            else -> tint
        }
        val textColor = when {
            charging || safeLevel < geometry.redThreshold -> Color.White
            tint == Color.White -> Color(0xFF1C1D21)
            fraction > 0.52f -> fillColor.contrastColor()
            else -> tint
        }

        drawRoundRect(
            color = Color.Black.copy(alpha = if (tint == Color.White) 0.10f else 0.06f),
            topLeft = Offset(body.left, body.top + body.height * 0.035f),
            size = Size(body.width, body.height),
            cornerRadius = CornerRadius(bodyRadius),
        )
        drawRoundRect(
            color = shell,
            topLeft = Offset(body.left, body.top),
            size = Size(body.width, body.height),
            cornerRadius = CornerRadius(bodyRadius),
        )
        val fillWidth = (body.width * fraction).coerceAtLeast(0f)
        if (fillWidth > 0f) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(body.left, body.top),
                size = Size(fillWidth, body.height),
                cornerRadius = CornerRadius(min(bodyRadius, fillWidth / 2f)),
            )
        }
        drawRoundRect(
            color = shell.copy(alpha = 0.92f),
            topLeft = Offset(terminal.left, terminal.top),
            size = Size(terminal.width, terminal.height),
            cornerRadius = CornerRadius(terminalRadius),
        )

        val textSize = body.height * when {
            safeLevel >= 100 -> 0.46f
            safeLevel < 10 -> 0.63f
            else -> 0.58f
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor.toArgb()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            this.textSize = textSize
        }
        drawContext.canvas.nativeCanvas.drawText(
            safeLevel.toString(),
            body.left + body.width / 2f,
            body.top + body.height / 2f - (paint.ascent() + paint.descent()) / 2f,
            paint,
        )
        if (charging) {
            val bolt = Path().apply {
                moveTo(body.right - body.width * 0.18f, body.top + body.height * 0.22f)
                lineTo(body.right - body.width * 0.30f, body.top + body.height * 0.54f)
                lineTo(body.right - body.width * 0.20f, body.top + body.height * 0.54f)
                lineTo(body.right - body.width * 0.31f, body.top + body.height * 0.84f)
                lineTo(body.right - body.width * 0.10f, body.top + body.height * 0.43f)
                lineTo(body.right - body.width * 0.21f, body.top + body.height * 0.43f)
                close()
            }
            drawPath(bolt, textColor)
        }
    }
}

private fun DrawScope.drawAndroid16SignalPart(
    bar: Android16MeasuredSignalBar,
    box: Rect,
    color: Color,
) {
    val rect = bar.rect.toRect(box)
    val radius = min(rect.width * bar.radiusToWidth, rect.height / 2f)
    drawRoundRect(
        color = color,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(radius),
    )
}

private fun DrawScope.drawAndroid16Arc(
    visibleRect: Rect,
    stroke: Float,
    color: Color,
    startAngle: Float,
    sweepAngle: Float,
) {
    val ovalHeight = visibleRect.height * 2.05f
    val ovalTop = visibleRect.top + stroke * 0.42f
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(visibleRect.left + stroke / 2f, ovalTop),
        size = Size(
            (visibleRect.width - stroke).coerceAtLeast(1f),
            (ovalHeight - stroke).coerceAtLeast(1f),
        ),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
}

private fun DrawScope.fitAndroid16Box(aspectRatio: Float): Rect {
    val targetWidth = min(size.width, size.height * aspectRatio)
    val targetHeight = min(size.height, targetWidth / aspectRatio)
    val left = (size.width - targetWidth) / 2f
    val top = (size.height - targetHeight) / 2f
    return Rect(left, top, left + targetWidth, top + targetHeight)
}

private fun Android16FractionRect.toRect(box: Rect): Rect = Rect(
    left = box.left + box.width * x,
    top = box.top + box.height * y,
    right = box.left + box.width * (x + width),
    bottom = box.top + box.height * (y + height),
)

private fun pixelActiveColor(tint: Color): Color = if (tint == Color.White) Color(0xFFF0F4F5) else tint

private fun pixelInactiveColor(tint: Color, alpha: Float): Color =
    if (tint == Color.White) Color(0xFF7F8D97) else tint.copy(alpha = alpha)

private fun pixelBatteryShell(tint: Color): Color =
    if (tint == Color.White) Color(0xFF99A1AA) else tint.copy(alpha = 0.18f)
