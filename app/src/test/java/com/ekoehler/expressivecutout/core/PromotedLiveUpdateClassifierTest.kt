package com.ekoehler.expressivecutout.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromotedLiveUpdateClassifierTest {
    @Test
    fun promotedFlagIsLiveUpdate() {
        assertTrue(
            PromotedLiveUpdateClassifier.isLiveUpdate(
                PromotedLiveUpdateClassifier.FLAG_PROMOTED_ONGOING_COMPAT,
                requestedPromotion = false,
            ),
        )
    }

    @Test
    fun requestedOngoingIsLiveUpdate() {
        assertTrue(
            PromotedLiveUpdateClassifier.isLiveUpdate(
                PromotedLiveUpdateClassifier.FLAG_ONGOING_EVENT_COMPAT,
                requestedPromotion = true,
            ),
        )
    }

    @Test
    fun plainOngoingIsNotLiveUpdate() {
        assertFalse(
            PromotedLiveUpdateClassifier.isLiveUpdate(
                PromotedLiveUpdateClassifier.FLAG_ONGOING_EVENT_COMPAT,
                requestedPromotion = false,
            ),
        )
    }
}
