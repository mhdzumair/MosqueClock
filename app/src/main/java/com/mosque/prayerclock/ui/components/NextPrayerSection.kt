package com.mosque.prayerclock.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.data.model.PrayerTimesWithIqamah
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.ui.LocalEffectiveLanguage
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.theme.ColorAzanTime
import com.mosque.prayerclock.ui.theme.ColorIqamahTime
import com.mosque.prayerclock.ui.theme.ColorSunriseTime
import com.mosque.prayerclock.utils.TimeUtils
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.min

/**
 * Next Prayer Countdown Section Component
 *
 * Displays the next prayer name, time, and countdown timer.
 * This is the prominent card shown in the middle of the main screen.
 *
 * Features:
 * - Dynamic font sizing based on available space
 * - Color-coded prayer times (Azan/Iqamah/Sunrise)
 * - Animated flip clock countdown
 * - Automatic transition detection between prayers
 */
@Composable
fun NextPrayerSection(
    prayerTimes: PrayerTimesWithIqamah,
    nextPrayer: PrayerType?,
    nextDayFajr: PrayerTimesWithIqamah? = null,
    show24Hour: Boolean = false,
    modifier: Modifier = Modifier,
    onPrayerTransition: () -> Unit = {},
    showCountdown: Boolean = false,
    currentTime: Instant = Clock.System.now(),
    isCurrentPrayer: Boolean = false,
) {
    val prayerInfoList = createPrayerInfoList(prayerTimes, currentTime)

    // If next prayer is Fajr and we have next day times, use tomorrow's Fajr
    val nextPrayerInfo =
        if (nextPrayer == PrayerType.FAJR && nextDayFajr != null) {
            val tomorrowPrayerInfoList = createPrayerInfoList(nextDayFajr)
            tomorrowPrayerInfoList.find { it.type == PrayerType.FAJR }
        } else {
            prayerInfoList.find { it.type == nextPrayer }
        }

    // Use passed current time instead of recalculating
    val now = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val currentTimeString =
        "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"

    // Calculate if we should show Iqamah time (for display purposes)
    val isAfterAzan =
        nextPrayerInfo?.let { prayer ->
            TimeUtils.compareTimeStrings(prayer.azanTime, currentTimeString) <= 0
        }
            ?: false

    val isBeforeIqamah =
        nextPrayerInfo?.iqamahTime?.let { iqamahTime ->
            TimeUtils.compareTimeStrings(currentTimeString, iqamahTime) < 0
        }
            ?: false

    val shouldShowIqamah = isAfterAzan && isBeforeIqamah && nextPrayerInfo?.iqamahTime != null

    // Track prayer transition to trigger ViewModel update
    val previousIsCurrentPrayer = remember { mutableStateOf(isCurrentPrayer) }
    LaunchedEffect(isCurrentPrayer) {
        if (previousIsCurrentPrayer.value != isCurrentPrayer) {
            // Prayer status changed, trigger update
            onPrayerTransition()
            previousIsCurrentPrayer.value = isCurrentPrayer
        }
    }

    if (nextPrayerInfo != null) {
        // Use passed showCountdown parameter instead of recalculating
        val targetTime =
            if (shouldShowIqamah) {
                nextPrayerInfo.iqamahTime ?: nextPrayerInfo.azanTime
            } else {
                nextPrayerInfo.azanTime
            }
        val countdownData = getCountdownData(targetTime, currentTime)

        Card(
            modifier = modifier.fillMaxWidth().padding(1.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxSize().padding(2.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Use BoxWithConstraints for dynamic font sizing
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val density = LocalDensity.current
                        val availableHeightPx = with(density) { maxHeight.toPx() }
                        val availableWidthPx = with(density) { maxWidth.toPx() }

                        // Get effective language and font scaling
                        val effectiveLanguage = LocalEffectiveLanguage.current
                        val fontScale = getLanguageFontScale(effectiveLanguage)

                        // Calculate dynamic font sizes based on available space
                        // Adjust based on whether countdown is visible
                        val calculatedTimeFontSize =
                            with(density) {
                                // When countdown is visible, use less height (18% instead of 25%)
                                // When no countdown, prayer time can use more (30%)
                                val heightPercentage = if (showCountdown) 0.18f else 0.30f
                                val heightBasedSize = (availableHeightPx * heightPercentage).toSp()

                                // Consider width for time display (typical: "12:00 PM" = 8 chars)
                                val widthBasedSize = (availableWidthPx / 8f * 1.5f).toSp()

                                // Use smaller to ensure it fits
                                min(heightBasedSize.value, widthBasedSize.value).sp
                            }

                        // Title text scales with language (45% of time size, then scaled)
                        val calculatedTitleFontSize = calculatedTimeFontSize * 0.45f * fontScale

                        // Minimal padding to maximize space for content
                        val paddingSize = if (showCountdown) 6.dp else 2.dp

                        Column(
                            modifier = Modifier.fillMaxSize().padding(paddingSize),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement =
                                if (showCountdown) {
                                    Arrangement.SpaceEvenly // Distribute space evenly when countdown is visible
                                } else {
                                    Arrangement.SpaceBetween // Use space between when no countdown
                                },
                        ) {
                            // Top section - Next/Current Prayer text (if not sunrise)
                            if (nextPrayerInfo.type != PrayerType.SUNRISE) {
                                Text(
                                    text =
                                        if (isCurrentPrayer) {
                                            localizedStringResource(R.string.current_prayer)
                                        } else {
                                            localizedStringResource(R.string.next_prayer)
                                        },
                                    style =
                                        MaterialTheme.typography.titleLarge.copy(
                                            fontSize = calculatedTitleFontSize,
                                        ),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium,
                                )
                            } else {
                                // Add empty spacer for sunrise to maintain layout balance
                                Spacer(modifier = Modifier.height(1.dp))
                            }

                            // Middle section - Prayer name
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                // Display prayer name with Azan/Iqamah suffix for prayers (not sunrise/jummah)
                                val displayText =
                                    when {
                                        nextPrayerInfo.iqamahTime == null -> nextPrayerInfo.name // Sunrise or Jummah
                                        shouldShowIqamah ->
                                            "${nextPrayerInfo.name} ${localizedStringResource(R.string.iqamah)}"
                                        else -> "${nextPrayerInfo.name} ${localizedStringResource(R.string.azan)}"
                                    }

                                // Calculate dynamic font size based on actual text length (with language scaling)
                                val dynamicPrayerNameFontSize =
                                    with(density) {
                                        val textLength = displayText.length.toFloat() + 3f
                                        val widthBasedSize = (availableWidthPx / textLength * 1.5f * fontScale).toSp()

                                        // Also constrain by time font size (should be smaller)
                                        val maxSize = (calculatedTimeFontSize.value * 0.65f).sp

                                        // Use smaller to ensure single line
                                        min(widthBasedSize.value, maxSize.value).sp
                                    }

                                Text(
                                    text = displayText,
                                    style =
                                        MaterialTheme.typography.headlineLarge.copy(
                                            fontSize = dynamicPrayerNameFontSize,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                )

                                // Dynamic spacing based on countdown visibility
                                Spacer(modifier = Modifier.height(if (showCountdown) 8.dp else 16.dp))

                                // Prayer time display
                                val timeToShow =
                                    if (shouldShowIqamah) {
                                        // Show Iqamah time if we're past Azan time
                                        nextPrayerInfo.iqamahTime ?: nextPrayerInfo.azanTime
                                    } else {
                                        // Show Azan time by default (or sunrise time for sunrise)
                                        nextPrayerInfo.azanTime
                                    }

                                // Apply color coding to time display
                                val timeColor =
                                    if (nextPrayerInfo.type == PrayerType.SUNRISE) {
                                        ColorSunriseTime // Sunrise time indicator
                                    } else if (shouldShowIqamah) {
                                        ColorIqamahTime // Iqamah time indicator
                                    } else {
                                        ColorAzanTime // Azan time indicator
                                    }

                                Text(
                                    text = TimeUtils.formatTimeBasedOnPreference(timeToShow, show24Hour),
                                    style =
                                        MaterialTheme.typography.displayLarge.copy(
                                            fontSize = calculatedTimeFontSize,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    color = timeColor,
                                )
                            }

                            // Bottom section - Countdown (if visible) or spacer
                            if (showCountdown) {
                                // Give countdown more space by wrapping in a Box that can expand
                                Box(
                                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CountdownDisplay(
                                        hours = countdownData.hours,
                                        minutes = countdownData.minutes,
                                        seconds = countdownData.seconds,
                                    )
                                }
                            } else {
                                // Add flexible spacer when no countdown to push content up
                                Spacer(modifier = Modifier.height(1.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Countdown Display
 * Shows the countdown timer with flip clock animation
 */
@Composable
fun CountdownDisplay(
    hours: Long,
    minutes: Long,
    seconds: Long,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val availableHeightPx = with(density) { maxHeight.toPx() }
        val availableWidthPx = with(density) { maxWidth.toPx() }

        // Calculate digit size dynamically
        val calculatedDigitSize =
            with(density) {
                val heightBasedSize = (availableHeightPx * 0.80f).toSp()
                val numElements = if (hours > 0) 8f else 5f // HH:MM:SS or MM:SS
                val widthBasedSize = (availableWidthPx / numElements * 1.0f).toSp()
                min(heightBasedSize.value, widthBasedSize.value).sp
            }

        FlipClockCountdown(
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            digitBoxSize = calculatedDigitSize,
        )
    }
}

/**
 * Flip Clock Countdown
 * Animated countdown timer using flip clock digits
 */
@Composable
fun FlipClockCountdown(
    hours: Long,
    minutes: Long,
    seconds: Long,
    digitBoxSize: androidx.compose.ui.unit.TextUnit = 64.sp,
) {
    val density = LocalDensity.current

    // Calculate spacing proportional to digit size
    val spacing = with(density) { (digitBoxSize.toPx() * 0.15f).toDp() }
    val colonSize = digitBoxSize * 0.70f

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Show hours only if > 0
        if (hours > 0) {
            FlipClockDigitPair(
                value = hours,
                digitBoxSize = digitBoxSize,
            )

            Text(
                text = ":",
                style =
                    MaterialTheme.typography.displayLarge.copy(
                        fontSize = colonSize,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Minutes
        FlipClockDigitPair(
            value = minutes,
            digitBoxSize = digitBoxSize,
        )

        Text(
            text = ":",
            style =
                MaterialTheme.typography.displayLarge.copy(
                    fontSize = colonSize,
                    fontWeight = FontWeight.Bold,
                ),
            color = MaterialTheme.colorScheme.primary,
        )

        // Seconds
        FlipClockDigitPair(
            value = seconds,
            digitBoxSize = digitBoxSize,
        )
    }
}

/**
 * Helper function to get countdown data
 * Public so it can be used by MainScreen for full-screen countdown logic
 */
fun getCountdownData(
    prayerTime: String,
    currentTime: Instant,
): CountdownData {
    try {
        val now = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
        val parts = prayerTime.split(":")
        val prayerHour = parts[0].toInt()
        val prayerMinute = parts[1].toInt()

        // Create prayer time for today
        val prayerDateTime =
            LocalDateTime(
                now.date,
                LocalTime(prayerHour, prayerMinute, 0), // Set seconds to 0 for consistency
            )

        val prayerInstant = prayerDateTime.toInstant(TimeZone.currentSystemDefault())
        val duration = prayerInstant - currentTime

        if (duration.isNegative()) {
            // Return -1 for totalSeconds to clearly indicate we've passed the target time
            return CountdownData(0, 0, 0, 0, -1)
        }

        val hours = duration.inWholeHours
        val minutes = (duration.inWholeMinutes % 60)
        val seconds = (duration.inWholeSeconds % 60)
        val totalMinutes = duration.inWholeMinutes
        val totalSeconds = duration.inWholeSeconds

        return CountdownData(hours, minutes, seconds, totalMinutes, totalSeconds)
    } catch (e: Exception) {
        // Return -1 for totalSeconds to indicate an error/invalid state
        return CountdownData(0, 0, 0, 0, -1)
    }
}

/**
 * Helper function to create prayer info list from prayer times
 */
@Composable
private fun createPrayerInfoList(
    prayerTimes: PrayerTimesWithIqamah,
    currentTime: Instant = Clock.System.now(),
): List<PrayerInfo> {
    val currentDate = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val isFriday = currentDate.dayOfWeek == DayOfWeek.FRIDAY

    return listOf(
        PrayerInfo(
            type = PrayerType.FAJR,
            azanTime = prayerTimes.fajrAzan,
            iqamahTime = prayerTimes.fajrIqamah,
            name = localizedStringResource(R.string.fajr),
        ),
        PrayerInfo(
            type = PrayerType.SUNRISE,
            azanTime = prayerTimes.sunrise,
            iqamahTime = null,
            name = localizedStringResource(R.string.sunrise),
        ),
        PrayerInfo(
            type = PrayerType.DHUHR,
            azanTime = prayerTimes.dhuhrAzan,
            iqamahTime =
                if (isFriday) {
                    null
                } else {
                    prayerTimes.dhuhrIqamah
                }, // No Iqamah on Friday (Bayan instead)
            name =
                if (isFriday) {
                    localizedStringResource(R.string.jummah)
                } else {
                    localizedStringResource(R.string.dhuhr)
                },
        ),
        PrayerInfo(
            type = PrayerType.ASR,
            azanTime = prayerTimes.asrAzan,
            iqamahTime = prayerTimes.asrIqamah,
            name = localizedStringResource(R.string.asr),
        ),
        PrayerInfo(
            type = PrayerType.MAGHRIB,
            azanTime = prayerTimes.maghribAzan,
            iqamahTime = prayerTimes.maghribIqamah,
            name = localizedStringResource(R.string.maghrib),
        ),
        PrayerInfo(
            type = PrayerType.ISHA,
            azanTime = prayerTimes.ishaAzan,
            iqamahTime = prayerTimes.ishaIqamah,
            name = localizedStringResource(R.string.isha),
        ),
    )
}

/**
 * Data class to hold countdown information
 */
data class CountdownData(
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val totalMinutes: Long,
    val totalSeconds: Long,
)
