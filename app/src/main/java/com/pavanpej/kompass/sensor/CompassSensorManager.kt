package com.pavanpej.kompass.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val SMOOTHING_FACTOR = 0.15f
private const val GRAVITY_SMOOTHING_FACTOR = 0.25f
private const val MAGNETIC_FIELD_SMOOTHING_FACTOR = 0.2f
private const val PRESSURE_SMOOTHING_FACTOR = 0.2f

class CompassSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var smoothedAzimuth: Float? = null

    private var smoothedGravityX = 0f
    private var smoothedGravityY = 0f
    private var smoothedGravityZ = SensorManager.GRAVITY_EARTH

    private var smoothedMagneticField: Float? = null
    private var smoothedPressure: Float? = null

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gravitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        magneticFieldSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        pressureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> handleRotationVector(event)
            Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER -> handleGravity(event)
            Sensor.TYPE_MAGNETIC_FIELD -> handleMagneticField(event)
            Sensor.TYPE_PRESSURE -> handlePressure(event)
        }
    }

    private fun handleRotationVector(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        val rawAzimuth = (Math.toDegrees(orientationAngles[0].toDouble()).toFloat() + 360f) % 360f
        smoothedAzimuth = AngleMath.lowPass(smoothedAzimuth, rawAzimuth, SMOOTHING_FACTOR)

        _sensorData.value = _sensorData.value.copy(azimuth = smoothedAzimuth ?: rawAzimuth)
    }

    private fun handleGravity(event: SensorEvent) {
        smoothedGravityX += GRAVITY_SMOOTHING_FACTOR * (event.values[0] - smoothedGravityX)
        smoothedGravityY += GRAVITY_SMOOTHING_FACTOR * (event.values[1] - smoothedGravityY)
        smoothedGravityZ += GRAVITY_SMOOTHING_FACTOR * (event.values[2] - smoothedGravityZ)

        _sensorData.value = _sensorData.value.copy(
            gravityX = smoothedGravityX,
            gravityY = smoothedGravityY,
            gravityZ = smoothedGravityZ
        )
    }

    private fun handleMagneticField(event: SensorEvent) {
        val magnitude = MagnetometerMath.magnitude(event.values[0], event.values[1], event.values[2])
        val previous = smoothedMagneticField
        smoothedMagneticField = if (previous == null) magnitude else {
            previous + MAGNETIC_FIELD_SMOOTHING_FACTOR * (magnitude - previous)
        }
        _sensorData.value = _sensorData.value.copy(magneticFieldMicroTesla = smoothedMagneticField ?: magnitude)
    }

    private fun handlePressure(event: SensorEvent) {
        val pressureHpa = event.values[0]
        val previous = smoothedPressure
        smoothedPressure = if (previous == null) pressureHpa else {
            previous + PRESSURE_SMOOTHING_FACTOR * (pressureHpa - previous)
        }
        val altitude = AltitudeMath.fromPressure(smoothedPressure ?: pressureHpa)
        _sensorData.value = _sensorData.value.copy(barometricAltitudeMeters = altitude)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            _sensorData.value = _sensorData.value.copy(accuracy = accuracy)
        }
    }
}
