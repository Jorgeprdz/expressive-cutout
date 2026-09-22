package com.ekoehler.expressivecutout.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopAreaRuntimeCoordinatorTest {

    @Test
    fun `truth table keeps modules independent`() {
        assertEquals(
            TopAreaRuntimeState(false, false, false),
            TopAreaRuntimePolicy.derive(false, false),
        )
        assertEquals(
            TopAreaRuntimeState(true, false, true),
            TopAreaRuntimePolicy.derive(true, false),
        )
        assertEquals(
            TopAreaRuntimeState(false, true, true),
            TopAreaRuntimePolicy.derive(false, true),
        )
        assertEquals(
            TopAreaRuntimeState(true, true, true),
            TopAreaRuntimePolicy.derive(true, true),
        )
    }

    @Test
    fun `reconciler ignores repeated states and accepts real transitions`() {
        val reconciler = TopAreaRuntimeReconciler()
        val on = TopAreaRuntimePolicy.derive(true, false)
        val off = TopAreaRuntimePolicy.derive(false, false)

        assertTrue(reconciler.accept(on))
        assertFalse(reconciler.accept(on))
        assertTrue(reconciler.accept(off))
        assertFalse(reconciler.accept(off))
        assertTrue(reconciler.accept(on))
    }
}
