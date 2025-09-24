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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
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
import kotlin.math.floor

@Composable
fun DigitalClock(
    show24Hour: Boolean = false,
    showSeconds: Boolean = true,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 96.sp,
    hijriDateRepository: com.mosque.prayerclock.data.repository.HijriDateRepository? = null,
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

    fun addOrdinalLocalized(day: Int): String =
        if (currentLocale.language.equals("en", ignoreCase = true)) {
            if (day in 11..13) {
                "${day}th"
            } else {
                when (day % 10) {
                    1 -> "${day}st"
                    2 -> "${day}nd"
                    3 -> "${day}rd"
                    else -> "${day}th"
                }
            }
        } else {
            // For non-English, just return the number; month/year are localized separately
            day.toString()
        }

    // Format Gregorian as: 8th August 2025
    val monthYear = SimpleDateFormat("MMMM yyyy", currentLocale).format(date)
    val gregorianPretty = "${addOrdinalLocalized(localDateTime.dayOfMonth)} $monthYear"

    val formattedDate =
        hijriDate?.let { hDate ->
            val hijriMonth = hijriMonthNames.getOrNull(hDate.month - 1) ?: hijriMonthNames[0]
            val dayName = dayNames[localDateTime.dayOfWeek.ordinal % 7]
            // Show day name + Hijri date + pretty Gregorian date
            "$dayName, $hijriMonth ${hDate.day}, ${hDate.year}\n$gregorianPretty"
        } ?: run {
            // Fallback if hijriDate is not loaded yet
            val dayName = dayNames[localDateTime.dayOfWeek.ordinal % 7]
            "$dayName, ${localizedStringResource(R.string.loading_hijri)}\n$gregorianPretty"
        }

    // Format time manually for better 12/24 hour control
    val formattedTime =
        if (show24Hour) {
            val timeStr = String.format("%02d:%02d", localDateTime.hour, localDateTime.minute)
            if (showSeconds) "$timeStr:${String.format("%02d", localDateTime.second)}" else timeStr
        } else {
            val hour12 =
                if (localDateTime.hour == 0) {
                    12
                } else if (localDateTime.hour > 12) {
                    localDateTime.hour - 12
                } else {
                    localDateTime.hour
                }
            val ampm = if (localDateTime.hour < 12) localizedStringResource(R.string.am) else localizedStringResource(R.string.pm)
            val timeStr = String.format("%d:%02d", hour12, localDateTime.minute)
            val timeWithSeconds = if (showSeconds) "$timeStr:${String.format("%02d", localDateTime.second)}" else timeStr
            "$timeWithSeconds $ampm"
        }

    Card(
        modifier =
            modifier
                .padding(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Time display with glow effect
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                // Glow effect
                Text(
                    text = formattedTime,
                    style =
                        MaterialTheme.typography.displayLarge.copy(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            shadow =
                                Shadow(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 20f,
                                ),
                        ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                )
                // Main text
                Text(
                    text = formattedTime,
                    style =
                        MaterialTheme.typography.displayLarge.copy(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                        ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                )
            }

            Spacer(modifier = Modifier.height((fontSize.value * 0.08f).dp))

            // Date with subtle styling
            Text(
                text = formattedDate,
                style =
                    MaterialTheme.typography.headlineLarge.copy(
                        fontSize = fontSize * 0.29f,
                        fontWeight = FontWeight.Medium,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}
