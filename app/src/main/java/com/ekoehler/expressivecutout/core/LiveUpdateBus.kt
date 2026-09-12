package com.ekoehler.expressivecutout.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks Android 16 promoted ongoing notifications that are currently alive.
 * The notification listener owns this state; the overlay only observes it so
 * a Live Update pill stays pinned until the source notification actually ends.
 */
object LiveUpdateBus {
    private val _activeKeys = MutableStateFlow<Set<String>>(emptySet())
    val activeKeys: StateFlow<Set<String>> = _activeKeys.asStateFlow()

    fun posted(key: String) {
        _activeKeys.value = _activeKeys.value + key
    }

    fun removed(key: String) {
        _activeKeys.value = _activeKeys.value - key
    }

    fun replace(keys: Set<String>) {
        _activeKeys.value = keys
    }
}
