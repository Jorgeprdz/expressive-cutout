package com.ekoehler.expressivecutout.statusbar

/** Pure intensity model shared by the Pixel-style Canvas glyphs and JVM tests. */
internal object PixelStatusBarGeometry {
    fun wifiStrengths(level: Int?): List<Float> {
        val active = level?.coerceIn(0, 4) ?: 0
        return List(4) { index -> if (index < active) 1f else 0f }
    }

    fun mobileStrengths(level: Int?): List<Float> {
        val active = level?.coerceIn(0, 4) ?: 0
        return List(4) { index -> if (index < active) 1f else 0f }
    }

    fun batteryFraction(level: Int?): Float =
        ((level ?: 0).coerceIn(0, 100) / 100f)
}
