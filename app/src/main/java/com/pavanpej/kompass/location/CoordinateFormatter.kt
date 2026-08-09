package com.pavanpej.kompass.location

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

/** Formats a lat/long pair as either decimal degrees or degrees-minutes-seconds (DMS). */
object CoordinateFormatter {

    fun format(latitude: Double, longitude: Double, format: CoordinateFormat): String =
        when (format) {
            CoordinateFormat.DECIMAL -> "${decimalPart(latitude, isLatitude = true)}, ${decimalPart(longitude, isLatitude = false)}"
            CoordinateFormat.DMS -> "${dmsPart(latitude, isLatitude = true)} ${dmsPart(longitude, isLatitude = false)}"
        }

    private fun decimalPart(value: Double, isLatitude: Boolean): String {
        val hemisphere = hemisphereLetter(value, isLatitude)
        return String.format(Locale.US, "%.4f° %s", abs(value), hemisphere)
    }

    private fun dmsPart(value: Double, isLatitude: Boolean): String {
        val hemisphere = hemisphereLetter(value, isLatitude)
        val absValue = abs(value)
        val degrees = floor(absValue).toInt()
        val minutesFull = (absValue - degrees) * 60.0
        val minutes = floor(minutesFull).toInt()
        val seconds = (minutesFull - minutes) * 60.0
        return String.format(Locale.US, "%d°%02d'%05.2f\"%s", degrees, minutes, seconds, hemisphere)
    }

    private fun hemisphereLetter(value: Double, isLatitude: Boolean): String =
        if (isLatitude) {
            if (value >= 0) "N" else "S"
        } else {
            if (value >= 0) "E" else "W"
        }
}
