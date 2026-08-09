package com.pavanpej.kompass.astronomy

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Low-precision sun/moon azimuth (compass bearing) from geographic position and UTC time.
 *
 * Sun position follows the widely-used NOAA solar position algorithm (accurate to a small
 * fraction of a degree for the date ranges relevant here). Moon position uses only the dozen or
 * so largest-amplitude periodic terms from Meeus's lunar theory (Astronomical Algorithms, ch.
 * 47) rather than the full ~60-term series -- expect roughly a degree of error, not arcminute
 * precision. Both are far more than sufficient for a compass-dial marker; this is NOT intended
 * for celestial navigation or anything safety-critical.
 */
object CelestialMath {

    private const val DEG_TO_RAD = PI / 180.0
    private const val RAD_TO_DEG = 180.0 / PI

    private fun norm360(deg: Double): Double = ((deg % 360.0) + 360.0) % 360.0

    private fun julianDate(epochMillis: Long): Double = epochMillis / 86400000.0 + 2440587.5

    private fun julianCenturies(jd: Double): Double = (jd - 2451545.0) / 36525.0

    fun sunAzimuthDegrees(latitude: Double, longitude: Double, epochMillis: Long): Float {
        val jd = julianDate(epochMillis)
        val t = julianCenturies(jd)

        val l0 = norm360(280.46646 + t * (36000.76983 + t * 0.0003032))
        val m = norm360(357.52911 + t * (35999.05029 - 0.0001537 * t))
        val e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)

        val mRad = m * DEG_TO_RAD
        val c = sin(mRad) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
            sin(2 * mRad) * (0.019993 - 0.000101 * t) +
            sin(3 * mRad) * 0.000289

        val trueLongitude = l0 + c
        val omega = 125.04 - 1934.136 * t
        val lambda = trueLongitude - 0.00569 - 0.00478 * sin(omega * DEG_TO_RAD)

        val obliquity = meanObliquityDegrees(t) + 0.00256 * cos(omega * DEG_TO_RAD)
        val gmst = greenwichMeanSiderealTimeDegrees(jd, t)

        return azimuthFromEcliptic(lambda, 0.0, obliquity, gmst, latitude, longitude)
    }

    fun moonAzimuthDegrees(latitude: Double, longitude: Double, epochMillis: Long): Float {
        val jd = julianDate(epochMillis)
        val t = julianCenturies(jd)

        val lPrime = norm360(218.3164477 + 481267.88123421 * t - 0.0015786 * t * t)
        val d = norm360(297.8501921 + 445267.1114034 * t - 0.0018819 * t * t)
        val m = norm360(357.5291092 + 35999.0502909 * t - 0.0001536 * t * t)
        val mPrime = norm360(134.9633964 + 477198.8675055 * t + 0.0087414 * t * t)
        val f = norm360(93.2720950 + 483202.0175233 * t - 0.0036539 * t * t)

        fun s(deg: Double) = sin(deg * DEG_TO_RAD)

        val longitudeCorrection =
            6.289 * s(mPrime) - 1.274 * s(mPrime - 2 * d) + 0.658 * s(2 * d) -
                0.186 * s(m) - 0.059 * s(2 * mPrime - 2 * d) -
                0.057 * s(mPrime - 2 * d + m) + 0.053 * s(mPrime + 2 * d) +
                0.046 * s(2 * d - m) + 0.041 * s(mPrime - m) -
                0.035 * s(d) - 0.031 * s(mPrime + m) -
                0.015 * s(2 * f - 2 * d) + 0.011 * s(mPrime - 4 * d)

        val latitudeTerm =
            5.128 * s(f) + 0.281 * s(mPrime + f) - 0.278 * s(f - mPrime) -
                0.173 * s(2 * d - f) + 0.055 * s(2 * d - mPrime + f) +
                0.046 * s(2 * d - mPrime - f) + 0.033 * s(2 * d + f) +
                0.017 * s(2 * mPrime + f)

        val lambda = lPrime + longitudeCorrection
        val beta = latitudeTerm

        val obliquity = meanObliquityDegrees(t)
        val gmst = greenwichMeanSiderealTimeDegrees(jd, t)

        return azimuthFromEcliptic(lambda, beta, obliquity, gmst, latitude, longitude)
    }

    private fun meanObliquityDegrees(t: Double): Double {
        val seconds = 21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))
        return 23.0 + (26.0 + seconds / 60.0) / 60.0
    }

    private fun greenwichMeanSiderealTimeDegrees(jd: Double, t: Double): Double {
        val gmst = 280.46061837 + 360.98564736629 * (jd - 2451545.0) +
            0.000387933 * t * t - t * t * t / 38710000.0
        return norm360(gmst)
    }

    /** Converts ecliptic coordinates to a compass azimuth (degrees from North, clockwise) for an observer. */
    private fun azimuthFromEcliptic(
        lambdaDeg: Double,
        betaDeg: Double,
        obliquityDeg: Double,
        gmstDeg: Double,
        latitude: Double,
        longitude: Double
    ): Float {
        val lambda = lambdaDeg * DEG_TO_RAD
        val beta = betaDeg * DEG_TO_RAD
        val eps = obliquityDeg * DEG_TO_RAD

        val rightAscension = atan2(sin(lambda) * cos(eps) - tan(beta) * sin(eps), cos(lambda)) * RAD_TO_DEG
        val declination = asin(sin(beta) * cos(eps) + cos(beta) * sin(eps) * sin(lambda))

        val localSiderealTime = norm360(gmstDeg + longitude)
        val hourAngle = norm360(localSiderealTime - rightAscension) * DEG_TO_RAD

        val latRad = latitude * DEG_TO_RAD
        val azimuth = atan2(
            sin(hourAngle),
            cos(hourAngle) * sin(latRad) - tan(declination) * cos(latRad)
        ) * RAD_TO_DEG + 180.0

        return norm360(azimuth).toFloat()
    }
}
