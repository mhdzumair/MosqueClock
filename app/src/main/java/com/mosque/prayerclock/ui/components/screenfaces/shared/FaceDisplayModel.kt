package com.mosque.prayerclock.ui.components.screenfaces.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.data.model.PrayerTimesWithIqamah
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.ui.components.TvAutoSizeText
import com.mosque.prayerclock.ui.components.getCountdownData
import com.mosque.prayerclock.ui.components.screenfaces.FullScreenCountdownData
import com.mosque.prayerclock.ui.components.screenfaces.CountdownKind
import com.mosque.prayerclock.ui.components.screenfaces.JummahData
import com.mosque.prayerclock.ui.components.screenfaces.ScreenDisplayData
import com.mosque.prayerclock.ui.localizedStringArrayResource
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.utils.TimeUtils
import com.mosque.prayerclock.viewmodel.WeatherUiState
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class FaceHeaderData(
    val mosque: String,
    val hijri: String,
    val gregorian: String,
    val weekday: String,
    val weather: String,
    val weatherAvailable: Boolean,
    val temperature: String,
    val condition: String,
    val feelsLike: String,
    val humidity: String,
    val wind: String,
    val uv: String,
    val weatherIcon: String?,
)

@Composable
fun headerData(data: ScreenDisplayData): FaceHeaderData {
    val local = data.currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val weekday = localizedStringArrayResource(R.array.weekdays)[local.dayOfWeek.ordinal]
    val weather =
        (data.weatherState as? WeatherUiState.Success)
            ?.weatherInfo
            ?.takeIf { data.settings.showWeather }
    return FaceHeaderData(
        mosque = data.settings.mosqueName,
        hijri = "${data.clock.hijriDay} ${data.clock.hijriMonthName} ${data.clock.hijriYear}",
        gregorian = "${data.clock.gregorianDay} ${data.clock.gregorianMonthName} ${data.clock.gregorianYear}",
        weekday = weekday,
        weather = weather?.let { "${it.temperature}°C · ${it.humidity}%" }.orEmpty(),
        weatherAvailable = weather != null,
        temperature = weather?.let { "%.1f°C".format(it.temperature) }.orEmpty(),
        condition = weather?.description.orEmpty(),
        feelsLike = weather?.let { "%.1f°".format(it.feelsLike) }.orEmpty(),
        humidity = weather?.let { "${it.humidity}%" }.orEmpty(),
        wind = weather?.windSpeed?.let { "%.1f km/h".format(it) }.orEmpty(),
        uv = weather?.uvIndex?.let { "%.1f".format(it) }.orEmpty(),
        weatherIcon = weather?.icon,
    )
}

/** Neutral display data used by faces. It deliberately contains no layout or palette decisions. */
@Composable
fun prayerRows(
    prayerTimes: PrayerTimesWithIqamah,
    currentTime: Instant,
): List<PrayerInfo> {
    val friday = currentTime.toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek == DayOfWeek.FRIDAY
    return listOf(
        PrayerInfo(
            PrayerType.FAJR,
            prayerTimes.fajrAzan,
            prayerTimes.fajrIqamah,
            localizedStringResource(R.string.fajr),
        ),
        PrayerInfo(PrayerType.SUNRISE, prayerTimes.sunrise, null, localizedStringResource(R.string.sunrise)),
        PrayerInfo(
            PrayerType.DHUHR,
            prayerTimes.dhuhrAzan,
            if (friday) null else prayerTimes.dhuhrIqamah,
            localizedStringResource(if (friday) R.string.jummah else R.string.dhuhr),
        ),
        PrayerInfo(PrayerType.ASR, prayerTimes.asrAzan, prayerTimes.asrIqamah, localizedStringResource(R.string.asr)),
        PrayerInfo(
            PrayerType.MAGHRIB,
            prayerTimes.maghribAzan,
            prayerTimes.maghribIqamah,
            localizedStringResource(R.string.maghrib),
        ),
        PrayerInfo(
            PrayerType.ISHA,
            prayerTimes.ishaAzan,
            prayerTimes.ishaIqamah,
            localizedStringResource(R.string.isha),
        ),
    )
}

fun minutesOfDay(time: String): Int {
    val normalized = TimeUtils.normalizeClockTime(time)
    val parts = normalized.split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return 0
    val minute = parts.getOrNull(1)?.take(2)?.toIntOrNull() ?: return 0
    return hour * 60 + minute
}

/** True while azan is still ahead, or we're between azan and iqamah. Sunrise never stays active after it passes. */
fun isPrayerUpcomingOrActive(
    prayerType: PrayerType,
    azanTime: String,
    iqamahTime: String?,
    nowMinutes: Int,
): Boolean {
    if (minutesOfDay(azanTime) > nowMinutes) return true
    if (prayerType == PrayerType.SUNRISE) return false
    iqamahTime?.takeIf { it.isNotBlank() }?.let { iqamah ->
        if (nowMinutes < minutesOfDay(iqamah)) return true
    }
    return false
}

/** Shared countdown event supplied by ScreenFace.Render, without adding timetable rows. */
internal val LocalInlineCountdownPrayer = staticCompositionLocalOf<PrayerInfo?> { null }

/** Prayer to display as "next" — skips events whose time has already passed. */
@Composable
fun resolveDisplayNextPrayer(
    rows: List<PrayerInfo>,
    nextPrayer: PrayerType?,
    currentTime: Instant,
    nextDayFajr: PrayerTimesWithIqamah? = null,
): PrayerInfo {
    LocalInlineCountdownPrayer.current?.let { return it }
    val now = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val nowMinutes = now.hour * 60 + now.minute
    rows.firstOrNull { it.type == nextPrayer }?.let { matched ->
        if (isPrayerUpcomingOrActive(matched.type, matched.azanTime, matched.iqamahTime, nowMinutes)) {
            return matched
        }
    }
    rows.firstOrNull {
        isPrayerUpcomingOrActive(it.type, it.azanTime, it.iqamahTime, nowMinutes)
    }?.let { return it }
    // All of today's events have ended. The next prayer is tomorrow's Fajr.
    val fajr = rows.first { it.type == PrayerType.FAJR }
    return if (nextDayFajr != null) {
        fajr.copy(azanTime = nextDayFajr.fajrAzan, iqamahTime = nextDayFajr.fajrIqamah)
    } else {
        fajr
    }
}

@Composable
fun prayerWithEventLabel(
    prayer: PrayerInfo,
    isIqamah: Boolean,
): String =
    when {
        prayer.type == PrayerType.SUNRISE -> prayer.name
        isIqamah -> "${prayer.name} ${localizedStringResource(R.string.iqamah)}"
        else -> "${prayer.name} ${localizedStringResource(R.string.azan)}"
    }

@Composable
fun prayerCountdownHeading(
    prayer: PrayerInfo,
    isIqamah: Boolean,
): String =
    when {
        prayer.type == PrayerType.SUNRISE -> prayer.name
        else ->
            localizedStringResource(
                R.string.prayer_countdown_heading,
                prayer.name,
                localizedStringResource(if (isIqamah) R.string.iqamah else R.string.azan),
            )
    }

@Composable
fun fullScreenCountdownTitle(
    prayerName: String,
    prayerType: PrayerType,
    isIqamah: Boolean,
    kind: CountdownKind = CountdownKind.AZAN,
): String =
    when {
        kind == CountdownKind.FRIDAY_NIGHT_BAYAN -> localizedStringResource(R.string.friday_night_bayan)
        prayerType == PrayerType.SUNRISE -> prayerName
        else ->
            "$prayerName ${localizedStringResource(if (isIqamah) R.string.iqamah else R.string.azan)}"
    }

@Composable
fun prayerCountdownInHeading(
    prayer: PrayerInfo,
    isIqamah: Boolean,
): String =
    when {
        prayer.type == PrayerType.SUNRISE -> prayer.name
        else ->
            localizedStringResource(
                R.string.prayer_countdown_in,
                prayer.name,
                localizedStringResource(if (isIqamah) R.string.iqamah else R.string.azan),
            )
    }

private fun statusLineNowText(data: ScreenDisplayData): String {
    val clock = data.clock
    return if (clock.show24Hour) {
        buildString {
            append("%02d:%02d".format(clock.hour24, clock.minute))
            if (clock.showSeconds) append(":%02d".format(clock.second))
        }
    } else {
        buildString {
            append("%d:%02d".format(if (clock.hour12 == 0) 12 else clock.hour12, clock.minute))
            if (clock.showSeconds) {
                append(":%02d".format(clock.second))
            }
            append(' ')
            append(if (clock.isAm) "AM" else "PM")
        }
    }
}

@Composable
fun buildPrayerStatusLine(
    data: ScreenDisplayData,
    prayer: PrayerInfo,
): String {
    val nowText = statusLineNowText(data)
    return buildString {
        append(localizedStringResource(R.string.now_label))
        append(' ')
        append(nowText)
        if (prayer.type != PrayerType.SUNRISE) {
            append(" · ")
            append(localizedStringResource(R.string.azan))
            append(' ')
            append(TimeUtils.formatTimeBasedOnPreference(prayer.azanTime, data.settings.show24HourFormat))
            prayer.iqamahTime?.let { iqamah ->
                append(" · ")
                append(localizedStringResource(R.string.iqamah))
                append(' ')
                append(TimeUtils.formatTimeBasedOnPreference(iqamah, data.settings.show24HourFormat))
            }
        }
    }
}

@Composable
fun PrayerStatusLine(
    data: ScreenDisplayData,
    prayer: PrayerInfo,
    color: Color,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
) {
    // Fit the complete line together; independent fill-width children consumed the whole row.
    TvAutoSizeText(
        buildPrayerStatusLine(data, prayer),
        color,
        maxFontSize,
        minFontSize,
        modifier,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

fun weatherGlyph(icon: String?): String =
    when (icon?.lowercase()) {
        "clear", "sunny" -> "☀"
        "partly_cloudy", "partly-cloudy" -> "☁"
        "cloudy", "overcast" -> "☁"
        "rain", "rainy", "showers" -> "☂"
        "thunderstorm", "storm" -> "ϟ"
        "snow" -> "❄"
        "fog", "mist" -> "≋"
        else -> ""
    }

data class InlineCountdown(
    val isIqamah: Boolean,
    val targetTime: String,
    val minutes: Long,
    val seconds: Long,
)

/** Selects azan or iqamah from time state; faces remain responsible for presentation. */
fun inlineCountdown(
    data: ScreenDisplayData,
    prayer: PrayerInfo,
): InlineCountdown {
    val local = data.currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val now = "%02d:%02d".format(local.hour, local.minute)
    val isIqamah =
        prayer.iqamahTime?.let {
            TimeUtils.compareTimeStrings(prayer.azanTime, now) <= 0 && TimeUtils.compareTimeStrings(now, it) < 0
        } == true
    val target = if (isIqamah) prayer.iqamahTime ?: prayer.azanTime else prayer.azanTime
    val countdown = getCountdownData(target, data.currentTime)
    return InlineCountdown(isIqamah, target, countdown.totalMinutes, countdown.seconds)
}

fun minutesBetween(
    start: String,
    end: String,
): Long {
    var difference = minutesOfDay(end) - minutesOfDay(start)
    if (difference < 0) difference += 24 * 60
    return difference.toLong()
}

fun FullScreenCountdownData.elapsedFraction(): Float {
    val remaining = minutes * 60L + seconds
    return (1f - remaining.toFloat() / totalDurationSeconds.coerceAtLeast(1L)).coerceIn(0f, 1f)
}

fun JummahData.formattedRemaining(): String =
    if (hoursRemaining > 0) {
        "%02d:%02d:%02d".format(hoursRemaining, minutesRemaining, secondsRemaining)
    } else {
        "%02d:%02d".format(minutesRemaining, secondsRemaining)
    }

/** Feels-like / humidity / wind / UV as a 2x2 label-value grid — the header's third weather tier. */
@Composable
fun MinorWeatherGrid(
    h: FaceHeaderData,
    labelColor: Color,
    valueColor: Color,
    labelSize: TextUnit,
    valueSize: TextUnit,
    modifier: Modifier = Modifier,
    horizontalGap: Dp = 22.dp,
    verticalGap: Dp = 6.dp,
) {
    val feel = localizedStringResource(R.string.weather_feels_like)
    val hum = localizedStringResource(R.string.weather_humidity)
    val wind = localizedStringResource(R.string.weather_wind)
    val uv = localizedStringResource(R.string.weather_uv)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(verticalGap)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(horizontalGap)) {
            MinorMetric(feel, h.feelsLike, labelColor, valueColor, labelSize, valueSize, Modifier.weight(1f))
            MinorMetric(hum, h.humidity, labelColor, valueColor, labelSize, valueSize, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(horizontalGap)) {
            MinorMetric(wind, h.wind, labelColor, valueColor, labelSize, valueSize, Modifier.weight(1f))
            MinorMetric(uv, h.uv, labelColor, valueColor, labelSize, valueSize, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MinorMetric(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
    labelSize: TextUnit,
    valueSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        TvAutoSizeText(label, labelColor, labelSize, maxLines = 1)
        TvAutoSizeText(value, valueColor, valueSize, maxLines = 1)
    }
}
