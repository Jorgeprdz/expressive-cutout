package com.ekoehler.expressivecutout.statusbar

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WindowPolicySnapshotReaderTest {

    @Test
    fun `shell transport output is parsed into appearance snapshot`() = runBlocking {
        val reader = WindowPolicySnapshotReader(
            transport = FakeTransport(
                """
                WINDOW MANAGER POLICY STATE (dumpsys window policy)
                  DisplayPolicy
                   mLastAppearance=LIGHT_STATUS_BARS
                   mLastStatusBarAppearanceRegions=
                    AppearanceRegion{LIGHT_STATUS_BARS bounds=[0,0][1080,96]}
                """.trimIndent(),
            ),
        )

        val snapshot = reader.snapshot()

        assertEquals(StatusBarAppearanceBits.LIGHT_STATUS_BARS, snapshot?.globalAppearance)
        assertEquals(1, snapshot?.regions?.size)
    }

    @Test
    fun `unavailable shell transport stays unavailable instead of inventing appearance`() =
        runBlocking {
            val reader = WindowPolicySnapshotReader(
                transport = FakeTransport(null),
            )

            assertNull(reader.snapshot())
        }

    @Test
    fun `malformed shell output stays unavailable`() = runBlocking {
        val reader = WindowPolicySnapshotReader(
            transport = FakeTransport("window policy unavailable"),
        )

        assertNull(reader.snapshot())
    }

    private class FakeTransport(
        private val raw: String?,
    ) : WindowPolicyDumpTransport {
        var calls: Int = 0
            private set

        override suspend fun dumpWindowPolicy(): String? {
            calls += 1
            return raw
        }
    }
}
