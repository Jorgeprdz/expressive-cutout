package com.ekoehler.expressivecutout.statusbar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ekoehler.expressivecutout.data.StatusBarNotificationDisplayMode

/** Visual-only notification lane. It owns no listeners, settings, gestures or platform state. */
@Composable
internal fun StatusBarNotificationLane(
    entries: List<StatusBarNotificationEntry>,
    mode: StatusBarNotificationDisplayMode,
    representedNotificationKeys: Set<String>,
    foreground: StatusBarForeground,
    bounds: StatusBarRect,
    ownPackageName: String,
    scale: Float = 1f,
    modifier: Modifier = Modifier,
) {
    if (!bounds.isValid || mode == StatusBarNotificationDisplayMode.HIDDEN) return

    val density = LocalDensity.current
    val safeScale = scale.coerceIn(0.75f, 1.4f)
    val iconSize = 16f * safeScale
    val spacing = 4f * safeScale
    val dotSize = 5f * safeScale
    val iconPx = with(density) { iconSize.dp.roundToPx() }
    val spacingPx = with(density) { spacing.dp.roundToPx() }
    val dotPx = with(density) { dotSize.dp.roundToPx() }
    val presentation = StatusBarNotificationPresentationPolicy.resolve(
        entries = entries,
        mode = mode,
        ownPackageName = ownPackageName,
        representedNotificationKeys = representedNotificationKeys,
        availableWidthPx = bounds.width,
        iconWidthPx = iconPx,
        spacingPx = spacingPx,
        overflowWidthPx = dotPx,
    )
    if (!presentation.showDot && !presentation.showOverflow && presentation.entries.isEmpty()) return

    val tint = StatusBarTint.colorFor(foreground)
    Row(
        modifier = modifier
            .offset { IntOffset(bounds.left, bounds.top) }
            .width(with(density) { bounds.width.toDp() })
            .clipToBounds(),
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (presentation.showDot) {
            NotificationDot(sizeDp = dotSize)
        } else {
            presentation.entries.forEach { entry ->
                StatusBarNotificationGlyph(
                    entry = entry,
                    sizeDp = iconSize,
                    tint = tint,
                )
            }
            if (presentation.showOverflow) {
                NotificationDot(sizeDp = dotSize)
            }
        }
    }
}

@Composable
private fun StatusBarNotificationGlyph(
    entry: StatusBarNotificationEntry,
    sizeDp: Float,
    tint: androidx.compose.ui.graphics.Color,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val px = with(density) { sizeDp.dp.roundToPx().coerceAtLeast(1) }
    val bitmap = remember(entry.key, entry.smallIcon, px) {
        runCatching {
            entry.smallIcon
                ?.loadDrawable(context)
                ?.toBitmap(width = px, height = px)
                ?.asImageBitmap()
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(sizeDp.dp),
        )
    } else {
        Icon(
            imageVector = Icons.Rounded.Notifications,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(sizeDp.dp),
        )
    }
}

@Composable
private fun NotificationDot(sizeDp: Float) {
    val tint = StatusBarTint.colorFor(StatusBarForeground.LIGHT)
    // Tint is overridden by the parent through LocalContentColor-independent Canvas? No: receive it
    // explicitly at call sites would duplicate the primitive, so draw using a neutral alpha mask.
    Box(modifier = Modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            drawCircle(color = tint)
        }
    }
}
