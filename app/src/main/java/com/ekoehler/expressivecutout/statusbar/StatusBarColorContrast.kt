package com.ekoehler.expressivecutout.statusbar

import androidx.compose.ui.graphics.Color

internal fun Color.contrastColor(): Color {
    val luminance = red * 0.299f + green * 0.587f + blue * 0.114f
    return if (luminance > 0.55f) Color.Black else Color.White
}
