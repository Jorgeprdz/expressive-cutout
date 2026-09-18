package com.ekoehler.expressivecutout.statusbar

import androidx.compose.ui.graphics.Color

internal object StatusBarTint {
    fun colorFor(foreground: StatusBarForeground): Color = when (foreground) {
        StatusBarForeground.LIGHT -> Color.White
        StatusBarForeground.DARK -> Color.Black
    }
}
