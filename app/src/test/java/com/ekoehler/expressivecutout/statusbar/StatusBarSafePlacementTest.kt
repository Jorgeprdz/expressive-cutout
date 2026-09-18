package com.ekoehler.expressivecutout.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StatusBarSafePlacementTest {

    @Test
    fun `clock positive offset cannot cross central exclusion`() {
        val region = StatusBarRect(0, 0, 400, 100)
        val placed = StatusBarSafePlacement.placeLeft(
            region = region,
            contentWidth = 100,
            contentHeight = 40,
            requestedOffsetX = 500,
            requestedOffsetY = 0,
            globalOffsetY = 0,
            edgeInset = 8,
        )

        assertEquals(400 - 8, placed.right)
    }

    @Test
    fun `right group negative offset cannot leave display edge`() {
        val region = StatusBarRect(600, 0, 1000, 100)
        val placed = StatusBarSafePlacement.placeRight(
            region = region,
            contentWidth = 180,
            contentHeight = 40,
            requestedOffsetX = -500,
            requestedOffsetY = 0,
            globalOffsetY = 0,
            edgeInset = 8,
        )

        assertEquals(600 + 8, placed.left)
    }

    @Test
    fun `vertical offset clamps content to status bar bounds`() {
        val region = StatusBarRect(0, 0, 400, 80)
        val down = StatusBarSafePlacement.placeLeft(region, 100, 40, 0, 100, 100, 0)
        val up = StatusBarSafePlacement.placeLeft(region, 100, 40, 0, -100, -100, 0)

        assertEquals(80, down.bottom)
        assertEquals(0, up.top)
    }

    @Test
    fun `expanded island safe regions still win over user offsets`() {
        val layout = StatusBarLayoutEngine.calculate(
            StatusBarLayoutInput(
                displayBounds = StatusBarRect(0, 0, 1000, 2000),
                statusBarBounds = StatusBarRect(0, 0, 1000, 100),
                occupancy = IslandOccupancy(
                    expandedIslandBounds = StatusBarRect(300, 0, 700, 100),
                ),
            ),
        )
        val left = StatusBarSafePlacement.placeLeft(
            layout.leftContentRegion!!, 120, 40, 500, 0, 0, 8,
        )
        val right = StatusBarSafePlacement.placeRight(
            layout.rightContentRegion!!, 160, 40, -500, 0, 0, 8,
        )

        assertFalse(left.intersects(layout.centralExclusion!!))
        assertFalse(right.intersects(layout.centralExclusion!!))
    }

    @Test
    fun `satellite trimmed region remains authoritative`() {
        val layout = StatusBarLayoutEngine.calculate(
            StatusBarLayoutInput(
                displayBounds = StatusBarRect(0, 0, 1000, 2000),
                statusBarBounds = StatusBarRect(0, 0, 1000, 100),
                occupancy = IslandOccupancy(
                    collapsedIslandBounds = StatusBarRect(450, 0, 550, 100),
                    satelliteBounds = StatusBarRect(780, 0, 850, 100),
                ),
            ),
        )
        val placed = StatusBarSafePlacement.placeRight(
            layout.rightContentRegion!!, 100, 40, 500, 0, 0, 8,
        )

        layout.reservedRegions.forEach { reserved ->
            assertFalse(placed.intersects(reserved))
        }
    }
}
