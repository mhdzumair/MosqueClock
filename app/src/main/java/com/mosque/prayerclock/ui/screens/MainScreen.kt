package com.mosque.prayerclock.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.data.model.PrayerTimesWithIqamah
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.data.model.WeatherInfo
import com.mosque.prayerclock.data.repository.HijriDateRepository
import com.mosque.prayerclock.ui.components.AnalogClock
import com.mosque.prayerclock.ui.components.DigitalClock
import com.mosque.prayerclock.ui.components.PrayerTimeCard
import com.mosque.prayerclock.ui.components.WeatherCard
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.utils.TimeUtils
import com.mosque.prayerclock.viewmodel.MainUiState
import com.mosque.prayerclock.viewmodel.MainViewModel
import com.mosque.prayerclock.viewmodel.WeatherUiState
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

// Helper data class for returning four values
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

@Composable
fun MainScreen(
    onOpenSettings: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // Global synchronized animation state for all prayer cards
    var globalShowAzan by remember { mutableStateOf(true) }

    // Synchronized animation timer - all cards will use this state
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(8000) // 8 seconds (slower, more readable)
            globalShowAzan = !globalShowAzan
        }
    }

    // Only reload data when prayer-related settings change
    LaunchedEffect(settings.prayerServiceType, settings.city, settings.country, settings.showWeather) {
        viewModel.loadPrayerTimes()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surface,
                            ),
                    ),
                ),
    ) {
        MainLayout(
            uiState = uiState,
            weatherState = weatherState,
            settings = settings,
            viewModel = viewModel,
            onOpenSettings = onOpenSettings,
            globalShowAzan = globalShowAzan,
        )
    }
}

@Composable
fun MainLayout(
    uiState: MainUiState,
    weatherState: WeatherUiState,
    settings: AppSettings,
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    globalShowAzan: Boolean,
) {
    val paddingSize = 12.dp

    // Centralized time source for all components
    var globalCurrentTime by remember { mutableStateOf(Clock.System.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            globalCurrentTime = Clock.System.now()
            kotlinx.coroutines.delay(1000)
        }
    }

    // Calculate countdown visibility, prayer state, and prayer-in-progress period once for the entire layout
    val (isCountdownVisibleForWeight, currentTimeGlobal, isCurrentPrayerGlobal, isPrayerInProgress) =
        when (uiState) {
            is MainUiState.Success -> {
                // Use centralized time source
                val currentTime = globalCurrentTime
                val prayerInfoList = createPrayerInfoList(uiState.prayerTimes, currentTime)
                val nextPrayerInfo = prayerInfoList.find { it.type == uiState.nextPrayer }

                // Memoize calculations that don't depend on current time
                val calculatedValues =
                    remember(nextPrayerInfo, currentTime) {
                        nextPrayerInfo?.let { prayer ->
                            val now =
                                currentTime.toLocalDateTime(
                                    TimeZone
                                        .currentSystemDefault(),
                                )
                            val currentTimeString =
                                "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"

                            val isAfterAzan =
                                TimeUtils.compareTimeStrings(
                                    prayer.azanTime,
                                    currentTimeString,
                                ) <= 0
                            val isBeforeIqamah =
                                prayer.iqamahTime?.let { iqamahTime ->
                                    TimeUtils.compareTimeStrings(
                                        currentTimeString,
                                        iqamahTime,
                                    ) < 0
                                }
                                    ?: false
                            val shouldShowIqamah =
                                isAfterAzan &&
                                    isBeforeIqamah &&
                                    prayer.iqamahTime != null

                            // Determine target time first
                            val targetTime =
                                if (shouldShowIqamah) {
                                    prayer.iqamahTime ?: prayer.azanTime
                                } else {
                                    prayer.azanTime
                                }

                            val countdownData =
                                getCountdownData(targetTime, currentTime)

                            // Determine when prayer is considered "current" vs "next"
                            val isCurrentPrayerLocal =
                                if (prayer.iqamahTime != null) {
                                    // For prayers with Iqamah: current from Azan
                                    // time until Iqamah + 5min buffer
                                    val bufferTime =
                                        TimeUtils.addMinutesToTime(prayer.iqamahTime, 5)
                                    isAfterAzan &&
                                        TimeUtils.compareTimeStrings(
                                            currentTimeString,
                                            bufferTime,
                                        ) <= 0
                                } else {
                                    // For prayers without Iqamah: current from Azan
                                    // time until Azan + 60min buffer
                                    val extendedTime =
                                        TimeUtils.addMinutesToTime(prayer.azanTime, 60)
                                    isAfterAzan &&
                                        TimeUtils.compareTimeStrings(
                                            currentTimeString,
                                            extendedTime,
                                        ) <= 0
                                }

                            // Determine when to hide countdown (only during buffer
                            // periods)
                            val isPastFinalTime =
                                if (prayer.iqamahTime != null) {
                                    // Hide countdown after Iqamah time (during
                                    // buffer period)
                                    TimeUtils.compareTimeStrings(
                                        currentTimeString,
                                        prayer.iqamahTime,
                                    ) > 0
                                } else {
                                    // Hide countdown after Azan + 60min for prayers
                                    // without Iqamah
                                    val extendedTime =
                                        TimeUtils.addMinutesToTime(prayer.azanTime, 60)
                                    TimeUtils.compareTimeStrings(
                                        currentTimeString,
                                        extendedTime,
                                    ) > 0
                                }

                            // Show countdown if time is positive, within 59 minutes,
                            // and not in buffer period
                            val isCountdownVisible =
                                countdownData.totalSeconds > 0 &&
                                    countdownData.totalMinutes < 60 &&
                                    !isPastFinalTime

                            // Check if we're in the 5-minute prayer-in-progress period after Iqamah
                            val isPrayerInProgress =
                                if (prayer.iqamahTime != null) {
                                    val isAfterIqamah =
                                        TimeUtils.compareTimeStrings(
                                            currentTimeString,
                                            prayer.iqamahTime,
                                        ) >= 0 // Changed from > 0 to >= 0 to include exact Iqamah time
                                    val prayerEndTime = TimeUtils.addMinutesToTime(prayer.iqamahTime, 5)
                                    val isBeforePrayerEnd =
                                        TimeUtils.compareTimeStrings(
                                            currentTimeString,
                                            prayerEndTime,
                                        ) <= 0
                                    isAfterIqamah && isBeforePrayerEnd
                                } else {
                                    false
                                }

                            Quadruple(
                                isCountdownVisible,
                                currentTime,
                                isCurrentPrayerLocal,
                                isPrayerInProgress,
                            )
                        }
                            ?: Quadruple(false, currentTime, false, false)
                    }

                calculatedValues
            }
            else -> Quadruple(false, Clock.System.now(), false, false)
        }

    Column(
        modifier = Modifier.fillMaxSize().padding(paddingSize),
    ) {
        // Main content row with revamped layout
        Row(
            modifier = Modifier.weight(0.75f),
            horizontalArrangement = Arrangement.spacedBy(4.dp), // Reduced spacing between columns
            verticalAlignment = Alignment.Top,
        ) {
            // Left column - Header and Clock section
            Column(
                modifier = Modifier.weight(1.6f), // Increased weight to give more space to clock
                verticalArrangement = Arrangement.Top, // Remove spacing between header and clock
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Mosque header at top of left column
                if (settings.mosqueName.isNotEmpty()) {
                    MosqueHeader(
                        mosqueName = settings.mosqueName,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Clock section below header
                ClockSection(
                    settings = settings,
                    onClockClick = onOpenSettings,
                    currentTime = globalCurrentTime,
                    hijriDateRepository = viewModel.hijriDateRepository,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Right column - Next Prayer and Weather section
            Column(
                modifier =
                    Modifier
                        .weight(1.0f)
                        .fillMaxHeight(),
                // Fill available height for proper scaling
                verticalArrangement = Arrangement.spacedBy(4.dp), // Reduced spacing to give more room
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Next prayer timer - Auto-scales based on available space
                when (uiState) {
                    is MainUiState.Loading -> {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(if (settings.showWeather) 2f else 1f),
                            // Adjust weight based on weather visibility
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    is MainUiState.Success -> {
                        NextPrayerCountdownSection(
                            prayerTimes = uiState.prayerTimes,
                            nextPrayer = uiState.nextPrayer,
                            nextDayFajr = uiState.nextDayFajr,
                            show24Hour = settings.show24HourFormat,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(if (settings.showWeather) 2f else 1f),
                            // Dynamic weight
                            onPrayerTransition = { viewModel.updateNextPrayer() },
                            showCountdown = isCountdownVisibleForWeight,
                            currentTime = currentTimeGlobal,
                            isCurrentPrayer = isCurrentPrayerGlobal,
                            isPrayerInProgress = isPrayerInProgress,
                        )
                    }
                    is MainUiState.Error -> {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(if (settings.showWeather) 2f else 1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = getFriendlyErrorMessage(uiState.message),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }

                // Weather section (if enabled and successful) - Auto-scales with remaining space
                if (settings.showWeather) {
                    when (weatherState) {
                        is WeatherUiState.Loading -> {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                // Takes proportional space
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        is WeatherUiState.Success -> {
                            WeatherCard(
                                weatherInfo = weatherState.weatherInfo,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                // Takes proportional space
                            )
                        }
                        is WeatherUiState.Error -> {
                            // Hide weather section completely on error
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Prayer times at bottom with animation
        when (uiState) {
            is MainUiState.Success -> {
                AnimatedPrayerTimesSection(
                    prayerTimes = uiState.prayerTimes,
                    nextPrayer = uiState.nextPrayer,
                    show24Hour = settings.show24HourFormat,
                    globalShowAzan = globalShowAzan,
                )
            }
            else -> {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun MosqueHeader(
    mosqueName: String,
    modifier: Modifier = Modifier,
) {
    if (mosqueName.isNotEmpty()) {
        val fontSize = 22.sp // Compact size for same-line layout

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
        ) {
            // Logo on the left
            Image(
                painter = painterResource(id = R.drawable.mosque_logo),
                contentDescription = "Mosque Logo",
                modifier = Modifier.size(48.dp), // Compact size
                contentScale = ContentScale.Fit,
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Mosque name on the right
            Text(
                text = mosqueName,
                style =
                    MaterialTheme.typography.headlineLarge.copy(
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Start,
                maxLines = 2,
            )
        }
    }
}

@Composable
fun ClockSection(
    settings: AppSettings,
    onClockClick: () -> Unit,
    hijriDateRepository: HijriDateRepository? = null,
    modifier: Modifier = Modifier,
    currentTime: Instant? = null,
) {
    val fontSize = 88.sp

    Card(
        modifier =
            modifier
                .padding(4.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable { onClockClick() },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(6.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            val boxPadding = 6.dp

            Box(
                modifier = Modifier.padding(boxPadding),
                contentAlignment = Alignment.Center,
            ) {
                // Handle cycling logic for BOTH option with smooth transitions
                val actualClockType =
                    when (settings.clockType) {
                        ClockType.BOTH -> {
                            // Cycle between Digital and Analog every 2 minutes
                            var currentType by remember { mutableStateOf(ClockType.DIGITAL) }

                            LaunchedEffect(Unit) {
                                while (true) {
                                    kotlinx.coroutines.delay(120000) // 2 minutes (120 seconds)
                                    currentType =
                                        if (currentType == ClockType.DIGITAL) {
                                            ClockType.ANALOG
                                        } else {
                                            ClockType.DIGITAL
                                        }
                                }
                            }

                            currentType
                        }
                        else -> settings.clockType
                    }

                // Smooth transition between clock types
                AnimatedContent(
                    targetState = actualClockType,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(1000)) togetherWith
                            fadeOut(animationSpec = tween(1000))
                    },
                    label = "clock_transition",
                ) { clockType ->
                    when (clockType) {
                        ClockType.DIGITAL -> {
                            DigitalClock(
                                show24Hour = settings.show24HourFormat,
                                showSeconds = settings.showSeconds,
                                fontSize = fontSize,
                                hijriDateRepository = hijriDateRepository,
                                currentTime = currentTime,
                            )
                        }
                        ClockType.ANALOG -> {
                            AnalogClock(
                                modifier = Modifier,
                                currentTime = currentTime,
                                hijriDateRepository = hijriDateRepository,
                            )
                        }
                        ClockType.BOTH -> {
                            // This case should not be reached due to the logic above
                            DigitalClock(
                                show24Hour = settings.show24HourFormat,
                                showSeconds = settings.showSeconds,
                                fontSize = fontSize,
                                hijriDateRepository = hijriDateRepository,
                                currentTime = currentTime,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getFriendlyErrorMessage(technicalMessage: String): String =
    when {
        // Network connectivity issues
        technicalMessage.contains("failed to connect", ignoreCase = true) ||
            technicalMessage.contains("connection refused", ignoreCase = true) ||
            technicalMessage.contains("timeout", ignoreCase = true) ||
            technicalMessage.contains("unable to resolve host", ignoreCase = true) -> {
            "Unable to connect to prayer time service.\nPlease check your internet connection."
        }

        // API or server issues
        technicalMessage.contains("500", ignoreCase = true) ||
            technicalMessage.contains("502", ignoreCase = true) ||
            technicalMessage.contains("503", ignoreCase = true) ||
            technicalMessage.contains("server error", ignoreCase = true) -> {
            "Prayer time service is temporarily unavailable.\nPlease try again later."
        }

        // Data not found
        technicalMessage.contains("404", ignoreCase = true) ||
            technicalMessage.contains("not found", ignoreCase = true) ||
            technicalMessage.contains("no data", ignoreCase = true) -> {
            "Prayer times not available for your location.\nPlease check your settings."
        }

        // General network errors
        technicalMessage.contains("network", ignoreCase = true) -> {
            "Network connection problem.\nPlease check your internet and try again."
        }

        // Generic fallback for any other technical errors
        else -> {
            "Unable to load prayer times.\nPlease check your internet connection and try again."
        }
    }

private fun formatTimeBasedOnPreference(
    time: String,
    show24Hour: Boolean,
): String {
    try {
        val parts = time.split(":")
        if (parts.size != 2) return time

        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        return if (show24Hour) {
            String.format("%02d:%02d", hour, minute)
        } else {
            val hour12 =
                if (hour == 0) {
                    12
                } else if (hour > 12) {
                    hour - 12
                } else {
                    hour
                }
            val ampm = if (hour < 12) "AM" else "PM"
            String.format("%d:%02d %s", hour12, minute, ampm)
        }
    } catch (e: Exception) {
        return time
    }
}

data class CountdownData(
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val totalMinutes: Long,
    val totalSeconds: Long,
)

private fun getCountdownData(
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
            return CountdownData(0, 0, 0, 0, 0)
        }

        val hours = duration.inWholeHours
        val minutes = (duration.inWholeMinutes % 60)
        val seconds = (duration.inWholeSeconds % 60) + 1 // Add 1 second to fix timing
        val totalMinutes = duration.inWholeMinutes
        val totalSeconds = duration.inWholeSeconds + 1 // Add 1 second to fix timing

        return CountdownData(hours, minutes, seconds, totalMinutes, totalSeconds)
    } catch (e: Exception) {
        return CountdownData(0, 0, 0, 0, 0)
    }
}

@Composable
fun FlipClockCountdown(
    hours: Long,
    minutes: Long,
    seconds: Long,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp), // Increased from 8.dp to 12.dp for larger boxes
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hours > 0) {
            FlipClockDigitPair(
                value = hours,
            )

            Text(
                text = ":",
                style =
                    MaterialTheme.typography.displayLarge.copy(
                        fontSize = 44.sp, // Increased from 36.sp to 44.sp to match larger digits
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        FlipClockDigitPair(
            value = minutes,
        )

        Text(
            text = ":",
            style =
                MaterialTheme.typography.displayLarge.copy(
                    fontSize = 44.sp, // Increased from 36.sp to 44.sp to match larger digits
                    fontWeight = FontWeight.Bold,
                ),
            color = MaterialTheme.colorScheme.onSurface,
        )

        FlipClockDigitPair(
            value = seconds,
        )
    }
}

@Composable
fun FlipClockDigitPair(value: Long) {
    val tens = (value / 10).toInt()
    val units = (value % 10).toInt()

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp), // Increased from 2.dp to 4.dp for larger boxes
    ) {
        AnimatedFlipDigit(digit = tens)
        AnimatedFlipDigit(digit = units)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedFlipDigit(digit: Int) {
    var previousDigit by remember { mutableStateOf(digit) }
    var currentDigit by remember { mutableStateOf(digit) }

    // Detect digit change for animation
    LaunchedEffect(digit) {
        if (digit != currentDigit) {
            previousDigit = currentDigit
            currentDigit = digit
        }
    }

    Box(
        modifier = Modifier.size(width = 64.dp, height = 88.dp), // Increased from 52x72 to 64x88
    ) {
        // Main flip card background - elegant natural color matching your mosque clock theme
        Card(
            modifier = Modifier.fillMaxSize(),
            colors =
                CardDefaults.cardColors(
                    containerColor = Color(0xFF2D4A22), // Deep forest green matching your analog clock
                ),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            // Inner card with elegant brass-tinted background
            Card(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(2.dp),
                // Thin border to show outer color
                colors =
                    CardDefaults.cardColors(
                        containerColor = Color(0xFF3A5F2A), // Slightly lighter forest green
                    ),
                shape = RoundedCornerShape(6.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    // Animated digit transition
                    AnimatedContent(
                        targetState = currentDigit,
                        transitionSpec = {
                            if (targetState > initialState) {
                                // Flip up animation
                                slideInVertically { height -> -height } + fadeIn() togetherWith
                                    slideOutVertically { height -> height } + fadeOut()
                            } else {
                                // Flip down animation
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                    slideOutVertically { height -> -height } + fadeOut()
                            }.using(
                                SizeTransform(clip = false),
                            )
                        },
                        label = "digit_flip",
                    ) { animatedDigit ->
                        Text(
                            text = animatedDigit.toString(),
                            style =
                                MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.Black, // Extra bold for visibility
                                ),
                            color = Color(0xFFB08D57), // Elegant brass/gold color matching your analog clock
                        )
                    }

                    // Horizontal line in the middle to simulate flip mechanism - subtle brass accent
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFB08D57).copy(alpha = 0.6f)) // Subtle brass line
                                .align(Alignment.Center),
                    )

                    // Add subtle gradient shadows for depth like your analog clock
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color.Black.copy(alpha = 0.2f),
                                                Color.Transparent,
                                            ),
                                    ),
                                ).align(Alignment.TopCenter),
                    )

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.2f),
                                            ),
                                    ),
                                ).align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

@Composable
fun CountdownDisplay(
    hours: Long,
    minutes: Long,
    seconds: Long,
) {
    FlipClockCountdown(
        hours = hours,
        minutes = minutes,
        seconds = seconds,
    )
}

@Composable
private fun CountdownUnit(
    value: Long,
    label: String,
    digitSize: androidx.compose.ui.unit.TextUnit,
    labelSize: androidx.compose.ui.unit.TextUnit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = String.format("%02d", value),
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        fontSize = digitSize,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = labelSize,
                        fontWeight = FontWeight.Medium,
                    ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NextPrayerCountdownSection(
    prayerTimes: PrayerTimesWithIqamah,
    nextPrayer: PrayerType?,
    nextDayFajr: PrayerTimesWithIqamah? = null,
    show24Hour: Boolean = false,
    modifier: Modifier = Modifier,
    onPrayerTransition: () -> Unit = {},
    showCountdown: Boolean = false,
    currentTime: Instant = Clock.System.now(),
    isCurrentPrayer: Boolean = false,
    isPrayerInProgress: Boolean = false,
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

    // No longer need alternating display - we show only the relevant time

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
            modifier = modifier.fillMaxWidth().padding(1.dp), // Further reduced padding
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxSize().padding(2.dp), // Fill max size instead of just width
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(2.dp), // Fill max size and minimal padding
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center, // Center content vertically
                ) {
                    // Show silent phone image during prayer-in-progress period
                    if (isPrayerInProgress) {
                        SilentPhoneSection()
                    } else {
                        // Dynamic font sizes that better utilize available space
                        val titleFontSize = if (showCountdown) 22.sp else 26.sp
                        val prayerNameFontSize = if (showCountdown) 36.sp else 42.sp
                        val timeFontSize = if (showCountdown) 48.sp else 64.sp
                        // Minimal padding to maximize space for content
                        val paddingSize =
                            if (showCountdown) {
                                6.dp
                            } else {
                                2.dp
                            }

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
                                            fontSize = titleFontSize,
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

                                Text(
                                    text = displayText,
                                    style =
                                        MaterialTheme.typography.headlineLarge.copy(
                                            fontSize = prayerNameFontSize,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    color = MaterialTheme.colorScheme.onSurface,
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
                                        Color(0xFFFF5722) // Red for Sunrise time
                                    } else if (shouldShowIqamah) {
                                        Color(0xFF2196F3) // Blue for Iqamah time
                                    } else {
                                        Color(0xFF4CAF50) // Green for Azan time
                                    }

                                Text(
                                    text = formatTimeBasedOnPreference(timeToShow, show24Hour),
                                    style =
                                        MaterialTheme.typography.displayLarge.copy(
                                            fontSize = timeFontSize,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    color = timeColor,
                                )
                            }

                            // Bottom section - Countdown (if visible) or spacer
                            if (showCountdown) {
                                CountdownDisplay(
                                    hours = countdownData.hours,
                                    minutes = countdownData.minutes,
                                    seconds = countdownData.seconds,
                                )
                            } else {
                                // Add flexible spacer when no countdown to push content up
                                Spacer(modifier = Modifier.height(1.dp))
                            }
                        } // End of else block
                    }
                }
            }
        }
    }
}

@Composable
private fun SilentPhoneSection() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        // Silent phone image - BACKGROUND - Expanded to use most of available space
        Image(
            painter = painterResource(id = R.drawable.silent_phone),
            contentDescription = "Silent Phone",
            modifier =
                Modifier
                    .fillMaxSize(0.9f),
            contentScale = ContentScale.Fit,
        )

        // Localized "Silent Your Phone" text - OVERLAID ON TOP OF IMAGE
        Text(
            text = localizedStringResource(R.string.silent_your_phone),
            style =
                MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp, // Significantly increased from 20.sp
                ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AnimatedPrayerTimesSection(
    prayerTimes: PrayerTimesWithIqamah,
    nextPrayer: PrayerType?,
    show24Hour: Boolean = false,
    currentTime: Instant = Clock.System.now(),
    globalShowAzan: Boolean = true,
) {
    val prayerInfoList = createPrayerInfoList(prayerTimes, currentTime)

    val horizontalPadding = 0.5.dp
    val itemSpacing = 0.5.dp

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        prayerInfoList.forEach { prayerInfo ->
            CompactPrayerTimeCard(
                prayerInfo = prayerInfo,
                isNext = prayerInfo.type == nextPrayer,
                show24Hour = show24Hour,
                globalShowAzan = globalShowAzan,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun CompactPrayerTimeCard(
    prayerInfo: PrayerInfo,
    isNext: Boolean = false,
    show24Hour: Boolean = false,
    globalShowAzan: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val cardPadding = 1.dp
    // Use consistent content padding for all cards to prevent size variations
    val contentPadding = 2.dp
    val cornerRadius = 8.dp

    // Use global synchronized animation state
    // All cards will show Azan/Iqamah at the same time
    val isShowingAzan = globalShowAzan || prayerInfo.iqamahTime == null

    // Get the prayer color based on current state
    val prayerColor =
        if (isNext) {
            // Special handling for next prayer colors
            if (prayerInfo.type == PrayerType.SUNRISE) {
                Color(0xFFFF5722) // Red for Sunrise (next)
            } else if (!isShowingAzan && prayerInfo.iqamahTime != null) {
                Color(0xFF2196F3) // Blue for Iqamah (next)
            } else {
                MaterialTheme.colorScheme.primary // Default primary for Azan (next)
            }
        } else {
            // Non-next prayer colors
            if (prayerInfo.type == PrayerType.SUNRISE) {
                Color(0xFFFF5722) // Red for Sunrise (always)
            } else if (isShowingAzan) {
                Color(0xFF4CAF50) // Bright Green for Azan
            } else {
                Color(0xFF2196F3) // Bright Blue for Iqamah
            }
        }

    Card(
        modifier =
            modifier
                .padding(cardPadding)
                .let { cardModifier ->
                    if (isNext) {
                        cardModifier.border(
                            width = 2.dp,
                            color = prayerColor,
                            shape = RoundedCornerShape(cornerRadius),
                        )
                    } else {
                        cardModifier
                    }
                },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 6.dp,
            ),
        shape = RoundedCornerShape(cornerRadius),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(2.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isNext) {
                            prayerColor.copy(alpha = 0.05f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        },
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(cornerRadius - 2.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Use consistent font sizes to prevent text cutoff while maintaining readability
                val prayerNameFontSize = 17.sp
                val prayerTimeFontSize = 32.sp
                val spacingSize = 1.dp

                Text(
                    text = prayerInfo.name,
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            fontSize = prayerNameFontSize,
                            fontWeight = FontWeight.Bold,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )

                Spacer(modifier = Modifier.height(spacingSize))

                // Simplified time display without abbreviation text for better readability
                AnimatedContent(
                    targetState = globalShowAzan,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(800)) togetherWith
                            fadeOut(animationSpec = tween(800))
                    },
                ) { isShowingAzan ->
                    Text(
                        text =
                            if (isShowingAzan || prayerInfo.iqamahTime == null) {
                                formatTimeBasedOnPreference(prayerInfo.azanTime, show24Hour)
                            } else {
                                formatTimeBasedOnPreference(
                                    prayerInfo.iqamahTime,
                                    show24Hour,
                                )
                            },
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontSize = prayerTimeFontSize,
                                fontWeight = FontWeight.Bold,
                            ),
                        maxLines = 1,
                        softWrap = false,
                        color = prayerColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

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
