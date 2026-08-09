package com.pavanpej.kompass.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pavanpej.kompass.sensor.AccuracyPresentation
import com.pavanpej.kompass.ui.theme.KompassColors

/**
 * There's no user-triggerable "calibrate" action on Android -- the rotation-vector sensor
 * self-corrects continuously. This just follows Android's own guidance for low accuracy
 * (SensorEventListener.onAccuracyChanged): prompt the user to wave the phone in a figure-8.
 */
@Composable
fun AccuracyBanner(accuracy: Int, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = AccuracyPresentation.needsCalibration(accuracy)) {
        Text(
            text = "Low accuracy — move your phone in a figure-8 to recalibrate",
            color = KompassColors.PrincetonOrange,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = modifier
                .background(color = KompassColors.SurfaceElevated, shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}
