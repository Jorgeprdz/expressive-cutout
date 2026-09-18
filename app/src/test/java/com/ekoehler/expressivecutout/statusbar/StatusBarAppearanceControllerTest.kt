package com.ekoehler.expressivecutout.statusbar

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusBarAppearanceControllerTest {

    @Test
    fun `real snapshot resolves with real system provenance`() = runBlocking {
        val source = FakeSource(
            snapshots = ArrayDeque<SystemBarAppearanceSnapshot?>(
                listOf(
                    SystemBarAppearanceSnapshot(
                        globalAppearance = StatusBarAppearanceBits.LIGHT_STATUS_BARS,
                    ),
                ),
            ),
        )
        val controller = StatusBarAppearanceController(source)

        controller.reconcile()

        val resolution = StatusBarAppearanceResolver.resolveDetailed(
            mode = StatusBarAppearanceMode.AUTO,
            state = controller.state.value,
            x = 100,
            y = 10,
            systemTheme = StatusBarSystemTheme.DARK,
        )
        assertEquals(StatusBarForeground.DARK, resolution.foreground)
        assertEquals(StatusBarAppearanceProvenance.REAL_SYSTEM, resolution.provenance)
    }

    @Test
    fun `null snapshot resolves with theme fallback provenance`() = runBlocking {
        val source = FakeSource(
            snapshots = ArrayDeque<SystemBarAppearanceSnapshot?>(listOf(null)),
        )
        val controller = StatusBarAppearanceController(source)

        controller.reconcile()

        val resolution = StatusBarAppearanceResolver.resolveDetailed(
            mode = StatusBarAppearanceMode.AUTO,
            state = controller.state.value,
            x = 100,
            y = 10,
            systemTheme = StatusBarSystemTheme.DARK,
        )
        assertEquals(StatusBarForeground.LIGHT, resolution.foreground)
        assertEquals(StatusBarAppearanceProvenance.THEME_FALLBACK, resolution.provenance)
    }

    @Test
    fun `manual override reports manual provenance even when real state exists`() {
        val resolution = StatusBarAppearanceResolver.resolveDetailed(
            mode = StatusBarAppearanceMode.FORCE_LIGHT_FOREGROUND,
            state = StatusBarAppearanceState(
                globalAppearance = StatusBarAppearanceBits.LIGHT_STATUS_BARS,
            ),
            x = 100,
            y = 10,
            systemTheme = StatusBarSystemTheme.LIGHT,
        )

        assertEquals(StatusBarForeground.LIGHT, resolution.foreground)
        assertEquals(StatusBarAppearanceProvenance.MANUAL, resolution.provenance)
    }

    @Test
    fun `reconcile performs exactly one source snapshot per call`() = runBlocking {
        val source = FakeSource(
            snapshots = ArrayDeque<SystemBarAppearanceSnapshot?>(
                listOf(
                    SystemBarAppearanceSnapshot(globalAppearance = 0),
                ),
            ),
        )
        val controller = StatusBarAppearanceController(source)

        controller.reconcile()

        assertEquals(1, source.snapshotCalls)
    }

    @Test
    fun `repeated identical snapshot does not emit duplicate state`() = runBlocking {
        val same = SystemBarAppearanceSnapshot(
            globalAppearance = StatusBarAppearanceBits.LIGHT_STATUS_BARS,
        )
        val source = FakeSource(
            snapshots = ArrayDeque<SystemBarAppearanceSnapshot?>(listOf(same, same)),
        )
        val controller = StatusBarAppearanceController(source)
        val emissions = mutableListOf<StatusBarAppearanceState?>()
        val collector: Job = launch {
            controller.state.collect { emissions += it }
        }
        yield()

        controller.reconcile()
        yield()
        controller.reconcile()
        yield()
        collector.cancel()

        // Initial null + one normalized state. MutableStateFlow must suppress the equal second state.
        assertEquals(2, emissions.size)
        assertEquals(2, source.snapshotCalls)
    }

    private class FakeSource(
        private val snapshots: ArrayDeque<SystemBarAppearanceSnapshot?>,
    ) : SystemBarAppearanceSource {
        override val changes: Flow<SystemBarAppearanceSnapshot> = emptyFlow()
        var snapshotCalls: Int = 0
            private set

        override suspend fun snapshot(): SystemBarAppearanceSnapshot? {
            snapshotCalls += 1
            return if (snapshots.isEmpty()) null else snapshots.removeFirst()
        }
    }
}
