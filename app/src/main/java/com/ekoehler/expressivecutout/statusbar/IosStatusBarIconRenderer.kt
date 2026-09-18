package com.ekoehler.expressivecutout.statusbar

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
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
internal fun IosMeasuredSignalGlyph(
    geometry: IosMeasuredSignalGeometry,
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
        val box = fitMeasuredBox(geometry.aspectRatio)
        geometry.bars.forEach { bar ->
            val rect = bar.rect.toRect(box)
            val radius = min(rect.width * bar.radiusToWidth, rect.height / 2f)
            drawRoundRect(
                color = tint,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                cornerRadius = CornerRadius(radius),
            )
        }
    }
}

@Composable
internal fun IosMeasuredWifiGlyph(
    geometry: IosMeasuredWifiGeometry,
    tint: Color,
    sizeDp: Float,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val safeScale = scale.coerceAtLeast(0.1f)
    Canvas(modifier = modifier.size((sizeDp * safeScale).dp)) {
        val box = fitMeasuredBox(geometry.aspectRatio)
        drawMeasuredArc(
            visibleRect = geometry.outer.toRect(box),
            stroke = box.height * geometry.outerStrokeToHeight,
            tint = tint,
            startAngle = 205f,
            sweepAngle = 130f,
        )
        drawMeasuredArc(
            visibleRect = geometry.middle.toRect(box),
            stroke = box.height * geometry.middleStrokeToHeight,
            tint = tint,
            startAngle = 208f,
            sweepAngle = 124f,
        )
        drawMeasuredTeardrop(
            rect = geometry.dot.toRect(box),
            tint = tint,
            soft = geometry.dotShape == "soft-teardrop",
        )
    }
}

@Composable
internal fun IosMeasuredBatteryGlyph(
    geometry: IosMeasuredBatteryGeometry,
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
        val box = fitMeasuredBox(geometry.aspectRatio)
        val body = geometry.body.toRect(box)
        val terminal = geometry.terminal.toRect(box)
        val bodyRadius = body.height * geometry.bodyRadiusToHeight
        val terminalRadius = terminal.width * geometry.terminalRadiusToWidth

        if (geometry.mode == "outline-fill") {
            val outline = if (tint == Color.White) {
                Color.White.copy(alpha = 0.66f)
            } else {
                Color(0xFFA0A0A0)
            }
            val terminalColor = if (tint == Color.White) {
                Color.White.copy(alpha = 0.60f)
            } else {
                Color(0xFF989898)
            }
            drawRoundRect(
                color = outline,
                topLeft = Offset(body.left, body.top),
                size = Size(body.width, body.height),
                cornerRadius = CornerRadius(bodyRadius),
                style = Stroke(width = body.height * 0.075f),
            )
            geometry.innerFill?.let { fillRect ->
                val fill = fillRect.toRect(box)
                val fillRadius = fill.height * (geometry.innerRadiusToHeight ?: 0.285f)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(fill.left, fill.top),
                    size = Size(fill.width, fill.height),
                    cornerRadius = CornerRadius(fillRadius),
                )
            }
            drawRoundRect(
                color = terminalColor,
                topLeft = Offset(terminal.left, terminal.top),
                size = Size(terminal.width, terminal.height),
                cornerRadius = CornerRadius(terminalRadius),
            )
        } else {
            drawRoundRect(
                color = tint,
                topLeft = Offset(body.left, body.top),
                size = Size(body.width, body.height),
                cornerRadius = CornerRadius(bodyRadius),
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(terminal.left, terminal.top),
                size = Size(terminal.width, terminal.height),
                cornerRadius = CornerRadius(terminalRadius),
            )
        }
    }
}

private fun DrawScope.fitMeasuredBox(aspectRatio: Float): Rect {
    val targetWidth = min(size.width, size.height * aspectRatio)
    val targetHeight = min(size.height, targetWidth / aspectRatio)
    val left = (size.width - targetWidth) / 2f
    val top = (size.height - targetHeight) / 2f
    return Rect(left, top, left + targetWidth, top + targetHeight)
}

private fun IosFractionRect.toRect(box: Rect): Rect = Rect(
    left = box.left + box.width * x,
    top = box.top + box.height * y,
    right = box.left + box.width * (x + width),
    bottom = box.top + box.height * (y + height),
)

private fun DrawScope.drawMeasuredArc(
    visibleRect: Rect,
    stroke: Float,
    tint: Color,
    startAngle: Float,
    sweepAngle: Float,
) {
    val ovalHeight = visibleRect.height * 2.18f
    val ovalTop = visibleRect.top + stroke / 2f
    drawArc(
        color = tint,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(
            visibleRect.left + stroke / 2f,
            ovalTop,
        ),
        size = Size(
            (visibleRect.width - stroke).coerceAtLeast(1f),
            (ovalHeight - stroke).coerceAtLeast(1f),
        ),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawMeasuredTeardrop(
    rect: Rect,
    tint: Color,
    soft: Boolean,
) {
    val cx = rect.left + rect.width / 2f
    val top = rect.top
    val bottom = rect.bottom
    val left = rect.left
    val right = rect.right
    val shoulderY = rect.top + rect.height * if (soft) 0.45f else 0.42f
    val pointY = rect.top + rect.height * if (soft) 0.96f else 0.98f
    val path = Path().apply {
        moveTo(cx, top)
        cubicTo(
            right - rect.width * 0.08f,
            top + rect.height * 0.04f,
            right,
            shoulderY,
            cx,
            pointY,
        )
        cubicTo(
            left,
            shoulderY,
            left + rect.width * 0.08f,
            top + rect.height * 0.04f,
            cx,
            top,
        )
        close()
    }
    drawPath(path, tint)
}
