package com.pavanpej.kompass.location

import android.hardware.GeomagneticField

/**
 * Resolves magnetic declination (the angle between magnetic north and true north) from a
 * [LocationFix]. Takes the fix as a parameter rather than looking up location itself, so it
 * shares the same continuously-updated [LocationTracker] fix used for the on-screen lat/long
 * readout, instead of doing a second, separate LocationManager lookup.
 */
object DeclinationProvider {
    fun declinationFor(fix: LocationFix): Float =
        GeomagneticField(
            fix.latitude.toFloat(),
            fix.longitude.toFloat(),
            fix.altitudeMeters.toFloat(),
            System.currentTimeMillis()
        ).declination
}
