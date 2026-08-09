package com.pavanpej.kompass.sensor

import android.hardware.SensorManager

/**
 * Barometric altitude from atmospheric pressure. Delegates to the platform's own
 * `SensorManager.getAltitude()` rather than reimplementing the barometric formula by hand --
 * Android already provides this exact calculation, correctly, so duplicating it would just be
 * a second place for a transcription bug to hide. See AGENTS.md's "don't reinvent the wheel"
 * convention: this is not unit-testable on the plain JVM as a result (the platform method
 * throws outside an Android runtime), and that's an accepted tradeoff -- see docs/TESTING.md's
 * "Not covered, and why" table.
 *
 * Uses the standard atmosphere reference pressure by default, which gives altitude *relative
 * to standard conditions*, not corrected for the actual local sea-level pressure of the day --
 * it will drift with weather. Pass a locally-observed sea-level pressure for better accuracy
 * if one is ever available.
 */
object AltitudeMath {
    fun fromPressure(
        pressureHpa: Float,
        seaLevelPressureHpa: Float = SensorManager.PRESSURE_STANDARD_ATMOSPHERE
    ): Float = SensorManager.getAltitude(seaLevelPressureHpa, pressureHpa)
}
