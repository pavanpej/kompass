package com.pavanpej.kompass.sensor

import org.junit.Assert.assertEquals
import org.junit.Test

/** Behavioural spec for [CompassPoint] -- the 16-point compass label shown next to the degree readout. */
class CompassPointTest {

    @Test
    fun `given a heading of exactly 0 degrees, when finding the compass point, then it is N`() {
        assertEquals("N", CompassPoint.label(0f))
    }

    @Test
    fun `given a heading of exactly 90 degrees, when finding the compass point, then it is E`() {
        assertEquals("E", CompassPoint.label(90f))
    }

    @Test
    fun `given a heading of exactly 180 degrees, when finding the compass point, then it is S`() {
        assertEquals("S", CompassPoint.label(180f))
    }

    @Test
    fun `given a heading of exactly 270 degrees, when finding the compass point, then it is W`() {
        assertEquals("W", CompassPoint.label(270f))
    }

    @Test
    fun `given a heading of exactly 45 degrees, when finding the compass point, then it is NE`() {
        assertEquals("NE", CompassPoint.label(45f))
    }

    @Test
    fun `given a heading of exactly 135 degrees, when finding the compass point, then it is SE`() {
        assertEquals("SE", CompassPoint.label(135f))
    }

    @Test
    fun `given a heading just past 360, when finding the compass point, then it wraps back to N rather than throwing or indexing out of bounds`() {
        assertEquals("N", CompassPoint.label(361f))
    }

    @Test
    fun `given a heading just below zero, when finding the compass point, then it still resolves to a valid point near N`() {
        assertEquals("N", CompassPoint.label(-1f))
    }

    @Test
    fun `given a heading of 349 degrees which is within half a sector of north, when finding the compass point, then it is N, not NNW`() {
        assertEquals("N", CompassPoint.label(349f))
    }
}
