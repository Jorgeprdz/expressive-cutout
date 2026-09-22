package com.ekoehler.expressivecutout.statusbar

import com.ekoehler.expressivecutout.data.StatusBarNotificationDisplayMode

internal data class StatusBarNotificationPresentation(
    val entries: List<StatusBarNotificationEntry> = emptyList(),
    val showDot: Boolean = false,
    val showOverflow: Boolean = false,
)

internal object StatusBarNotificationPresentationPolicy {

    fun resolve(
        entries: List<StatusBarNotificationEntry>,
        mode: StatusBarNotificationDisplayMode,
        ownPackageName: String,
        representedNotificationKeys: Set<String>,
        availableWidthPx: Int,
        iconWidthPx: Int,
        spacingPx: Int,
        overflowWidthPx: Int,
    ): StatusBarNotificationPresentation {
        if (mode == StatusBarNotificationDisplayMode.HIDDEN) {
            return StatusBarNotificationPresentation()
        }

        val eligible = entries.asSequence()
            .filter { it.key.isNotBlank() }
            .filterNot { it.packageName == ownPackageName }
            .filterNot { it.isGroupSummary }
            .filterNot { it.key in representedNotificationKeys }
            .sortedWith(
                compareBy<StatusBarNotificationEntry> { it.rank ?: Int.MAX_VALUE }
                    .thenByDescending { it.postTime },
            )
            .toList()

        if (eligible.isEmpty()) return StatusBarNotificationPresentation()

        val width = availableWidthPx.coerceAtLeast(0)
        val icon = iconWidthPx.coerceAtLeast(1)
        val spacing = spacingPx.coerceAtLeast(0)
        val overflow = overflowWidthPx.coerceAtLeast(1)

        if (mode == StatusBarNotificationDisplayMode.DOT) {
            return StatusBarNotificationPresentation(showDot = width >= overflow)
        }
        val allWidth = eligible.size * icon + (eligible.size - 1).coerceAtLeast(0) * spacing
        if (allWidth <= width) {
            return StatusBarNotificationPresentation(entries = eligible)
        }

        if (width < overflow) return StatusBarNotificationPresentation()

        val usableForIcons = (width - overflow - spacing).coerceAtLeast(0)
        val visibleCount = ((usableForIcons + spacing) / (icon + spacing))
            .coerceAtMost(eligible.size)
        return StatusBarNotificationPresentation(
            entries = eligible.take(visibleCount),
            showOverflow = true,
        )
    }
}
