package com.ekoehler.expressivecutout.core

import com.ekoehler.expressivecutout.service.ProgressData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** Unit coverage for the conservative smart-notification progress heuristics. */
class SmartNotificationEnricherTest {

    /** Explicit transfer percentages become determinate island progress. */
    @Test
    fun downloadPercentageBecomesProgress() {
        val enriched = SmartNotificationEnricher.enrich(
            notification(title = "Descargando archivo", text = "42% completado"),
        )

        assertEquals(100, enriched.progressData?.max)
        assertEquals(42, enriched.progressData?.current)
    }

    /** Battery percentages must remain ordinary notifications rather than fake progress. */
    @Test
    fun batteryPercentageIsNotProgress() {
        val enriched = SmartNotificationEnricher.enrich(
            notification(title = "Batería", text = "85% restante"),
        )

        assertNull(enriched.progressData)
    }

    /** Delivery wording without a numeric percentage still gets a useful coarse stage. */
    @Test
    fun deliveryStatusBecomesSemanticProgress() {
        val enriched = SmartNotificationEnricher.enrich(
            notification(title = "Tu pedido", text = "Repartidor en camino"),
        )

        assertEquals(100, enriched.progressData?.max)
        assertEquals(72, enriched.progressData?.current)
    }

    /** Native source-app progress is authoritative and is never replaced by heuristics. */
    @Test
    fun nativeProgressAlwaysWins() {
        val native = ProgressData(max = 500, current = 125, title = "Native")
        val original = notification(
            title = "Descargando",
            text = "90%",
            progressData = native,
        )

        val enriched = SmartNotificationEnricher.enrich(original)

        assertSame(original, enriched)
        assertSame(native, enriched.progressData)
    }

    /** Creates the smallest notification signal needed by these pure parser tests. */
    private fun notification(
        title: String?,
        text: String?,
        progressData: ProgressData? = null,
    ) = CutoutSignal.Notification(
        packageName = "com.example.source",
        title = title,
        text = text,
        progressData = progressData,
    )
}
