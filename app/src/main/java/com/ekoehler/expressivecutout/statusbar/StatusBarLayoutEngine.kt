package com.ekoehler.expressivecutout.statusbar

/** Live island/camera occupancy translated into platform-neutral geometry for the status-bar layer. */
internal data class IslandOccupancy(
    val cutoutBounds: StatusBarRect? = null,
    val collapsedIslandBounds: StatusBarRect? = null,
    val expandedIslandBounds: StatusBarRect? = null,
    val satelliteBounds: StatusBarRect? = null,
)

internal data class StatusBarLayoutInput(
    val displayBounds: StatusBarRect,
    val statusBarBounds: StatusBarRect,
    val occupancy: IslandOccupancy = IslandOccupancy(),
)

internal data class StatusBarLayoutResult(
    val leftContentRegion: StatusBarRect?,
    val rightContentRegion: StatusBarRect?,
    val centralExclusion: StatusBarRect?,
    val reservedRegions: List<StatusBarRect>,
)

/**
 * Pure geometry reducer used by the future renderer.
 *
 * DisplayCutout/WindowInsets and live island bounds are converted to [StatusBarRect] by Android
 * adapters outside this class. Nothing here assumes a device width, density or centered camera.
 */
internal object StatusBarLayoutEngine {

    fun calculate(input: StatusBarLayoutInput): StatusBarLayoutResult {
        val bar = input.statusBarBounds.intersection(input.displayBounds)
            ?: return StatusBarLayoutResult(null, null, null, emptyList())

        val activeIsland =
            input.occupancy.expandedIslandBounds ?: input.occupancy.collapsedIslandBounds

        val centralExclusion = listOfNotNull(
            input.occupancy.cutoutBounds?.intersection(bar),
            activeIsland?.intersection(bar),
        ).reduceOrNull(StatusBarRect::union)

        val initialLeft: StatusBarRect?
        val initialRight: StatusBarRect?
        if (centralExclusion == null) {
            val split = bar.centerX
            initialLeft = StatusBarRect(bar.left, bar.top, split, bar.bottom).takeIf { it.isValid }
            initialRight = StatusBarRect(split, bar.top, bar.right, bar.bottom).takeIf { it.isValid }
        } else {
            initialLeft = StatusBarRect(
                bar.left,
                bar.top,
                centralExclusion.left.coerceIn(bar.left, bar.right),
                bar.bottom,
            ).takeIf { it.isValid }
            initialRight = StatusBarRect(
                centralExclusion.right.coerceIn(bar.left, bar.right),
                bar.top,
                bar.right,
                bar.bottom,
            ).takeIf { it.isValid }
        }

        val reserved = listOfNotNull(input.occupancy.satelliteBounds?.intersection(bar))

        return StatusBarLayoutResult(
            leftContentRegion = trimReservations(initialLeft, reserved),
            rightContentRegion = trimReservations(initialRight, reserved),
            centralExclusion = centralExclusion,
            reservedRegions = reserved,
        )
    }

    /**
     * A side group needs one contiguous lane. If a satellite cuts through it, keep the larger free
     * segment rather than pretending content can flow through the reserved rectangle.
     */
    private fun trimReservations(
        original: StatusBarRect?,
        reservations: List<StatusBarRect>,
    ): StatusBarRect? = reservations.fold(original) { area, reservation ->
        trimReservation(area, reservation)
    }

    private fun trimReservation(
        area: StatusBarRect?,
        reservation: StatusBarRect,
    ): StatusBarRect? {
        area ?: return null
        val occupied = area.intersection(reservation) ?: return area

        val before = StatusBarRect(
            area.left,
            area.top,
            occupied.left,
            area.bottom,
        ).takeIf { it.isValid }
        val after = StatusBarRect(
            occupied.right,
            area.top,
            area.right,
            area.bottom,
        ).takeIf { it.isValid }

        return when {
            before == null -> after
            after == null -> before
            before.width >= after.width -> before
            else -> after
        }
    }
}
