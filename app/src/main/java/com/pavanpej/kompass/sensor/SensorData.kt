package com.pavanpej.kompass.sensor

import android.hardware.SensorManager

data class SensorData(
    val azimuth: Float = 0f,
    val gravityX: Float = 0f,
    val gravityY: Float = 0f,
    val gravityZ: Float = SensorManager.GRAVITY_EARTH,
    val magneticFieldMicroTesla: Float = 0f,
    val barometricAltitudeMeters: Float? = null,
    val accuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE
)
