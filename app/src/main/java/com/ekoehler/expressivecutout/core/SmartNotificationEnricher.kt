package com.ekoehler.expressivecutout.core

import com.ekoehler.expressivecutout.service.ProgressData
import java.util.Locale

/**
 * Enriches ordinary notifications with LiveBridge-style semantic progress without changing the
 * producer. Native progress published by the source app always wins; text and status heuristics are
 * only a fallback for apps that expose useful state in their title/body but no progress extras.
 */
object SmartNotificationEnricher {

    /**
     * Returns [signal] unchanged when it carries native progress or no trustworthy semantic match;
     * otherwise supplies synthetic [ProgressData] so the existing island progress UI can render it.
     */
    fun enrich(signal: CutoutSignal.Notification): CutoutSignal.Notification {
        if (signal.progressData != null) return signal

        val source = listOfNotNull(signal.title, signal.text)
            .joinToString(" ")
            .trim()
        if (source.isBlank()) return signal

        val normalized = source.lowercase(Locale.ROOT)
        val progress = textProgress(normalized, signal.title)
            ?: semanticStage(normalized, signal.title)
            ?: return signal

        return signal.copy(progressData = progress)
    }

    /**
     * Extracts an explicit percentage only when nearby wording makes it look like real progress,
     * avoiding false positives such as battery percentages, discounts, finance figures or scores.
     */
    private fun textProgress(text: String, title: String?): ProgressData? {
        if (!PROGRESS_CONTEXT.containsMatchIn(text)) return null
        if (PROGRESS_EXCLUSION.containsMatchIn(text)) return null

        val percent = PERCENT.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.coerceIn(0, 100)
            ?: return null

        return ProgressData(
            max = 100,
            current = percent,
            title = title,
        )
    }

    /**
     * Converts common delivery, food-order and ride states into coarse progress. The values are not
     * ETAs; they only give the island a stable visual stage that can move forward as wording changes.
     */
    private fun semanticStage(text: String, title: String?): ProgressData? {
        if (!SEMANTIC_CONTEXT.containsMatchIn(text)) return null
        val stage = STATUS_STAGES.firstOrNull { it.pattern.containsMatchIn(text) } ?: return null
        return ProgressData(
            max = 100,
            current = stage.progress,
            title = title,
        )
    }

    /** A semantic stage and its coarse visual progress position. */
    private data class StatusStage(
        val progress: Int,
        val pattern: Regex,
    )

    /** A standalone percentage from 0 through 100. */
    private val PERCENT = Regex("(?<!\\d)(100|[1-9]?\\d)\\s?%")

    /** Wording that makes a percentage likely to represent operation progress. */
    private val PROGRESS_CONTEXT = Regex(
        "\\b(download(?:ing)?|upload(?:ing)?|install(?:ing|ation)?|sync(?:ing)?|backup|restore|" +
            "processing|copying|transfer(?:ring)?|descargando|descarga|subiendo|instalando|" +
            "sincronizando|respaldo|restaurando|procesando|copiando|transfiriendo|progreso|progress)\\b",
        RegexOption.IGNORE_CASE,
    )

    /** Wording that makes a percentage likely to be a value rather than progress. */
    private val PROGRESS_EXCLUSION = Regex(
        "\\b(battery|bater[ií]a|discount|descuento|off|interest|inter[eé]s|score|calificaci[oó]n)\\b",
        RegexOption.IGNORE_CASE,
    )

    /** Context required before generic states such as "complete" can become delivery/ride progress. */
    private val SEMANTIC_CONTEXT = Regex(
        "\\b(order|delivery|deliver|courier|driver|ride|trip|pickup|food|uber|didi|rappi|" +
            "pedido|entrega|repartidor|conductor|viaje|recogida|recolecci[oó]n|comida)\\b",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Ordered most-specific/terminal first so a phrase such as "delivered, order complete" cannot
     * be caught by a weaker earlier state.
     */
    private val STATUS_STAGES = listOf(
        StatusStage(
            100,
            Regex(
                "\\b(delivered|delivery complete|completed|complete|entregado|entrega completada|" +
                    "completado|finalizado|viaje finalizado|trip completed)\\b",
                RegexOption.IGNORE_CASE,
            ),
        ),
        StatusStage(
            92,
            Regex(
                "\\b(arriving|almost there|nearby|approaching|llegando|por llegar|cerca de ti|" +
                    "est[aá] cerca|pr[oó]ximo a llegar)\\b",
                RegexOption.IGNORE_CASE,
            ),
        ),
        StatusStage(
            72,
            Regex(
                "\\b(on the way|out for delivery|driver is on the way|courier is on the way|" +
                    "en camino|sali[oó] a reparto|repartidor en camino|conductor en camino|" +
                    "pedido en camino|viaje en curso)\\b",
                RegexOption.IGNORE_CASE,
            ),
        ),
        StatusStage(
            50,
            Regex(
                "\\b(picked up|pickup complete|collected|recogido|recolectado|pedido recogido|" +
                    "repartidor recogi[oó]|conductor asignado|driver assigned)\\b",
                RegexOption.IGNORE_CASE,
            ),
        ),
        StatusStage(
            28,
            Regex(
                "\\b(preparing|being prepared|preparing your order|confirmed|preparando|" +
                    "preparando tu pedido|pedido confirmado|confirmado|en preparaci[oó]n)\\b",
                RegexOption.IGNORE_CASE,
            ),
        ),
    )
}
