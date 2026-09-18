package com.ekoehler.expressivecutout.statusbar

import com.ekoehler.expressivecutout.data.PixelMobileBarStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelStatusBarGeometryTest {

    @Test
    fun `wifi levels are cumulative and clamped from null through four`() {
        val expected = listOf(
            listOf(0f, 0f, 0f, 0f),
            listOf(0f, 0f, 0f, 0f),
            listOf(1f, 0f, 0f, 0f),
            listOf(1f, 1f, 0f, 0f),
            listOf(1f, 1f, 1f, 0f),
            listOf(1f, 1f, 1f, 1f),
        )
        assertEquals(expected[0], PixelStatusBarGeometry.wifiStrengths(null))
        (-1..4).forEachIndexed { index, level ->
            assertEquals(expected[index], PixelStatusBarGeometry.wifiStrengths(level))
        }
        assertEquals(expected.last(), PixelStatusBarGeometry.wifiStrengths(99))
    }

    @Test
    fun `wifi geometry keeps owned pixel language while staying contained`() {
        val side = 100f
        val g = PixelStatusBarGeometry.wifiGlyph(side)

        assertEquals(50f, g.centerX, 0.001f)
        assertEquals(72f, g.centerY, 0.001f)
        listOf(16f, 30f, 44f).zip(g.radii).forEach { (expected, actual) ->
            assertEquals(expected, actual, 0.001f)
        }
        assertEquals(8.2f, g.strokeWidth, 0.001f)
        assertEquals(220f, g.startAngle, 0.001f)
        assertEquals(100f, g.sweepAngle, 0.001f)
        assertEquals(g.centerX, g.dotX, 0.001f)
        assertEquals(84f, g.dotY, 0.001f)
        assertTrue(g.dotRadius > 0f)
        assertTrue(g.radii.zipWithNext().all { (a, b) -> b > a })
        val innerArcTop = g.centerY - g.radii.first()
        assertTrue("inner arc must sit visibly above the dot", g.dotY - innerArcTop >= side * 0.20f)
        assertTrue("inner arc must remain distinct from the dot", g.dotY - innerArcTop <= side * 0.35f)
    }

    @Test
    fun `wifi geometry scales once rather than scaling stroke twice`() {
        val one = PixelStatusBarGeometry.wifiGlyph(100f)
        val two = PixelStatusBarGeometry.wifiGlyph(200f)

        assertEquals(2f, two.strokeWidth / one.strokeWidth, 0.001f)
        assertEquals(2f, two.dotRadius / one.dotRadius, 0.001f)
        one.radii.indices.forEach { index ->
            assertEquals(2f, two.radii[index] / one.radii[index], 0.001f)
        }
    }

    @Test
    fun `mobile levels are cumulative and clamped`() {
        assertEquals(listOf(0f, 0f, 0f, 0f), PixelStatusBarGeometry.mobileStrengths(null))
        assertEquals(listOf(0f, 0f, 0f, 0f), PixelStatusBarGeometry.mobileStrengths(0))
        assertEquals(listOf(1f, 0f, 0f, 0f), PixelStatusBarGeometry.mobileStrengths(1))
        assertEquals(listOf(1f, 1f, 0f, 0f), PixelStatusBarGeometry.mobileStrengths(2))
        assertEquals(listOf(1f, 1f, 1f, 0f), PixelStatusBarGeometry.mobileStrengths(3))
        assertEquals(listOf(1f, 1f, 1f, 1f), PixelStatusBarGeometry.mobileStrengths(4))
        assertEquals(listOf(1f, 1f, 1f, 1f), PixelStatusBarGeometry.mobileStrengths(50))
    }

    @Test
    fun `all mobile bar styles are four contained ascending bars on one baseline`() {
        PixelMobileBarStyle.entries.forEach { style ->
            val g = PixelStatusBarGeometry.mobileGlyph(100f, 100f, style)

            assertEquals(4, g.bars.size)
            assertTrue(g.gap > 0f)
            assertTrue(g.barWidth > 0f)
            g.bars.forEach { bar ->
                assertEquals(g.bottom, bar.bottom, 0.001f)
                assertEquals(g.barWidth, bar.width, 0.001f)
                assertTrue(bar.left >= 0f)
                assertTrue(bar.right <= g.width)
                assertTrue(bar.top >= 0f)
                assertTrue(bar.cornerRadius > 0f)
                assertTrue(bar.cornerRadius <= bar.width / 2f + 0.001f)
            }
            assertTrue(g.bars.zipWithNext().all { (a, b) -> b.height > a.height })
        }
    }

    @Test
    fun `classic mobile style matches pixel reference rhythm`() {
        val g = PixelStatusBarGeometry.mobileGlyph(100f, 100f, PixelMobileBarStyle.CLASSIC)

        assertEquals(4, g.bars.size)
        assertEquals(88f, g.bottom, 0.001f)
        assertEquals(14f, g.bars.first().left, 0.001f)
        assertEquals(12f, g.barWidth, 0.001f)
        assertEquals(8f, g.gap, 0.001f)
        listOf(24f, 42f, 60f, 78f).zip(g.bars.map { it.height }).forEach { (expected, actual) ->
            assertEquals(expected, actual, 0.001f)
        }
        g.bars.forEach { bar ->
            assertEquals(bar.width / 2f, bar.cornerRadius, 0.001f)
        }
    }

    @Test
    fun `mobile styles expose distinct pixel proportions`() {
        val classic = PixelStatusBarGeometry.mobileGlyph(100f, 100f, PixelMobileBarStyle.CLASSIC)
        val compact = PixelStatusBarGeometry.mobileGlyph(100f, 100f, PixelMobileBarStyle.COMPACT)
        val tall = PixelStatusBarGeometry.mobileGlyph(100f, 100f, PixelMobileBarStyle.TALL)

        assertEquals(86f, classic.bottom, 0.001f)
        assertEquals(87f, compact.bottom, 0.001f)
        assertEquals(88f, tall.bottom, 0.001f)
        assertTrue(compact.gap < classic.gap)
        assertTrue(tall.bars.last().height > classic.bars.last().height)
        assertTrue(classic.bars.first().height < classic.bars.last().height)
    }

    @Test
    fun `mobile intrinsic widths are stable and style specific`() {
        assertEquals(15f, PixelStatusBarGeometry.mobileWidthDp(PixelMobileBarStyle.CLASSIC), 0.001f)
        assertEquals(13.5f, PixelStatusBarGeometry.mobileWidthDp(PixelMobileBarStyle.COMPACT), 0.001f)
        assertEquals(14.5f, PixelStatusBarGeometry.mobileWidthDp(PixelMobileBarStyle.TALL), 0.001f)
    }

    @Test
    fun `battery fill clamps percentage safely`() {
        assertEquals(0f, PixelStatusBarGeometry.batteryFraction(null))
        assertEquals(0f, PixelStatusBarGeometry.batteryFraction(-10))
        assertEquals(0.01f, PixelStatusBarGeometry.batteryFraction(1), 0.0001f)
        assertEquals(0.5f, PixelStatusBarGeometry.batteryFraction(50))
        assertEquals(0.99f, PixelStatusBarGeometry.batteryFraction(99), 0.0001f)
        assertEquals(1f, PixelStatusBarGeometry.batteryFraction(100))
        assertEquals(1f, PixelStatusBarGeometry.batteryFraction(150))
    }

    @Test
    fun `battery geometry keeps terminal fill and bolt inside its bounds`() {
        listOf(null, 0, 1, 50, 99, 100).forEach { level ->
            val g = PixelStatusBarGeometry.batteryGlyph(215f, 115f, level)

            assertTrue(g.body.left >= 0f)
            assertTrue(g.body.top >= 0f)
            assertTrue(g.body.right <= g.width)
            assertTrue(g.body.bottom <= g.height)

            assertTrue(g.terminal.left >= g.body.right)
            assertTrue(g.terminal.right <= g.width)
            assertEquals(g.height / 2f, (g.terminal.top + g.terminal.bottom) / 2f, 0.001f)

            assertTrue(g.fill.left >= g.body.left)
            assertTrue(g.fill.top >= g.body.top)
            assertTrue(g.fill.right <= g.body.right)
            assertTrue(g.fill.bottom <= g.body.bottom)

            g.bolt.forEach { point ->
                assertTrue(point.x in g.body.left..g.body.right)
                assertTrue(point.y in g.body.top..g.body.bottom)
            }
        }
    }

    @Test
    fun `battery zero is empty and full never exceeds fill track`() {
        val zero = PixelStatusBarGeometry.batteryGlyph(215f, 115f, 0)
        val full = PixelStatusBarGeometry.batteryGlyph(215f, 115f, 100)

        assertEquals(0f, zero.fill.width, 0.001f)
        assertEquals(full.fillMaxWidth, full.fill.width, 0.001f)
        assertTrue(full.fill.right <= full.body.right)
    }

    @Test
    fun `inactive alpha matches the owned pixel profile`() {
        assertEquals(0.22f, PixelStatusBarGeometry.INACTIVE_ALPHA, 0.0001f)
    }
}
