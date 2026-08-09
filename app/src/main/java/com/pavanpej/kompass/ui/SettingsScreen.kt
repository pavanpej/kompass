package com.pavanpej.kompass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pavanpej.kompass.location.CoordinateFormat
import com.pavanpej.kompass.sensor.NorthMode
import com.pavanpej.kompass.ui.theme.KompassColors

/** Grouped-list settings screen, following standard Material list conventions (section label + rounded card group). */
@Composable
fun SettingsScreen(
    hapticsEnabled: Boolean,
    onHapticsEnabledChange: (Boolean) -> Unit,
    defaultNorthMode: NorthMode,
    onDefaultNorthModeChange: (NorthMode) -> Unit,
    coordinateFormat: CoordinateFormat,
    onCoordinateFormatChange: (CoordinateFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SettingsSectionLabel("PREFERENCES")

        SettingsGroup {
            SettingsListItem(label = "Haptic feedback") {
                Switch(
                    checked = hapticsEnabled,
                    onCheckedChange = onHapticsEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = KompassColors.PrincetonOrange,
                        uncheckedThumbColor = KompassColors.OnSurfaceMuted,
                        uncheckedTrackColor = KompassColors.SurfaceElevated
                    )
                )
            }
            SettingsDivider()
            SettingsListItem(label = "Default north on launch") {
                SegmentedToggle(
                    options = listOf(NorthMode.MAGNETIC, NorthMode.TRUE),
                    selected = defaultNorthMode,
                    onSelect = onDefaultNorthModeChange,
                    label = { if (it == NorthMode.MAGNETIC) "MAGNETIC" else "TRUE" }
                )
            }
            SettingsDivider()
            SettingsListItem(label = "Coordinate format") {
                SegmentedToggle(
                    options = listOf(CoordinateFormat.DECIMAL, CoordinateFormat.DMS),
                    selected = coordinateFormat,
                    onSelect = onCoordinateFormatChange,
                    label = { if (it == CoordinateFormat.DECIMAL) "DECIMAL" else "DMS" }
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        color = KompassColors.OnSurfaceMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = KompassColors.SurfaceElevated, shape = RoundedCornerShape(16.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = KompassColors.OnSurfaceFaint, modifier = Modifier.padding(start = 16.dp))
}

@Composable
private fun SettingsListItem(label: String, trailing: @Composable () -> Unit) {
    ListItem(
        headlineContent = {
            Text(text = label, color = KompassColors.OnSurface, fontSize = 15.sp)
        },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
