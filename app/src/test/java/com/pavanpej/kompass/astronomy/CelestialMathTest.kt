package com.pavanpej.kompass.astronomy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Behavioural spec for [CelestialMath]. These are deliberately structural checks (valid range,
 * determinism, expected daily trend) rather than assertions against precise reference ephemeris
 * values -- there's no internet access available while developing this to verify against a real
 * ephemeris service, so asserting a specific "correct" degree value would be asserting something
 * unverified. If you have a way to check these against a real ephemeris, tightening these tests
 * with reference values would be a good follow-up.
 */
class CelestialMathTest {

    private val sanFranciscoLat = 37.7749
    private val sanFranciscoLon = -122.4194

    private fun epochMillisAt(hourUtc: Int, minuteUtc: Int = 0): Long =
        ZonedDateTime.of(2024, 6, 21, hourUtc, minuteUtc, 0, 0, ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

    // --- sunAzimuthDegrees -----------------------------------------------------------------

    @Test
    fun `given any valid input, when computing sun azimuth, then the result is within 0 to 360 degrees`() {
        val azimuth = CelestialMath.sunAzimuthDegrees(sanFranciscoLat, sanFranciscoLon, epochMillisAt(18))
        assertTrue("expected azimuth in [0, 360), got $azimuth", azimuth >= 0f && azimuth < 360f)
    }

    @Test
    fun `given the same location and time twice, when computing sun azimuth, then the result is identical (deterministic)`() {
        val time = epochMillisAt(18)
        val first = CelestialMath.sunAzimuthDegrees(sanFranciscoLat, sanFranciscoLon, time)
        val second = CelestialMath.sunAzimuthDegrees(sanFranciscoLat, sanFranciscoLon, time)
        assertEquals(first, second, 0.0001f)
    }

    @Test
    fun `given a mid-latitude location on a summer day, when sampling sun azimuth from morning to evening, then it increases monotonically (sunrise in the east toward sunset in the west)`() {
        // 14:00-01:00 UTC on 2024-06-21 covers roughly sunrise through sunset in San Francisco (UTC-7).
        val samples = listOf(14, 16, 18, 20, 22, 24, 25).map { hour ->
            CelestialMath.sunAzimuthDegrees(sanFranciscoLat, sanFranciscoLon, epochMillisAt(hour % 24))
        }
        for (i in 1 until samples.size) {
            assertTrue(
                "expected sun azimuth to increase through the day, but went from ${samples[i - 1]} to ${samples[i]}",
                samples[i] > samples[i - 1]
            )
        }
    }

    // --- moonAzimuthDegrees ------------------------------------------------------------------

    @Test
    fun `given any valid input, when computing moon azimuth, then the result is within 0 to 360 degrees`() {
        val azimuth = CelestialMath.moonAzimuthDegrees(sanFranciscoLat, sanFranciscoLon, epochMillisAt(18))
        assertTrue("expected azimuth in [0, 360), got $azimuth", azimuth >= 0f && azimuth < 360f)
    }

    @Test
    fun `given the same location and time twice, when computing moon azimuth, then the result is identical (deterministic)`() {
        val time = epochMillisAt(18)
        val first = CelestialMath.moonAzimuthDegrees(sanFranciscoLat, sanFranciscoLon, time)
        val second = CelestialMath.moonAzimuthDegrees(sanFranciscoLat, sanFranciscoLon, time)
        assertEquals(first, second, 0.0001f)
    }

    @Test
    fun `given different locations at the same instant, when computing moon azimuth, then the results differ`() {
        val time = epochMillisAt(18)
        val here = CelestialMath.moonAzimuthDegrees(sanFranciscoLat, sanFranciscoLon, time)
        val sydney = CelestialMath.moonAzimuthDegrees(-33.8688, 151.2093, time)
        assertTrue(here != sydney)
    }
}
