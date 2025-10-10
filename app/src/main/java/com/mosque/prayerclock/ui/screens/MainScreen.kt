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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.platform.LocalDensity
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
import com.mosque.prayerclock.ui.components.DuaForJoiningSaff
import com.mosque.prayerclock.ui.components.FlipClockDigitPair
import com.mosque.prayerclock.ui.components.FullScreenCountdown
import com.mosque.prayerclock.ui.components.JummahInProgress
import com.mosque.prayerclock.ui.components.NextPrayerSection
import com.mosque.prayerclock.ui.components.PrayerTimeCard
import com.mosque.prayerclock.ui.components.WeatherCard
import com.mosque.prayerclock.ui.components.getCountdownData
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.theme.ColorAzanTime
import com.mosque.prayerclock.ui.theme.ColorIqamahTime
import com.mosque.prayerclock.ui.theme.ColorSunriseTime
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
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

// Helper data class for returning four values
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

// Helper data class for returning six values
data class Sextuple<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
)

// Helper data class for returning seven values
data class Septuple<A, B, C, D, E, F, G>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
    val seventh: G,
)

// Data class for full-screen countdown information
data class FullScreenCountdownData(
    val prayerName: String,
    val prayerType: PrayerType,
    val isIqamah: Boolean,
    val minutes: Long,
    val seconds: Long,
    val azanTime: String, // The actual Azan time to display
)

// Data class for Jummah in progress information
data class JummahData(
    val hoursRemaining: Long,
    val minutesRemaining: Long,
    val secondsRemaining: Long,
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

    // Calculate countdown visibility, prayer state, full-screen countdown state, dua display, and Jummah display
    val (
        isCountdownVisibleForWeight,
        currentTimeGlobal,
        isCurrentPrayerGlobal,
        shouldShowFullScreenCountdown,
        fullScreenCountdownData,
        shouldShowDua,
        jummahData,
    ) =
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
                                    // time until Iqamah + Dua duration buffer
                                    val bufferTime =
                                        TimeUtils.addMinutesToTime(
                                            prayer.iqamahTime,
                                            settings.duaDisplayDurationMinutes,
                                        )
                                    isAfterAzan &&
                                        TimeUtils.compareTimeStrings(
                                            currentTimeString,
                                            bufferTime,
                                        ) <= 0
                                } else {
                                    // For prayers without Iqamah: current from Azan time until Azan + buffer
                                    // Use Jummah duration for Dhuhr on Friday, otherwise 60min for Sunrise
                                    val isFriday = now.dayOfWeek == DayOfWeek.FRIDAY
                                    val bufferMinutes =
                                        if (prayer.type == PrayerType.DHUHR && isFriday) {
                                            settings.jummahDurationMinutes
                                        } else {
                                            60 // Default 60 minutes for Sunrise
                                        }
                                    val extendedTime =
                                        TimeUtils.addMinutesToTime(prayer.azanTime, bufferMinutes)
                                    isAfterAzan &&
                                        TimeUtils.compareTimeStrings(
                                            currentTimeString,
                                            extendedTime,
                                        ) <= 0
                                }

                            // Determine when to hide countdown (only during buffer periods)
                            val isPastFinalTime =
                                if (prayer.iqamahTime != null) {
                                    // Hide countdown after Iqamah time (during buffer period)
                                    TimeUtils.compareTimeStrings(
                                        currentTimeString,
                                        prayer.iqamahTime,
                                    ) > 0
                                } else {
                                    // Hide countdown after Azan + buffer for prayers without Iqamah
                                    // Use Jummah duration for Dhuhr on Friday, otherwise 60min for Sunrise
                                    val isFriday = now.dayOfWeek == DayOfWeek.FRIDAY
                                    val bufferMinutes =
                                        if (prayer.type == PrayerType.DHUHR && isFriday) {
                                            settings.jummahDurationMinutes
                                        } else {
                                            60 // Default 60 minutes for Sunrise
                                        }
                                    val extendedTime =
                                        TimeUtils.addMinutesToTime(prayer.azanTime, bufferMinutes)
                                    TimeUtils.compareTimeStrings(
                                        currentTimeString,
                                        extendedTime,
                                    ) > 0
                                }

                            // Show countdown if time is zero or positive, within 59 minutes,
                            // and not in buffer period
                            val isCountdownVisible =
                                countdownData.totalSeconds >= 0 &&
                                    countdownData.totalMinutes < 60 &&
                                    !isPastFinalTime

                            // Check if we should show Dua for joining Saff (configurable duration after Iqamah ends)
                            val shouldShowDua =
                                if (prayer.iqamahTime != null && prayer.type != PrayerType.SUNRISE) {
                                    val iqamahEndTime = TimeUtils.addMinutesToTime(prayer.iqamahTime, 0)
                                    val duaEndTime =
                                        TimeUtils.addMinutesToTime(
                                            iqamahEndTime,
                                            settings.duaDisplayDurationMinutes,
                                        )
                                    val isAfterIqamah =
                                        TimeUtils.compareTimeStrings(
                                            currentTimeString,
                                            iqamahEndTime,
                                        ) >= 0
                                    val isBeforeDuaEnd =
                                        TimeUtils.compareTimeStrings(
                                            currentTimeString,
                                            duaEndTime,
                                        ) < 0
                                    isAfterIqamah && isBeforeDuaEnd
                                } else {
                                    false
                                }

                            // Check if full-screen countdown should be shown
                            // Show for 10 minutes before Azan, OR full Iqamah gap when counting to Iqamah
                            val maxCountdownMinutes =
                                if (shouldShowIqamah && prayer.iqamahTime != null) {
                                    // When showing Iqamah countdown, show for the full Iqamah gap
                                    // Calculate the gap by getting minutes between Azan and Iqamah
                                    val azanParts = prayer.azanTime.split(":")
                                    val iqamahParts = prayer.iqamahTime.split(":")
                                    val azanMinutes = azanParts[0].toInt() * 60 + azanParts[1].toInt()
                                    val iqamahMinutes = iqamahParts[0].toInt() * 60 + iqamahParts[1].toInt()
                                    val gapMinutes = iqamahMinutes - azanMinutes
                                    gapMinutes.toLong()
                                } else {
                                    // Before Azan: show for 10 minutes
                                    10L
                                }

                            val shouldShowFullScreenCountdown =
                                settings.fullScreenCountdownEnabled &&
                                    countdownData.totalMinutes in 0..maxCountdownMinutes &&
                                    countdownData.totalSeconds >= 0 &&
                                    !isPastFinalTime

                            val fullScreenCountdownData =
                                if (shouldShowFullScreenCountdown) {
                                    FullScreenCountdownData(
                                        prayerName = prayer.name,
                                        prayerType = prayer.type,
                                        isIqamah = shouldShowIqamah,
                                        minutes = countdownData.minutes,
                                        seconds = countdownData.seconds,
                                        azanTime = prayer.azanTime,
                                    )
                                } else {
                                    null
                                }

                            // Check if we should show Jummah in progress screen
                            val jummahData =
                                if (settings.showJummahScreen &&
                                    prayer.type == PrayerType.DHUHR &&
                                    now.dayOfWeek == DayOfWeek.FRIDAY &&
                                    prayer.iqamahTime == null
                                ) {
                                    // It's Jummah (Friday Dhuhr with no Iqamah)
                                    val jummahEndTime =
                                        TimeUtils.addMinutesToTime(prayer.azanTime, settings.jummahDurationMinutes)
                                    val isAfterJummahAzan =
                                        TimeUtils.compareTimeStrings(
                                            prayer.azanTime,
                                            currentTimeString,
                                        ) <= 0
                                    val isBeforeJummahEnd =
                                        TimeUtils.compareTimeStrings(
                                            currentTimeString,
                                            jummahEndTime,
                                        ) < 0

                                    if (isAfterJummahAzan && isBeforeJummahEnd) {
                                        // Calculate countdown to Jummah end
                                        val jummahCountdown = getCountdownData(jummahEndTime, currentTime)
                                        JummahData(
                                            hoursRemaining = jummahCountdown.hours,
                                            minutesRemaining = jummahCountdown.minutes,
                                            secondsRemaining = jummahCountdown.seconds,
                                        )
                                    } else {
                                        null
                                    }
                                } else {
                                    null
                                }

                            Septuple(
                                isCountdownVisible,
                                currentTime,
                                isCurrentPrayerLocal,
                                shouldShowFullScreenCountdown,
                                fullScreenCountdownData,
                                shouldShowDua,
                                jummahData,
                            )
                        }
                            ?: Septuple(false, currentTime, false, false, null, false, null)
                    }

                calculatedValues
            }
            else -> Septuple(false, Clock.System.now(), false, false, null, false, null)
        }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main layout content
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
                            NextPrayerSection(
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
                                    textAlign = TextAlign.Center,
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

        // Full-screen countdown overlay (shown when enabled and 10 minutes or less remain)
        if (shouldShowFullScreenCountdown && fullScreenCountdownData != null) {
            FullScreenCountdown(
                prayerName = fullScreenCountdownData.prayerName,
                prayerType = fullScreenCountdownData.prayerType,
                isIqamah = fullScreenCountdownData.isIqamah,
                minutes = fullScreenCountdownData.minutes,
                seconds = fullScreenCountdownData.seconds,
                azanTime = fullScreenCountdownData.azanTime,
                show24Hour = settings.show24HourFormat,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Dua for joining Saff (shown for configurable duration after Iqamah ends)
        if (shouldShowDua) {
            DuaForJoiningSaff(
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Jummah in progress screen (shown during Jummah prayer time if enabled)
        if (jummahData != null) {
            JummahInProgress(
                hoursRemaining = jummahData.hoursRemaining,
                minutesRemaining = jummahData.minutesRemaining,
                secondsRemaining = jummahData.secondsRemaining,
                modifier = Modifier.fillMaxSize(),
            )
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
            PrayerTimeCard(
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
