package com.mosque.prayerclock.ui.components.screenfaces

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mosque.prayerclock.ui.components.clockfaces.ClockDisplayData
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Minimal, high-contrast analogue dial belonging to the Horizon display family. */
@Composable
fun HorizonAnalogClock(data: ClockDisplayData, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val foreground = MaterialTheme.colorScheme.onBackground
    val muted = foreground.copy(alpha = 0.30f)
    val secondHand = MaterialTheme.colorScheme.secondary

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val diameter = min(maxWidth.value, maxHeight.value).dp
        Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension * 0.46f

                drawCircle(foreground.copy(alpha = 0.025f), radius)
                drawCircle(primary.copy(alpha = 0.72f), radius, style = Stroke(radius * 0.014f))
                drawCircle(muted.copy(alpha = 0.18f), radius * 0.76f, style = Stroke(radius * 0.006f))

                repeat(60) { index ->
                    val angle = index * 6f - 90f
                    val major = index % 5 == 0
                    if (major) {
                        drawLine(
                            color = if (index % 15 == 0) primary else foreground.copy(alpha = 0.72f),
                            start = polar(center, radius * 0.84f, angle),
                            end = polar(center, radius * 0.94f, angle),
                            strokeWidth = radius * if (index % 15 == 0) 0.030f else 0.020f,
                            cap = StrokeCap.Round,
                        )
                    } else {
                        drawCircle(muted, radius * 0.008f, polar(center, radius * 0.90f, angle))
                    }
                }

                val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = foreground.copy(alpha = 0.84f).toArgb()
                    textAlign = Paint.Align.CENTER
                    textSize = radius * 0.18f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                drawIntoCanvas { canvas ->
                    listOf("12" to -90f, "3" to 0f, "6" to 90f, "9" to 180f).forEach { (label, angle) ->
                        val point = polar(center, radius * 0.66f, angle)
                        val baseline = point.y - (numberPaint.ascent() + numberPaint.descent()) / 2f
                        canvas.nativeCanvas.drawText(label, point.x, baseline, numberPaint)
                    }
                }

                val hourAngle = (data.hour12 * 30f) + (data.minute * 0.5f) - 90f
                val minuteAngle = (data.minute * 6f) + (data.second * 0.1f) - 90f
                drawLine(
                    foreground,
                    polar(center, radius * 0.10f, hourAngle + 180f),
                    polar(center, radius * 0.50f, hourAngle),
                    radius * 0.055f,
                    StrokeCap.Round,
                )
                drawLine(
                    primary,
                    polar(center, radius * 0.12f, minuteAngle + 180f),
                    polar(center, radius * 0.74f, minuteAngle),
                    radius * 0.038f,
                    StrokeCap.Round,
                )
                if (data.showSeconds) {
                    val secondAngle = data.second * 6f - 90f
                    drawLine(
                        secondHand,
                        polar(center, radius * 0.16f, secondAngle + 180f),
                        polar(center, radius * 0.82f, secondAngle),
                        radius * 0.012f,
                        StrokeCap.Round,
                    )
                }
                drawCircle(foreground, radius * 0.060f, center)
                drawCircle(primary, radius * 0.040f, center)
                drawCircle(foreground, radius * 0.018f, center)
            }
        }
    }
}

private fun polar(center: Offset, length: Float, degrees: Float): Offset {
    val radians = degrees * PI.toFloat() / 180f
    return Offset(center.x + cos(radians) * length, center.y + sin(radians) * length)
}
