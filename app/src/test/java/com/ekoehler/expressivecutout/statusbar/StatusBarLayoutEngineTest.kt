package com.ekoehler.expressivecutout.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusBarLayoutEngineTest {

    private val portraitDisplay = StatusBarRect(0, 0, 1080, 2400)
    private val portraitStatusBar = StatusBarRect(0, 0, 1080, 100)

    @Test
    fun `no cutout and no island produce valid left and right content regions`() {
        val result = StatusBarLayoutEngine.calculate(
            StatusBarLayoutInput(
                displayBounds = portraitDisplay,
                statusBarBounds = portraitStatusBar,
            ),
        )

        assertNull(result.centralExclusion)
        assertTrue(result.leftContentRegion?.isValid == true)
        assertTrue(result.rightContentRegion?.isValid == true)
        assertEquals(540, result.leftContentRegion?.right)
        assertEquals(540, result.rightContentRegion?.left)
    }

    @Test
    fun `centered camera cutout becomes the central exclusion`() {
        val camera = StatusBarRect(500, 0, 580, 80)

        val result = StatusBarLayoutEngine.calculate(
            StatusBarLayoutInput(
                displayBounds = portraitDisplay,
                statusBarBounds = portraitStatusBar,
                occupancy = IslandOccupancy(cutoutBounds = camera),
            ),
        )

        assertEquals(camera, result.centralExclusion)
    }

    @Test
    fun `collapsed island larger than camera controls exclusion`() {
        val camera = StatusBarRect(510, 0, 570, 80)
        val collapsed = StatusBarRect(430, 0, 650, 92)

        val result = StatusBarLayoutEngine.calculate(
            StatusBarLayoutInput(
                displayBounds = portraitDisplay,
                statusBarBounds = portraitStatusBar,
                occupancy = IslandOccupancy(
                    cutoutBounds = camera,
                    collapsedIslandBounds = collapsed,
                ),
            ),
        )

        assertEquals(collapsed, result.centralExclusion)
    }

    @Test
    fun `expanded island wins over collapsed island bounds`() {
        val collapsed = StatusBarRect(440, 0, 640, 92)
        val expanded = StatusBarRect(260, 0, 820, 100)

        val result = StatusBarLayoutEngine.calculate(
            StatusBarLayoutInput(
                displayBounds = portraitDisplay,
                statusBarBounds = portraitStatusBar,
                occupancy = IslandOccupancy(
                    collapsedIslandBounds = collapsed,
                    expandedIslandBounds = expanded,
                ),
            ),
        )

        assertEquals(expanded, result.centralExclusion)
    }

    @Test
    fun `satellite is explicitly reserved and removed from its side content region`() {
        val collapsed = StatusBarRect(440, 0, 640, 92)
        val satellite = StatusBarRect(680, 8, 760, 84)

        val result = StatusBarLayoutEngine.calculate(
            StatusBarLayoutInput(
                displayBounds = portraitDisplay,
                statusBarBounds = portraitStatusBar,
                occupancy = IslandOccupancy(
                    collapsedIslandBounds = collapsed,
                    satelliteBounds = satellite,
                ),
            ),
        )

        assertEquals(listOf(satellite), result.reservedRegions)
        assertEquals(760, result.rightContentRegion?.left)
    }

    @Test
    fun `collapsing an expanded island restores available side width`() {
        val expanded = StatusBarLayoutEngine.calculate(
            StatusBarLayoutInput(
                displayBounds = portraitDisplay,
                statusBarBounds = portraitStatusBar,
                occupancy = IslandOccupancy(
                    expandedIslandBounds = StatusBarRect(240, 0, 840, 100),
                ),
            ),
        )
        val collapsed = StatusBarLayoutEngine.calculate(
            StatusBarLayoutInput(
                displayBounds = portraitDisplay,
                statusBarBounds = portraitStatusBar,
                occupancy = IslandOccupancy(
                    collapsedIslandBounds = StatusBarRect(440, 0, 640, 92),
                ),
            ),
        )

        val expandedAvailable =
            (expanded.leftContentRegion?.width ?: 0) + (expanded.rightContentRegion?.width ?: 0)
        val collapsedAvailable =
            (collapsed.leftContentRegion?.width ?: 0) + (collapsed.rightContentRegion?.width ?: 0)

        assertTrue(collapsedAvailable > expandedAvailable)
    }

    @Test
    fun `off center cutout uses its real coordinates rather than display midpoint`() {
        val camera = StatusBarRect(180, 0, 260, 80)

        val result = StatusBarLayoutEngine.calculate(
            StatusBarLayoutInput(
                displayBounds = portraitDisplay,
                statusBarBounds = portraitStatusBar,
                occupancy = IslandOccupancy(cutoutBounds = camera),
            ),
        )

        assertEquals(180, result.leftContentRegion?.right)
        assertEquals(260, result.rightContentRegion?.left)
    }

    @Test
    fun `landscape geometry uses supplied bounds without portrait constants`() {
        val display = StatusBarRect(0, 0, 2400, 1080)
        val status = StatusBarRect(0, 0, 2400, 72)
        val camera = StatusBarRect(2050, 0, 2120, 72)

        val result = StatusBarLayoutEngine.calculate(
            StatusBarLayoutInput(
                displayBounds = display,
                statusBarBounds = status,
                occupancy = IslandOccupancy(cutoutBounds = camera),
            ),
        )

        assertEquals(camera, result.centralExclusion)
        assertEquals(2050, result.leftContentRegion?.right)
        assertEquals(2120, result.rightContentRegion?.left)
    }
}
