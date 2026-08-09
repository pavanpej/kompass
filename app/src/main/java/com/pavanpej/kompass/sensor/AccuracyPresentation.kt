package com.pavanpej.kompass.sensor

import android.hardware.SensorManager

/**
 * Android has no user-facing "calibrate sensors" action -- a rotation-vector sensor is a
 * continuously self-correcting fusion of accelerometer/gyroscope/magnetometer, and the only
 * real remedy for low accuracy is the classic figure-8 wave so the fusion filter sees more
 * magnetic field samples. This just decides when to prompt for that, per Android's own
 * SensorEventListener.onAccuracyChanged guidance (SensorManager.SENSOR_STATUS_* thresholds).
 */
object AccuracyPresentation {
    fun needsCalibration(accuracy: Int): Boolean =
        accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW
}
