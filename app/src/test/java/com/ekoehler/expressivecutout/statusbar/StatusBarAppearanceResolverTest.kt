package com.ekoehler.expressivecutout.statusbar

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusBarAppearanceResolverTest {

    @Test
    fun `light status bars appearance requests dark foreground`() {
        val state = StatusBarAppearanceState(
            globalAppearance = StatusBarAppearanceBits.LIGHT_STATUS_BARS,
        )

        assertEquals(
            StatusBarForeground.DARK,
            StatusBarAppearanceResolver.resolve(
                mode = StatusBarAppearanceMode.AUTO,
                state = state,
                x = 100,
                y = 10,
                systemTheme = StatusBarSystemTheme.DARK,
            ),
        )
    }

    @Test
    fun `absence of light status bars bit requests light foreground`() {
        val state = StatusBarAppearanceState(globalAppearance = 0)

        assertEquals(
            StatusBarForeground.LIGHT,
            StatusBarAppearanceResolver.resolve(
                mode = StatusBarAppearanceMode.AUTO,
                state = state,
                x = 100,
                y = 10,
                systemTheme = StatusBarSystemTheme.LIGHT,
            ),
        )
    }

    @Test
    fun `manual foreground override wins over system appearance`() {
        val darkRequested = StatusBarAppearanceState(
            globalAppearance = StatusBarAppearanceBits.LIGHT_STATUS_BARS,
        )

        assertEquals(
            StatusBarForeground.LIGHT,
            StatusBarAppearanceResolver.resolve(
                mode = StatusBarAppearanceMode.FORCE_LIGHT_FOREGROUND,
                state = darkRequested,
                x = 20,
                y = 10,
                systemTheme = StatusBarSystemTheme.LIGHT,
            ),
        )
        assertEquals(
            StatusBarForeground.DARK,
            StatusBarAppearanceResolver.resolve(
                mode = StatusBarAppearanceMode.FORCE_DARK_FOREGROUND,
                state = StatusBarAppearanceState(globalAppearance = 0),
                x = 20,
                y = 10,
                systemTheme = StatusBarSystemTheme.DARK,
            ),
        )
    }

    @Test
    fun `unavailable appearance falls back to system theme`() {
        assertEquals(
            StatusBarForeground.DARK,
            StatusBarAppearanceResolver.resolve(
                mode = StatusBarAppearanceMode.AUTO,
                state = null,
                x = 20,
                y = 10,
                systemTheme = StatusBarSystemTheme.LIGHT,
            ),
        )
        assertEquals(
            StatusBarForeground.LIGHT,
            StatusBarAppearanceResolver.resolve(
                mode = StatusBarAppearanceMode.AUTO,
                state = null,
                x = 20,
                y = 10,
                systemTheme = StatusBarSystemTheme.DARK,
            ),
        )
    }

    @Test
    fun `appearance regions resolve left and right independently`() {
        val state = StatusBarAppearanceState(
            regions = listOf(
                StatusBarAppearanceRegion(
                    bounds = StatusBarRect(0, 0, 500, 100),
                    appearance = StatusBarAppearanceBits.LIGHT_STATUS_BARS,
                ),
                StatusBarAppearanceRegion(
                    bounds = StatusBarRect(500, 0, 1000, 100),
                    appearance = 0,
                ),
            ),
        )

        assertEquals(
            StatusBarForeground.DARK,
            StatusBarAppearanceResolver.resolve(
                StatusBarAppearanceMode.AUTO,
                state,
                x = 100,
                y = 50,
                systemTheme = StatusBarSystemTheme.DARK,
            ),
        )
        assertEquals(
            StatusBarForeground.LIGHT,
            StatusBarAppearanceResolver.resolve(
                StatusBarAppearanceMode.AUTO,
                state,
                x = 900,
                y = 50,
                systemTheme = StatusBarSystemTheme.LIGHT,
            ),
        )
    }

    @Test
    fun `point outside all regions uses theme fallback when global appearance is unknown`() {
        val state = StatusBarAppearanceState(
            regions = listOf(
                StatusBarAppearanceRegion(
                    bounds = StatusBarRect(0, 0, 100, 100),
                    appearance = StatusBarAppearanceBits.LIGHT_STATUS_BARS,
                ),
            ),
        )

        assertEquals(
            StatusBarForeground.LIGHT,
            StatusBarAppearanceResolver.resolve(
                StatusBarAppearanceMode.AUTO,
                state,
                x = 900,
                y = 50,
                systemTheme = StatusBarSystemTheme.DARK,
            ),
        )
    }
}
