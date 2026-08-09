package com.pavanpej.kompass

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pavanpej.kompass.astronomy.CelestialMath
import com.pavanpej.kompass.location.CoordinateFormat
import com.pavanpej.kompass.location.DeclinationProvider
import com.pavanpej.kompass.location.LocationFix
import com.pavanpej.kompass.location.LocationTracker
import com.pavanpej.kompass.sensor.AngleMath
import com.pavanpej.kompass.sensor.CompassSensorManager
import com.pavanpej.kompass.sensor.LevelMath
import com.pavanpej.kompass.sensor.NorthMode
import com.pavanpej.kompass.sensor.SensorData
import com.pavanpej.kompass.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CompassViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorManager = CompassSensorManager(application)
    val sensorData: StateFlow<SensorData> = sensorManager.sensorData

    private val locationTracker = LocationTracker(application)
    val locationFix: StateFlow<LocationFix?> = locationTracker.locationFix

    private val settingsRepository = SettingsRepository(application)

    private val _northMode = MutableStateFlow(NorthMode.MAGNETIC)
    val northMode: StateFlow<NorthMode> = _northMode.asStateFlow()

    /** The user's saved startup preference, shown/edited on the settings screen. Distinct from [northMode]. */
    val defaultNorthMode: StateFlow<NorthMode> = settingsRepository.defaultNorthMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, NorthMode.MAGNETIC)

    val hapticsEnabled: StateFlow<Boolean> = settingsRepository.hapticsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val coordinateFormat: StateFlow<CoordinateFormat> = settingsRepository.coordinateFormat
        .stateIn(viewModelScope, SharingStarted.Eagerly, CoordinateFormat.DECIMAL)

    private val _declinationDegrees = MutableStateFlow<Float?>(null)
    val declinationDegrees: StateFlow<Float?> = _declinationDegrees.asStateFlow()

    private val _lockedBearing = MutableStateFlow<Float?>(null)
    val lockedBearing: StateFlow<Float?> = _lockedBearing.asStateFlow()

    /** Approximate compass bearings to the sun/moon -- see docs/ARCHITECTURE.md for accuracy caveats. */
    val sunAzimuth: StateFlow<Float?> = locationFix
        .map { fix -> fix?.let { CelestialMath.sunAzimuthDegrees(it.latitude, it.longitude, System.currentTimeMillis()) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val moonAzimuth: StateFlow<Float?> = locationFix
        .map { fix -> fix?.let { CelestialMath.moonAzimuthDegrees(it.latitude, it.longitude, System.currentTimeMillis()) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private var wasLevel = false
    private var lastCardinalHit = -1
    private var wasOnLockedBearing = false

    init {
        sensorManager.start()
        locationTracker.start()
        viewModelScope.launch {
            val startupMode = settingsRepository.defaultNorthMode.first()
            _northMode.value = startupMode
        }
        // Keep declination fresh as location updates arrive, while in true-north mode.
        locationFix.onEach { fix ->
            if (fix != null && _northMode.value == NorthMode.TRUE) {
                _declinationDegrees.value = DeclinationProvider.declinationFor(fix)
            }
        }.launchIn(viewModelScope)
        sensorData.onEach(::checkFeedback).launchIn(viewModelScope)
    }

    /** Call once location permission is confirmed granted; safe to call again to refresh the fix. */
    fun setNorthMode(mode: NorthMode) {
        _northMode.value = mode
        if (mode == NorthMode.TRUE) refreshDeclination()
    }

    /** Persists [mode] as the app's startup default (from the settings screen) and applies it now. */
    fun setDefaultNorthMode(mode: NorthMode) {
        viewModelScope.launch { settingsRepository.setDefaultNorthMode(mode) }
        setNorthMode(mode)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHapticsEnabled(enabled) }
    }

    fun setCoordinateFormat(format: CoordinateFormat) {
        viewModelScope.launch { settingsRepository.setCoordinateFormat(format) }
    }

    /** Re-attempts location registration -- call after location permission is newly granted, since
     *  the initial attempt in [init] silently no-ops if permission wasn't granted yet at that point. */
    fun restartLocationTracking() {
        locationTracker.start()
    }

    fun refreshDeclination() {
        _declinationDegrees.value = locationFix.value?.let { DeclinationProvider.declinationFor(it) }
    }

    /** Locks [currentDisplayedHeading] as a target bearing, or clears the lock if one is already set. */
    fun toggleBearingLock(currentDisplayedHeading: Float) {
        _lockedBearing.value = if (_lockedBearing.value == null) currentDisplayedHeading else null
    }

    private fun checkFeedback(data: SensorData) {
        val horizontalTilt = LevelMath.horizontalTilt(data.gravityX, data.gravityZ)
        val verticalTilt = LevelMath.verticalTilt(data.gravityY, data.gravityZ)
        val isLevel = FeedbackRules.isLevel(horizontalTilt, verticalTilt)
        if (isLevel && !wasLevel) tick()
        wasLevel = isLevel

        // Same north-mode adjustment CompassDial displays, so haptics match what's on screen.
        val declination = _declinationDegrees.value
        val displayedAzimuth = if (_northMode.value == NorthMode.TRUE && declination != null) {
            AngleMath.addDegrees(data.azimuth, declination)
        } else {
            data.azimuth
        }

        val nearestCardinal = FeedbackRules.nearestCardinal(displayedAzimuth)
        if (FeedbackRules.isOnCardinal(displayedAzimuth)) {
            if (lastCardinalHit != nearestCardinal) tick()
            lastCardinalHit = nearestCardinal
        } else {
            lastCardinalHit = -1
        }

        val bearing = _lockedBearing.value
        if (bearing != null) {
            val onBearing = FeedbackRules.isOnBearing(displayedAzimuth, bearing)
            if (onBearing && !wasOnLockedBearing) tick()
            wasOnLockedBearing = onBearing
        } else {
            wasOnLockedBearing = false
        }
    }

    private fun tick() {
        if (!hapticsEnabled.value) return
        vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onCleared() {
        sensorManager.stop()
        locationTracker.stop()
    }
}
