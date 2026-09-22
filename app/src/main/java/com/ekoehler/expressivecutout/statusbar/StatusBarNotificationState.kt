package com.ekoehler.expressivecutout.statusbar

import android.graphics.drawable.Icon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class StatusBarNotificationEntry(
    val key: String,
    val packageName: String,
    val postTime: Long,
    val smallIcon: Icon? = null,
    val rank: Int? = null,
    val isClearable: Boolean = true,
    val isOngoing: Boolean = false,
    val isGroupSummary: Boolean = false,
    val isSensitive: Boolean = false,
)

/** Process-local mirror of currently active notifications used only by the custom status bar. */
internal object StatusBarNotificationStore {
    private val _entries = MutableStateFlow<List<StatusBarNotificationEntry>>(emptyList())
    val entries: StateFlow<List<StatusBarNotificationEntry>> = _entries.asStateFlow()

    fun replaceAll(entries: List<StatusBarNotificationEntry>) {
        _entries.value = entries.distinctBy { it.key }
    }

    fun upsert(entry: StatusBarNotificationEntry) {
        if (entry.key.isBlank()) return
        _entries.update { current ->
            val index = current.indexOfFirst { it.key == entry.key }
            if (index < 0) current + entry
            else current.toMutableList().apply { this[index] = entry }
        }
    }

    fun remove(key: String) {
        if (key.isBlank()) return
        _entries.update { current -> current.filterNot { it.key == key } }
    }

    fun updateRanks(ranks: Map<String, Int>) {
        _entries.update { current ->
            current.map { entry -> entry.copy(rank = ranks[entry.key]) }
        }
    }

    fun clear() {
        _entries.value = emptyList()
    }
}
