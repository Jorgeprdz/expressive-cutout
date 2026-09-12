package com.ekoehler.expressivecutout.notifications

import android.content.Context
import android.graphics.drawable.Icon
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.IslandEventBus
import com.ekoehler.expressivecutout.overlay.NotificationHeaderResolver

/**
 * Emits semantic-only sample notifications through the real island event bus, proving the smart
 * enrichment layer can create LiveBridge-style state even when a source app publishes no progress.
 */
object SmartTestNotifier {

    /**
     * Emits an order-status notification with no native progress. The smart router should infer the
     * "on the way" stage and the existing island UI should render it as coarse progress.
     */
    fun sendDelivery(context: Context) {
        val appName = NotificationHeaderResolver.resolveAppName(context, context.packageName)
            ?: context.getString(R.string.app_name)
        IslandEventBus.emit(
            CutoutSignal.Notification(
                packageName = context.packageName,
                title = "Tu pedido",
                text = "Repartidor en camino",
                appName = appName,
                smallIcon = Icon.createWithResource(context, R.drawable.ic_stat_island),
            ),
        )
    }
}
