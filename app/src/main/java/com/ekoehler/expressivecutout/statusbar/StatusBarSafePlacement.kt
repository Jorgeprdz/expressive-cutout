package com.ekoehler.expressivecutout.statusbar

/**
 * Pure placement helper that keeps user-tunable offsets inside the safe regions already produced
 * by StatusBarLayoutEngine. Units are arbitrary integer pixels so JVM tests need no Android types.
 */
internal object StatusBarSafePlacement {

    fun placeLeft(
        region: StatusBarRect,
        contentWidth: Int,
        contentHeight: Int,
        requestedOffsetX: Int,
        requestedOffsetY: Int,
        globalOffsetY: Int,
        edgeInset: Int,
    ): StatusBarRect = place(
        region = region,
        contentWidth = contentWidth,
        contentHeight = contentHeight,
        requestedOffsetX = requestedOffsetX,
        requestedOffsetY = requestedOffsetY,
        globalOffsetY = globalOffsetY,
        edgeInset = edgeInset,
        anchorRight = false,
    )

    fun placeRight(
        region: StatusBarRect,
        contentWidth: Int,
        contentHeight: Int,
        requestedOffsetX: Int,
        requestedOffsetY: Int,
        globalOffsetY: Int,
        edgeInset: Int,
    ): StatusBarRect = place(
        region = region,
        contentWidth = contentWidth,
        contentHeight = contentHeight,
        requestedOffsetX = requestedOffsetX,
        requestedOffsetY = requestedOffsetY,
        globalOffsetY = globalOffsetY,
        edgeInset = edgeInset,
        anchorRight = true,
    )

    private fun place(
        region: StatusBarRect,
        contentWidth: Int,
        contentHeight: Int,
        requestedOffsetX: Int,
        requestedOffsetY: Int,
        globalOffsetY: Int,
        edgeInset: Int,
        anchorRight: Boolean,
    ): StatusBarRect {
        val inset = edgeInset.coerceAtLeast(0).coerceAtMost(region.width / 2)
        val availableWidth = (region.width - inset * 2).coerceAtLeast(0)
        val width = contentWidth.coerceAtLeast(0).coerceAtMost(availableWidth)
        val height = contentHeight.coerceAtLeast(0).coerceAtMost(region.height)

        val minLeft = region.left + inset
        val maxLeft = (region.right - inset - width).coerceAtLeast(minLeft)
        val baseLeft = if (anchorRight) maxLeft else minLeft
        val left = (baseLeft + requestedOffsetX).coerceIn(minLeft, maxLeft)

        val minTop = region.top
        val maxTop = (region.bottom - height).coerceAtLeast(minTop)
        val centeredTop = region.top + (region.height - height) / 2
        val top = (centeredTop + requestedOffsetY + globalOffsetY).coerceIn(minTop, maxTop)

        return StatusBarRect(
            left = left,
            top = top,
            right = left + width,
            bottom = top + height,
        )
    }
}
