package com.ekoehler.expressivecutout.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WindowPolicyAppearanceParserTest {

    @Test
    fun `parses light status bar appearance from Android 16 policy dump`() {
        val snapshot = WindowPolicyAppearanceParser.parse(
            """
            WINDOW MANAGER POLICY STATE (dumpsys window policy)
              DisplayPolicy
               mLastAppearance=LIGHT_STATUS_BARS
               mLastStatusBarAppearanceRegions=
                AppearanceRegion{LIGHT_STATUS_BARS bounds=[0,0][1080,96]}
            """.trimIndent(),
        )

        assertEquals(StatusBarAppearanceBits.LIGHT_STATUS_BARS, snapshot?.globalAppearance)
        assertEquals(1, snapshot?.regions?.size)
        assertEquals(
            StatusBarAppearanceBits.LIGHT_STATUS_BARS,
            snapshot?.regions?.single()?.appearance,
        )
        assertEquals(0, snapshot?.regions?.single()?.left)
        assertEquals(1080, snapshot?.regions?.single()?.right)
    }

    @Test
    fun `parses dark foreground regions independently`() {
        val snapshot = WindowPolicyAppearanceParser.parse(
            """
            WINDOW MANAGER POLICY STATE (dumpsys window policy)
              DisplayPolicy
               mLastStatusBarAppearanceRegions=
                AppearanceRegion{LIGHT_STATUS_BARS bounds=[0,0][500,100]}
                AppearanceRegion{0 bounds=[500,0][1000,100]}
            """.trimIndent(),
        )

        assertEquals(2, snapshot?.regions?.size)
        assertEquals(
            StatusBarAppearanceBits.LIGHT_STATUS_BARS,
            snapshot?.regions?.get(0)?.appearance,
        )
        assertEquals(0, snapshot?.regions?.get(1)?.appearance)
        assertEquals(500, snapshot?.regions?.get(1)?.left)
        assertEquals(1000, snapshot?.regions?.get(1)?.right)
    }

    @Test
    fun `global appearance without light bit normalizes to zero`() {
        val snapshot = WindowPolicyAppearanceParser.parse(
            """
            WINDOW MANAGER POLICY STATE (dumpsys window policy)
              DisplayPolicy
               mLastAppearance=OPAQUE_STATUS_BARS
               mLastStatusBarAppearanceRegions=
                AppearanceRegion{OPAQUE_STATUS_BARS bounds=[0,0][1080,96]}
            """.trimIndent(),
        )

        assertEquals(0, snapshot?.globalAppearance)
        assertEquals(0, snapshot?.regions?.single()?.appearance)
    }

    @Test
    fun `malformed dump is unavailable instead of inventing appearance`() {
        assertNull(WindowPolicyAppearanceParser.parse("DisplayPolicy without appearance state"))
    }
}
