package com.ekoehler.expressivecutout.statusbar

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
        assertEquals(54.5f, g.centerY, 0.001f)
        listOf(19f, 30f, 41f).zip(g.radii).forEach { (expected, actual) ->
            assertEquals(expected, actual, 0.001f)
        }
        assertEquals(8.2f, g.strokeWidth, 0.001f)
        assertEquals(220f, g.startAngle, 0.001f)
        assertEquals(100f, g.sweepAngle, 0.001f)
        assertEquals(g.centerX, g.dotX, 0.001f)
        assertEquals(80f, g.dotY, 0.001f)
        assertTrue(g.dotRadius > 0f)
        assertTrue(g.radii.zipWithNext().all { (a, b) -> b > a })
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
    fun `mobile geometry is four equal-width ascending bars on one baseline`() {
        val g = PixelStatusBarGeometry.mobileGlyph(100f, 100f)

        assertEquals(4, g.bars.size)
        assertEquals(86f, g.bottom, 0.001f)
        assertEquals(13f, g.bars.first().left, 0.001f)
        assertEquals(74f * 0.055f, g.gap, 0.001f)

        g.bars.forEach { bar ->
            assertEquals(g.bottom, bar.bottom, 0.001f)
            assertEquals(g.barWidth, bar.width, 0.001f)
            assertTrue(bar.left >= 0f)
            assertTrue(bar.right <= g.width)
            assertTrue(bar.top >= 0f)
            assertTrue(bar.cornerRadius >= 0f)
        }
        assertTrue(g.bars.zipWithNext().all { (a, b) -> b.height > a.height })
        listOf(20f, 36f, 52f, 68f).zip(g.bars.map { it.height }).forEach { (expected, actual) ->
            assertEquals(expected, actual, 0.001f)
        }
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
