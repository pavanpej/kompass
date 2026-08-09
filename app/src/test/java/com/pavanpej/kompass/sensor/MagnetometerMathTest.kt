package com.pavanpej.kompass.sensor

import org.junit.Assert.assertEquals
import org.junit.Test

/** Behavioural spec for [MagnetometerMath] -- magnetic field strength shown next to the compass. */
class MagnetometerMathTest {

    @Test
    fun `given a field vector aligned entirely along one axis, when computing magnitude, then it equals that axis value`() {
        assertEquals(45f, MagnetometerMath.magnitude(x = 45f, y = 0f, z = 0f), 0.001f)
    }

    @Test
    fun `given a 3-4-5 Pythagorean triple as the vector components, when computing magnitude, then it matches the triple's hypotenuse`() {
        assertEquals(5f, MagnetometerMath.magnitude(x = 3f, y = 4f, z = 0f), 0.001f)
    }

    @Test
    fun `given a zero field vector, when computing magnitude, then it is zero`() {
        assertEquals(0f, MagnetometerMath.magnitude(x = 0f, y = 0f, z = 0f), 0.001f)
    }

    @Test
    fun `given negative components, when computing magnitude, then the sign does not matter`() {
        assertEquals(5f, MagnetometerMath.magnitude(x = -3f, y = -4f, z = 0f), 0.001f)
    }
}
