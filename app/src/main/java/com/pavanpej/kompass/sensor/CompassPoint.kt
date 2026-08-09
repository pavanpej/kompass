package com.pavanpej.kompass.sensor

private val POINTS = listOf(
    "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
)

/** Maps an azimuth to its nearest 16-point compass label ("N", "NE", "ENE", ...). */
object CompassPoint {
    fun label(azimuthDegrees: Float): String {
        val normalized = ((azimuthDegrees % 360f) + 360f) % 360f
        val index = ((normalized / 22.5f) + 0.5f).toInt() % 16
        return POINTS[index]
    }
}
