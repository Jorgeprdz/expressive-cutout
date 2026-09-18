package com.ekoehler.expressivecutout.statusbar

/** Android-free integer rectangle used by status-bar policies and JVM tests. */
internal data class StatusBarRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
    val centerX: Int get() = left + width / 2
    val centerY: Int get() = top + height / 2
    val isValid: Boolean get() = right > left && bottom > top

    fun contains(x: Int, y: Int): Boolean =
        isValid && x >= left && x < right && y >= top && y < bottom

    fun intersects(other: StatusBarRect): Boolean =
        isValid && other.isValid &&
            left < other.right && other.left < right &&
            top < other.bottom && other.top < bottom

    fun intersection(other: StatusBarRect): StatusBarRect? {
        val result = StatusBarRect(
            left = maxOf(left, other.left),
            top = maxOf(top, other.top),
            right = minOf(right, other.right),
            bottom = minOf(bottom, other.bottom),
        )
        return result.takeIf { it.isValid }
    }

    fun union(other: StatusBarRect): StatusBarRect = StatusBarRect(
        left = minOf(left, other.left),
        top = minOf(top, other.top),
        right = maxOf(right, other.right),
        bottom = maxOf(bottom, other.bottom),
    )
}
