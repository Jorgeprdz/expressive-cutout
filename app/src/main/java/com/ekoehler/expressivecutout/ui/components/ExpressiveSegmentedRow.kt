package com.ekoehler.expressivecutout.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** The segmented row's fixed height, and the gap that separates one segment from the next. */
private val SEGMENT_HEIGHT = 40.dp
private val SEGMENT_GAP = 4.dp
private val MIN_READABLE_SEGMENT_WIDTH = 128.dp

/**
 * A Material 3 "expressive" single-choice selector: a rounded container with a filled pill that
 * physically slides to the selected option (spring-animated), rather than cross-fading. Consistent
 * with [ExpressiveNavBar]; used for tabs, corner mode and theme. Purely presentational.
 */
@Composable
fun ExpressiveSegmentedRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    // Indices that read as greyed-out and can't be selected — e.g. an option that's coming soon.
    disabledIndices: Set<Int> = emptySet(),
) {
    val count = options.size.coerceAtLeast(1)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(4.dp)
                .horizontalScroll(rememberScrollState()),
        ) {
            val naturalSegmentWidth = (maxWidth - SEGMENT_GAP * (count - 1)) / count
            val segmentWidth = if (count > 4) {
                maxOf(naturalSegmentWidth, MIN_READABLE_SEGMENT_WIDTH)
            } else {
                naturalSegmentWidth
            }
            val contentWidth = segmentWidth * count + SEGMENT_GAP * (count - 1)
            val indicatorPosition by animateFloatAsState(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "segmentIndicator",
            )

            Box(
                modifier = Modifier
                    .width(contentWidth)
                    .height(SEGMENT_HEIGHT),
            ) {
                // The sliding selected pill, drawn behind the labels.
                Box(
                    modifier = Modifier
                        .offset(x = (segmentWidth + SEGMENT_GAP) * indicatorPosition)
                        .width(segmentWidth)
                        .height(SEGMENT_HEIGHT)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )

                Row(modifier = Modifier.width(contentWidth)) {
                    options.forEachIndexed { index, label ->
                        val selected = index == selectedIndex
                        val disabled = index in disabledIndices
                        val contentColor by animateColorAsState(
                            targetValue = when {
                                disabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                selected -> MaterialTheme.colorScheme.onPrimary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "segmentContent",
                        )
                        Box(
                            modifier = Modifier
                                .width(segmentWidth)
                                .height(SEGMENT_HEIGHT)
                                .selectable(
                                    selected = selected,
                                    enabled = !disabled,
                                    onClick = { onSelect(index) },
                                    role = Role.RadioButton,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                )
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                color = contentColor,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                            )
                        }
                        if (index < count - 1) {
                            androidx.compose.foundation.layout.Spacer(Modifier.width(SEGMENT_GAP))
                        }
                    }
                }
            }
        }
    }
}
