package com.mosque.prayerclock.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DigitalClock(
    show24Hour: Boolean = false,
    showSeconds: Boolean = true,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 96.sp,
    language: com.mosque.prayerclock.data.model.Language = com.mosque.prayerclock.data.model.Language.ENGLISH
) {
    var currentTime by remember { mutableStateOf(Clock.System.now()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Clock.System.now()
            delay(1000)
        }
    }
    
    val localDateTime = currentTime.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    
    val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
    val date = Date(currentTime.toEpochMilliseconds())
    
    // Get Arabic month names in Tamil
    val arabicMonthsTamil = listOf(
        "முஹர்ரம்", "ஸபர்", "ரபீ அவ்வல்", "ரபீ ஆகிர்",
        "ஜுமாதா அவ்வல்", "ஜுமாதா ஆகிர்", "ரஜப்", "ஷக்பான்",
        "ரமலான்", "ஷவ்வல்", "துல் க்வித்தா", "துல் ஹிஜ்ஜா"
    )
    
    val formattedDate = if (language == com.mosque.prayerclock.data.model.Language.TAMIL) {
        // Get current Islamic/Arabic month (simplified - using Gregorian month for demo)
        val currentMonth = localDateTime.monthNumber - 1
        val arabicMonth = arabicMonthsTamil.getOrNull(currentMonth) ?: arabicMonthsTamil[0]
        val dayNames = listOf("ஞாயிறு", "திங்கள்", "செவ்வாய்", "புதன்", "வியாழன்", "வெள்ளி", "சனி")
        val dayName = dayNames[(localDateTime.dayOfWeek.ordinal) % 7]
        "$dayName, $arabicMonth ${localDateTime.dayOfMonth}, ${localDateTime.year}"
    } else {
        dateFormat.format(date)
    }
    
    // Format time manually for better 12/24 hour control
    val formattedTime = if (show24Hour) {
        val timeStr = String.format("%02d:%02d", localDateTime.hour, localDateTime.minute)
        if (showSeconds) "$timeStr:${String.format("%02d", localDateTime.second)}" else timeStr
    } else {
        val hour12 = if (localDateTime.hour == 0) 12 else if (localDateTime.hour > 12) localDateTime.hour - 12 else localDateTime.hour
        val ampm = if (localDateTime.hour < 12) "AM" else "PM"
        val timeStr = String.format("%d:%02d", hour12, localDateTime.minute)
        val timeWithSeconds = if (showSeconds) "$timeStr:${String.format("%02d", localDateTime.second)}" else timeStr
        "$timeWithSeconds $ampm"
    }
    
    Card(
        modifier = modifier
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Time display with glow effect
            Box {
                // Glow effect
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            offset = Offset(0f, 0f),
                            blurRadius = 20f
                        )
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    textAlign = TextAlign.Center
                )
                // Main text
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height((fontSize.value * 0.08f).dp))
            
            // Date with subtle styling
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = fontSize * 0.29f,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}