package com.ekoehler.expressivecutout.core

/**
 * Android 16 Live Update detection kept compileSdk-35 compatible.
 *
 * API 36 exposes Notification.FLAG_PROMOTED_ONGOING (0x00040000) and
 * Notification.EXTRA_REQUEST_PROMOTED_ONGOING ("android.requestPromotedOngoing").
 * Until this project moves compileSdk to 36 we read those stable wire values directly.
 */
object PromotedLiveUpdateClassifier {
    const val FLAG_ONGOING_EVENT_COMPAT = 0x00000002
    const val FLAG_PROMOTED_ONGOING_COMPAT = 0x00040000
    const val EXTRA_REQUEST_PROMOTED_ONGOING_COMPAT = "android.requestPromotedOngoing"

    fun isLiveUpdate(flags: Int, requestedPromotion: Boolean): Boolean {
        val promotedBySystem = flags and FLAG_PROMOTED_ONGOING_COMPAT != 0
        val ongoing = flags and FLAG_ONGOING_EVENT_COMPAT != 0
        return promotedBySystem || (ongoing && requestedPromotion)
    }
}
