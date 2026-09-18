package com.ekoehler.expressivecutout.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import com.ekoehler.expressivecutout.data.BatteryPercentageMode
import com.ekoehler.expressivecutout.data.CustomStatusBarAppearancePreference
import com.ekoehler.expressivecutout.data.CustomStatusBarSettings
import com.ekoehler.expressivecutout.data.PixelMobileBarStyle
import com.ekoehler.expressivecutout.statusbar.CustomStatusBarPreviewState
import com.ekoehler.expressivecutout.statusbar.IslandOccupancy
import com.ekoehler.expressivecutout.statusbar.PixelStatusBarLayer
import com.ekoehler.expressivecutout.statusbar.StatusBarForeground
import com.ekoehler.expressivecutout.statusbar.StatusBarLayoutEngine
import com.ekoehler.expressivecutout.statusbar.StatusBarLayoutInput
import com.ekoehler.expressivecutout.statusbar.StatusBarRect
import com.ekoehler.expressivecutout.statusbar.StatusBarStyleRegistry
import com.ekoehler.expressivecutout.ui.AppViewModel
import com.ekoehler.expressivecutout.ui.components.ExpressiveSegmentedRow
import kotlin.math.roundToInt

@Composable
internal fun CustomStatusBarScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val settings by viewModel.customStatusBarSettings.collectAsStateWithLifecycle()
    var previewForeground by rememberSaveable { mutableIntStateOf(0) }
    val statusBarStyles = remember { StatusBarStyleRegistry.allStyles }
    val selectedStyleIndex = statusBarStyles.indexOfFirst { it.id == settings.style }
        .let { if (it >= 0) it else 0 }

    var masterScale by remember(settings.masterScale) { mutableStateOf(settings.masterScale) }
    var clockScale by remember(settings.clockScale) { mutableStateOf(settings.clockScale) }
    var clockX by remember(settings.clockOffsetXDp) { mutableStateOf(settings.clockOffsetXDp) }
    var clockY by remember(settings.clockOffsetYDp) { mutableStateOf(settings.clockOffsetYDp) }
    var systemScale by remember(settings.systemIconsScale) { mutableStateOf(settings.systemIconsScale) }
    var wifiScale by remember(settings.wifiScale) { mutableStateOf(settings.wifiScale) }
    var systemSpacing by remember(settings.systemIconsSpacingDp) { mutableStateOf(settings.systemIconsSpacingDp) }
    var systemX by remember(settings.systemIconsOffsetXDp) { mutableStateOf(settings.systemIconsOffsetXDp) }
    var systemY by remember(settings.systemIconsOffsetYDp) { mutableStateOf(settings.systemIconsOffsetYDp) }
    var batteryScale by remember(settings.batteryScale) { mutableStateOf(settings.batteryScale) }
    var globalY by remember(settings.statusBarOffsetYDp) { mutableStateOf(settings.statusBarOffsetYDp) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PixelStatusBarPreview(
            settings = settings.copy(
                masterScale = masterScale,
                clockScale = clockScale,
                clockOffsetXDp = clockX,
                clockOffsetYDp = clockY,
                systemIconsScale = systemScale,
                wifiScale = wifiScale,
                systemIconsSpacingDp = systemSpacing,
                systemIconsOffsetXDp = systemX,
                systemIconsOffsetYDp = systemY,
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
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.custom_status_bar_title),
            description = stringResource(R.string.custom_status_bar_desc),
            checked = settings.enabled,
            onCheckedChange = viewModel::setCustomStatusBarEnabled,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.custom_status_bar_style_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                ExpressiveSegmentedRow(
                    options = statusBarStyles.map { it.displayName },
                    selectedIndex = selectedStyleIndex,
                    onSelect = { index ->
                        viewModel.setCustomStatusBarSettings(
                            settings.copy(
                                style = statusBarStyles.getOrNull(index)?.id
                                    ?: StatusBarStyleRegistry.defaultStyle.id,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.custom_status_bar_appearance_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                ExpressiveSegmentedRow(
                    options = listOf(
                        stringResource(R.string.custom_status_bar_auto),
                        stringResource(R.string.custom_status_bar_light),
                        stringResource(R.string.custom_status_bar_dark),
                    ),
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
                Text(
                    text = stringResource(R.string.custom_status_bar_auto_known_issue),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SettingsSliderCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.custom_status_bar_master_scale),
            description = stringResource(R.string.custom_status_bar_master_scale_desc),
            valueText = "${(masterScale * 100).roundToInt()}%",
            value = masterScale,
            valueRange = CustomStatusBarSettings.MIN_MASTER_SCALE..CustomStatusBarSettings.MAX_MASTER_SCALE,
            step = 0.05f,
            onValueChange = { masterScale = it },
            onCommit = {
                viewModel.setCustomStatusBarSettings(settings.copy(masterScale = masterScale))
            },
        )

        SectionTitle(stringResource(R.string.custom_status_bar_clock_section))
        SettingsSliderCard(
            RoundedCornerShape(24.dp),
            stringResource(R.string.custom_status_bar_scale),
            stringResource(R.string.custom_status_bar_clock_scale_desc),
            "${(clockScale * 100).roundToInt()}%",
            clockScale,
            CustomStatusBarSettings.MIN_COMPONENT_SCALE..CustomStatusBarSettings.MAX_COMPONENT_SCALE,
            0.05f,
            { clockScale = it },
            { viewModel.setCustomStatusBarSettings(settings.copy(clockScale = clockScale)) },
        )
        SettingsSliderCard(
            RoundedCornerShape(24.dp),
            stringResource(R.string.custom_status_bar_horizontal_offset),
            stringResource(R.string.custom_status_bar_clock_x_desc),
            signedDp(clockX),
            clockX,
            -CustomStatusBarSettings.MAX_OFFSET_DP..CustomStatusBarSettings.MAX_OFFSET_DP,
            1f,
            { clockX = it },
            { viewModel.setCustomStatusBarSettings(settings.copy(clockOffsetXDp = clockX)) },
        )
        SettingsSliderCard(
            RoundedCornerShape(24.dp),
            stringResource(R.string.custom_status_bar_vertical_offset),
            stringResource(R.string.custom_status_bar_clock_y_desc),
            signedDp(clockY),
            clockY,
            -CustomStatusBarSettings.MAX_OFFSET_DP..CustomStatusBarSettings.MAX_OFFSET_DP,
            1f,
            { clockY = it },
            { viewModel.setCustomStatusBarSettings(settings.copy(clockOffsetYDp = clockY)) },
        )

        SectionTitle(stringResource(R.string.custom_status_bar_system_section))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.custom_status_bar_mobile_bar_style),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.custom_status_bar_mobile_bar_style_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ExpressiveSegmentedRow(
                    options = listOf(
                        stringResource(R.string.custom_status_bar_mobile_bar_classic),
                        stringResource(R.string.custom_status_bar_mobile_bar_compact),
                        stringResource(R.string.custom_status_bar_mobile_bar_tall),
                    ),
                    selectedIndex = when (settings.mobileBarStyle) {
                        PixelMobileBarStyle.CLASSIC -> 0
                        PixelMobileBarStyle.COMPACT -> 1
                        PixelMobileBarStyle.TALL -> 2
                    },
                    onSelect = { index ->
                        viewModel.setCustomStatusBarSettings(
                            settings.copy(
                                mobileBarStyle = when (index) {
                                    1 -> PixelMobileBarStyle.COMPACT
                                    2 -> PixelMobileBarStyle.TALL
                                    else -> PixelMobileBarStyle.CLASSIC
                                },
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        SettingsSliderCard(
            RoundedCornerShape(24.dp),
            stringResource(R.string.custom_status_bar_scale),
            stringResource(R.string.custom_status_bar_system_scale_desc),
            "${(systemScale * 100).roundToInt()}%",
            systemScale,
            CustomStatusBarSettings.MIN_COMPONENT_SCALE..CustomStatusBarSettings.MAX_COMPONENT_SCALE,
            0.05f,
            { systemScale = it },
            { viewModel.setCustomStatusBarSettings(settings.copy(systemIconsScale = systemScale)) },
        )
        SettingsSliderCard(
            RoundedCornerShape(24.dp),
            stringResource(R.string.custom_status_bar_wifi_scale),
            stringResource(R.string.custom_status_bar_wifi_scale_desc),
            "${(wifiScale * 100).roundToInt()}%",
            wifiScale,
            CustomStatusBarSettings.MIN_COMPONENT_SCALE..CustomStatusBarSettings.MAX_COMPONENT_SCALE,
            0.05f,
            { wifiScale = it },
            { viewModel.setCustomStatusBarSettings(settings.copy(wifiScale = wifiScale)) },
        )
        SettingsSliderCard(
            RoundedCornerShape(24.dp),
            stringResource(R.string.custom_status_bar_spacing),
            stringResource(R.string.custom_status_bar_spacing_desc),
            "${systemSpacing.roundToInt()} dp",
            systemSpacing,
            CustomStatusBarSettings.MIN_SPACING_DP..CustomStatusBarSettings.MAX_SPACING_DP,
            1f,
            { systemSpacing = it },
            { viewModel.setCustomStatusBarSettings(settings.copy(systemIconsSpacingDp = systemSpacing)) },
        )
        SettingsSliderCard(
            RoundedCornerShape(24.dp),
            stringResource(R.string.custom_status_bar_horizontal_offset),
            stringResource(R.string.custom_status_bar_system_x_desc),
            signedDp(systemX),
            systemX,
            -CustomStatusBarSettings.MAX_OFFSET_DP..CustomStatusBarSettings.MAX_OFFSET_DP,
            1f,
            { systemX = it },
            { viewModel.setCustomStatusBarSettings(settings.copy(systemIconsOffsetXDp = systemX)) },
        )
        SettingsSliderCard(
            RoundedCornerShape(24.dp),
            stringResource(R.string.custom_status_bar_vertical_offset),
            stringResource(R.string.custom_status_bar_system_y_desc),
            signedDp(systemY),
            systemY,
            -CustomStatusBarSettings.MAX_OFFSET_DP..CustomStatusBarSettings.MAX_OFFSET_DP,
            1f,
            { systemY = it },
            { viewModel.setCustomStatusBarSettings(settings.copy(systemIconsOffsetYDp = systemY)) },
        )

        SectionTitle(stringResource(R.string.custom_status_bar_battery_section))
        SettingsSliderCard(
            RoundedCornerShape(24.dp),
            stringResource(R.string.custom_status_bar_scale),
            stringResource(R.string.custom_status_bar_battery_scale_desc),
            "${(batteryScale * 100).roundToInt()}%",
            batteryScale,
            CustomStatusBarSettings.MIN_COMPONENT_SCALE..CustomStatusBarSettings.MAX_COMPONENT_SCALE,
            0.05f,
            { batteryScale = it },
            { viewModel.setCustomStatusBarSettings(settings.copy(batteryScale = batteryScale)) },
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.custom_status_bar_battery_percentage),
                    style = MaterialTheme.typography.titleMedium,
                )
                ExpressiveSegmentedRow(
                    options = listOf(
                        stringResource(R.string.custom_status_bar_percentage_off),
                        stringResource(R.string.custom_status_bar_percentage_outside),
                    ),
                    selectedIndex = if (settings.batteryPercentageMode == BatteryPercentageMode.OUTSIDE) 1 else 0,
                    onSelect = { index ->
                        viewModel.setCustomStatusBarSettings(
                            settings.copy(
                                batteryPercentageMode =
                                    if (index == 1) BatteryPercentageMode.OUTSIDE else BatteryPercentageMode.OFF,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SectionTitle(stringResource(R.string.custom_status_bar_position_section))
        SettingsSliderCard(
            RoundedCornerShape(24.dp),
            stringResource(R.string.custom_status_bar_global_y),
            stringResource(R.string.custom_status_bar_global_y_desc),
            signedDp(globalY),
            globalY,
            -CustomStatusBarSettings.MAX_GLOBAL_Y_DP..CustomStatusBarSettings.MAX_GLOBAL_Y_DP,
            1f,
            { globalY = it },
            { viewModel.setCustomStatusBarSettings(settings.copy(statusBarOffsetYDp = globalY)) },
        )

        Button(
            onClick = {
                viewModel.resetCustomStatusBarPixelDefaults()
                val d = settings.withPixelDefaults()
                masterScale = d.masterScale
                clockScale = d.clockScale
                clockX = d.clockOffsetXDp
                clockY = d.clockOffsetYDp
                systemScale = d.systemIconsScale
                wifiScale = d.wifiScale
                systemSpacing = d.systemIconsSpacingDp
                systemX = d.systemIconsOffsetXDp
                systemY = d.systemIconsOffsetYDp
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
private fun PixelStatusBarPreview(
    settings: CustomStatusBarSettings,
    lightForeground: Boolean,
) {
    val foreground = if (lightForeground) StatusBarForeground.LIGHT else StatusBarForeground.DARK
    val background = if (lightForeground) Color(0xFF151515) else Color(0xFFF3F4F7)
    val previewState = remember { CustomStatusBarPreviewState.create() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = background),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(background),
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.roundToPx() }
            val heightPx = with(density) { 60.dp.roundToPx() }
            val exclusionWidth = with(density) { 110.dp.roundToPx() }
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

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 6.dp, start = 4.dp),
    )
}

private fun signedDp(value: Float): String {
    val rounded = value.roundToInt()
    return if (rounded > 0) "+${rounded} dp" else "${rounded} dp"
}
