package com.pavanpej.kompass.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.pavanpej.kompass.ui.theme.KompassColors
import kotlin.math.roundToInt

/**
 * Barometric altitude. Deliberately labeled "approx" -- see docs/ARCHITECTURE.md: this is
 * relative to the standard atmosphere reference pressure, not the actual local sea-level
 * pressure, so it drifts with weather. Experimental/tentative per product decision -- kept as
 * its own small composable so it's trivial to remove later if it doesn't earn its place.
 */
@Composable
fun AltitudeReadout(altitudeMeters: Float, modifier: Modifier = Modifier) {
    Text(
        text = "Altitude ≈ ${altitudeMeters.roundToInt()} m",
        color = KompassColors.OnSurfaceMuted,
        fontSize = 13.sp,
        modifier = modifier
    )
}
