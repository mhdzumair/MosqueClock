package com.mosque.prayerclock.ui.components.screenfaces

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.ui.components.AnalogClock
import com.mosque.prayerclock.data.model.PrayerTimesWithIqamah
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.ui.components.NextPrayerSection
import com.mosque.prayerclock.ui.components.WeatherCard
import com.mosque.prayerclock.ui.components.clockfaces.ClockDisplayData
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.theme.ColorAzanTime
import com.mosque.prayerclock.ui.theme.ColorNextAzanTime
import com.mosque.prayerclock.ui.theme.ColorNextSunriseTime
import com.mosque.prayerclock.ui.theme.ColorPrimaryAccent
import com.mosque.prayerclock.ui.theme.ColorSunriseTime
import com.mosque.prayerclock.utils.TimeUtils
import com.mosque.prayerclock.viewmodel.MainUiState
import com.mosque.prayerclock.viewmodel.WeatherUiState
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.min

object HorizonMobileScreenFace : ScreenFace {
    override val id = "horizon_mobile"
    override val legacyIds = setOf("modern_mobile")
    override val name = "Horizon Mobile"
    override val description = "A clear portrait schedule with azan and iqamah visible together"

    @Composable
    override fun Countdown(data: ScreenDisplayData, countdown: FullScreenCountdownData, modifier: Modifier) =
        HorizonScreenFace.Countdown(data, countdown, modifier)

    @Composable
    override fun FridayNightBayanCountdown(
        data: ScreenDisplayData,
        countdown: FullScreenCountdownData,
        modifier: Modifier,
    ) = HorizonScreenFace.FridayNightBayanCountdown(data, countdown, modifier)

    @Composable
    override fun Dua(data: ScreenDisplayData, modifier: Modifier) = HorizonScreenFace.Dua(data, modifier)

    @Composable
    override fun FridayBayan(data: ScreenDisplayData, bayan: JummahData, modifier: Modifier) =
        HorizonScreenFace.FridayBayan(data, bayan, modifier)

    @Composable
    override fun Dashboard(
        data: ScreenDisplayData,
        callbacks: ScreenFaceCallbacks,
        modifier: Modifier,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = data.settings.mosqueName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = callbacks.onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = localizedStringResource(R.string.settings), tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    // Compact time + date strip (no cards, clean text-only)
                    if (data.settings.clockType == ClockType.ANALOG || data.settings.clockType == ClockType.BOTH) {
                        AnalogClock(data = data.clock, modifier = Modifier.fillMaxWidth().height(240.dp))
                    }
                    ModernTimeStrip(
                        clock = data.clock,
                        modifier = Modifier.fillMaxWidth().height(144.dp),
                        onTap = callbacks.onOpenSettings,
                    )

                    // Next prayer countdown hero
                    when (val state = data.mainState) {
                        is MainUiState.Loading -> Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        is MainUiState.Success -> NextPrayerSection(
                            prayerTimes = state.prayerTimes,
                            nextPrayer = state.nextPrayer,
                            nextDayFajr = state.nextDayFajr,
                            show24Hour = data.settings.show24HourFormat,
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            onPrayerTransition = callbacks.onPrayerTransition,
                            showCountdown = data.isCountdownVisibleForWeight,
                            currentTime = data.currentTime,
                            isCurrentPrayer = data.isCurrentPrayerGlobal,
                        )
                        is MainUiState.Error -> Box(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = data.errorMessage ?: state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    // Prayer times — vertical list showing azan + iqamah simultaneously
                    when (val state = data.mainState) {
                        is MainUiState.Success -> ModernPrayerList(
                            prayerTimes = state.prayerTimes,
                            nextPrayer = state.nextPrayer,
                            show24Hour = data.settings.show24HourFormat,
                            currentTime = data.currentTime,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        else -> Unit
                    }

                    // Weather strip
                    if (data.settings.showWeather) {
                        when (val ws = data.weatherState) {
                            is WeatherUiState.Loading -> Box(
                                modifier = Modifier.fillMaxWidth().height(144.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            is WeatherUiState.Success -> WeatherCard(
                                weatherInfo = ws.weatherInfo,
                                modifier = Modifier.fillMaxWidth().height(144.dp),
                            )
                            is WeatherUiState.Error -> Unit
                        }
                    }
                }

            }
        }
    }
}

// ── Time strip ──────────────────────────────────────────────────────────────

@Composable
private fun ModernTimeStrip(
    clock: ClockDisplayData,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.clickable(onClick = onTap)) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val timeFontSize = with(density) {
            val charCount = clock.formattedTime.length.coerceAtLeast(1).toFloat()
            val widthBased = (widthPx / charCount * 1.7f).toSp()
            val safeHeight = if (heightPx.isInfinite()) widthPx * 0.4f else heightPx
            val heightBased = (safeHeight * 0.55f).toSp()
            min(widthBased.value, heightBased.value).sp
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = clock.formattedTime,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = timeFontSize,
                    fontWeight = FontWeight.Bold,
                ),
                color = ColorPrimaryAccent,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${clock.hijriDay} ${clock.hijriMonthName} ${clock.hijriYear}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = "${clock.gregorianDay} ${clock.gregorianMonthName} ${clock.gregorianYear}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

// ── Prayer list ──────────────────────────────────────────────────────────────

@Composable
private fun ModernPrayerList(
    prayerTimes: PrayerTimesWithIqamah,
    nextPrayer: PrayerType?,
    show24Hour: Boolean,
    currentTime: kotlinx.datetime.Instant,
    modifier: Modifier = Modifier,
) {
    val isFriday = currentTime.toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek == DayOfWeek.FRIDAY

    val prayers = listOf(
        PrayerInfo(PrayerType.FAJR, prayerTimes.fajrAzan, prayerTimes.fajrIqamah, localizedStringResource(R.string.fajr)),
        PrayerInfo(PrayerType.SUNRISE, prayerTimes.sunrise, null, localizedStringResource(R.string.sunrise)),
        PrayerInfo(
            PrayerType.DHUHR, prayerTimes.dhuhrAzan,
            if (isFriday) null else prayerTimes.dhuhrIqamah,
            if (isFriday) localizedStringResource(R.string.jummah) else localizedStringResource(R.string.dhuhr),
        ),
        PrayerInfo(PrayerType.ASR, prayerTimes.asrAzan, prayerTimes.asrIqamah, localizedStringResource(R.string.asr)),
        PrayerInfo(PrayerType.MAGHRIB, prayerTimes.maghribAzan, prayerTimes.maghribIqamah, localizedStringResource(R.string.maghrib)),
        PrayerInfo(PrayerType.ISHA, prayerTimes.ishaAzan, prayerTimes.ishaIqamah, localizedStringResource(R.string.isha)),
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(localizedStringResource(R.string.prayer), modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(localizedStringResource(R.string.azan), modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(localizedStringResource(R.string.iqamah), modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            prayers.forEachIndexed { index, prayer ->
                ModernPrayerRow(
                    prayer = prayer,
                    isNext = prayer.type == nextPrayer,
                    show24Hour = show24Hour,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                )
                if (index < prayers.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernPrayerRow(
    prayer: PrayerInfo,
    isNext: Boolean,
    show24Hour: Boolean,
    modifier: Modifier = Modifier,
) {
    val prayerColor = when {
        prayer.type == PrayerType.SUNRISE -> if (isNext) ColorNextSunriseTime else ColorSunriseTime
        isNext -> ColorNextAzanTime
        else -> ColorAzanTime
    }

    Row(
        modifier = modifier
            .background(
                color = if (isNext) prayerColor.copy(alpha = 0.09f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = prayer.name,
            modifier = Modifier.weight(1.2f).padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = TimeUtils.formatTimeBasedOnPreference(prayer.azanTime, show24Hour),
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = prayerColor,
            textAlign = TextAlign.End,
        )
        Text(
            text = prayer.iqamahTime?.let { TimeUtils.formatTimeBasedOnPreference(it, show24Hour) } ?: "—",
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}
