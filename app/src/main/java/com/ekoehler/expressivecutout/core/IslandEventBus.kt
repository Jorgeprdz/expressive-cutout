package com.ekoehler.expressivecutout.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Process-wide, hot channel that decouples signal producers (which live in their own
 * framework services) from the single consumer that owns the overlay. Buffered and
 * drop-oldest so a burst of events can never block or crash a producer.
 *
 * Notifications pass through [SmartNotificationEnricher] here because this is the one common gate
 * shared by every notification producer before the overlay resolves and renders the signal.
 */
object IslandEventBus {

    private val mutableSignals = MutableSharedFlow<CutoutSignal>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Hot stream consumed by the island overlay. */
    val signals: SharedFlow<CutoutSignal> = mutableSignals

    /**
     * Publishes one signal, enriching notification semantics before they reach the renderer.
     */
    fun emit(signal: CutoutSignal) {
        val routed = when (signal) {
            is CutoutSignal.Notification -> SmartNotificationEnricher.enrich(signal)
            else -> signal
        }
        mutableSignals.tryEmit(routed)
    }
}
