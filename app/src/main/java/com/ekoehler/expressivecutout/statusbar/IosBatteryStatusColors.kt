package com.ekoehler.expressivecutout.statusbar

import androidx.compose.ui.graphics.Color
import com.ekoehler.expressivecutout.data.IosBatteryColorMode

internal object IosBatteryStatusColors {
    val Yellow: Color = Color(0xFFFFD60A)
    val Red: Color = Color(0xFFFF453A)

    fun resolve(
        level: Int?,
        tint: Color,
        mode: IosBatteryColorMode,
    ): Color {
        if (mode == IosBatteryColorMode.MONOCHROME) return tint
        val safeLevel = level?.coerceIn(0, 100) ?: return tint
        return when {
            safeLevel < 10 -> Red
            safeLevel < 20 -> Yellow
            else -> tint
        }
    }
}
