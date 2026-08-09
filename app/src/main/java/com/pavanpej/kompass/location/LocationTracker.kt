package com.pavanpej.kompass.location

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val MIN_UPDATE_INTERVAL_MS = 5000L
private const val MIN_UPDATE_DISTANCE_M = 5f

/**
 * Continuous (lifecycle-managed) location updates, for the on-screen lat/long readout and for
 * resolving magnetic declination. Uses plain LocationManager, not Play Services
 * FusedLocationProvider -- see docs/ARCHITECTURE.md for why. Only requires
 * ACCESS_COARSE_LOCATION; GPS_PROVIDER updates are coarsened by the OS without
 * ACCESS_FINE_LOCATION, which is an accepted tradeoff here rather than requesting a second
 * permission.
 */
class LocationTracker(context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _locationFix = MutableStateFlow<LocationFix?>(null)
    val locationFix: StateFlow<LocationFix?> = _locationFix

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _locationFix.value = LocationFix(
                latitude = location.latitude,
                longitude = location.longitude,
                altitudeMeters = location.altitude,
                accuracyMeters = location.accuracy
            )
        }
    }

    fun start() {
        for (provider in listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    locationManager.requestLocationUpdates(
                        provider,
                        MIN_UPDATE_INTERVAL_MS,
                        MIN_UPDATE_DISTANCE_M,
                        listener
                    )
                }
            } catch (e: SecurityException) {
                // Permission not granted -- lat/long simply stays unavailable.
            } catch (e: IllegalArgumentException) {
                // Provider not present on this device.
            }
        }
    }

    fun stop() {
        locationManager.removeUpdates(listener)
    }
}
