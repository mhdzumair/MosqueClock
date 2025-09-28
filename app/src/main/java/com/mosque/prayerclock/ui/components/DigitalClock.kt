package com.mosque.prayerclock.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    val localDateTime = timeToUse.toLocalDateTime(TimeZone.currentSystemDefault())

    val date = Date(timeToUse.toEpochMilliseconds())

    // Get Hijri date from repository or fallback
    var hijriDate by remember {
        mutableStateOf<com.mosque.prayerclock.data.repository.HijriDateRepository.HijriDate?>(null)
    }

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
            val ampm =
                if (localDateTime.hour <
                    12
                ) {
                    localizedStringResource(R.string.am)
                } else {
                    localizedStringResource(R.string.pm)
                }
            val timeStr = String.format("%d:%02d", hour12, localDateTime.minute)
            val timeWithSeconds =
                if (showSeconds) {
                    "$timeStr:${String.format(
                        "%02d",
                        localDateTime.second,
                    )}"
                } else {
                    timeStr
                }
            "$timeWithSeconds $ampm"
        }

    // Clean, elegant digital clock with subtle styling
    Card(
        modifier = modifier.padding(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), // Clean, subtle background
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Clean time display with elegant brass color
            Text(
                text = formattedTime,
                style =
                    MaterialTheme.typography.displayLarge.copy(
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        shadow =
                            Shadow(
                                color =
                                    androidx.compose.ui.graphics
                                        .Color(0xFFB08D57)
                                        .copy(alpha = 0.3f),
                                offset = Offset(0f, 2f),
                                blurRadius = 8f,
                            ),
                    ),
                color =
                    androidx.compose.ui.graphics
                        .Color(0xFFB08D57),
                // Elegant brass color
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )

            Spacer(modifier = Modifier.height((fontSize.value * 0.1f).dp))

            // Clean date display - animated for language transitions
            AnimatedContent(
                targetState = formattedDate,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(80, easing = LinearEasing),
                    ) togetherWith
                        fadeOut(
                            animationSpec = tween(40, easing = LinearEasing),
                        )
                },
                label = "digital_date_transition",
            ) { animatedDate ->
                Text(
                    text = animatedDate,
                    style =
                        MaterialTheme.typography.headlineLarge.copy(
                            fontSize = fontSize * 0.28f,
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), // Subtle text color
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
