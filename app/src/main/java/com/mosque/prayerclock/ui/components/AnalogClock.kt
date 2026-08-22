package com.mosque.prayerclock.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mosque.prayerclock.ui.components.clockfaces.ClockDisplayData
import com.mosque.prayerclock.ui.theme.AlphaValues
import com.mosque.prayerclock.ui.theme.rememberThemeColors
import kotlin.math.cos
import kotlin.math.sin
import android.graphics.Paint as AndroidPaint

/**
 * Pure rendering composable for the classic analog clock face.
 * Performs no data processing — all display values come from [ClockDisplayData].
 * Use [rememberClockDisplayData] in the call site to produce the data.
 */
@Composable
fun AnalogClock(
    data: ClockDisplayData,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val density = LocalDensity.current
        val availableWidthPx = with(density) { maxWidth.toPx() }
        val availableHeightPx = with(density) { maxHeight.toPx() }
        val minDimension = minOf(availableWidthPx, availableHeightPx)
        val clockRadius = minDimension * 0.5f

        val calculatedMonthFontSize = with(density) { (clockRadius * 0.08f).toSp() }
        val calculatedDayFontSize = with(density) { (clockRadius * 0.28f).toSp() }
        val calculatedYearFontSize = with(density) { (clockRadius * 0.15f).toSp() }

        val colors = rememberThemeColors()

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = this.size.minDimension * 0.5f
                val center = this.size.center

                drawCircle(
                    brush =
                        Brush.radialGradient(
                            colors = listOf(colors.surfaceCenter, colors.surfaceDark),
                            radius = radius,
                            center = center,
                        ),
                    radius = radius,
                    center = center,
                )
                drawCircle(
                    color = colors.primaryAccent,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 6.dp.toPx()),
                )
                drawCircle(
                    color = colors.primaryAccent.copy(alpha = AlphaValues.SUBTLE),
                    radius = radius * 0.95f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )

                drawSunburstPattern(center, radius * 0.9f, colors.primaryAccent.copy(alpha = 0.4f))
                drawModernNumbers(center, radius * 0.8f, colors.primaryAccent, colors.surfaceDark)
                drawModernHourMarkers(center, radius, colors.primaryAccent)

                drawModernHourHand(center, radius * 0.5f, data.hour12, data.minute, colors.primaryAccent, radius)
                drawModernMinuteHand(center, radius * 0.7f, data.minute, colors.primaryAccent, radius)
                drawModernSecondHand(center, radius * 0.75f, data.second, colors.secondaryAccent, radius)

                drawCircle(color = colors.primaryAccent, radius = 12f, center = center)
                drawCircle(color = colors.surfaceCenter, radius = 8f, center = center)
                drawCircle(color = colors.primaryAccent, radius = 4f, center = center)
            }

            // Hijri Month — Top Left
            AnimatedContent(
                targetState = data.hijriMonthName,
                transitionSpec = {
                    fadeIn(animationSpec = tween(80, easing = LinearEasing)) togetherWith
                        fadeOut(animationSpec = tween(40, easing = LinearEasing))
                },
                label = "hijri_month_transition",
                modifier = Modifier.align(Alignment.TopStart).padding(top = 16.dp),
            ) { monthName ->
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp,
                ) {
                    Text(
                        text = monthName,
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontSize = calculatedMonthFontSize,
                                fontWeight = FontWeight.Bold,
                            ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }

            // Hijri Day — Center Left
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 6.dp,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Text(
                    text = data.hijriDay.toString(),
                    style =
                        MaterialTheme.typography.headlineLarge.copy(
                            fontSize = calculatedDayFontSize,
                            fontWeight = FontWeight.Bold,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }

            // Hijri Year — Bottom Left
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp,
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 16.dp),
            ) {
                Text(
                    text = data.hijriYear.toString(),
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontSize = calculatedYearFontSize,
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }

            // Gregorian Month — Top Right
            AnimatedContent(
                targetState = data.gregorianMonthName,
                transitionSpec = {
                    fadeIn(animationSpec = tween(60, easing = LinearEasing)) togetherWith
                        fadeOut(animationSpec = tween(30, easing = LinearEasing))
                },
                label = "gregorian_month_transition",
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp),
            ) { monthName ->
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp,
                ) {
                    Text(
                        text = monthName,
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontSize = calculatedMonthFontSize,
                                fontWeight = FontWeight.Bold,
                            ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }

            // Gregorian Day — Center Right
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 6.dp,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Text(
                    text = data.gregorianDay.toString(),
                    style =
                        MaterialTheme.typography.headlineLarge.copy(
                            fontSize = calculatedDayFontSize,
                            fontWeight = FontWeight.Bold,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }

            // Gregorian Year — Bottom Right
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp),
            ) {
                Text(
                    text = data.gregorianYear.toString(),
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontSize = calculatedYearFontSize,
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// ── Canvas drawing functions ──────────────────────────────────────────────────
// These are pure geometric / colour operations with no data dependencies.

private fun DrawScope.drawSunburstPattern(center: Offset, radius: Float, color: Color) {
    for (i in 0 until 60) {
        val angle = i * 6f - 90f
        val startRadius = radius * 0.3f
        val endRadius = radius * 0.85f
        val startX = center.x + startRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val startY = center.y + startRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
        val endX = center.x + endRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val endY = center.y + endRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
        drawLine(
            color = color.copy(alpha = if (i % 5 == 0) 0.6f else 0.2f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = if (i % 5 == 0) 2.dp.toPx() else 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawModernNumbers(
    center: Offset,
    radius: Float,
    color: Color,
    backgroundColor: Color,
) {
    val numbers = listOf(Pair("12", 0), Pair("3", 3), Pair("6", 6), Pair("9", 9))
    val dynamicTextSize = radius * 0.12f
    val backgroundRadius = dynamicTextSize * 1.25f

    drawIntoCanvas { canvas ->
        val paint =
            AndroidPaint().apply {
                this.color = color.toArgb()
                textSize = dynamicTextSize
                textAlign = AndroidPaint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT,
                    android.graphics.Typeface.BOLD,
                )
            }

        numbers.forEach { (number, position) ->
            val angle = position * 30f - 90f
            val x = center.x + radius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val y = center.y + radius * sin(Math.toRadians(angle.toDouble())).toFloat()

            drawCircle(color = backgroundColor, radius = backgroundRadius, center = Offset(x, y))
            drawCircle(
                color = color.copy(alpha = 0.1f),
                radius = backgroundRadius * 0.93f,
                center = Offset(x, y),
                style = Stroke(width = 2.dp.toPx()),
            )
            canvas.nativeCanvas.drawText(number, x, y + paint.textSize / 3f, paint)
        }
    }
}

private fun DrawScope.drawModernHourMarkers(center: Offset, radius: Float, color: Color) {
    for (i in 0 until 12) {
        if (i % 3 == 0) continue
        val angle = i * 30f - 90f
        val startRadius = radius * 0.85f
        val endRadius = radius * 0.95f
        drawLine(
            color = color,
            start = Offset(
                center.x + startRadius * cos(Math.toRadians(angle.toDouble())).toFloat(),
                center.y + startRadius * sin(Math.toRadians(angle.toDouble())).toFloat(),
            ),
            end = Offset(
                center.x + endRadius * cos(Math.toRadians(angle.toDouble())).toFloat(),
                center.y + endRadius * sin(Math.toRadians(angle.toDouble())).toFloat(),
            ),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawModernHourHand(
    center: Offset,
    length: Float,
    hour: Int,
    minute: Int,
    color: Color,
    clockRadius: Float,
) {
    val angle = (hour * 30f + minute * 0.5f) - 90f
    val endX = center.x + length * cos(Math.toRadians(angle.toDouble())).toFloat()
    val endY = center.y + length * sin(Math.toRadians(angle.toDouble())).toFloat()
    drawLine(
        color = color,
        start = center,
        end = Offset(endX, endY),
        strokeWidth = clockRadius * 0.055f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color.White.copy(alpha = 0.3f),
        start = center,
        end = Offset(endX, endY),
        strokeWidth = clockRadius * 0.024f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawModernMinuteHand(
    center: Offset,
    length: Float,
    minute: Int,
    color: Color,
    clockRadius: Float,
) {
    val angle = minute * 6f - 90f
    val endX = center.x + length * cos(Math.toRadians(angle.toDouble())).toFloat()
    val endY = center.y + length * sin(Math.toRadians(angle.toDouble())).toFloat()
    drawLine(
        color = color,
        start = center,
        end = Offset(endX, endY),
        strokeWidth = clockRadius * 0.032f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color.White.copy(alpha = 0.3f),
        start = center,
        end = Offset(endX, endY),
        strokeWidth = clockRadius * 0.014f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawModernSecondHand(
    center: Offset,
    length: Float,
    second: Int,
    color: Color,
    clockRadius: Float,
) {
    val angle = second * 6f - 90f
    val endX = center.x + length * cos(Math.toRadians(angle.toDouble())).toFloat()
    val endY = center.y + length * sin(Math.toRadians(angle.toDouble())).toFloat()
    drawLine(
        color = color,
        start = center,
        end = Offset(endX, endY),
        strokeWidth = clockRadius * 0.010f,
        cap = StrokeCap.Round,
    )
    drawCircle(
        color = color,
        radius = clockRadius * 0.020f,
        center = Offset(endX, endY),
    )
}
