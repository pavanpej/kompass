package com.pavanpej.kompass

import com.pavanpej.kompass.sensor.AngleMath
import kotlin.math.abs

/**
 * Pure decision logic behind Kompass's haptic feedback: "is the phone level", "is the phone
 * pointing at a cardinal direction", and "is the phone pointing at a locked target bearing".
 * Framework-free (no Vibrator, no SensorManager) so it's unit-testable on the plain JVM.
 */
object FeedbackRules {

    val CARDINAL_HEADINGS = listOf(0, 90, 180, 270)

    fun angularDistance(a: Float, b: Float): Float = abs(AngleMath.shortestDelta(a, b))

    fun nearestCardinal(azimuth: Float): Int =
        CARDINAL_HEADINGS.minByOrNull { angularDistance(it.toFloat(), azimuth) } ?: 0

    fun isOnCardinal(azimuth: Float, thresholdDegrees: Float = 1f): Boolean =
        angularDistance(nearestCardinal(azimuth).toFloat(), azimuth) < thresholdDegrees

    fun isLevel(horizontalTilt: Float, verticalTilt: Float, thresholdDegrees: Float = 1f): Boolean =
        abs(horizontalTilt) < thresholdDegrees && abs(verticalTilt) < thresholdDegrees

    /** Is [azimuth] currently pointing at a locked target [bearing] (any arbitrary angle, not just a cardinal)? */
    fun isOnBearing(azimuth: Float, bearing: Float, thresholdDegrees: Float = 1f): Boolean =
        angularDistance(bearing, azimuth) < thresholdDegrees
}
