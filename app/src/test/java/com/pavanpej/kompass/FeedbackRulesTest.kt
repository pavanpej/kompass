package com.pavanpej.kompass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavioural spec for [FeedbackRules] -- the rules deciding when Kompass gives a haptic tick:
 * "the phone just became level", "the phone just crossed a cardinal direction", and "the phone
 * just pointed at a locked target bearing".
 */
class FeedbackRulesTest {

    // --- angularDistance ---------------------------------------------------------------

    @Test
    fun `given two identical headings, when measuring angular distance, then it is zero`() {
        assertEquals(0f, FeedbackRules.angularDistance(45f, 45f), 0.001f)
    }

    @Test
    fun `given headings on either side of the 0 to 360 seam, when measuring angular distance, then it is the short way (2 degrees), not 358`() {
        assertEquals(2f, FeedbackRules.angularDistance(359f, 1f), 0.001f)
    }

    // --- nearestCardinal -----------------------------------------------------------------

    @Test
    fun `given a heading of 3 degrees, when finding the nearest cardinal, then it is north`() {
        assertEquals(0, FeedbackRules.nearestCardinal(3f))
    }

    @Test
    fun `given a heading of 358 degrees, when finding the nearest cardinal, then it is still north, not west`() {
        assertEquals(0, FeedbackRules.nearestCardinal(358f))
    }

    @Test
    fun `given a heading of 91 degrees, when finding the nearest cardinal, then it is east`() {
        assertEquals(90, FeedbackRules.nearestCardinal(91f))
    }

    @Test
    fun `given a heading of 45 degrees exactly between north and east, when finding the nearest cardinal, then it picks one of the two consistently`() {
        val result = FeedbackRules.nearestCardinal(45f)
        assertTrue("expected either N (0) or E (90) at the exact midpoint, got $result", result == 0 || result == 90)
    }

    // --- isOnCardinal ----------------------------------------------------------------------

    @Test
    fun `given a heading of exactly 180 degrees, when checking if on a cardinal, then it is true`() {
        assertTrue(FeedbackRules.isOnCardinal(180f))
    }

    @Test
    fun `given a heading 5 degrees off south, when checking if on a cardinal with the default 1 degree threshold, then it is false`() {
        assertFalse(FeedbackRules.isOnCardinal(185f))
    }

    @Test
    fun `given a heading half a degree off north but across the 0 to 360 seam, when checking if on a cardinal, then it is still true`() {
        assertTrue(FeedbackRules.isOnCardinal(359.5f))
    }

    // --- isLevel -----------------------------------------------------------------------------

    @Test
    fun `given horizontal and vertical tilt both at zero, when checking if level, then it is true`() {
        assertTrue(FeedbackRules.isLevel(horizontalTilt = 0f, verticalTilt = 0f))
    }

    @Test
    fun `given horizontal tilt within threshold but vertical tilt outside it, when checking if level, then it is false`() {
        assertFalse(FeedbackRules.isLevel(horizontalTilt = 0.2f, verticalTilt = 3f))
    }

    @Test
    fun `given both tilts just inside the default 1 degree threshold, when checking if level, then it is true`() {
        assertTrue(FeedbackRules.isLevel(horizontalTilt = 0.9f, verticalTilt = -0.9f))
    }

    // --- isOnBearing -------------------------------------------------------------------------

    @Test
    fun `given the azimuth exactly matches a locked bearing, when checking if on bearing, then it is true`() {
        assertTrue(FeedbackRules.isOnBearing(azimuth = 137f, bearing = 137f))
    }

    @Test
    fun `given the azimuth is 5 degrees off a locked bearing, when checking if on bearing with the default threshold, then it is false`() {
        assertFalse(FeedbackRules.isOnBearing(azimuth = 142f, bearing = 137f))
    }

    @Test
    fun `given a locked bearing near the 0 to 360 seam, when the azimuth is just across it, then it is still true`() {
        assertTrue(FeedbackRules.isOnBearing(azimuth = 0.3f, bearing = 359.8f))
    }
}
