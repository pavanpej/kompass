package com.pavanpej.kompass.sensor

import kotlin.math.sqrt

/** Magnetic field strength from the raw TYPE_MAGNETIC_FIELD vector, in microtesla (uT). */
object MagnetometerMath {
    fun magnitude(x: Float, y: Float, z: Float): Float =
        sqrt(x * x + y * y + z * z)
}
