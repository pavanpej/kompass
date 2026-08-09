package com.pavanpej.kompass.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavioural spec for [AngleMath].
 *
 * This is the exact math behind the original "spin reset" bug: the compass dial would spin
 * a full 360 degrees whenever the heading crossed the 359 -> 0 seam, because naive linear
 * interpolation treats 359 and 1 as almost-opposite instead of 2 degrees apart. Every test
 * below is written as a scenario that should be readable without knowing Kotlin.
 */
class AngleMathTest {

    // --- shortestDelta: "how far apart are these two headings, going the short way?" -------

    @Test
    fun `given two headings 10 degrees apart, when finding the shortest delta, then it returns plus 10`() {
        assertEquals(10f, AngleMath.shortestDelta(from = 350f, to = 0f), 0.001f)
    }

    @Test
    fun `given heading crosses the 359 to 1 degree seam, when finding the shortest delta, then it is only 2 degrees, not 358`() {
        assertEquals(2f, AngleMath.shortestDelta(from = 359f, to = 1f), 0.001f)
    }

    @Test
    fun `given heading crosses 1 to 359 degrees backwards, when finding the shortest delta, then it is negative 2 degrees`() {
        assertEquals(-2f, AngleMath.shortestDelta(from = 1f, to = 359f), 0.001f)
    }

    @Test
    fun `given two headings exactly opposite each other, when finding the shortest delta, then it is 180 degrees`() {
        assertEquals(180f, Math.abs(AngleMath.shortestDelta(from = 0f, to = 180f)), 0.001f)
    }

    // --- lowPass: "the smoothed heading the dial actually animates towards" -----------------

    @Test
    fun `given no previous reading, when smoothing the first sample, then it returns that sample unchanged`() {
        assertEquals(45f, AngleMath.lowPass(previous = null, current = 45f, factor = 0.15f), 0.001f)
    }

    @Test
    fun `given a steady heading of 90 degrees, when smoothing repeatedly, then it converges to 90 without overshooting`() {
        var smoothed: Float? = null
        repeat(50) {
            smoothed = AngleMath.lowPass(previous = smoothed, current = 90f, factor = 0.15f)
        }
        assertEquals(90f, smoothed!!, 0.5f)
    }

    @Test
    fun `given the heading crosses 359 to 1 degree, when smoothing one step, then the result lands near the seam, not near 180`() {
        val result = AngleMath.lowPass(previous = 359f, current = 1f, factor = 0.15f)
        assertTrue("expected result near the 0/360 seam, got $result", result > 359f || result < 10f)
    }

    // --- unwrap: "the continuous angle CompassDial animates, so it never resets to 0" -------

    @Test
    fun `given the dial is unwrapped at 358 degrees, when the raw heading reads 2 degrees, then the unwrapped angle grows past 360 instead of jumping back to 2`() {
        assertEquals(362f, AngleMath.unwrap(previousUnwrapped = 358f, newWrapped = 2f), 0.001f)
    }

    @Test
    fun `given the dial is unwrapped at 2 degrees, when the raw heading reads 358 degrees, then the unwrapped angle goes negative instead of jumping forward to 358`() {
        assertEquals(-2f, AngleMath.unwrap(previousUnwrapped = 2f, newWrapped = 358f), 0.001f)
    }

    @Test
    fun `given many small clockwise turns crossing the seam repeatedly, when unwrapping each step, then the unwrapped angle only ever grows and never resets`() {
        var unwrapped = 355f
        val rawReadings = listOf(357f, 359f, 1f, 3f, 5f)
        val history = mutableListOf(unwrapped)
        for (raw in rawReadings) {
            unwrapped = AngleMath.unwrap(unwrapped, raw)
            history.add(unwrapped)
        }
        for (i in 1 until history.size) {
            assertTrue(
                "unwrapped angle should be monotonically increasing, but went from ${history[i - 1]} to ${history[i]}",
                history[i] > history[i - 1]
            )
        }
    }

    // --- addDegrees: "applying magnetic declination to get true north" --------------------

    @Test
    fun `given a magnetic heading and a positive declination, when adding declination, then it shifts forward and wraps correctly`() {
        assertEquals(50f, AngleMath.addDegrees(base = 40f, delta = 10f), 0.001f)
    }

    @Test
    fun `given a magnetic heading near 360, when adding a positive declination, then it wraps back around to just past zero`() {
        assertEquals(5f, AngleMath.addDegrees(base = 355f, delta = 10f), 0.001f)
    }

    @Test
    fun `given a magnetic heading near zero, when adding a negative declination, then it wraps to just under 360`() {
        assertEquals(355f, AngleMath.addDegrees(base = 5f, delta = -10f), 0.001f)
    }
}
