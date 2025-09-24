package com.mosque.prayerclock.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.ui.localizedStringArrayResource
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.utils.LocaleManager
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

@Composable
fun AnalogClock(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 200.dp,
    currentTime: Instant? = null,
    hijriDateRepository: com.mosque.prayerclock.data.repository.HijriDateRepository? = null,
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

    val date = Date(timeToUse.toEpochMilliseconds())

    // Get Hijri date from repository or fallback
    var hijriDate by remember { mutableStateOf<com.mosque.prayerclock.data.repository.HijriDateRepository.HijriDate?>(null) }

    LaunchedEffect(localDateTime.date) {
        hijriDate = hijriDateRepository?.getCurrentHijriDate()
            ?: com.mosque.prayerclock.data.repository.HijriDateRepository
                .HijriDate(7, 2, 1447) // Default fallback
    }

    val dayNames = localizedStringArrayResource(R.array.day_names)
    val hijriMonthNames = localizedStringArrayResource(R.array.hijri_months)

    // Helper to add ordinal suffix to day of month (English only)
    val currentLocale =
        LocaleManager.getCurrentLocale(
            com.mosque.prayerclock.ui.LocalLocalizedContext.current,
        )

    // Use Box layout to maximize space utilization with overlays
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Main clock - takes maximum available space
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            val radius = this.size.minDimension * 0.47f
            val center = this.size.center

            // Elegant forest green gradient background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2D5016), // Deep forest green center
                        Color(0xFF1A2E0A), // Darker forest green edge
                    ),
                    radius = radius,
                    center = center,
                ),
                radius = radius,
                center = center,
            )

            // Elegant brass/bronze outer rim
            drawCircle(
                color = Color(0xFFB08D57), // Realistic brass color
                radius = radius,
                center = center,
                style = Stroke(width = 6.dp.toPx()),
            )

            // Inner brass accent ring
            drawCircle(
                color = Color(0xFFB08D57).copy(alpha = 0.3f),
                radius = radius * 0.95f,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )

            // Draw sunburst pattern
            drawSunburstPattern(center, radius * 0.9f, Color(0xFFB08D57).copy(alpha = 0.4f))

            // Draw modern minimalist numbers (only 12, 3, 6, 9)
            drawModernNumbers(center, radius * 0.8f, Color(0xFFB08D57))

            // Draw modern hour markers
            drawModernHourMarkers(center, radius, Color(0xFFB08D57))

            // Draw modern hands
            drawModernHourHand(center, radius * 0.5f, hour, minute, Color(0xFFB08D57))
            drawModernMinuteHand(center, radius * 0.7f, minute, Color(0xFFB08D57))
            drawModernSecondHand(center, radius * 0.75f, second, Color(0xFFF5F5DC)) // Elegant cream color

            // Modern center piece
            drawCircle(
                color = Color(0xFFB08D57),
                radius = 12f,
                center = center,
            )
            drawCircle(
                color = Color(0xFF2D5016),
                radius = 8f,
                center = center,
            )
            drawCircle(
                color = Color(0xFFB08D57),
                radius = 4f,
                center = center,
            )
        }

        // Left side - Hijri date info (compact overlay)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp)
                .width(90.dp), // Match the Gregorian date card width
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                hijriDate?.let { hDate ->
                    val hijriMonth = hijriMonthNames.getOrNull(hDate.month - 1) ?: hijriMonthNames[0]
                    
                    // Month name (slightly larger)
                    Text(
                        text = hijriMonth,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                    
                    Spacer(modifier = Modifier.height(3.dp))
                    
                    // Day (more prominent)
                    Text(
                        text = hDate.day.toString(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // Year (slightly larger)
                    Text(
                        text = hDate.year.toString(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Right side - Gregorian date info (compact overlay)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 12.dp)
                .width(90.dp), // Slightly wider for full month name
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val dayName = dayNames[localDateTime.dayOfWeek.ordinal % 7]
                val monthName = SimpleDateFormat("MMMM", currentLocale).format(date)
                
                // Day name (slightly larger)
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                
                Spacer(modifier = Modifier.height(3.dp))
                
                // Day number (more prominent)
                Text(
                    text = localDateTime.dayOfMonth.toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Month name (larger for readability)
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    maxLines = 2, // Allow wrapping for long month names
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Year (slightly larger)
                Text(
                    text = localDateTime.year.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun DrawScope.drawEnhancedClockTicks(
    center: Offset,
    radius: Float,
    tickColor: Color,
    hourTickColor: Color,
) {
    // Draw hour ticks (thicker and more prominent)
    for (i in 0 until 12) {
        val angle = i * 30f - 90f
        val startRadius = radius * 0.82f
        val endRadius = radius * 0.95f

        val startX = center.x + startRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val startY = center.y + startRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
        val endX = center.x + endRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val endY = center.y + endRadius * sin(Math.toRadians(angle.toDouble())).toFloat()

        // Draw shadow for depth
        drawLine(
            color = Color.Black.copy(alpha = 0.1f),
            start = Offset(startX + 1, startY + 1),
            end = Offset(endX + 1, endY + 1),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round,
        )

        // Draw main tick
        drawLine(
            color = hourTickColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    // Draw minute ticks (thinner but more visible)
    for (i in 0 until 60) {
        if (i % 5 != 0) {
            val angle = i * 6f - 90f
            val startRadius = radius * 0.88f
            val endRadius = radius * 0.94f

            val startX = center.x + startRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val startY = center.y + startRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
            val endX = center.x + endRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val endY = center.y + endRadius * sin(Math.toRadians(angle.toDouble())).toFloat()

            drawLine(
                color = tickColor.copy(alpha = 0.6f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawEnhancedClockNumbers(
    center: Offset,
    radius: Float,
    color: Color,
    primaryColor: Color,
) {
    val numbers = listOf("12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")

    drawIntoCanvas { canvas ->
        val paint =
            Paint().apply {
                this.color = color.toArgb()
                textSize = 42f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            }

        for (i in numbers.indices) {
            val angle = i * 30f - 90f
            val x = center.x + radius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val y = center.y + radius * sin(Math.toRadians(angle.toDouble())).toFloat()

            // Draw enhanced number background circle with gradient effect
            drawCircle(
                color = primaryColor.copy(alpha = 0.15f),
                radius = 28f,
                center = Offset(x, y),
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.05f),
                radius = 24f,
                center = Offset(x, y),
            )

            // Draw shadow for the text
            val shadowPaint = Paint().apply {
                this.color = Color.Black.copy(alpha = 0.2f).toArgb()
                textSize = 42f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            }
            canvas.nativeCanvas.drawText(
                numbers[i],
                x + 1,
                y + paint.textSize / 3f + 1,
                shadowPaint,
            )

            // Draw the main number text
            canvas.nativeCanvas.drawText(
                numbers[i],
                x,
                y + paint.textSize / 3f,
                paint,
            )
        }
    }
}

private fun DrawScope.drawEnhancedHourHand(
    center: Offset,
    length: Float,
    hour: Int,
    minute: Int,
    color: Color,
) {
    val angle = (hour * 30f + minute * 0.5f) - 90f
    val endX = center.x + length * cos(Math.toRadians(angle.toDouble())).toFloat()
    val endY = center.y + length * sin(Math.toRadians(angle.toDouble())).toFloat()

    // Draw enhanced shadow
    drawLine(
        color = Color.Black.copy(alpha = 0.3f),
        start = Offset(center.x + 3, center.y + 3),
        end = Offset(endX + 3, endY + 3),
        strokeWidth = 10.dp.toPx(),
        cap = StrokeCap.Round,
    )

    // Draw outer border
    drawLine(
        color = Color.Black.copy(alpha = 0.1f),
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 10.dp.toPx(),
        cap = StrokeCap.Round,
    )

    // Draw main hand with gradient effect
    drawLine(
        color = color,
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 8.dp.toPx(),
        cap = StrokeCap.Round,
    )

    // Draw inner highlight
    drawLine(
        color = color.copy(alpha = 0.8f),
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawEnhancedMinuteHand(
    center: Offset,
    length: Float,
    minute: Int,
    color: Color,
) {
    val angle = minute * 6f - 90f
    val endX = center.x + length * cos(Math.toRadians(angle.toDouble())).toFloat()
    val endY = center.y + length * sin(Math.toRadians(angle.toDouble())).toFloat()

    // Draw enhanced shadow
    drawLine(
        color = Color.Black.copy(alpha = 0.3f),
        start = Offset(center.x + 2, center.y + 2),
        end = Offset(endX + 2, endY + 2),
        strokeWidth = 8.dp.toPx(),
        cap = StrokeCap.Round,
    )

    // Draw outer border
    drawLine(
        color = Color.Black.copy(alpha = 0.1f),
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 7.dp.toPx(),
        cap = StrokeCap.Round,
    )

    // Draw main hand
    drawLine(
        color = color,
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 6.dp.toPx(),
        cap = StrokeCap.Round,
    )

    // Draw inner highlight
    drawLine(
        color = color.copy(alpha = 0.7f),
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawEnhancedSecondHand(
    center: Offset,
    length: Float,
    second: Int,
    color: Color,
) {
    val angle = second * 6f - 90f
    val endX = center.x + length * cos(Math.toRadians(angle.toDouble())).toFloat()
    val endY = center.y + length * sin(Math.toRadians(angle.toDouble())).toFloat()

    // Draw shadow for second hand
    drawLine(
        color = Color.Black.copy(alpha = 0.2f),
        start = Offset(center.x + 1, center.y + 1),
        end = Offset(endX + 1, endY + 1),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
    )

    // Draw thin second hand
    drawLine(
        color = color,
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
    )

    // Draw enhanced second hand circle with gradient
    val circleCenter = Offset(
        center.x + (length * 0.8f) * cos(Math.toRadians(angle.toDouble())).toFloat(),
        center.y + (length * 0.8f) * sin(Math.toRadians(angle.toDouble())).toFloat(),
    )
    
    // Shadow for circle
    drawCircle(
        color = Color.Black.copy(alpha = 0.2f),
        radius = 10f,
        center = Offset(circleCenter.x + 1, circleCenter.y + 1),
    )
    
    // Main circle
    drawCircle(
        color = color,
        radius = 10f,
        center = circleCenter,
    )
    
    // Inner highlight circle
    drawCircle(
        color = color.copy(alpha = 0.6f),
        radius = 6f,
        center = circleCenter,
    )
}

// Modern sunburst pattern drawing function
private fun DrawScope.drawSunburstPattern(
    center: Offset,
    radius: Float,
    color: Color,
) {
    val lineCount = 60 // One line per minute
    for (i in 0 until lineCount) {
        val angle = i * 6f - 90f // 6 degrees per line
        val startRadius = radius * 0.3f // Start from inner area
        val endRadius = radius * 0.85f // End near outer edge
        
        val startX = center.x + startRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val startY = center.y + startRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
        val endX = center.x + endRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val endY = center.y + endRadius * sin(Math.toRadians(angle.toDouble())).toFloat()

        // Vary line opacity for sunburst effect
        val alpha = if (i % 5 == 0) 0.6f else 0.2f
        
        drawLine(
            color = color.copy(alpha = alpha),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = if (i % 5 == 0) 2.dp.toPx() else 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

// Modern minimalist numbers (only 12, 3, 6, 9)
private fun DrawScope.drawModernNumbers(
    center: Offset,
    radius: Float,
    color: Color,
) {
    val numbers = listOf(
        Pair("12", 0),
        Pair("3", 3),
        Pair("6", 6),
        Pair("9", 9)
    )

    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            this.color = color.toArgb()
            textSize = 48f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }

        numbers.forEach { (number, position) ->
            val angle = position * 30f - 90f
            val x = center.x + radius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val y = center.y + radius * sin(Math.toRadians(angle.toDouble())).toFloat()

            // Draw modern number with background
            drawCircle(
                color = Color(0xFF1A2E0A), // Dark forest green background
                radius = 30f,
                center = Offset(x, y),
            )
            
            drawCircle(
                color = color.copy(alpha = 0.1f),
                radius = 28f,
                center = Offset(x, y),
                style = Stroke(width = 2.dp.toPx()),
            )

            canvas.nativeCanvas.drawText(
                number,
                x,
                y + paint.textSize / 3f,
                paint,
            )
        }
    }
}

// Modern hour markers (rectangular bars)
private fun DrawScope.drawModernHourMarkers(
    center: Offset,
    radius: Float,
    color: Color,
) {
    for (i in 0 until 12) {
        // Skip positions where numbers are displayed (12, 3, 6, 9)
        if (i % 3 == 0) continue
        
        val angle = i * 30f - 90f
        val startRadius = radius * 0.85f
        val endRadius = radius * 0.95f
        
        val startX = center.x + startRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val startY = center.y + startRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
        val endX = center.x + endRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val endY = center.y + endRadius * sin(Math.toRadians(angle.toDouble())).toFloat()

        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

// Modern hour hand design
private fun DrawScope.drawModernHourHand(
    center: Offset,
    length: Float,
    hour: Int,
    minute: Int,
    color: Color,
) {
    val angle = (hour * 30f + minute * 0.5f) - 90f
    val endX = center.x + length * cos(Math.toRadians(angle.toDouble())).toFloat()
    val endY = center.y + length * sin(Math.toRadians(angle.toDouble())).toFloat()

    // Modern tapered hand design
    drawLine(
        color = color,
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 8.dp.toPx(),
        cap = StrokeCap.Round,
    )
    
    // Inner highlight
    drawLine(
        color = Color.White.copy(alpha = 0.3f),
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

// Modern minute hand design
private fun DrawScope.drawModernMinuteHand(
    center: Offset,
    length: Float,
    minute: Int,
    color: Color,
) {
    val angle = minute * 6f - 90f
    val endX = center.x + length * cos(Math.toRadians(angle.toDouble())).toFloat()
    val endY = center.y + length * sin(Math.toRadians(angle.toDouble())).toFloat()

    // Modern sleek minute hand
    drawLine(
        color = color,
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 6.dp.toPx(),
        cap = StrokeCap.Round,
    )
    
    // Inner highlight
    drawLine(
        color = Color.White.copy(alpha = 0.3f),
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

// Modern second hand design
private fun DrawScope.drawModernSecondHand(
    center: Offset,
    length: Float,
    second: Int,
    color: Color,
) {
    val angle = second * 6f - 90f
    val endX = center.x + length * cos(Math.toRadians(angle.toDouble())).toFloat()
    val endY = center.y + length * sin(Math.toRadians(angle.toDouble())).toFloat()

    // Thin modern second hand
    drawLine(
        color = color,
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
    )

    // Small circle at the end
    drawCircle(
        color = color,
        radius = 6f,
        center = Offset(endX, endY),
    )
}
