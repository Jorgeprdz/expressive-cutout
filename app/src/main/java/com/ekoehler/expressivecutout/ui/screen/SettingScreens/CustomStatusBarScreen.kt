package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.data.CustomStatusBarAppearancePreference
import com.ekoehler.expressivecutout.data.CustomStatusBarSettings
import com.ekoehler.expressivecutout.data.CustomStatusBarStyle
import com.ekoehler.expressivecutout.data.CustomStatusBarStyleUiPolicy
import com.ekoehler.expressivecutout.data.IosBatteryColorMode
import com.ekoehler.expressivecutout.statusbar.CustomStatusBarPreviewState
import com.ekoehler.expressivecutout.statusbar.IslandOccupancy
import com.ekoehler.expressivecutout.statusbar.PixelStatusBarLayer
import com.ekoehler.expressivecutout.statusbar.StatusBarForeground
import com.ekoehler.expressivecutout.statusbar.StatusBarLayoutEngine
import com.ekoehler.expressivecutout.statusbar.StatusBarLayoutInput
import com.ekoehler.expressivecutout.statusbar.StatusBarRect
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ExpressiveSegmentedRow
import kotlin.math.roundToInt

@Composable
internal fun CustomStatusBarScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val rawSettings by viewModel.customStatusBarSettings.collectAsStateWithLifecycle()
    val settings = rawSettings.sanitized()
    var previewForeground by rememberSaveable { mutableIntStateOf(0) }
    val statusBarStyles = remember { CustomStatusBarStyleUiPolicy.visibleStyles }
    val selectedStyleIndex = statusBarStyles.indexOf(settings.style).let { if (it >= 0) it else 0 }

    var masterScale by remember(settings.masterScale) { mutableStateOf(settings.masterScale) }
    var systemScale by remember(settings.systemIconsScale) { mutableStateOf(settings.systemIconsScale) }
    var systemSpacing by remember(settings.systemIconsSpacingDp) { mutableStateOf(settings.systemIconsSpacingDp) }
    var batteryScale by remember(settings.batteryScale) { mutableStateOf(settings.batteryScale) }
    var globalY by remember(settings.statusBarOffsetYDp) { mutableStateOf(settings.statusBarOffsetYDp) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Custom status bar",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Choose a polished status bar style and keep only the controls that matter.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PremiumPreviewCard(
            settings = settings.copy(
                masterScale = masterScale,
                systemIconsScale = systemScale,
                systemIconsSpacingDp = systemSpacing,
                batteryScale = batteryScale,
                statusBarOffsetYDp = globalY,
            ).sanitized(),
            lightForeground = previewForeground == 0,
        )
        ExpressiveSegmentedRow(
            options = listOf(
                stringResource(R.string.custom_status_bar_preview_light),
                stringResource(R.string.custom_status_bar_preview_dark),
            ),
            selectedIndex = previewForeground,
            onSelect = { previewForeground = it },
            modifier = Modifier.fillMaxWidth(),
        )

        SettingsToggleCard(
            shape = RoundedCornerShape(28.dp),
            title = "Enable custom status bar",
            description = "Draw the selected status bar above SystemUI while preserving the island area.",
            checked = settings.enabled,
            onCheckedChange = viewModel::setCustomStatusBarEnabled,
        )

        SettingSectionCard(
            title = "Style",
            description = "Two focused options replace the experimental style pack.",
        ) {
            ExpressiveSegmentedRow(
                options = statusBarStyles.map(CustomStatusBarStyleUiPolicy::displayName),
                selectedIndex = selectedStyleIndex,
                onSelect = { index ->
                    val nextStyle = statusBarStyles.getOrNull(index) ?: CustomStatusBarStyle.IOS_27
                    viewModel.setCustomStatusBarSettings(settings.copy(style = nextStyle))
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = CustomStatusBarStyleUiPolicy.description(settings.style),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingSectionCard(
            title = "Icon appearance",
            description = "Auto follows the real foreground app appearance; Light and Dark are manual overrides.",
        ) {
            ExpressiveSegmentedRow(
                options = listOf("Auto", "Light", "Dark"),
                selectedIndex = when (settings.appearance) {
                    CustomStatusBarAppearancePreference.AUTO -> 0
                    CustomStatusBarAppearancePreference.LIGHT -> 1
                    CustomStatusBarAppearancePreference.DARK -> 2
                },
                onSelect = { index ->
                    viewModel.setCustomStatusBarAppearance(
                        when (index) {
                            1 -> CustomStatusBarAppearancePreference.LIGHT
                            2 -> CustomStatusBarAppearancePreference.DARK
                            else -> CustomStatusBarAppearancePreference.AUTO
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SettingsSliderCard(
            shape = RoundedCornerShape(28.dp),
            title = "Master scale",
            description = "Resize the whole custom bar without changing individual geometry.",
            valueText = "${(masterScale * 100).roundToInt()}%",
            value = masterScale,
            valueRange = CustomStatusBarSettings.MIN_MASTER_SCALE..CustomStatusBarSettings.MAX_MASTER_SCALE,
            step = 0.05f,
            onValueChange = { masterScale = it },
            onCommit = {
                viewModel.setCustomStatusBarSettings(settings.copy(masterScale = masterScale))
            },
        )

        if (CustomStatusBarStyleUiPolicy.showsIosBatteryColor(settings.style)) {
            SettingSectionCard(
                title = "Battery color",
                description = "Black keeps iOS monochrome. Status turns under 20% yellow and under 10% red.",
            ) {
                ExpressiveSegmentedRow(
                    options = listOf("Black", "Status"),
                    selectedIndex = if (settings.iosBatteryColorMode == IosBatteryColorMode.STATUS_COLOR) 1 else 0,
                    onSelect = { index ->
                        viewModel.setCustomStatusBarSettings(
                            settings.copy(
                                iosBatteryColorMode = if (index == 1) {
                                    IosBatteryColorMode.STATUS_COLOR
                                } else {
                                    IosBatteryColorMode.MONOCHROME
                                },
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SettingSectionCard(
            title = "Fine tuning",
            description = "Small adjustments for density, spacing and vertical alignment.",
        ) {
            SettingsSliderCard(
                RoundedCornerShape(22.dp),
                "Icon scale",
                "Resize signal, Wi-Fi and network indicators together.",
                "${(systemScale * 100).roundToInt()}%",
                systemScale,
                CustomStatusBarSettings.MIN_COMPONENT_SCALE..CustomStatusBarSettings.MAX_COMPONENT_SCALE,
                0.05f,
                { systemScale = it },
                { viewModel.setCustomStatusBarSettings(settings.copy(systemIconsScale = systemScale)) },
            )
            SettingsSliderCard(
                RoundedCornerShape(22.dp),
                "Spacing",
                "Adjust the right-side icon rhythm.",
                "${systemSpacing.roundToInt()} dp",
                systemSpacing,
                CustomStatusBarSettings.MIN_SPACING_DP..CustomStatusBarSettings.MAX_SPACING_DP,
                1f,
                { systemSpacing = it },
                { viewModel.setCustomStatusBarSettings(settings.copy(systemIconsSpacingDp = systemSpacing)) },
            )
            SettingsSliderCard(
                RoundedCornerShape(22.dp),
                "Battery scale",
                "Resize only the battery glyph.",
                "${(batteryScale * 100).roundToInt()}%",
                batteryScale,
                CustomStatusBarSettings.MIN_COMPONENT_SCALE..CustomStatusBarSettings.MAX_COMPONENT_SCALE,
                0.05f,
                { batteryScale = it },
                { viewModel.setCustomStatusBarSettings(settings.copy(batteryScale = batteryScale)) },
            )
            SettingsSliderCard(
                RoundedCornerShape(22.dp),
                "Vertical position",
                "Move the full custom bar up or down.",
                signedDp(globalY),
                globalY,
                -CustomStatusBarSettings.MAX_GLOBAL_Y_DP..CustomStatusBarSettings.MAX_GLOBAL_Y_DP,
                1f,
                { globalY = it },
                { viewModel.setCustomStatusBarSettings(settings.copy(statusBarOffsetYDp = globalY)) },
            )
        }

        Button(
            onClick = {
                viewModel.resetCustomStatusBarPixelDefaults()
                val d = settings.withPixelDefaults()
                masterScale = d.masterScale
                systemScale = d.systemIconsScale
                systemSpacing = d.systemIconsSpacingDp
                batteryScale = d.batteryScale
                globalY = d.statusBarOffsetYDp
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = null)
            Text(
                text = stringResource(R.string.custom_status_bar_reset),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun PremiumPreviewCard(
    settings: CustomStatusBarSettings,
    lightForeground: Boolean,
) {
    val foreground = if (lightForeground) StatusBarForeground.LIGHT else StatusBarForeground.DARK
    val background = if (lightForeground) Color(0xFF111317) else Color(0xFFF5F7FA)
    val previewState = remember(settings.style) { CustomStatusBarPreviewState.create() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium,
                color = if (lightForeground) Color.White else Color(0xFF14171C),
            )
            Text(
                text = CustomStatusBarStyleUiPolicy.displayName(settings.style),
                style = MaterialTheme.typography.bodySmall,
                color = if (lightForeground) Color(0xFFB7C0CC) else Color(0xFF667085),
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(background, RoundedCornerShape(22.dp)),
            ) {
                val density = LocalDensity.current
                val widthPx = with(density) { maxWidth.roundToPx() }
                val heightPx = with(density) { 56.dp.roundToPx() }
                val exclusionWidth = with(density) { 112.dp.roundToPx() }
                val center = widthPx / 2
                val layout = StatusBarLayoutEngine.calculate(
                    StatusBarLayoutInput(
                        displayBounds = StatusBarRect(0, 0, widthPx, heightPx),
                        statusBarBounds = StatusBarRect(0, 0, widthPx, heightPx),
                        occupancy = IslandOccupancy(
                            collapsedIslandBounds = StatusBarRect(
                                center - exclusionWidth / 2,
                                0,
                                center + exclusionWidth / 2,
                                heightPx,
                            ),
                        ),
                    ),
                )
                PixelStatusBarLayer(
                    state = previewState,
                    leftForeground = foreground,
                    rightForeground = foreground,
                    layout = layout,
                    settings = settings,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun SettingSectionCard(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

private fun signedDp(value: Float): String {
    val rounded = value.roundToInt()
    return if (rounded > 0) "+${rounded} dp" else "${rounded} dp"
}
