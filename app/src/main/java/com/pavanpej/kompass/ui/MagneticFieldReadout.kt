package com.pavanpej.kompass.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pavanpej.kompass.ui.theme.KompassColors
import kotlin.math.roundToInt

/** Raw magnetic field strength -- also a rough proxy for magnetometer interference/quality. */
@Composable
fun MagneticFieldReadout(microTesla: Float, modifier: Modifier = Modifier) {
    Text(
        text = "${microTesla.roundToInt()} µT",
        color = KompassColors.Accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}
