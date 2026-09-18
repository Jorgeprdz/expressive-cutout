package com.ekoehler.expressivecutout.statusbar

import com.ekoehler.expressivecutout.data.PixelMobileBarStyle
import kotlin.math.min

internal data class PixelPoint(
    val x: Float,
    val y: Float,
)

internal data class PixelFloatRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
}

internal data class PixelWifiGlyphGeometry(
    val side: Float,
    val centerX: Float,
    val centerY: Float,
    val radii: List<Float>,
    val strokeWidth: Float,
    val startAngle: Float,
    val sweepAngle: Float,
    val dotX: Float,
    val dotY: Float,
    val dotRadius: Float,
)

internal data class PixelMobileBarGeometry(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val cornerRadius: Float,
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
}

internal data class PixelMobileGlyphGeometry(
    val width: Float,
    val height: Float,
    val bottom: Float,
    val barWidth: Float,
    val gap: Float,
    val bars: List<PixelMobileBarGeometry>,
)

internal data class PixelBatteryGlyphGeometry(
    val width: Float,
    val height: Float,
    val body: PixelFloatRect,
    val terminal: PixelFloatRect,
    val fill: PixelFloatRect,
    val fillMaxWidth: Float,
    val outlineStroke: Float,
    val bodyCornerRadius: Float,
    val terminalCornerRadius: Float,
    val fillCornerRadius: Float,
    val bolt: List<PixelPoint>,
)

/** Pure intensity and drawing geometry shared by the Pixel-style Canvas glyphs and JVM tests. */
internal object PixelStatusBarGeometry {
    const val WIFI_SIZE_DP = 15f
    const val MOBILE_SIZE_DP = 15f
    const val MOBILE_HEIGHT_DP = 15f
    const val BATTERY_WIDTH_DP = 21.5f
    const val BATTERY_HEIGHT_DP = 11.5f
    const val RIGHT_GROUP_HEIGHT_DP = 24f
    const val INACTIVE_ALPHA = 0.22f

    fun wifiStrengths(level: Int?): List<Float> {
        val active = level?.coerceIn(0, 4) ?: 0
        return List(4) { index -> if (index < active) 1f else 0f }
    }

    /**
     * Compact concentric Wi-Fi geometry. All dimensions derive from [side], so external scaling is
     * applied exactly once by the Canvas size rather than once to size and again to stroke/radii.
     */
    fun wifiGlyph(side: Float): PixelWifiGlyphGeometry {
        val safeSide = side.coerceAtLeast(0.1f)
        val centerX = safeSide * 0.50f
        val centerY = safeSide * 0.72f
        return PixelWifiGlyphGeometry(
            side = safeSide,
            centerX = centerX,
            centerY = centerY,
            radii = listOf(0.16f, 0.30f, 0.44f).map { safeSide * it },
            strokeWidth = safeSide * 0.082f,
            startAngle = 220f,
            sweepAngle = 100f,
            dotX = centerX,
            dotY = safeSide * 0.84f,
            dotRadius = maxOf(safeSide * 0.055f, safeSide * 0.085f * 0.62f),
        )
    }

    fun mobileStrengths(level: Int?): List<Float> {
        val active = level?.coerceIn(0, 4) ?: 0
        return List(4) { index -> if (index < active) 1f else 0f }
    }

    private data class MobileProfile(
        val intrinsicWidthDp: Float,
        val leftFraction: Float,
        val bottomFraction: Float,
        val availableWidthFraction: Float,
        val gapFraction: Float,
        val heightFractions: List<Float>,
        val cornerFraction: Float,
    )

    private fun mobileProfile(style: PixelMobileBarStyle): MobileProfile = when (style) {
        PixelMobileBarStyle.CLASSIC -> MobileProfile(
            intrinsicWidthDp = 15f,
            leftFraction = 0.14f,
            bottomFraction = 0.88f,
            availableWidthFraction = 0.66f,
            gapFraction = 0.12121212f,
            heightFractions = listOf(0.24f, 0.42f, 0.60f, 0.78f),
            cornerFraction = 0.50f,
        )
        PixelMobileBarStyle.COMPACT -> MobileProfile(
            intrinsicWidthDp = 13.5f,
            leftFraction = 0.08f,
            bottomFraction = 0.87f,
            availableWidthFraction = 0.84f,
            gapFraction = 0.045f,
            heightFractions = listOf(0.23f, 0.39f, 0.55f, 0.71f),
            cornerFraction = 0.44f,
        )
        PixelMobileBarStyle.TALL -> MobileProfile(
            intrinsicWidthDp = 14.5f,
            leftFraction = 0.14f,
            bottomFraction = 0.88f,
            availableWidthFraction = 0.72f,
            gapFraction = 0.065f,
            heightFractions = listOf(0.26f, 0.45f, 0.64f, 0.83f),
            cornerFraction = 0.40f,
        )
    }

    fun mobileWidthDp(style: PixelMobileBarStyle): Float = mobileProfile(style).intrinsicWidthDp

    /** Four status-bar bars on one baseline; style changes proportions, never signal semantics. */
    fun mobileGlyph(
        width: Float,
        height: Float,
        style: PixelMobileBarStyle = PixelMobileBarStyle.CLASSIC,
    ): PixelMobileGlyphGeometry {
        val safeWidth = width.coerceAtLeast(0.1f)
        val safeHeight = height.coerceAtLeast(0.1f)
        val profile = mobileProfile(style)
        val left = safeWidth * profile.leftFraction
        val bottom = safeHeight * profile.bottomFraction
        val availableWidth = safeWidth * profile.availableWidthFraction
        val gap = availableWidth * profile.gapFraction
        val barWidth = ((availableWidth - gap * 3f) / 4f).coerceAtLeast(0f)
        val heights = profile.heightFractions.map { safeHeight * it }
        val bars = heights.mapIndexed { index, barHeight ->
            val x = left + index * (barWidth + gap)
            PixelMobileBarGeometry(
                left = x,
                top = bottom - barHeight,
                right = x + barWidth,
                bottom = bottom,
                cornerRadius = min(barWidth * profile.cornerFraction, barHeight / 2f),
            )
        }
        return PixelMobileGlyphGeometry(
            width = safeWidth,
            height = safeHeight,
            bottom = bottom,
            barWidth = barWidth,
            gap = gap,
            bars = bars,
        )
    }

    fun batteryFraction(level: Int?): Float =
        ((level ?: 0).coerceIn(0, 100) / 100f)

    /**
     * Battery geometry keeps the cap visually integrated and reserves an inset fill region. The
     * charging bolt is expressed as body-relative points so it remains centered at every scale.
     */
    fun batteryGlyph(width: Float, height: Float, level: Int?): PixelBatteryGlyphGeometry {
        val safeWidth = width.coerceAtLeast(0.1f)
        val safeHeight = height.coerceAtLeast(0.1f)
        val stroke = min(safeWidth, safeHeight) * 0.095f
        val bodyWidth = safeWidth * 0.88f
        val body = PixelFloatRect(
            left = 0f,
            top = stroke / 2f,
            right = bodyWidth,
            bottom = safeHeight - stroke / 2f,
        )
        val terminalWidth = safeWidth * 0.063f
        val terminalGap = safeWidth * 0.021f
        val terminalHeight = safeHeight * 0.34f
        val terminalLeft = body.right + terminalGap
        val terminal = PixelFloatRect(
            left = terminalLeft,
            top = (safeHeight - terminalHeight) / 2f,
            right = (terminalLeft + terminalWidth).coerceAtMost(safeWidth),
            bottom = (safeHeight + terminalHeight) / 2f,
        )

        val fillInset = maxOf(stroke * 1.55f, safeHeight * 0.14f)
        val fillLeft = body.left + fillInset
        val fillTop = body.top + fillInset
        val fillMaxWidth = (body.width - fillInset * 2f).coerceAtLeast(0f)
        val fillHeight = (body.height - fillInset * 2f).coerceAtLeast(0f)
        val fillWidth = fillMaxWidth * batteryFraction(level)
        val fill = PixelFloatRect(
            left = fillLeft,
            top = fillTop,
            right = fillLeft + fillWidth,
            bottom = fillTop + fillHeight,
        )

        fun boltPoint(xFraction: Float, yFraction: Float) = PixelPoint(
            x = body.left + body.width * xFraction,
            y = body.top + body.height * yFraction,
        )

        return PixelBatteryGlyphGeometry(
            width = safeWidth,
            height = safeHeight,
            body = body,
            terminal = terminal,
            fill = fill,
            fillMaxWidth = fillMaxWidth,
            outlineStroke = stroke,
            bodyCornerRadius = safeHeight * 0.24f,
            terminalCornerRadius = terminal.width / 2f,
            fillCornerRadius = min(fillHeight / 2f, safeHeight * 0.15f),
            bolt = listOf(
                boltPoint(0.56f, 0.16f),
                boltPoint(0.39f, 0.51f),
                boltPoint(0.52f, 0.51f),
                boltPoint(0.43f, 0.82f),
                boltPoint(0.68f, 0.43f),
                boltPoint(0.55f, 0.43f),
            ),
        )
    }
}

internal data class PixelStatusBarOpticalOffsets(
    val networkTypeYDp: Float,
    val mobileYDp: Float,
    val wifiYDp: Float,
    val batteryYDp: Float,
    val percentageYDp: Float,
)

/** Tiny optical corrections only; safe-region placement remains authoritative. */
internal object PixelStatusBarOpticalMetrics {
    fun resolve(scale: Float): PixelStatusBarOpticalOffsets {
        val safe = scale.coerceAtLeast(0.1f)
        return PixelStatusBarOpticalOffsets(
            networkTypeYDp = 0.10f * safe,
            mobileYDp = -0.15f * safe,
            wifiYDp = -0.35f * safe,
            batteryYDp = 0.05f * safe,
            percentageYDp = 0.10f * safe,
        )
    }
}
