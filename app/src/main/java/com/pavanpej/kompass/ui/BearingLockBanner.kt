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
import com.pavanpej.kompass.ui.theme.KompassColors

/**
 * Shown as an absolutely-positioned overlay (see MainActivity), deliberately NOT part of
 * CompassDial's own Column -- when this lived inside that Column, appearing/disappearing changed
 * the Column's total height, and since the Column centers its content vertically, the whole dial
 * visibly shifted up every time a bearing was locked. An overlay can never do that to its siblings.
 */
@Composable
fun BearingLockBanner(lockedBearing: Float?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = lockedBearing != null) {
        Text(
            text = "LOCKED ${lockedBearing?.toInt() ?: 0}° · tap dial to release",
            color = KompassColors.PrincetonOrange,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = modifier
                .background(color = KompassColors.SurfaceElevated, shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
