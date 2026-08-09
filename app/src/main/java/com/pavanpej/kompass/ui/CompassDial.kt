package com.pavanpej.kompass.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pavanpej.kompass.location.CoordinateFormat
import com.pavanpej.kompass.location.CoordinateFormatter
import com.pavanpej.kompass.sensor.AngleMath
import com.pavanpej.kompass.sensor.CompassPoint
import com.pavanpej.kompass.sensor.LevelMath
import com.pavanpej.kompass.ui.theme.KompassColors
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val LEVEL_THRESHOLD_DEGREES = 1f
private const val MAX_TILT_DEGREES = 20f

@Composable
fun CompassDial(
    heading: Float,
    gravityX: Float,
    gravityY: Float,
    gravityZ: Float,
    lockedBearing: Float?,
    onTapLock: (currentHeading: Float) -> Unit,
    latitude: Double?,
    longitude: Double?,
    coordinateFormat: CoordinateFormat,
    onCoordinatesTap: () -> Unit,
    sunAzimuth: Float?,
    moonAzimuth: Float?,
    modifier: Modifier = Modifier
) {
    val accent = KompassColors.Accent
    val north = KompassColors.PrincetonOrange
    val levelColor = KompassColors.PrincetonOrange
    val faint = KompassColors.OnSurfaceFaint
    val strong = KompassColors.OnSurface
    val muted = KompassColors.OnSurfaceMuted

    // Compose's animateFloatAsState has no concept of angle wraparound, so animating the raw
    // 0-360 heading directly causes a full-circle "reset spin" every time it crosses the 359/0
    // seam. Instead we keep an unwrapped, continuously-growing angle and only wrap it for display.
    var unwrappedHeading by remember { mutableFloatStateOf(heading) }
    LaunchedEffect(heading) {
        unwrappedHeading = AngleMath.unwrap(unwrappedHeading, heading)
    }

    val animatedHeading by animateFloatAsState(
        targetValue = unwrappedHeading,
        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
        label = "heading"
    )
    val displayHeading = ((animatedHeading % 360f) + 360f) % 360f
    val directionLabel = CompassPoint.label(displayHeading)

    val horizontalTilt = LevelMath.horizontalTilt(gravityX, gravityZ)
    val verticalTilt = LevelMath.verticalTilt(gravityY, gravityZ)
    val animatedHorizontalTilt by animateFloatAsState(targetValue = horizontalTilt, label = "horizontalTilt")
    val animatedVerticalTilt by animateFloatAsState(targetValue = verticalTilt, label = "verticalTilt")
    val isLevel = abs(animatedHorizontalTilt) < LEVEL_THRESHOLD_DEGREES && abs(animatedVerticalTilt) < LEVEL_THRESHOLD_DEGREES

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clickable(onClick = { onTapLock(displayHeading) })
        ) {
            val radius = min(size.width, size.height) / 2f * 0.74f
            val center = Offset(size.width / 2f, size.height / 2f)

            rotate(degrees = -animatedHeading, pivot = center) {
                drawCircle(color = faint, radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))

                for (deg in 0 until 360 step 10) {
                    val isMajor = deg % 90 == 0
                    val isMid = deg % 30 == 0
                    val tickLength = if (isMajor) {
                        18.dp.toPx()
                    } else if (isMid) {
                        12.dp.toPx()
                    } else {
                        6.dp.toPx()
                    }
                    val angleRad = Math.toRadians((deg - 90).toDouble())
                    val outer = Offset(
                        center.x + radius * cos(angleRad).toFloat(),
                        center.y + radius * sin(angleRad).toFloat()
                    )
                    val inner = Offset(
                        center.x + (radius - tickLength) * cos(angleRad).toFloat(),
                        center.y + (radius - tickLength) * sin(angleRad).toFloat()
                    )
                    drawLine(
                        color = if (isMajor) accent else faint,
                        start = inner,
                        end = outer,
                        strokeWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx()
                    )
                }

                val labelRadius = radius - 34.dp.toPx()
                listOf("N" to 0, "E" to 90, "S" to 180, "W" to 270).forEach { (label, deg) ->
                    val angleRad = Math.toRadians((deg - 90).toDouble())
                    val pos = Offset(
                        center.x + labelRadius * cos(angleRad).toFloat(),
                        center.y + labelRadius * sin(angleRad).toFloat()
                    )
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = (if (label == "N") north else strong).toArgb()
                            textSize = 16.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.create(
                                android.graphics.Typeface.DEFAULT,
                                android.graphics.Typeface.BOLD
                            )
                        }
                        drawText(label, pos.x, pos.y + paint.textSize / 3, paint)
                    }
                }

                // Locked target bearing marker -- a fixed compass bearing, so it rotates with
                // the dial exactly like the N/E/S/W labels above, not with the phone's tilt.
                if (lockedBearing != null) {
                    drawBearingSpoke(center, radius, lockedBearing, north, spokeLength = 22.dp.toPx())
                    val angleRad = Math.toRadians((lockedBearing - 90).toDouble())
                    val markerOuter = Offset(
                        center.x + radius * cos(angleRad).toFloat(),
                        center.y + radius * sin(angleRad).toFloat()
                    )
                    drawCircle(color = north, radius = 4.dp.toPx(), center = markerOuter)
                }

                // Sun/moon bearing markers -- approximate (see docs/ARCHITECTURE.md), drawn as a
                // small filled dot (sun) and hollow ring (moon) so they're distinguishable from
                // the solid orange locked-bearing marker and each other at a glance. Each gets a
                // small text label so they don't read as unexplained stray lines on the ring.
                if (sunAzimuth != null) {
                    drawBearingSpoke(center, radius, sunAzimuth, accent, spokeLength = 14.dp.toPx())
                    val angleRad = Math.toRadians((sunAzimuth - 90).toDouble())
                    val pos = Offset(
                        center.x + radius * cos(angleRad).toFloat(),
                        center.y + radius * sin(angleRad).toFloat()
                    )
                    drawCircle(color = accent, radius = 3.5.dp.toPx(), center = pos)
                    drawBearingLabel(center, radius, sunAzimuth, "SUN", accent)
                }
                if (moonAzimuth != null) {
                    drawBearingSpoke(center, radius, moonAzimuth, accent, spokeLength = 14.dp.toPx())
                    val angleRad = Math.toRadians((moonAzimuth - 90).toDouble())
                    val pos = Offset(
                        center.x + radius * cos(angleRad).toFloat(),
                        center.y + radius * sin(angleRad).toFloat()
                    )
                    drawCircle(color = accent, radius = 3.5.dp.toPx(), center = pos, style = Stroke(width = 1.2.dp.toPx()))
                    drawBearingLabel(center, radius, moonAzimuth, "MOON", accent)
                }
            }

            // Fixed heading indicator, drawn outside the dial (not rotating) so it always marks
            // "where the phone is pointing". A rounded notched arrowhead, echoing the app icon,
            // rather than a plain sharp triangle.
            val arrowHeight = 22.dp.toPx()
            val arrowHalfWidth = 9.dp.toPx()
            val notchDepth = 0.625f
            val gap = 18.dp.toPx()
            val tipY = center.y - radius - gap - arrowHeight
            val baseY = tipY + arrowHeight
            val notchY = tipY + arrowHeight * notchDepth
            val arrowPath = Path().apply {
                moveTo(center.x, tipY)
                lineTo(center.x + arrowHalfWidth, baseY)
                lineTo(center.x, notchY)
                lineTo(center.x - arrowHalfWidth, baseY)
                close()
            }
            drawPath(path = arrowPath, color = north, style = Fill)
            drawPath(
                path = arrowPath,
                color = north,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Bubble level, merged into the same circle. Deliberately NOT drawn inside the
            // rotate() block above -- tilt is relative to the device, not to magnetic heading,
            // so this grid must stay fixed while the tick marks spin. Flat-surface only.
            val levelRadius = radius * 0.5f
            drawCircle(color = faint, radius = levelRadius, center = center, style = Stroke(width = 1.5.dp.toPx()))
            drawLine(
                color = faint,
                start = Offset(center.x - levelRadius, center.y),
                end = Offset(center.x + levelRadius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = faint,
                start = Offset(center.x, center.y - levelRadius),
                end = Offset(center.x, center.y + levelRadius),
                strokeWidth = 1.dp.toPx()
            )
            drawCircle(color = faint, radius = levelRadius * 0.18f, center = center, style = Stroke(width = 1.dp.toPx()))

            val bubbleRadius = levelRadius * 0.16f
            val clampedHorizontal = animatedHorizontalTilt.coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES)
            val clampedVertical = animatedVerticalTilt.coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES)
            val bubbleOffset = Offset(
                x = center.x + (clampedHorizontal / MAX_TILT_DEGREES) * (levelRadius - bubbleRadius),
                y = center.y - (clampedVertical / MAX_TILT_DEGREES) * (levelRadius - bubbleRadius)
            )
            val bubbleColor = if (isLevel) levelColor else accent
            drawCircle(color = bubbleColor.copy(alpha = 0.25f), radius = bubbleRadius * 1.6f, center = bubbleOffset)
            drawCircle(color = bubbleColor, radius = bubbleRadius, center = bubbleOffset)
        }

        HeadingReadout(
            headingDegrees = displayHeading.toInt(),
            style = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Light, color = strong)
        )
        Text(
            text = directionLabel,
            style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = muted)
        )
        if (latitude != null && longitude != null) {
            Text(
                text = CoordinateFormatter.format(latitude, longitude, coordinateFormat),
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, color = muted),
                modifier = Modifier.clickable(onClick = onCoordinatesTap)
            )
        }
    }
}

/** Draws a short radial "spoke" from just inside the ring out to it, at [bearingDegrees]. */
private fun DrawScope.drawBearingSpoke(
    center: Offset,
    radius: Float,
    bearingDegrees: Float,
    color: Color,
    spokeLength: Float
) {
    val angleRad = Math.toRadians((bearingDegrees - 90).toDouble())
    val outer = Offset(
        center.x + radius * cos(angleRad).toFloat(),
        center.y + radius * sin(angleRad).toFloat()
    )
    val inner = Offset(
        center.x + (radius - spokeLength) * cos(angleRad).toFloat(),
        center.y + (radius - spokeLength) * sin(angleRad).toFloat()
    )
    drawLine(color = color, start = inner, end = outer, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
}

/** Draws a small text label just inside a bearing spoke's inner end, e.g. "SUN" / "MOON". */
private fun DrawScope.drawBearingLabel(
    center: Offset,
    radius: Float,
    bearingDegrees: Float,
    text: String,
    color: Color
) {
    val angleRad = Math.toRadians((bearingDegrees - 90).toDouble())
    val labelRadius = radius - 30.dp.toPx()
    val pos = Offset(
        center.x + labelRadius * cos(angleRad).toFloat(),
        center.y + labelRadius * sin(angleRad).toFloat()
    )
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = color.toArgb()
            textSize = 9.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        drawText(text, pos.x, pos.y + paint.textSize / 3, paint)
    }
}

/**
 * Renders "142°" but reports only the number's width to its parent layout, so a centering
 * container (like the Column above) centers the number itself -- not the number+degree-symbol
 * pair. Without this, the degree symbol's extra width visually shifts the number left of center.
 */
@Composable
private fun HeadingReadout(headingDegrees: Int, style: TextStyle, modifier: Modifier = Modifier) {
    Layout(
        modifier = modifier,
        content = {
            Text(text = "$headingDegrees", style = style)
            Text(text = "°", style = style)
        }
    ) { measurables, constraints ->
        val loosened = constraints.copy(minWidth = 0, minHeight = 0)
        val numberPlaceable = measurables[0].measure(loosened)
        val degreePlaceable = measurables[1].measure(loosened)

        val height = maxOf(numberPlaceable.height, degreePlaceable.height)
        // Reported width is just the number's -- the degree symbol draws past this boundary,
        // which is fine since Compose doesn't clip children to their measured size by default.
        layout(numberPlaceable.width, height) {
            numberPlaceable.placeRelative(x = 0, y = (height - numberPlaceable.height) / 2)
            degreePlaceable.placeRelative(x = numberPlaceable.width, y = (height - degreePlaceable.height) / 2)
        }
    }
}
