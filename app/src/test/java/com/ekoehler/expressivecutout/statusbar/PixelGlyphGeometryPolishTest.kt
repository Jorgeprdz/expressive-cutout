package com.ekoehler.expressivecutout.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelGlyphGeometryPolishTest {

    @Test
    fun `wifi strengths stay cumulative for null and levels zero through four`() {
        val cases = listOf<Int?>(null, 0, 1, 2, 3, 4)
        cases.forEach { level ->
            val strengths = PixelStatusBarGeometry.wifiStrengths(level)
            assertEquals(4, strengths.size)
            assertTrue(strengths.all { it in 0f..1f })
            val active = level?.coerceIn(0, 4) ?: 0
            assertEquals(List(4) { if (it < active) 1f else 0f }, strengths)
        }
    }

    @Test
    fun `wifi arcs are concentric ordered and contained at nominal size`() {
        val g = PixelStatusBarGeometry.wifiGlyph(side = 15f)

        assertEquals(3, g.radii.size)
        assertTrue(g.radii.zipWithNext().all { (a, b) -> a < b })
        assertEquals(g.centerX, g.dotX, 0.0001f)
        assertTrue(g.strokeWidth > 0f)
        assertTrue(g.dotRadius > 0f)

        val outer = g.radii.last()
        val halfStroke = g.strokeWidth / 2f
        assertTrue(g.centerX - outer - halfStroke >= 0f)
        assertTrue(g.centerX + outer + halfStroke <= g.side)
        assertTrue(g.centerY - outer - halfStroke >= 0f)
        assertTrue(g.centerY + outer + halfStroke <= g.side)
        assertTrue(g.dotY - g.dotRadius >= 0f)
        assertTrue(g.dotY + g.dotRadius <= g.side)
    }

    @Test
    fun `wifi geometry scales once with canvas side`() {
        val small = PixelStatusBarGeometry.wifiGlyph(side = 15f)
        val large = PixelStatusBarGeometry.wifiGlyph(side = 30f)

        assertEquals(small.strokeWidth * 2f, large.strokeWidth, 0.0001f)
        assertEquals(small.radii.last() * 2f, large.radii.last(), 0.0001f)
        assertEquals(small.dotRadius * 2f, large.dotRadius, 0.0001f)
    }

    @Test
    fun `mobile strengths expose exactly four cumulative bars for every level`() {
        val cases = listOf<Int?>(null, 0, 1, 2, 3, 4)
        cases.forEach { level ->
            val strengths = PixelStatusBarGeometry.mobileStrengths(level)
            assertEquals(4, strengths.size)
            assertTrue(strengths.all { it in 0f..1f })
            val active = level?.coerceIn(0, 4) ?: 0
            assertEquals(List(4) { if (it < active) 1f else 0f }, strengths)
        }
    }

    @Test
    fun `mobile bars share baseline grow monotonically and stay contained`() {
        val g = PixelStatusBarGeometry.mobileGlyph(width = 15f, height = 15f)

        assertEquals(4, g.bars.size)
        assertTrue(g.bars.zipWithNext().all { (a, b) -> a.height < b.height })
        assertTrue(g.bars.all { kotlin.math.abs(it.bottom - g.bottom) < 0.0001f })
        assertTrue(g.bars.all { kotlin.math.abs(it.width - g.barWidth) < 0.0001f })
        assertTrue(g.gap > 0f)
        assertTrue(g.bars.first().left >= 0f)
        assertTrue(g.bars.last().right <= g.width)
        assertTrue(g.bars.all { it.top >= 0f && it.bottom <= g.height })
    }

    @Test
    fun `battery geometry keeps body terminal fill and bolt inside bounds`() {
        listOf<Int?>(null, 0, 1, 50, 99, 100).forEach { level ->
            val g = PixelStatusBarGeometry.batteryGlyph(
                width = 21.5f,
                height = 11.5f,
                level = level,
            )

            assertTrue(g.body.left >= 0f)
            assertTrue(g.body.top >= 0f)
            assertTrue(g.body.right <= g.width)
            assertTrue(g.body.bottom <= g.height)
            assertTrue(g.terminal.left >= g.body.right)
            assertTrue(g.terminal.right <= g.width)
            assertTrue(g.terminal.top >= 0f && g.terminal.bottom <= g.height)
            assertTrue(g.fill.left >= g.body.left && g.fill.right <= g.body.right)
            assertTrue(g.fill.top >= g.body.top && g.fill.bottom <= g.body.bottom)
            assertTrue(g.bolt.all { it.x in g.body.left..g.body.right && it.y in g.body.top..g.body.bottom })
        }
    }

    @Test
    fun `battery zero has no fill and full battery uses all available fill width`() {
        val empty = PixelStatusBarGeometry.batteryGlyph(21.5f, 11.5f, 0)
        val full = PixelStatusBarGeometry.batteryGlyph(21.5f, 11.5f, 100)

        assertEquals(0f, empty.fill.width, 0.0001f)
        assertEquals(full.fillMaxWidth, full.fill.width, 0.0001f)
    }

    @Test
    fun `optical offsets remain subtle and scale linearly`() {
        val base = PixelStatusBarOpticalMetrics.resolve(1f)
        val doubled = PixelStatusBarOpticalMetrics.resolve(2f)

        listOf(
            base.networkTypeYDp,
            base.mobileYDp,
            base.wifiYDp,
            base.batteryYDp,
            base.percentageYDp,
        ).forEach { assertTrue(kotlin.math.abs(it) <= 1f) }

        assertEquals(base.mobileYDp * 2f, doubled.mobileYDp, 0.0001f)
        assertEquals(base.wifiYDp * 2f, doubled.wifiYDp, 0.0001f)
        assertEquals(base.batteryYDp * 2f, doubled.batteryYDp, 0.0001f)
    }

    @Test
    fun `text width estimates track actual label length`() {
        val fiveG = PixelStatusBarPresentation.networkTypeWidthDp(StatusBarNetworkType.FIVE_G, 1f)
        val lte = PixelStatusBarPresentation.networkTypeWidthDp(StatusBarNetworkType.LTE, 1f)
        val eightyTwo = PixelStatusBarPresentation.batteryPercentageWidthDp("82%", 1f)
        val hundred = PixelStatusBarPresentation.batteryPercentageWidthDp("100%", 1f)

        assertTrue(fiveG > 0f)
        assertTrue(lte >= fiveG)
        assertTrue(hundred > eightyTwo)
    }
}
