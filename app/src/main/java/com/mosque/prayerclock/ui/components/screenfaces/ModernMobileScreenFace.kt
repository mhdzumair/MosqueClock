package com.mosque.prayerclock.ui.components.screenfaces

import androidx.compose.foundation.background
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
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Compact time + date strip (no cards, clean text-only)
                    ModernTimeStrip(
                        clock = data.clock,
                        modifier = Modifier.fillMaxWidth().weight(1.2f),
                        onTap = callbacks.onOpenSettings,
                    )

                    // Next prayer countdown hero
                    when (val state = data.mainState) {
                        is MainUiState.Loading -> Box(
                            modifier = Modifier.fillMaxWidth().weight(2f),
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
                            modifier = Modifier.fillMaxWidth().weight(2f),
                            onPrayerTransition = callbacks.onPrayerTransition,
                            showCountdown = data.isCountdownVisibleForWeight,
                            currentTime = data.currentTime,
                            isCurrentPrayer = data.isCurrentPrayerGlobal,
                        )
                        is MainUiState.Error -> Box(
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

                    // Prayer times — vertical list showing azan + iqamah simultaneously
                    when (val state = data.mainState) {
                        is MainUiState.Success -> ModernPrayerList(
                            prayerTimes = state.prayerTimes,
                            nextPrayer = state.nextPrayer,
                            show24Hour = data.settings.show24HourFormat,
                            currentTime = data.currentTime,
                            modifier = Modifier.fillMaxWidth().weight(3.5f),
                        )
                        else -> Spacer(modifier = Modifier.weight(3.5f))
                    }

                    // Weather strip
                    if (data.settings.showWeather) {
                        when (val ws = data.weatherState) {
                            is WeatherUiState.Loading -> Box(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            is WeatherUiState.Success -> WeatherCard(
                                weatherInfo = ws.weatherInfo,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            )
                            is WeatherUiState.Error -> Spacer(modifier = Modifier.weight(1f))
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
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            prayers.forEachIndexed { index, prayer ->
                ModernPrayerRow(
                    prayer = prayer,
                    isNext = prayer.type == nextPrayer,
                    show24Hour = show24Hour,
                    modifier = Modifier.fillMaxWidth().weight(1f),
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
            .padding(end = 10.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left accent bar (4 dp wide, full row height, shown only for next prayer)
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .padding(vertical = 5.dp)
                .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                .background(if (isNext) prayerColor else Color.Transparent),
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Prayer name — left-aligned, adaptive font size
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val density = LocalDensity.current
            val heightPx = with(density) { maxHeight.toPx() }
            val fontSize = with(density) {
                val raw = if (heightPx.isInfinite() || heightPx == 0f) 15f
                          else (heightPx * 0.38f).toSp().value
                raw.coerceIn(11f, 18f).sp
            }
            Text(
                text = prayer.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSize,
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
            )
        }

        // Azan time — main, colored
        BoxWithConstraints {
            val density = LocalDensity.current
            val heightPx = with(density) { maxHeight.toPx() }
            val fontSize = with(density) {
                val raw = if (heightPx.isInfinite() || heightPx == 0f) 16f
                          else (heightPx * 0.38f).toSp().value
                raw.coerceIn(12f, 19f).sp
            }
            Text(
                text = TimeUtils.formatTimeBasedOnPreference(prayer.azanTime, show24Hour),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                ),
                color = prayerColor,
                maxLines = 1,
            )
        }

        // Iqamah time — shown statically to the right (the key visual difference vs. classic faces)
        if (prayer.iqamahTime != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = localizedStringResource(R.string.iqamah_letter),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                )
                Text(
                    text = TimeUtils.formatTimeBasedOnPreference(prayer.iqamahTime, show24Hour),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 1,
                )
            }
        }
    }
}
