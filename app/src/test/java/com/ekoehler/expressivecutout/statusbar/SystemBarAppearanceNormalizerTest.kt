package com.ekoehler.expressivecutout.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemBarAppearanceNormalizerTest {

    @Test
    fun `structured snapshot maps global and region appearance without format assumptions`() {
        val normalized = SystemBarAppearanceNormalizer.normalize(
            SystemBarAppearanceSnapshot(
                globalAppearance = 0,
                regions = listOf(
                    SystemBarAppearanceSnapshot.Region(
                        left = 0,
                        top = 0,
                        right = 500,
                        bottom = 100,
                        appearance = StatusBarAppearanceBits.LIGHT_STATUS_BARS,
                    ),
                ),
            ),
        )

        assertEquals(0, normalized?.globalAppearance)
        assertEquals(
            StatusBarRect(0, 0, 500, 100),
            normalized?.regions?.single()?.bounds,
        )
        assertEquals(
            StatusBarAppearanceBits.LIGHT_STATUS_BARS,
            normalized?.regions?.single()?.appearance,
        )
    }

    @Test
    fun `malformed regions are ignored and empty malformed snapshot becomes unavailable`() {
        val normalized = SystemBarAppearanceNormalizer.normalize(
            SystemBarAppearanceSnapshot(
                regions = listOf(
                    SystemBarAppearanceSnapshot.Region(
                        left = 500,
                        top = 0,
                        right = 100,
                        bottom = 100,
                        appearance = StatusBarAppearanceBits.LIGHT_STATUS_BARS,
                    ),
                    SystemBarAppearanceSnapshot.Region(
                        left = 0,
                        top = 0,
                        right = 100,
                        bottom = 100,
                        appearance = null,
                    ),
                ),
            ),
        )

        assertNull(normalized)
    }

    @Test
    fun `malformed region does not erase a valid global appearance`() {
        val normalized = SystemBarAppearanceNormalizer.normalize(
            SystemBarAppearanceSnapshot(
                globalAppearance = StatusBarAppearanceBits.LIGHT_STATUS_BARS,
                regions = listOf(
                    SystemBarAppearanceSnapshot.Region(
                        left = 10,
                        top = 0,
                        right = 5,
                        bottom = 10,
                        appearance = 0,
                    ),
                ),
            ),
        )

        assertEquals(StatusBarAppearanceBits.LIGHT_STATUS_BARS, normalized?.globalAppearance)
        assertEquals(emptyList<StatusBarAppearanceRegion>(), normalized?.regions)
    }
}
