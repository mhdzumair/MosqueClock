package com.mosque.prayerclock.ui.components.screenfaces

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.ui.components.NextPrayerSection
import com.mosque.prayerclock.ui.components.WeatherCard
import com.mosque.prayerclock.ui.components.AnalogClock
import com.mosque.prayerclock.ui.components.DigitalClock
import com.mosque.prayerclock.ui.screens.AnimatedPrayerTimesSection
import com.mosque.prayerclock.viewmodel.MainUiState
import com.mosque.prayerclock.viewmodel.WeatherUiState

object ClassicScreenFace : ScreenFace {
    override val id = "classic"
    override val name = "Classic TV"
    override val description = "Traditional landscape layout with clock, prayer cards, and weather"

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
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                ) {
                    Row(
                        modifier = Modifier.weight(0.75f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        // Left column — header + clock
                        Column(
                            modifier = Modifier.weight(1.6f),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (data.settings.mosqueName.isNotEmpty()) {
                                ClassicMosqueHeader(
                                    mosqueName = data.settings.mosqueName,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            ClassicClockSection(
                                data = data,
                                onClockClick = callbacks.onOpenSettings,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        // Right column — next prayer + weather
                        Column(
                            modifier = Modifier.weight(1.0f).fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            when (val state = data.mainState) {
                                is MainUiState.Loading -> {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .weight(if (data.settings.showWeather) 2f else 1f),
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
                                        prayerTimes = state.prayerTimes,
                                        nextPrayer = state.nextPrayer,
                                        nextDayFajr = state.nextDayFajr,
                                        show24Hour = data.settings.show24HourFormat,
                                        modifier = Modifier.fillMaxWidth()
                                            .weight(if (data.settings.showWeather) 2f else 1f),
                                        onPrayerTransition = callbacks.onPrayerTransition,
                                        showCountdown = data.isCountdownVisibleForWeight,
                                        currentTime = data.currentTime,
                                        isCurrentPrayer = data.isCurrentPrayerGlobal,
                                    )
                                }
                                is MainUiState.Error -> {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .weight(if (data.settings.showWeather) 2f else 1f),
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

                            if (data.settings.showWeather) {
                                when (val ws = data.weatherState) {
                                    is WeatherUiState.Loading -> {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().weight(1f),
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
                                            weatherInfo = ws.weatherInfo,
                                            modifier = Modifier.fillMaxWidth().weight(1f),
                                        )
                                    }
                                    is WeatherUiState.Error -> { /* hidden on error */ }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (val state = data.mainState) {
                        is MainUiState.Success -> {
                            AnimatedPrayerTimesSection(
                                prayerTimes = state.prayerTimes,
                                nextPrayer = state.nextPrayer,
                                show24Hour = data.settings.show24HourFormat,
                                globalShowAzan = data.globalShowAzan,
                            )
                        }
                        else -> Spacer(modifier = Modifier.height(100.dp))
                    }
                }

            }
        }
    }

}

@Composable
private fun ClassicMosqueHeader(
    mosqueName: String,
    modifier: Modifier = Modifier,
) {
    if (mosqueName.isNotEmpty()) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
        ) {
            Image(
                painter = painterResource(id = R.drawable.mosque_logo),
                contentDescription = "Mosque Logo",
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = mosqueName,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 22.sp,
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
private fun ClassicClockSection(
    data: ScreenDisplayData,
    onClockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .padding(4.dp)
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
                            kotlinx.coroutines.delay(120000)
                            currentType = if (currentType == ClockType.DIGITAL) ClockType.ANALOG else ClockType.DIGITAL
                        }
                    }
                    currentType
                }
                else -> data.settings.clockType
            }

            Box(
                modifier = Modifier.padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = actualClockType,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(1000)) togetherWith fadeOut(animationSpec = tween(1000))
                    },
                    label = "clock_transition",
                ) { clockType ->
                    when (clockType) {
                        ClockType.DIGITAL -> DigitalClock(data = data.clock, modifier = Modifier)
                        ClockType.ANALOG -> AnalogClock(data = data.clock, modifier = Modifier)
                        ClockType.BOTH -> DigitalClock(data = data.clock, modifier = Modifier)
                    }
                }
            }
        }
    }
}
