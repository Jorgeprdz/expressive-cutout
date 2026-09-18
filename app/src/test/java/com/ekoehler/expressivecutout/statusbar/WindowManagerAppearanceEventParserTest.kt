package com.ekoehler.expressivecutout.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WindowManagerAppearanceEventParserTest {

    @Test
    fun `parse window manager log line detects light status bars`() {
        val header = WindowManagerAppearanceEventParser.parseTraceLine(
            "09-18 21:00:00.000  1000  2000 D WindowManager: updateSystemBarAttributes: win=Window{abc123 u0 com.test/.Main}",
        )!!
        val regions = WindowManagerAppearanceEventParser.parseTraceLine(
            "09-18 21:00:00.120  1000  2000 D WindowManager: updateSystemBarAttributes: statusBarAprRegions=[LIGHT_STATUS_BARS]",
        )!!

        val event = WindowManagerAppearanceEventParser.appearanceFromTrace(
            current = regions,
            prior = listOf(header),
            focusedWindow = "abc123",
        )

        assertEquals(true, event?.lightStatusBars)
        assertEquals("abc123", event?.sourceWindow)
    }

    @Test
    fun `ignores appearance for non focused window`() {
        val header = WindowManagerAppearanceEventParser.parseTraceLine(
            "09-18 21:00:00.000  1000  2000 D WindowManager: updateSystemBarAttributes: win=Window{abc123 u0 com.test/.Main}",
        )!!
        val regions = WindowManagerAppearanceEventParser.parseTraceLine(
            "09-18 21:00:00.120  1000  2000 D WindowManager: updateSystemBarAttributes: statusBarAprRegions=[LIGHT_STATUS_BARS]",
        )!!

        assertNull(
            WindowManagerAppearanceEventParser.appearanceFromTrace(
                current = regions,
                prior = listOf(header),
                focusedWindow = "def456",
            ),
        )
    }

    @Test
    fun `focused window line is parsed`() {
        assertEquals(
            "def456",
            WindowManagerAppearanceEventParser.focusedWindowFromMessage(
                "Changing focus from Window{abc123 u0 old} to Window{def456 u0 new}",
            ),
        )
    }

    @Test
    fun `snapshot fallback detects light status bars`() {
        val raw = """
            WINDOW MANAGER WINDOWS
              mLastAppearance=LIGHT_STATUS_BARS NAVIGATION_BARS
        """.trimIndent()

        assertEquals(true, WindowManagerAppearanceEventParser.lightStatusBarsFromDumpsysWindow(raw))
    }

    @Test
    fun `missing appearance fails closed to null`() {
        assertNull(WindowManagerAppearanceEventParser.lightStatusBarsFromDumpsysWindow("no appearance here"))
    }
}
