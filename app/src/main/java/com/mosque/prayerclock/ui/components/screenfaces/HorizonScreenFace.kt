package com.mosque.prayerclock.ui.components.screenfaces

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.data.model.PrayerTimesWithIqamah
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.localizedStringArrayResource
import com.mosque.prayerclock.ui.LocalEffectiveLanguage
import com.mosque.prayerclock.ui.components.getCountdownData
import com.mosque.prayerclock.ui.components.rememberTvTextScale
import com.mosque.prayerclock.ui.components.TvAutoSizeText
import com.mosque.prayerclock.ui.components.screenfaces.shared.fullScreenCountdownTitle
import com.mosque.prayerclock.ui.components.screenfaces.shared.FacePanelClock
import com.mosque.prayerclock.ui.components.screenfaces.shared.resolveDisplayNextPrayer
import com.mosque.prayerclock.ui.theme.ColorAzanTime
import com.mosque.prayerclock.ui.theme.ColorNextAzanTime
import com.mosque.prayerclock.ui.theme.ColorNextSunriseTime
import com.mosque.prayerclock.ui.theme.ColorSunriseTime
import com.mosque.prayerclock.utils.TimeUtils
import com.mosque.prayerclock.viewmodel.MainUiState
import com.mosque.prayerclock.viewmodel.WeatherUiState
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * A calm, distance-readable TV face. The hierarchy is deliberately horizontal:
 * identity and dates, current/next prayer, then the full day at a glance.
 */
object HorizonScreenFace : ScreenFace {
    override val recommendedColorThemeId = "slate_steel"
    override val id = "horizon"
    override val legacyIds = setOf("modern_tv", "modern")
    override val name = "Horizon"
    override val description = "Wide TV layout with a strong clock, next prayer, and one-glance schedule"

    @Composable
    override fun Dashboard(data: ScreenDisplayData, callbacks: ScreenFaceCallbacks, modifier: Modifier) {
        Column(
            modifier = modifier
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface),
                    ),
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HorizonHeader(data, Modifier.fillMaxWidth().weight(0.12f))

            Row(
                modifier = Modifier.fillMaxWidth().weight(0.52f),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                HorizonClock(
                    data = data,
                    onClick = callbacks.onOpenSettings,
                    modifier = Modifier.weight(1.45f).fillMaxHeight(),
                )
                Box(
                    Modifier.width(1.dp).fillMaxHeight(0.72f).align(Alignment.CenterVertically)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.16f)),
                )
                HorizonNextPrayer(
                    data = data,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }

            when (val state = data.mainState) {
                is MainUiState.Success -> HorizonSchedule(
                    prayerTimes = state.prayerTimes,
                    nextPrayer = state.nextPrayer,
                    show24Hour = data.settings.show24HourFormat,
                    currentTime = data.currentTime,
                    modifier = Modifier.fillMaxWidth().weight(0.36f),
                )
                else -> Spacer(Modifier.weight(0.36f))
            }
        }
    }

    @Composable
    override fun Countdown(data: ScreenDisplayData, countdown: FullScreenCountdownData, modifier: Modifier) {
        HorizonBackdrop(modifier) { scale ->
            val event = fullScreenCountdownTitle(countdown.prayerName, countdown.prayerType, countdown.isIqamah, countdown.kind)
            TvAutoSizeText(
                text = event.uppercase(),
                color = MaterialTheme.colorScheme.primary,
                maxFontSize = scale.title,
                minFontSize = scale.title * 0.55f,
                lineHeightRatio = 1.18f,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Spacer(Modifier.height(scale.spacing))
            TvAutoSizeText(
                text = "%02d:%02d".format(countdown.minutes, countdown.seconds),
                color = MaterialTheme.colorScheme.onBackground,
                maxFontSize = scale.hero,
                minFontSize = scale.hero * 0.55f,
                lineHeightRatio = 1.02f,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            TvAutoSizeText(
                text = if (countdown.isIqamah) {
                    TimeUtils.formatTimeBasedOnPreference(countdown.targetTime, data.settings.show24HourFormat)
                } else {
                    TimeUtils.formatTimeBasedOnPreference(countdown.azanTime, data.settings.show24HourFormat)
                },
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                maxFontSize = scale.body,
                minFontSize = scale.body * 0.55f,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }

    @Composable
    override fun FridayNightBayanCountdown(
        data: ScreenDisplayData,
        countdown: FullScreenCountdownData,
        modifier: Modifier,
    ) {
        HorizonBackdrop(modifier) { scale ->
            TvAutoSizeText(
                text = localizedStringResource(R.string.friday_night_bayan).uppercase(),
                color = MaterialTheme.colorScheme.primary,
                maxFontSize = scale.title,
                minFontSize = scale.title * 0.55f,
                lineHeightRatio = 1.18f,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Spacer(Modifier.height(scale.spacing))
            TvAutoSizeText(
                text = "%02d:%02d".format(countdown.minutes, countdown.seconds),
                color = MaterialTheme.colorScheme.onBackground,
                maxFontSize = scale.hero,
                minFontSize = scale.hero * 0.55f,
                lineHeightRatio = 1.02f,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            TvAutoSizeText(
                text = localizedStringResource(R.string.silent_your_phone),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                maxFontSize = scale.body,
                minFontSize = scale.body * 0.55f,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }

    @Composable
    override fun Dua(data: ScreenDisplayData, modifier: Modifier) {
        HorizonBackdrop(modifier) { scale ->
            TvAutoSizeText(
                text = localizedStringResource(R.string.silent_your_phone).uppercase(),
                color = MaterialTheme.colorScheme.primary,
                maxFontSize = scale.eyebrow,
                minFontSize = scale.eyebrow * 0.55f,
                lineHeightRatio = 1.16f,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Spacer(Modifier.height(scale.spacing))
            TvAutoSizeText(
                text = localizedStringResource(R.string.dua_joining_saff_arabic),
                color = MaterialTheme.colorScheme.onBackground,
                maxFontSize = scale.arabic,
                minFontSize = scale.arabic * 0.52f,
                lineHeightRatio = 1.28f,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Spacer(Modifier.height(scale.spacing * 0.8f))
            TvAutoSizeText(
                text = localizedStringResource(R.string.dua_joining_saff_transliteration),
                color = MaterialTheme.colorScheme.primary,
                maxFontSize = scale.body,
                minFontSize = scale.body * 0.5f,
                lineHeightRatio = 1.22f,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Spacer(Modifier.height(scale.spacing * 0.45f))
            TvAutoSizeText(
                text = localizedStringResource(R.string.dua_joining_saff_meaning),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                maxFontSize = scale.detail,
                minFontSize = scale.detail * 0.5f,
                lineHeightRatio = 1.24f,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
        }
    }

    @Composable
    override fun FridayBayan(data: ScreenDisplayData, bayan: JummahData, modifier: Modifier) {
        HorizonBackdrop(modifier) { scale ->
            TvAutoSizeText(
                text = localizedStringResource(R.string.silent_your_phone).uppercase(),
                color = MaterialTheme.colorScheme.primary,
                maxFontSize = scale.eyebrow,
                minFontSize = scale.eyebrow * 0.55f,
                lineHeightRatio = 1.16f,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Spacer(Modifier.height(scale.spacing))
            TvAutoSizeText(
                text = localizedStringResource(R.string.jummah_in_progress_title),
                color = MaterialTheme.colorScheme.onBackground,
                maxFontSize = scale.title * 1.2f,
                minFontSize = scale.title * 0.55f,
                lineHeightRatio = 1.12f,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Spacer(Modifier.height(scale.spacing * 0.8f))
            val remaining = if (bayan.hoursRemaining > 0) {
                "%02d:%02d:%02d".format(bayan.hoursRemaining, bayan.minutesRemaining, bayan.secondsRemaining)
            } else {
                "%02d:%02d".format(bayan.minutesRemaining, bayan.secondsRemaining)
            }
            TvAutoSizeText(
                text = remaining,
                color = MaterialTheme.colorScheme.primary,
                maxFontSize = scale.hero,
                minFontSize = scale.hero * 0.5f,
                lineHeightRatio = 1.02f,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            TvAutoSizeText(
                text = localizedStringResource(R.string.jummah_prayer_reminder),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                maxFontSize = scale.detail,
                minFontSize = scale.detail * 0.5f,
                lineHeightRatio = 1.22f,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
        }
    }
}

private data class HorizonSpecialScale(
    val eyebrow: TextUnit,
    val title: TextUnit,
    val hero: TextUnit,
    val arabic: TextUnit,
    val body: TextUnit,
    val detail: TextUnit,
    val spacing: Dp,
)

@Composable
private fun HorizonBackdrop(
    modifier: Modifier,
    content: @Composable ColumnScope.(HorizonSpecialScale) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .background(Color(0xFF050705))
            .background(
                Brush.radialGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                        Color.Transparent,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        val scaleForRegion = rememberTvTextScale(maxWidth, maxHeight)
        val scale = HorizonSpecialScale(
            eyebrow = scaleForRegion.height(0.064f),
            title = scaleForRegion.height(0.100f),
            hero = if (maxHeight > maxWidth) {
                scaleForRegion.fit(0.20f, characterSlots = 5f, glyphWidthFactor = 1.5f)
            } else {
                scaleForRegion.fit(0.285f, characterSlots = 8f, glyphWidthFactor = 1.15f)
            },
            arabic = scaleForRegion.height(0.120f),
            body = scaleForRegion.height(0.11f),
            detail = scaleForRegion.height(0.052f),
            spacing = scaleForRegion.heightDp(0.014f),
        )
        // TV overscan needs a safe edge, but excessive side padding forces translated content to
        // shrink unnecessarily. Two-and-a-half percent remains safe while preserving text size.
        val horizontalPadding = scaleForRegion.widthDp(0.025f)
        val verticalPadding = scaleForRegion.heightDp(0.025f)
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            content(scale)
        }
    }
}

@Composable
private fun HorizonHeader(data: ScreenDisplayData, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val scale = rememberTvTextScale(maxWidth, maxHeight)
        val mosqueSize = scale.height(0.55f)
        val dateSize = scale.height(0.32f)
        val subDateSize = scale.height(0.25f)
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(7.dp).fillMaxHeight(0.65f).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Spacer(Modifier.width(14.dp))
            TvAutoSizeText(
                text = data.settings.mosqueName,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onBackground,
                maxFontSize = mosqueSize,
                minFontSize = mosqueSize * 0.52f,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            if (data.settings.showWeather) {
                HorizonWeather(data.weatherState, Modifier.fillMaxHeight().width(scale.widthDp(0.16f)))
                Spacer(Modifier.width(18.dp))
            }
            val weekday = localizedStringArrayResource(R.array.weekdays)[
                data.currentTime.toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek.ordinal
            ]
            Column(
                modifier = Modifier.width(scale.widthDp(0.28f)),
                horizontalAlignment = Alignment.End,
            ) {
                TvAutoSizeText(
                    text = "${data.clock.hijriDay} ${data.clock.hijriMonthName} ${data.clock.hijriYear}",
                    color = MaterialTheme.colorScheme.primary,
                    maxFontSize = dateSize,
                    minFontSize = dateSize * 0.5f,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                TvAutoSizeText(
                    text = "$weekday · ${data.clock.gregorianDay} ${data.clock.gregorianMonthName} ${data.clock.gregorianYear}",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                    maxFontSize = subDateSize,
                    minFontSize = subDateSize * 0.5f,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HorizonWeather(state: WeatherUiState, modifier: Modifier = Modifier) {
    when (state) {
        is WeatherUiState.Success -> BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
            val info = state.weatherInfo
            val scale = rememberTvTextScale(maxWidth, maxHeight)
            val iconSize = scale.heightDp(0.64f)
            val temperatureSize = scale.height(0.38f)
            val descriptionSize = scale.height(0.22f)
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizonWeatherIcon(info.icon, info.description, iconSize)
                Spacer(Modifier.width(7.dp))
                Column {
                    Text(
                        text = "${info.temperature.toInt()}°C",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = temperatureSize,
                        lineHeight = temperatureSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    TvAutoSizeText(
                        text = info.description,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                        maxFontSize = descriptionSize,
                        minFontSize = descriptionSize * 0.5f,
                        maxLines = 1,
                    )
                }
            }
        }
        is WeatherUiState.Loading -> Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        }
        is WeatherUiState.Error -> Unit
    }
}

@Composable
private fun HorizonWeatherIcon(icon: String, description: String, size: Dp) {
    val fallback: @Composable () -> Unit = {
        Icon(
            imageVector = if (description.contains("sun", ignoreCase = true) ||
                description.contains("clear", ignoreCase = true)
            ) Icons.Filled.WbSunny else Icons.Filled.Cloud,
            contentDescription = description,
            modifier = Modifier.size(size),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
    val iconUrl = when {
        icon.startsWith("http") -> icon
        icon.startsWith("//") -> "https:$icon"
        else -> null
    }
    if (iconUrl == null) {
        fallback()
    } else {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(iconUrl).crossfade(true).build(),
            contentDescription = description,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit,
            loading = { fallback() },
            error = { fallback() },
        )
    }
}

@Composable
private fun HorizonClock(data: ScreenDisplayData, onClick: () -> Unit, modifier: Modifier) {
    val clockType = when (data.settings.clockType) {
        ClockType.BOTH -> if ((data.clock.minute / 2) % 2 == 0) ClockType.DIGITAL else ClockType.ANALOG
        else -> data.settings.clockType
    }
    Box(modifier.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        when (clockType) {
            ClockType.ANALOG -> HorizonAnalogClock(data.clock, Modifier.fillMaxSize().padding(4.dp))
            else -> HorizonDigitalTime(data, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun HorizonDigitalTime(data: ScreenDisplayData, modifier: Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val scale = rememberTvTextScale(maxWidth, maxHeight)
        Column(Modifier.fillMaxSize()) {
            FacePanelClock(
                clock = data.clock,
                primaryColor = MaterialTheme.colorScheme.onBackground,
                mutedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                fontWeight = FontWeight.Black,
                secondaryScale = .40f,
                amLabel = localizedStringResource(R.string.am),
                pmLabel = localizedStringResource(R.string.pm),
            )
            Box(
                Modifier.padding(top = scale.heightDp(0.035f))
                    .width(scale.widthDp(0.17f)).height(scale.heightDp(0.014f))
                    .clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun HorizonNextPrayer(data: ScreenDisplayData, modifier: Modifier) {
    when (val state = data.mainState) {
        is MainUiState.Success -> {
            val rows = horizonPrayers(state.prayerTimes, data.currentTime)
            val prayer = resolveDisplayNextPrayer(rows, state.nextPrayer, data.currentTime, state.nextDayFajr)
            val currentLocalTime = data.currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
            val currentTimeString = "%02d:%02d".format(currentLocalTime.hour, currentLocalTime.minute)
            val isIqamahTarget = prayer.iqamahTime?.let { iqamah ->
                TimeUtils.compareTimeStrings(prayer.azanTime, currentTimeString) <= 0 &&
                    TimeUtils.compareTimeStrings(currentTimeString, iqamah) < 0
            } == true
            val targetTime = if (isIqamahTarget) prayer.iqamahTime ?: prayer.azanTime else prayer.azanTime
            val countdown = getCountdownData(
                targetTime,
                data.currentTime,
                rollToNextDay = prayer.type == PrayerType.FAJR,
            )
            val showInlineCountdown = data.isCountdownVisibleForWeight &&
                countdown.totalSeconds >= 0 && countdown.totalMinutes <= 60
            BoxWithConstraints(modifier.padding(horizontal = 12.dp)) {
                val scale = rememberTvTextScale(maxWidth, maxHeight)
                val titleSize = scale.height(0.085f)
                val nameSize = scale.height(if (showInlineCountdown) 0.16f else 0.20f)
                val timeSize = scale.fit(
                    heightFraction = if (showInlineCountdown) 0.21f else 0.27f,
                    characterSlots = 8f,
                    glyphWidthFactor = 1.35f,
                )
                val countdownSize = scale.height(0.16f)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                ) {
                TvAutoSizeText(
                    text = localizedStringResource(R.string.next_prayer).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    maxFontSize = titleSize,
                    minFontSize = titleSize * 0.55f,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.height(12.dp))
                TvAutoSizeText(
                    text = prayer.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxFontSize = nameSize,
                    minFontSize = nameSize * 0.58f,
                    lineHeightRatio = 1.12f,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Spacer(Modifier.height(8.dp))
                TvAutoSizeText(
                    text = TimeUtils.formatTimeBasedOnPreference(targetTime, data.settings.show24HourFormat),
                    color = ColorNextAzanTime,
                    maxFontSize = timeSize,
                    minFontSize = timeSize * 0.58f,
                    lineHeightRatio = 1.04f,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
                if (showInlineCountdown) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (prayer.type != PrayerType.SUNRISE) {
                            TvAutoSizeText(
                                text = localizedStringResource(if (isIqamahTarget) R.string.iqamah else R.string.azan).uppercase(),
                                color = MaterialTheme.colorScheme.primary,
                                maxFontSize = countdownSize * 0.42f,
                                minFontSize = countdownSize * 0.24f,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.weight(0.38f),
                                maxLines = 1,
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        TvAutoSizeText(
                            text = "%02d:%02d".format(countdown.totalMinutes, countdown.seconds),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxFontSize = countdownSize,
                            minFontSize = countdownSize * 0.7f,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(0.62f),
                            maxLines = 1,
                        )
                    }
                }
                }
            }
        }
        is MainUiState.Loading -> Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(48.dp))
        }
        is MainUiState.Error -> Box(modifier, contentAlignment = Alignment.Center) {
            Text(data.errorMessage ?: state.message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun HorizonSchedule(
    prayerTimes: PrayerTimesWithIqamah,
    nextPrayer: PrayerType?,
    show24Hour: Boolean,
    currentTime: kotlinx.datetime.Instant,
    modifier: Modifier,
) {
    val prayers = horizonPrayers(prayerTimes, currentTime)
    val effectiveLanguage = LocalEffectiveLanguage.current
    BoxWithConstraints(modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.24f))) {
        // Calibrated so 720p retains its current readable sizes without relying on fixed floors.
        // At 1080p these proportions then grow by the available-height ratio instead of staying
        // pinned to the same absolute sp values.
        val scale = rememberTvTextScale(maxWidth, maxHeight)
        val nameSize = scale.height(0.135f)
        val prayerTimeSize = scale.height(0.185f)
        val iqamahLabelSize = scale.height(0.120f)
        val iqamahTimeSize = scale.height(0.170f)
        Column(Modifier.fillMaxSize()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.16f))
            Row(Modifier.fillMaxSize().padding(vertical = 10.dp)) {
        prayers.forEachIndexed { index, prayer ->
            val isNext = prayer.type == nextPrayer
            val color = when {
                prayer.type == PrayerType.SUNRISE && isNext -> ColorNextSunriseTime
                prayer.type == PrayerType.SUNRISE -> ColorSunriseTime
                isNext -> ColorNextAzanTime
                else -> ColorAzanTime
            }
            if (index > 0) {
                Box(
                    Modifier.width(1.dp).fillMaxHeight(0.72f).align(Alignment.CenterVertically)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)),
                )
            }
            Column(
                Modifier.weight(1f).fillMaxHeight().padding(horizontal = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                    Box(
                        Modifier.width(if (isNext) 42.dp else 0.dp).height(4.dp)
                            .clip(CircleShape).background(if (isNext) color else Color.Transparent),
                    )
                    val prayerName = if (
                        prayer.type == PrayerType.SUNRISE && effectiveLanguage == Language.TAMIL
                    ) {
                        prayer.name.replaceFirst(" ", "\n")
                    } else {
                        prayer.name
                    }
                    TvAutoSizeText(
                        text = prayerName,
                        color = if (isNext) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
                        maxFontSize = nameSize,
                        minFontSize = nameSize * 0.52f,
                        lineHeightRatio = 1.12f,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = if (prayer.type == PrayerType.SUNRISE) 2 else 1,
                        textAlign = TextAlign.Center,
                    )
                    TvAutoSizeText(
                        text = TimeUtils.formatTimeBasedOnPreference(prayer.azanTime, show24Hour),
                        color = color,
                        maxFontSize = prayerTimeSize,
                        minFontSize = prayerTimeSize * 0.58f,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                    prayer.iqamahTime?.let {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TvAutoSizeText(
                                text = localizedStringResource(R.string.iqamah_letter),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                                maxFontSize = iqamahLabelSize,
                                minFontSize = iqamahLabelSize * 0.52f,
                                lineHeightRatio = 1.08f,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            TvAutoSizeText(
                                text = TimeUtils.formatTimeBasedOnPreference(it, show24Hour),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f),
                                maxFontSize = iqamahTimeSize,
                                minFontSize = iqamahTimeSize * 0.58f,
                                lineHeightRatio = 1.08f,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun horizonPrayers(
    prayerTimes: PrayerTimesWithIqamah,
    currentTime: kotlinx.datetime.Instant,
): List<PrayerInfo> {
    val isFriday = currentTime.toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek == DayOfWeek.FRIDAY
    return listOf(
        PrayerInfo(PrayerType.FAJR, prayerTimes.fajrAzan, prayerTimes.fajrIqamah, localizedStringResource(R.string.fajr)),
        PrayerInfo(PrayerType.SUNRISE, prayerTimes.sunrise, null, localizedStringResource(R.string.sunrise)),
        PrayerInfo(
            PrayerType.DHUHR,
            prayerTimes.dhuhrAzan,
            if (isFriday) null else prayerTimes.dhuhrIqamah,
            localizedStringResource(if (isFriday) R.string.jummah else R.string.dhuhr),
        ),
        PrayerInfo(PrayerType.ASR, prayerTimes.asrAzan, prayerTimes.asrIqamah, localizedStringResource(R.string.asr)),
        PrayerInfo(PrayerType.MAGHRIB, prayerTimes.maghribAzan, prayerTimes.maghribIqamah, localizedStringResource(R.string.maghrib)),
        PrayerInfo(PrayerType.ISHA, prayerTimes.ishaAzan, prayerTimes.ishaIqamah, localizedStringResource(R.string.isha)),
    )
}
