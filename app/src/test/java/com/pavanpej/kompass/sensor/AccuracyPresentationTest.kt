package com.pavanpej.kompass.sensor

import android.hardware.SensorManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavioural spec for [AccuracyPresentation] -- when Kompass prompts the user to
 * recalibrate (wave the phone in a figure-8), per Android's own accuracy thresholds.
 */
class AccuracyPresentationTest {

    @Test
    fun `given the sensor is unreliable, when checking if calibration is needed, then it is true`() {
        assertTrue(AccuracyPresentation.needsCalibration(SensorManager.SENSOR_STATUS_UNRELIABLE))
    }

    @Test
    fun `given the sensor accuracy is low, when checking if calibration is needed, then it is true`() {
        assertTrue(AccuracyPresentation.needsCalibration(SensorManager.SENSOR_STATUS_ACCURACY_LOW))
    }

    @Test
    fun `given the sensor accuracy is medium, when checking if calibration is needed, then it is false`() {
        assertFalse(AccuracyPresentation.needsCalibration(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM))
    }

    @Test
    fun `given the sensor accuracy is high, when checking if calibration is needed, then it is false`() {
        assertFalse(AccuracyPresentation.needsCalibration(SensorManager.SENSOR_STATUS_ACCURACY_HIGH))
    }
}
