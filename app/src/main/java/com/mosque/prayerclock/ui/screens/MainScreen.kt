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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import java.text.SimpleDateFormat
import java.util.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.*
import com.mosque.prayerclock.ui.components.DigitalClock
import com.mosque.prayerclock.ui.components.AnalogClock
import com.mosque.prayerclock.ui.components.PrayerTimeCard
import com.mosque.prayerclock.viewmodel.MainViewModel
import com.mosque.prayerclock.viewmodel.MainUiState
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.seconds

@Composable
private fun isAndroidTV(): Boolean {
    val context = LocalContext.current
    val uiModeManager = context.getSystemService(android.content.Context.UI_MODE_SERVICE) as UiModeManager
    return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

@Composable
fun MainScreen(
    onOpenSettings: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadPrayerTimes()
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
            settings = settings,
            onOpenSettings = onOpenSettings,
            onRetry = { viewModel.loadPrayerTimes() }
        )
    }
}

@Composable
fun NewMainLayout(
    uiState: MainUiState,
    settings: AppSettings,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit
) {
    val isTV = isAndroidTV()
    val paddingSize = if (isTV) 32.dp else 16.dp
    val spacingSize = if (isTV) 32.dp else 24.dp
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingSize)
    ) {
        // Mosque title at top center
        MosqueHeader(
            mosqueName = settings.mosqueName,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(spacingSize))
        
        // Clock and Next Prayer Timer side by side
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(spacingSize),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clock section
            ClockSection(
                settings = settings,
                onClockClick = onOpenSettings,
                isCompact = true,
                isTV = isTV,
                modifier = Modifier.weight(1f)
            )
            
            // Next prayer timer
            when (uiState) {
                is MainUiState.Loading -> {
                    Box(
                        modifier = Modifier.weight(1f),
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
                        isTV = isTV,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                is MainUiState.Error -> {
                    Box(
                        modifier = Modifier.weight(1f),
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
        }
        
        // Prayer times at bottom with animation
        when (uiState) {
            is MainUiState.Success -> {
                AnimatedPrayerTimesSection(
                    prayerTimes = uiState.prayerTimes,
                    nextPrayer = uiState.nextPrayer,
                    language = settings.language,
                    show24Hour = settings.show24HourFormat,
                    isTV = isTV
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        if (mosqueName.isNotEmpty()) {
            Text(
                text = mosqueName,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ClockSection(
    settings: AppSettings,
    onClockClick: () -> Unit,
    isLandscape: Boolean = false,
    isCompact: Boolean = false,
    isTV: Boolean = false,
    modifier: Modifier = Modifier
) {
    val clockSize = when {
        isTV && isCompact -> 220.dp
        isTV -> 300.dp
        isCompact -> 180.dp
        isLandscape -> 200.dp
        else -> 250.dp
    }
    
    val fontSize = when {
        isTV && isCompact -> 80.sp
        isTV -> 100.sp
        isLandscape -> 72.sp
        else -> 96.sp
    }
    
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClockClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        val boxPadding = if (isTV) 16.dp else 24.dp
        
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
                        language = settings.language
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
    val prayerInfoList = remember(prayerTimes, language) {
        createPrayerInfoList(prayerTimes, language)
    }
    
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
                    text = if (language == Language.TAMIL) nextPrayerInfo.nameTa else nextPrayerInfo.nameEn,
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
                
                val timeUntilPrayer = calculateTimeUntilPrayer(nextPrayerInfo.azanTime, currentTime)
                if (timeUntilPrayer.isNotEmpty()) {
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
    val prayerInfoList = remember(prayerTimes, language) {
        createPrayerInfoList(prayerTimes, language)
    }
    
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
            Text(stringResource(R.string.refresh))
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

private fun calculateTimeUntilPrayer(prayerTime: String, currentTime: Instant): String {
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
            return ""
        }
        
        val hours = duration.inWholeHours
        val minutes = (duration.inWholeMinutes % 60)
        val seconds = (duration.inWholeSeconds % 60)
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m remaining"
            minutes > 0 -> "${minutes}m ${seconds}s remaining"
            else -> "${seconds}s remaining"
        }
    } catch (e: Exception) {
        return ""
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NextPrayerCountdownSection(
    prayerTimes: PrayerTimes,
    nextPrayer: PrayerType?,
    language: Language,
    show24Hour: Boolean = false,
    isTV: Boolean = false,
    modifier: Modifier = Modifier
) {
    val prayerInfoList = remember(prayerTimes, language) {
        createPrayerInfoList(prayerTimes, language)
    }
    
    val nextPrayerInfo = prayerInfoList.find { it.type == nextPrayer }
    var currentTime by remember { mutableStateOf(Clock.System.now()) }
    var showAzan by remember { mutableStateOf(true) }
    
    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Clock.System.now()
            kotlinx.coroutines.delay(1000)
        }
    }
    
    // Alternate between Azan and Iqamah display every 5 seconds
    LaunchedEffect(nextPrayerInfo) {
        if (nextPrayerInfo?.iqamahTime != null) {
            while (true) {
                kotlinx.coroutines.delay(5000)
                showAzan = !showAzan
            }
        }
    }
    
    if (nextPrayerInfo != null) {
        Card(
            modifier = modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            val titleFontSize = if (isTV) 28.sp else 20.sp
            val prayerNameFontSize = if (isTV) 48.sp else 32.sp
            val timeFontSize = if (isTV) 64.sp else 48.sp
            val paddingSize = if (isTV) 32.dp else 24.dp
            val spacingSize = if (isTV) 16.dp else 8.dp
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingSize),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Next Prayer",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = titleFontSize
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(spacingSize))
                
                Text(
                    text = if (language == Language.TAMIL) nextPrayerInfo.nameTa else nextPrayerInfo.nameEn,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = prayerNameFontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Animated time display
                AnimatedContent(
                    targetState = showAzan,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(800)) togetherWith fadeOut(animationSpec = tween(800))
                    }
                ) { isShowingAzan ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isShowingAzan) {
                            Text(
                                text = "Azan",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                text = "Iqamah",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        
                        Text(
                            text = if (isShowingAzan) 
                                formatTimeBasedOnPreference(nextPrayerInfo.azanTime, show24Hour) 
                            else 
                                formatTimeBasedOnPreference(nextPrayerInfo.iqamahTime ?: nextPrayerInfo.azanTime, show24Hour),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = timeFontSize,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Countdown timer
                val timeUntilPrayer = calculateTimeUntilPrayer(nextPrayerInfo.azanTime, currentTime)
                if (timeUntilPrayer.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = timeUntilPrayer,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
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
    isTV: Boolean = false
) {
    val prayerInfoList = remember(prayerTimes, language) {
        createPrayerInfoList(prayerTimes, language)
    }
    
    val gridSpacing = if (isTV) 16.dp else 8.dp
    val gridPadding = if (isTV) 16.dp else 8.dp
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = gridPadding),
        horizontalArrangement = Arrangement.spacedBy(gridSpacing),
        verticalArrangement = Arrangement.spacedBy(gridSpacing)
    ) {
        items(prayerInfoList) { prayerInfo ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { it },
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
                    isTV = isTV
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
    isTV: Boolean = false
) {
    val cardPadding = if (isTV) 8.dp else 4.dp
    val contentPadding = if (isTV) 16.dp else 12.dp
    val cornerRadius = if (isTV) 16.dp else 12.dp
    val cardElevation = if (isNext) (if (isTV) 12.dp else 8.dp) else (if (isTV) 8.dp else 4.dp)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(cardPadding),
        colors = CardDefaults.cardColors(
            containerColor = if (isNext) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
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
            val prayerNameFontSize = if (isTV) 18.sp else 14.sp
            val prayerTimeFontSize = if (isTV) 20.sp else 16.sp
            val iqamahTimeFontSize = if (isTV) 14.sp else 12.sp
            val spacingSize = if (isTV) 8.dp else 4.dp
            
            Text(
                text = if (language == Language.TAMIL) prayerInfo.nameTa else prayerInfo.nameEn,
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
            
            Text(
                text = formatTimeBasedOnPreference(prayerInfo.azanTime, show24Hour),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = prayerTimeFontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = if (isNext) 
                    MaterialTheme.colorScheme.primary
                else 
                    MaterialTheme.colorScheme.onSurface
            )
            
            prayerInfo.iqamahTime?.let { iqamahTime ->
                Text(
                    text = "I: ${formatTimeBasedOnPreference(iqamahTime, show24Hour)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = iqamahTimeFontSize
                    ),
                    color = if (isNext) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun createPrayerInfoList(prayerTimes: PrayerTimes, @Suppress("UNUSED_PARAMETER") language: Language): List<PrayerInfo> {
    return listOf(
        PrayerInfo(
            type = PrayerType.FAJR,
            azanTime = prayerTimes.fajrAzan,
            iqamahTime = prayerTimes.fajrIqamah,
            nameEn = "Fajr",
            nameTa = "ஃபஜ்ர்"
        ),
        PrayerInfo(
            type = PrayerType.SUNRISE,
            azanTime = prayerTimes.sunrise,
            iqamahTime = null,
            nameEn = "Sunrise",
            nameTa = "சூரிய உதயம்"
        ),
        PrayerInfo(
            type = PrayerType.DHUHR,
            azanTime = prayerTimes.dhuhrAzan,
            iqamahTime = prayerTimes.dhuhrIqamah,
            nameEn = "Dhuhr",
            nameTa = "துயுஹர்"
        ),
        PrayerInfo(
            type = PrayerType.ASR,
            azanTime = prayerTimes.asrAzan,
            iqamahTime = prayerTimes.asrIqamah,
            nameEn = "Asr",
            nameTa = "அஸர்"
        ),
        PrayerInfo(
            type = PrayerType.MAGHRIB,
            azanTime = prayerTimes.maghribAzan,
            iqamahTime = prayerTimes.maghribIqamah,
            nameEn = "Maghrib",
            nameTa = "மஃக்ரிப்"
        ),
        PrayerInfo(
            type = PrayerType.ISHA,
            azanTime = prayerTimes.ishaAzan,
            iqamahTime = prayerTimes.ishaIqamah,
            nameEn = "Isha",
            nameTa = "இஷா"
        )
    )
}