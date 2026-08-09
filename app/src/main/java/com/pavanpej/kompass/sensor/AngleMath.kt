package com.pavanpej.kompass.sensor

/**
 * Pure angle math shared by the sensor smoothing pipeline and the dial animation.
 * Deliberately framework-free (no Android SensorManager/Context) so it can be unit
 * tested on the plain JVM without Robolectric or a device.
 */
object AngleMath {

    private const val FULL_CIRCLE = 360f
    private const val HALF_CIRCLE = 180f

    /** Signed angular delta from [from] to [to], taking the shorter arc. Result is in (-180, 180]. */
    fun shortestDelta(from: Float, to: Float): Float {
        var delta = (to - from) % FULL_CIRCLE
        if (delta > HALF_CIRCLE) delta -= FULL_CIRCLE
        if (delta < -HALF_CIRCLE) delta += FULL_CIRCLE
        return delta
    }

    /** Exponentially smooths towards [current], moving along the shorter arc. Result wrapped to [0, 360). */
    fun lowPass(previous: Float?, current: Float, factor: Float): Float {
        if (previous == null) return (current % FULL_CIRCLE + FULL_CIRCLE) % FULL_CIRCLE
        val delta = shortestDelta(previous, current)
        return (previous + factor * delta + FULL_CIRCLE) % FULL_CIRCLE
    }

    /**
     * Extends [previousUnwrapped] toward the new wrapped reading [newWrapped] (0-360) by the
     * shortest arc, WITHOUT wrapping the result back into [0, 360). This keeps a continuously
     * growing/shrinking angle so a naive UI animation never sees a 359 -> 0 style jump.
     */
    fun unwrap(previousUnwrapped: Float, newWrapped: Float): Float {
        return previousUnwrapped + shortestDelta(previousUnwrapped, newWrapped)
    }

    /** Adds [delta] degrees to [base] and wraps the result to [0, 360). Used to apply magnetic declination. */
    fun addDegrees(base: Float, delta: Float): Float =
        ((base + delta) % FULL_CIRCLE + FULL_CIRCLE) % FULL_CIRCLE
}
