package com.mosque.prayerclock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.app.UiModeManager
import android.content.res.Configuration
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.localizedStringArrayResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.*
import com.mosque.prayerclock.ui.components.DigitalClock
import com.mosque.prayerclock.ui.components.AnalogClock
import com.mosque.prayerclock.ui.components.PrayerTimeCard
import com.mosque.prayerclock.ui.components.WeatherCard
import com.mosque.prayerclock.viewmodel.MainViewModel
import com.mosque.prayerclock.viewmodel.MainUiState
import com.mosque.prayerclock.viewmodel.WeatherUiState
import com.mosque.prayerclock.data.repository.HijriDateRepository
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.seconds



@Composable
fun MainScreen(
    onOpenSettings: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) { viewModel.loadPrayerTimes() }
    
    // Reload prayer times when prayer service settings change
    LaunchedEffect(settings.prayerServiceType, settings.selectedZone, settings.selectedRegion) { viewModel.loadPrayerTimes() }
    
    // Reload weather when weather settings change
    // Only reload weather when weather settings change, not full prayer reload
    LaunchedEffect(settings.weatherCity, settings.weatherCountry, settings.showWeather) {
        if (settings.showWeather) {
            viewModel.loadPrayerTimes()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        NewMainLayout(
            uiState = uiState,
            weatherState = weatherState,
            settings = settings,
            viewModel = viewModel,
            onOpenSettings = onOpenSettings,
            onRetry = { viewModel.loadPrayerTimes() }
        )
    }
}

@Composable
fun NewMainLayout(
    uiState: MainUiState,
    weatherState: WeatherUiState,
    settings: AppSettings,
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit
) {
    val paddingSize = 16.dp
    val spacingSize = 12.dp
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingSize)
    ) {
        // Mosque title at top center
        if (settings.mosqueName.isNotEmpty()) {
            MosqueHeader(
                mosqueName = settings.mosqueName,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(spacingSize))
        }
        
        // Calculate countdown visibility and prayer state once for the entire layout
        val (isCountdownVisibleForWeight, currentTimeGlobal, isCurrentPrayerGlobal) = when (uiState) {
            is MainUiState.Success -> {
                val prayerInfoList = createPrayerInfoList(uiState.prayerTimes)
                val nextPrayerInfo = prayerInfoList.find { it.type == uiState.nextPrayer }
                var currentTime by remember { mutableStateOf(Clock.System.now()) }
                
                LaunchedEffect(Unit) {
                    while (true) {
                        currentTime = Clock.System.now()
                        kotlinx.coroutines.delay(1000)
                    }
                }
                
                // Memoize calculations that don't depend on current time
                val calculatedValues = remember(nextPrayerInfo, currentTime) {
                    nextPrayerInfo?.let { prayer ->
                        val now = currentTime.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                        val currentTimeString = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
                        
                        val isAfterAzan = compareTimeStrings(prayer.azanTime, currentTimeString) <= 0
                        val isBeforeIqamah = prayer.iqamahTime?.let { iqamahTime ->
                            compareTimeStrings(currentTimeString, iqamahTime) < 0
                        } ?: false
                        val shouldShowIqamah = isAfterAzan && isBeforeIqamah && prayer.iqamahTime != null
                        
                        // Determine target time first
                        val targetTime = if (shouldShowIqamah) {
                            prayer.iqamahTime ?: prayer.azanTime
                        } else {
                            prayer.azanTime
                        }
                        
                        val countdownData = getCountdownData(targetTime, currentTime)
                        
                        // Determine when prayer is considered "current" vs "next"
                        val isCurrentPrayerLocal = if (prayer.iqamahTime != null) {
                            // For prayers with Iqamah: current from Azan time until Iqamah + 5min buffer
                            val bufferTime = addMinutesToTime(prayer.iqamahTime, 5)
                            isAfterAzan && compareTimeStrings(currentTimeString, bufferTime) <= 0
                        } else {
                            // For prayers without Iqamah: current from Azan time until Azan + 60min buffer
                            val extendedTime = addMinutesToTime(prayer.azanTime, 60)
                            isAfterAzan && compareTimeStrings(currentTimeString, extendedTime) <= 0
                        }
                        
                        // Determine when to hide countdown (only during buffer periods)
                        val isPastFinalTime = if (prayer.iqamahTime != null) {
                            // Hide countdown after Iqamah time (during buffer period)
                            compareTimeStrings(currentTimeString, prayer.iqamahTime) > 0
                        } else {
                            // Hide countdown after Azan + 60min for prayers without Iqamah
                            val extendedTime = addMinutesToTime(prayer.azanTime, 60)
                            compareTimeStrings(currentTimeString, extendedTime) > 0
                        }
                        
                        // Show countdown if time is positive, within 60 minutes, and not in buffer period
                        val isCountdownVisible = countdownData.totalSeconds > 0 && countdownData.totalMinutes <= 60 && !isPastFinalTime
                        
                        Triple(isCountdownVisible, currentTime, isCurrentPrayerLocal)
                    } ?: Triple(false, currentTime, false)
                }
                
                calculatedValues
            }
            else -> Triple(false, Clock.System.now(), false)
        }
        
        // Main content row with Clock and combined Next Prayer + Weather
        Row(
            modifier = Modifier.weight(0.75f),
            horizontalArrangement = Arrangement.spacedBy(spacingSize),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clock section - Takes available space
            ClockSection(
                settings = settings,
                onClockClick = onOpenSettings,
                isCompact = false, // Make clock larger
                hijriDateRepository = viewModel.hijriDateRepository,
                modifier = Modifier.weight(1.5f) // Increased weight for bigger clock
            )
            
            // Combined Next Prayer and Weather section
            Column(
                modifier = Modifier.weight(0.9f), // Reduced weight to give more space to clock
                verticalArrangement = Arrangement.spacedBy(if (isCountdownVisibleForWeight) 8.dp else 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Next prayer timer - Takes most of the space
                when (uiState) {
                    is MainUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(3f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    is MainUiState.Success -> {
                        NextPrayerCountdownSection(
                            prayerTimes = uiState.prayerTimes,
                            nextPrayer = uiState.nextPrayer,
                            language = settings.language,
                            show24Hour = settings.show24HourFormat,
                            modifier = Modifier.fillMaxWidth(),
                            onPrayerTransition = { viewModel.updateNextPrayer() },
                            showCountdown = isCountdownVisibleForWeight,
                            currentTime = currentTimeGlobal,
                            isCurrentPrayer = isCurrentPrayerGlobal
                        )
                    }
                    
                    is MainUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(3f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Weather section (if enabled) - Takes minimal space
                if (settings.showWeather) {
                    when (weatherState) {
                        is WeatherUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        is WeatherUiState.Success -> {
                            WeatherCard(
                                weatherInfo = weatherState.weatherInfo,
                                language = settings.language,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        is WeatherUiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Weather unavailable",
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
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
                    language = settings.language,
                    show24Hour = settings.show24HourFormat,
                    isCountdownVisible = isCountdownVisibleForWeight
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
    modifier: Modifier = Modifier
) {
    if (mosqueName.isNotEmpty()) {
        val fontSize = 28.sp
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
        ) {
            Text(
                text = mosqueName,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
fun ClockSection(
    settings: AppSettings,
    onClockClick: () -> Unit,
    isLandscape: Boolean = false,
    isCompact: Boolean = false,
    hijriDateRepository: HijriDateRepository? = null,
    modifier: Modifier = Modifier
) {
    val clockSize = when {
        isCompact -> 280.dp
        isLandscape -> 320.dp
        else -> 360.dp
    }
    
    val fontSize = 88.sp
    
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClockClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        val boxPadding = 6.dp
        
        Box(
            modifier = Modifier
                .padding(boxPadding),
            contentAlignment = Alignment.Center
        ) {
            when (settings.clockType) {
                ClockType.DIGITAL -> {
                    DigitalClock(
                        show24Hour = settings.show24HourFormat,
                        showSeconds = settings.showSeconds,
                        fontSize = fontSize,
                        hijriDateRepository = hijriDateRepository
                    )
                }
                ClockType.ANALOG -> {
                    AnalogClock(
                        size = clockSize,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

@Composable
fun NextPrayerSection(
    prayerTimes: PrayerTimes,
    nextPrayer: PrayerType?,
    language: Language,
    modifier: Modifier = Modifier
) {
    val prayerInfoList = createPrayerInfoList(prayerTimes)
    
    val nextPrayerInfo = prayerInfoList.find { it.type == nextPrayer }
    var currentTime by remember { mutableStateOf(Clock.System.now()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Clock.System.now()
            kotlinx.coroutines.delay(1000)
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Next Prayer",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (nextPrayerInfo != null) {
                Text(
                    text = nextPrayerInfo.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = nextPrayerInfo.azanTime,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                nextPrayerInfo.iqamahTime?.let { iqamahTime ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Iqamah: $iqamahTime",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val countdownData = calculateTimeUntilPrayerData(nextPrayerInfo.azanTime, currentTime)
                if (countdownData != null) {
                    val timeUntilPrayer = formatTimeRemaining(countdownData)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = timeUntilPrayer,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "All prayers completed for today",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PrayerTimesSection(
    prayerTimes: PrayerTimes,
    nextPrayer: PrayerType?,
    language: Language,
    isLandscape: Boolean = false
) {
    val prayerInfoList = createPrayerInfoList(prayerTimes)
    
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(prayerInfoList) { prayerInfo ->
            PrayerTimeCard(
                prayerInfo = prayerInfo,
                isNext = prayerInfo.type == nextPrayer && prayerInfo.type != PrayerType.SUNRISE,
                modifier = Modifier.width(if (isLandscape) 240.dp else 280.dp),
                isCompact = true
            )
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(localizedStringResource(R.string.refresh))
        }
    }
}

private fun formatTimeBasedOnPreference(time: String, show24Hour: Boolean): String {
    try {
        val parts = time.split(":")
        if (parts.size != 2) return time
        
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        
        return if (show24Hour) {
            String.format("%02d:%02d", hour, minute)
        } else {
            val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val ampm = if (hour < 12) "AM" else "PM"
            String.format("%d:%02d %s", hour12, minute, ampm)
        }
    } catch (e: Exception) {
        return time
    }
}

private fun calculateTimeUntilPrayerData(prayerTime: String, currentTime: Instant): CountdownData? {
    try {
        val now = currentTime.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val parts = prayerTime.split(":")
        val prayerHour = parts[0].toInt()
        val prayerMinute = parts[1].toInt()
        
        val prayerDateTime = LocalDateTime(
            now.date,
            LocalTime(prayerHour, prayerMinute)
        )
        
        val prayerInstant = prayerDateTime.toInstant(kotlinx.datetime.TimeZone.currentSystemDefault())
        val duration = prayerInstant - currentTime
        
        if (duration.isNegative()) {
            return null
        }
        
        val hours = duration.inWholeHours
        val minutes = (duration.inWholeMinutes % 60)
        val seconds = (duration.inWholeSeconds % 60)
        val totalMinutes = duration.inWholeMinutes
        val totalSeconds = duration.inWholeSeconds
        
        return CountdownData(hours, minutes, seconds, totalMinutes, totalSeconds)
    } catch (e: Exception) {
        return null
    }
}

@Composable
private fun formatTimeRemaining(countdownData: CountdownData): String {
    return when {
        countdownData.hours > 0 -> localizedStringResource(R.string.time_format_hours_minutes, countdownData.hours, countdownData.minutes)
        countdownData.minutes > 0 -> localizedStringResource(R.string.time_format_minutes_seconds, countdownData.minutes, countdownData.seconds)
        else -> localizedStringResource(R.string.time_format_seconds, countdownData.seconds)
    }
}

data class CountdownData(
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val totalMinutes: Long,
    val totalSeconds: Long
)

private fun getCountdownData(prayerTime: String, currentTime: Instant): CountdownData {
    try {
        val now = currentTime.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val parts = prayerTime.split(":")
        val prayerHour = parts[0].toInt()
        val prayerMinute = parts[1].toInt()
        
        val prayerDateTime = LocalDateTime(
            now.date,
            LocalTime(prayerHour, prayerMinute)
        )
        
        val prayerInstant = prayerDateTime.toInstant(kotlinx.datetime.TimeZone.currentSystemDefault())
        val duration = prayerInstant - currentTime
        
        if (duration.isNegative()) {
            return CountdownData(0, 0, 0, 0, 0)
        }
        
        val hours = duration.inWholeHours
        val minutes = (duration.inWholeMinutes % 60)
        val seconds = (duration.inWholeSeconds % 60)
        val totalMinutes = duration.inWholeMinutes
        val totalSeconds = duration.inWholeSeconds
        
        return CountdownData(hours, minutes, seconds, totalMinutes, totalSeconds)
    } catch (e: Exception) {
        return CountdownData(0, 0, 0, 0, 0)
    }
}

@Composable
fun CountdownDisplay(
    hours: Long,
    minutes: Long,
    seconds: Long
) {
    val digitSize = 46.sp
    val labelSize = 14.sp
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hours > 0) {
            CountdownUnit(
                value = hours,
                label = "H",
                digitSize = digitSize,
                labelSize = labelSize
            )
        }
        
        CountdownUnit(
            value = minutes,
            label = "M",
            digitSize = digitSize,
            labelSize = labelSize
        )
        
        CountdownUnit(
            value = seconds,
            label = "S",
            digitSize = digitSize,
            labelSize = labelSize
        )
    }
}

@Composable
private fun CountdownUnit(
    value: Long,
    label: String,
    digitSize: androidx.compose.ui.unit.TextUnit,
    labelSize: androidx.compose.ui.unit.TextUnit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format("%02d", value),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = digitSize,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = labelSize,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NextPrayerCountdownSection(
    prayerTimes: PrayerTimes,
    nextPrayer: PrayerType?,
    language: Language,
    show24Hour: Boolean = false,
    modifier: Modifier = Modifier,
    onPrayerTransition: () -> Unit = {},
    showCountdown: Boolean = false,
    currentTime: Instant = Clock.System.now(),
    isCurrentPrayer: Boolean = false
) {
    val prayerInfoList = createPrayerInfoList(prayerTimes)
    val nextPrayerInfo = prayerInfoList.find { it.type == nextPrayer }
    
    // Use passed current time instead of recalculating
    val now = currentTime.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    val currentTimeString = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
    
    // Calculate if we should show Iqamah time (for display purposes)
    val isAfterAzan = nextPrayerInfo?.let { prayer ->
        compareTimeStrings(prayer.azanTime, currentTimeString) <= 0
    } ?: false
    
    val isBeforeIqamah = nextPrayerInfo?.iqamahTime?.let { iqamahTime ->
        compareTimeStrings(currentTimeString, iqamahTime) < 0
    } ?: false
    
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
        val targetTime = if (shouldShowIqamah) {
            nextPrayerInfo.iqamahTime ?: nextPrayerInfo.azanTime
        } else {
            nextPrayerInfo.azanTime
        }
        val countdownData = getCountdownData(targetTime, currentTime)
        
        Card(
            modifier = modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            // Use TV sizes for all devices
            val titleFontSize = 24.sp
            val prayerNameFontSize = 44.sp
            val timeFontSize = 56.sp
            // Further reduce padding when countdown is not visible to make component more compact
            val paddingSize = if (showCountdown) {
                12.dp
            } else {
                4.dp
            }
            val spacingSize = if (showCountdown) {
                8.dp
            } else {
                2.dp
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingSize),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (showCountdown) Arrangement.Center else Arrangement.Top
            ) {
                Text(
                    text = if (isCurrentPrayer) {
                        localizedStringResource(R.string.current_prayer)
                    } else {
                        localizedStringResource(R.string.next_prayer)
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = titleFontSize
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(spacingSize))
                
                Text(
                    text = nextPrayerInfo.name,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = prayerNameFontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(spacingSize))
                
                // Single time display - show only relevant time
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Determine which time to show based on current timing
                    val timeToShow = if (shouldShowIqamah) {
                        // Show Iqamah time if we're past Azan time
                        nextPrayerInfo.iqamahTime ?: nextPrayerInfo.azanTime
                    } else {
                        // Show Azan time by default
                        nextPrayerInfo.azanTime
                    }
                    
                    Text(
                        text = formatTimeBasedOnPreference(timeToShow, show24Hour),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = timeFontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(spacingSize))
                
                // Show countdown only if it was determined to be visible
                if (showCountdown) {
                    CountdownDisplay(
                        hours = countdownData.hours,
                        minutes = countdownData.minutes,
                        seconds = countdownData.seconds
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedPrayerTimesSection(
    prayerTimes: PrayerTimes,
    nextPrayer: PrayerType?,
    language: Language,
    show24Hour: Boolean = false,
    isCountdownVisible: Boolean = true
) {
    val prayerInfoList = createPrayerInfoList(prayerTimes)
    
    val horizontalPadding = 0.5.dp
    val itemSpacing = 0.5.dp
    
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        contentPadding = PaddingValues(horizontal = 0.5.dp)
    ) {
        items(prayerInfoList) { prayerInfo ->
            AnimatedVisibility(
                visible = true,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = prayerInfoList.indexOf(prayerInfo) * 100
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = prayerInfoList.indexOf(prayerInfo) * 100
                    )
                )
            ) {
                CompactPrayerTimeCard(
                    prayerInfo = prayerInfo,
                    isNext = prayerInfo.type == nextPrayer && prayerInfo.type != PrayerType.SUNRISE,
                    language = language,
                    show24Hour = show24Hour,
                    isCountdownVisible = isCountdownVisible,
                    modifier = Modifier.width(if (prayerInfo.type == PrayerType.SUNRISE) 180.dp else 150.dp)
                )
            }
        }
    }
}

@Composable
fun CompactPrayerTimeCard(
    prayerInfo: PrayerInfo,
    isNext: Boolean = false,
    language: Language,
    show24Hour: Boolean = false,
    isCountdownVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val cardPadding = 2.dp
    // Increase content padding when countdown is not visible to make cards taller
    val contentPadding = 5.dp
    val cornerRadius = 8.dp
    val cardElevation = if (isNext) 6.dp else 3.dp
    
    // Animation state for alternating between Azan and Iqamah
    var showAzan by remember { mutableStateOf(true) }
    
    // Only animate if there's an Iqamah time
    LaunchedEffect(prayerInfo.iqamahTime) {
        if (prayerInfo.iqamahTime != null) {
            while (true) {
                kotlinx.coroutines.delay(4000) // 4 seconds delay
                showAzan = !showAzan
            }
        }
    }
    
    Card(
        modifier = modifier
            .padding(cardPadding),
        colors = CardDefaults.cardColors(
            containerColor = if (isNext) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = cardElevation
        ),
        shape = RoundedCornerShape(cornerRadius)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Increase font sizes significantly when countdown is not visible to better utilize space
            val prayerNameFontSize = if (isCountdownVisible) {
                16.sp
            } else {
                20.sp  // Larger increase
            }
            val prayerTimeFontSize = if (isCountdownVisible) {
                20.sp
            } else {
                26.sp  // Larger increase
            }
            val labelFontSize = if (isCountdownVisible) {
                12.sp
            } else {
                15.sp  // Larger increase
            }
            val spacingSize = 3.dp
            
            Text(
                text = prayerInfo.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = prayerNameFontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = if (isNext) 
                    MaterialTheme.colorScheme.primary
                else 
                    MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(spacingSize))
            
            // Animated content for time display
            AnimatedContent(
                targetState = showAzan,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                }
            ) { isShowingAzan ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (prayerInfo.iqamahTime != null) {
                        // Show label only if there's Iqamah time
                        Text(
                            text = if (isShowingAzan) {
                                localizedStringResource(R.string.azan_letter)
                            } else {
                                localizedStringResource(R.string.iqamah_letter)
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = labelFontSize
                            ),
                            color = if (isNext) 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    
                    Text(
                        text = if (isShowingAzan || prayerInfo.iqamahTime == null) {
                            formatTimeBasedOnPreference(prayerInfo.azanTime, show24Hour)
                        } else {
                            formatTimeBasedOnPreference(prayerInfo.iqamahTime, show24Hour)
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = prayerTimeFontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        softWrap = false,
                        color = if (isNext) 
                            MaterialTheme.colorScheme.primary
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun compareTimeStrings(time1: String, time2: String): Int {
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    return try {
        val date1 = format.parse(time1)
        val date2 = format.parse(time2)
        date1?.compareTo(date2) ?: 0
    } catch (e: Exception) {
        0
    }
}

private fun addMinutesToTime(timeString: String, minutesToAdd: Int): String {
    try {
        val parts = timeString.split(":")
        val hours = parts[0].toInt()
        val minutes = parts[1].toInt()
        
        val totalMinutes = (hours * 60) + minutes + minutesToAdd
        val newHours = (totalMinutes / 60) % 24
        val newMinutes = totalMinutes % 60
        
        return String.format("%02d:%02d", newHours, newMinutes)
    } catch (e: Exception) {
        return timeString
    }
}

@Composable
private fun createPrayerInfoList(prayerTimes: PrayerTimes): List<PrayerInfo> {
    val currentDate = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    val isFriday = currentDate.dayOfWeek == DayOfWeek.FRIDAY
    
    return listOf(
        PrayerInfo(
            type = PrayerType.FAJR,
            azanTime = prayerTimes.fajrAzan,
            iqamahTime = prayerTimes.fajrIqamah,
            name = localizedStringResource(R.string.fajr)
        ),
        PrayerInfo(
            type = PrayerType.SUNRISE,
            azanTime = prayerTimes.sunrise,
            iqamahTime = null,
            name = localizedStringResource(R.string.sunrise)
        ),
        PrayerInfo(
            type = PrayerType.DHUHR,
            azanTime = prayerTimes.dhuhrAzan,
            iqamahTime = if (isFriday) null else prayerTimes.dhuhrIqamah, // No Iqamah on Friday (Bayan instead)
            name = if (isFriday) localizedStringResource(R.string.jummah) else localizedStringResource(R.string.dhuhr)
        ),
        PrayerInfo(
            type = PrayerType.ASR,
            azanTime = prayerTimes.asrAzan,
            iqamahTime = prayerTimes.asrIqamah,
            name = localizedStringResource(R.string.asr)
        ),
        PrayerInfo(
            type = PrayerType.MAGHRIB,
            azanTime = prayerTimes.maghribAzan,
            iqamahTime = prayerTimes.maghribIqamah,
            name = localizedStringResource(R.string.maghrib)
        ),
        PrayerInfo(
            type = PrayerType.ISHA,
            azanTime = prayerTimes.ishaAzan,
            iqamahTime = prayerTimes.ishaIqamah,
            name = localizedStringResource(R.string.isha)
        )
    )
}