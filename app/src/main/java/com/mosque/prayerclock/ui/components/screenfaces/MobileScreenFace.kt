package com.mosque.prayerclock.ui.components.screenfaces

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.ui.components.AnalogClock
import com.mosque.prayerclock.ui.components.DigitalClock
import com.mosque.prayerclock.ui.components.NextPrayerSection
import com.mosque.prayerclock.ui.components.PrayerTimeCard
import com.mosque.prayerclock.ui.components.WeatherCard
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.viewmodel.MainUiState
import com.mosque.prayerclock.viewmodel.WeatherUiState
import kotlinx.coroutines.delay
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object MobileScreenFace : ScreenFace {
    override val id = "mobile"
    override val name = "Classic Mobile"
    override val description = "Portrait layout with clock, grid prayer cards, and weather"

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
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Clock card
                    MobileClockSection(
                        data = data,
                        onClockClick = callbacks.onOpenSettings,
                        modifier = Modifier.fillMaxWidth().weight(2f),
                    )

                    // Next prayer section
                    when (val state = data.mainState) {
                        is MainUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().weight(2f),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        is MainUiState.Success -> {
                            NextPrayerSection(
                                prayerTimes = state.prayerTimes,
                                nextPrayer = state.nextPrayer,
                                nextDayFajr = state.nextDayFajr,
                                show24Hour = data.settings.show24HourFormat,
                                modifier = Modifier.fillMaxWidth().weight(2f),
                                onPrayerTransition = callbacks.onPrayerTransition,
                                showCountdown = data.isCountdownVisibleForWeight,
                                currentTime = data.currentTime,
                                isCurrentPrayer = data.isCurrentPrayerGlobal,
                            )
                        }
                        is MainUiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().weight(2f),
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
                    }

                    // Prayer times 2×3 grid
                    when (val state = data.mainState) {
                        is MainUiState.Success -> {
                            MobilePortraitPrayerGrid(
                                prayerTimes = state.prayerTimes,
                                nextPrayer = state.nextPrayer,
                                show24Hour = data.settings.show24HourFormat,
                                globalShowAzan = data.globalShowAzan,
                                currentTime = data.currentTime,
                                modifier = Modifier.fillMaxWidth().weight(2f),
                            )
                        }
                        else -> Spacer(modifier = Modifier.weight(2f))
                    }

                    // Weather card
                    if (data.settings.showWeather) {
                        when (val ws = data.weatherState) {
                            is WeatherUiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth().weight(1.2f),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            is WeatherUiState.Success -> {
                                WeatherCard(
                                    weatherInfo = ws.weatherInfo,
                                    modifier = Modifier.fillMaxWidth().weight(1.2f),
                                )
                            }
                            is WeatherUiState.Error -> Spacer(modifier = Modifier.weight(1.2f))
                        }
                    }
                }

            }
        }
    }
}

@Composable
private fun MobileClockSection(
    data: ScreenDisplayData,
    onClockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClockClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            val actualClockType = when (data.settings.clockType) {
                ClockType.BOTH -> {
                    var currentType by remember { mutableStateOf(ClockType.DIGITAL) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(120000)
                            currentType = if (currentType == ClockType.DIGITAL) ClockType.ANALOG else ClockType.DIGITAL
                        }
                    }
                    currentType
                }
                else -> data.settings.clockType
            }

            Box(
                modifier = Modifier.padding(6.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                when (actualClockType) {
                    ClockType.DIGITAL -> DigitalClock(data = data.clock, modifier = Modifier.fillMaxWidth())
                    ClockType.ANALOG -> AnalogClock(data = data.clock, modifier = Modifier.fillMaxWidth())
                    ClockType.BOTH -> DigitalClock(data = data.clock, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun MobilePortraitPrayerGrid(
    prayerTimes: com.mosque.prayerclock.data.model.PrayerTimesWithIqamah,
    nextPrayer: PrayerType?,
    show24Hour: Boolean,
    globalShowAzan: Boolean,
    currentTime: kotlinx.datetime.Instant,
    modifier: Modifier = Modifier,
) {
    val isFriday = currentTime.toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek == DayOfWeek.FRIDAY

    val prayers = listOf(
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
            iqamahTime = if (isFriday) null else prayerTimes.dhuhrIqamah,
            name = if (isFriday) localizedStringResource(R.string.jummah) else localizedStringResource(R.string.dhuhr),
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

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Row 1: Fajr, Sunrise, Dhuhr
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            prayers.take(3).forEach { prayer ->
                PrayerTimeCard(
                    prayerInfo = prayer,
                    isNext = prayer.type == nextPrayer,
                    show24Hour = show24Hour,
                    globalShowAzan = globalShowAzan,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
        // Row 2: Asr, Maghrib, Isha
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            prayers.drop(3).forEach { prayer ->
                PrayerTimeCard(
                    prayerInfo = prayer,
                    isNext = prayer.type == nextPrayer,
                    show24Hour = show24Hour,
                    globalShowAzan = globalShowAzan,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}
