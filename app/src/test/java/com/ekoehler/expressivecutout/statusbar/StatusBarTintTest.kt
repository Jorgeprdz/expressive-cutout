package com.ekoehler.expressivecutout.statusbar

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusBarTintTest {

    @Test
    fun `light foreground maps to white glyphs`() {
        assertEquals(Color.White, StatusBarTint.colorFor(StatusBarForeground.LIGHT))
    }

    @Test
    fun `dark foreground maps to black glyphs`() {
        assertEquals(Color.Black, StatusBarTint.colorFor(StatusBarForeground.DARK))
    }
}
