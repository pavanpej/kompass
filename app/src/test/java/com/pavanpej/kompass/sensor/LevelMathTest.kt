package com.pavanpej.kompass.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Behavioural spec for [LevelMath] -- the flat-surface bubble level merged into the compass dial. */
class LevelMathTest {

    @Test
    fun `given the phone flat and perfectly level, when reading horizontal tilt, then it is zero`() {
        assertEquals(0f, LevelMath.horizontalTilt(gravityX = 0f, gravityZ = 9.8f), 0.01f)
    }

    @Test
    fun `given the phone flat and perfectly level, when reading vertical tilt, then it is zero`() {
        assertEquals(0f, LevelMath.verticalTilt(gravityY = 0f, gravityZ = 9.8f), 0.01f)
    }

    @Test
    fun `given the phone flat but tilted right, when reading horizontal tilt, then it is positive`() {
        val tilt = LevelMath.horizontalTilt(gravityX = 1.7f, gravityZ = 9.65f)
        assertTrue("expected a positive angle, got $tilt", tilt > 0f)
    }

    @Test
    fun `given the phone flat but tilted left, when reading horizontal tilt, then it is negative`() {
        val tilt = LevelMath.horizontalTilt(gravityX = -1.7f, gravityZ = 9.65f)
        assertTrue("expected a negative angle, got $tilt", tilt < 0f)
    }
}
