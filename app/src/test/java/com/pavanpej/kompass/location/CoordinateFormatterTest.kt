package com.pavanpej.kompass.location

import org.junit.Assert.assertEquals
import org.junit.Test

/** Behavioural spec for [CoordinateFormatter] -- the lat/long text shown under the compass heading. */
class CoordinateFormatterTest {

    @Test
    fun `given a positive latitude and negative longitude, when formatting as decimal, then hemisphere letters are N and W`() {
        val result = CoordinateFormatter.format(latitude = 37.7749, longitude = -122.4194, format = CoordinateFormat.DECIMAL)
        assertEquals("37.7749° N, 122.4194° W", result)
    }

    @Test
    fun `given a negative latitude and positive longitude, when formatting as decimal, then hemisphere letters are S and E`() {
        val result = CoordinateFormatter.format(latitude = -33.8688, longitude = 151.2093, format = CoordinateFormat.DECIMAL)
        assertEquals("33.8688° S, 151.2093° E", result)
    }

    @Test
    fun `given a coordinate, when formatting as DMS, then it breaks into degrees minutes and seconds`() {
        val result = CoordinateFormatter.format(latitude = 37.7749, longitude = -122.4194, format = CoordinateFormat.DMS)
        assertEquals("37°46'29.64\"N 122°25'09.84\"W", result)
    }

    @Test
    fun `given exactly zero latitude and longitude, when formatting as decimal, then it defaults to the positive hemisphere letters`() {
        val result = CoordinateFormatter.format(latitude = 0.0, longitude = 0.0, format = CoordinateFormat.DECIMAL)
        assertEquals("0.0000° N, 0.0000° E", result)
    }
}
