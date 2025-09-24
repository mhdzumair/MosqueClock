package com.mosque.prayerclock.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlin.math.*

@Composable
fun AnalogClock(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 200.dp,
    currentTime: Instant? = null,
) {
    // Use provided time or fallback to local time source
    val timeToUse =
        currentTime ?: run {
            var localCurrentTime by remember { mutableStateOf(Clock.System.now()) }
            LaunchedEffect(Unit) {
                while (true) {
                    localCurrentTime = Clock.System.now()
                    delay(1000)
                }
            }
            localCurrentTime
        }

    val localDateTime = timeToUse.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    val hour = localDateTime.hour % 12
    val minute = localDateTime.minute
    val second = localDateTime.second

    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier.size(size),
        ) {
            val radius = this.size.minDimension * 0.45f
            val center = this.size.center

            // Draw clock face background
            drawCircle(
                color = surfaceColor,
                radius = radius,
                center = center,
            )

            // Draw outer rim
            drawCircle(
                color = primaryColor,
                radius = radius,
                center = center,
                style = Stroke(width = 6.dp.toPx()),
            )

            // Draw inner shadow circle
            drawCircle(
                color = outlineColor.copy(alpha = 0.1f),
                radius = radius * 0.95f,
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )

            drawClockTicks(center, radius, onSurfaceColor, primaryColor)

            drawClockNumbers(center, radius * 0.8f, onSurfaceColor)

            drawHourHand(center, radius * 0.55f, hour, minute, primaryColor)

            drawMinuteHand(center, radius * 0.75f, minute, primaryColor)

            drawSecondHand(center, radius * 0.85f, second, secondaryColor)

            // Center dot with gradient effect
            drawCircle(
                color = primaryColor,
                radius = 16f,
                center = center,
            )
            drawCircle(
                color = surfaceColor,
                radius = 8f,
                center = center,
            )
        }
    }
}

private fun DrawScope.drawClockTicks(
    center: Offset,
    radius: Float,
    tickColor: Color,
    hourTickColor: Color,
) {
    // Draw hour ticks (thicker)
    for (i in 0 until 12) {
        val angle = i * 30f - 90f
        val startRadius = radius * 0.85f
        val endRadius = radius * 0.95f

        val startX = center.x + startRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val startY = center.y + startRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
        val endX = center.x + endRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val endY = center.y + endRadius * sin(Math.toRadians(angle.toDouble())).toFloat()

        drawLine(
            color = hourTickColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    // Draw minute ticks (thinner)
    for (i in 0 until 60) {
        if (i % 5 != 0) {
            val angle = i * 6f - 90f
            val startRadius = radius * 0.9f
            val endRadius = radius * 0.95f

            val startX = center.x + startRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val startY = center.y + startRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
            val endX = center.x + endRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val endY = center.y + endRadius * sin(Math.toRadians(angle.toDouble())).toFloat()

            drawLine(
                color = tickColor.copy(alpha = 0.5f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawClockNumbers(
    center: Offset,
    radius: Float,
    color: Color,
) {
    val numbers = listOf("12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")

    drawIntoCanvas { canvas ->
        val paint =
            Paint().apply {
                this.color = color.toArgb()
                textSize = 40f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            }

        for (i in numbers.indices) {
            val angle = i * 30f - 90f
            val x = center.x + radius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val y = center.y + radius * sin(Math.toRadians(angle.toDouble())).toFloat()

            // Draw number background circle
            drawCircle(
                color = color.copy(alpha = 0.1f),
                radius = 22f,
                center = Offset(x, y),
            )

            // Draw the number text
            canvas.nativeCanvas.drawText(
                numbers[i],
                x,
                y + paint.textSize / 3f, // Adjust vertical alignment
                paint,
            )
        }
    }
}

private fun DrawScope.drawHourHand(
    center: Offset,
    length: Float,
    hour: Int,
    minute: Int,
    color: Color,
) {
    val angle = (hour * 30f + minute * 0.5f) - 90f
    val endX = center.x + length * cos(Math.toRadians(angle.toDouble())).toFloat()
    val endY = center.y + length * sin(Math.toRadians(angle.toDouble())).toFloat()

    // Draw shadow
    drawLine(
        color = Color.Black.copy(alpha = 0.2f),
        start = Offset(center.x + 2, center.y + 2),
        end = Offset(endX + 2, endY + 2),
        strokeWidth = 8.dp.toPx(),
        cap = StrokeCap.Round,
    )

    // Draw main hand
    drawLine(
        color = color,
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 7.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawMinuteHand(
    center: Offset,
    length: Float,
    minute: Int,
    color: Color,
) {
    val angle = minute * 6f - 90f
    val endX = center.x + length * cos(Math.toRadians(angle.toDouble())).toFloat()
    val endY = center.y + length * sin(Math.toRadians(angle.toDouble())).toFloat()

    // Draw shadow
    drawLine(
        color = Color.Black.copy(alpha = 0.2f),
        start = Offset(center.x + 2, center.y + 2),
        end = Offset(endX + 2, endY + 2),
        strokeWidth = 6.dp.toPx(),
        cap = StrokeCap.Round,
    )

    // Draw main hand
    drawLine(
        color = color,
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 5.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawSecondHand(
    center: Offset,
    length: Float,
    second: Int,
    color: Color,
) {
    val angle = second * 6f - 90f
    val endX = center.x + length * cos(Math.toRadians(angle.toDouble())).toFloat()
    val endY = center.y + length * sin(Math.toRadians(angle.toDouble())).toFloat()

    // Draw thin second hand
    drawLine(
        color = color,
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 2.5.dp.toPx(),
        cap = StrokeCap.Round,
    )

    // Draw second hand circle
    drawCircle(
        color = color,
        radius = 8f,
        center =
            Offset(
                center.x + (length * 0.8f) * cos(Math.toRadians(angle.toDouble())).toFloat(),
                center.y + (length * 0.8f) * sin(Math.toRadians(angle.toDouble())).toFloat(),
            ),
    )
}
