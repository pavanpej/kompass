package com.pavanpej.kompass.sensor

import kotlin.math.atan2

/**
 * Flat-surface tilt angles derived from the gravity vector, for the bubble level merged into
 * the compass dial. Deliberately flat-only -- no side/upright orientation modes. That auto-
 * adaptive multi-orientation version turned out to be more trouble than it was worth (sign
 * ambiguity per edge, flickering at boundaries) and was removed; this only covers the common
 * case of the phone lying roughly flat on a surface.
 */
object LevelMath {
    fun horizontalTilt(gravityX: Float, gravityZ: Float): Float =
        Math.toDegrees(atan2(gravityX.toDouble(), gravityZ.toDouble())).toFloat()

    fun verticalTilt(gravityY: Float, gravityZ: Float): Float =
        Math.toDegrees(atan2(gravityY.toDouble(), gravityZ.toDouble())).toFloat()
}
