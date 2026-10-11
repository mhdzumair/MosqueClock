package com.mosque.prayerclock.ui.components.screenfaces

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.data.model.PrayerTimesWithIqamah
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.data.model.WeatherInfo
import com.mosque.prayerclock.ui.components.clockfaces.ClockDisplayData
import com.mosque.prayerclock.ui.components.clockfaces.rememberClockDisplayData
import com.mosque.prayerclock.ui.components.getCountdownData
import com.mosque.prayerclock.ui.components.screenfaces.shared.isPrayerUpcomingOrActive
import com.mosque.prayerclock.ui.components.screenfaces.shared.minutesOfDay
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.utils.TimeUtils
import com.mosque.prayerclock.viewmodel.MainUiState
import com.mosque.prayerclock.viewmodel.MainViewModel
import com.mosque.prayerclock.viewmodel.WeatherUiState
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class FullScreenCountdownData(
    val prayerName: String,
    val prayerType: PrayerType,
    val isIqamah: Boolean,
    val minutes: Long,
    val seconds: Long,
    val azanTime: String,
    val targetTime: String = azanTime,
    val totalDurationSeconds: Long = 600,
    val kind: CountdownKind,
)

enum class CountdownKind { AZAN, IQAMAH, FRIDAY_NIGHT_BAYAN }

data class JummahData(
    val hoursRemaining: Long,
    val minutesRemaining: Long,
    val secondsRemaining: Long,
)

data class ScreenFaceCallbacks(
    val onOpenSettings: () -> Unit,
    val onPrayerTransition: () -> Unit,
)

data class ScreenDisplayData(
    val settings: AppSettings,
    val clock: ClockDisplayData,
    val mainState: MainUiState,
    val weatherState: WeatherUiState,
    val currentTime: Instant,
    val globalShowAzan: Boolean,
    val isCurrentPrayerGlobal: Boolean,
    val isCountdownVisibleForWeight: Boolean,
    val shouldShowFullScreenCountdown: Boolean,
    val fullScreenCountdownData: FullScreenCountdownData?,
    val shouldShowDua: Boolean,
    val jummahData: JummahData?,
    val errorMessage: String?,
) {
    companion object {
        val PREVIEW: ScreenDisplayData by lazy {
            val previewPrayer = PrayerTimes(
                id = "preview",
                date = "2026-01-01",
                fajrAzan = "05:30",
                dhuhrAzan = "12:15",
                asrAzan = "15:30",
                maghribAzan = "18:30",
                ishaAzan = "19:45",
                sunrise = "06:15",
            )
            val previewPrayerWithIqamah = PrayerTimesWithIqamah(
                prayerTimes = previewPrayer,
                fajrIqamah = "05:50",
                dhuhrIqamah = "12:25",
                asrIqamah = "15:40",
                maghribIqamah = "18:40",
                ishaIqamah = "19:55",
            )
            ScreenDisplayData(
                settings = AppSettings(mosqueName = "Dickwella Jummah Mosque"),
                clock = ClockDisplayData.PREVIEW,
                mainState = MainUiState.Success(
                    prayerTimes = previewPrayerWithIqamah,
                    nextPrayer = PrayerType.DHUHR,
                ),
                weatherState = WeatherUiState.Success(
                    weatherInfo = WeatherInfo(
                        temperature = 28.5,
                        description = "Partly Cloudy",
                        icon = "partly_cloudy",
                        humidity = 75,
                        feelsLike = 31.0,
                        windSpeed = 14.8,
                        uvIndex = 1.0,
                    )
                ),
                currentTime = Clock.System.now(),
                globalShowAzan = true,
                isCurrentPrayerGlobal = false,
                isCountdownVisibleForWeight = false,
                shouldShowFullScreenCountdown = false,
                fullScreenCountdownData = null,
                shouldShowDua = false,
                jummahData = null,
                errorMessage = null,
            )
        }
    }
}

private data class ComputedScreenValues(
    val isCountdownVisibleForWeight: Boolean,
    val currentTime: Instant,
    val isCurrentPrayer: Boolean,
    val shouldShowFullScreenCountdown: Boolean,
    val fullScreenCountdownData: FullScreenCountdownData?,
    val shouldShowDua: Boolean,
    val jummahData: JummahData?,
)

@Composable
fun rememberScreenDisplayData(viewModel: MainViewModel): ScreenDisplayData {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var globalCurrentTime by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            globalCurrentTime = Clock.System.now()
            delay(1000)
        }
    }

    var globalShowAzan by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(8000)
            globalShowAzan = !globalShowAzan
        }
    }

    val clockData = rememberClockDisplayData(
        show24Hour = settings.show24HourFormat,
        showSeconds = settings.showSeconds,
        currentTime = globalCurrentTime,
        hijriDateRepository = viewModel.hijriDateRepository,
    )

    val prayerNames = rememberLocalizedPrayerNames()

    val computed = when (val state = uiState) {
        is MainUiState.Success -> {
            val currentTime = globalCurrentTime
            val prayerInfoList = createPrayerInfoListSimple(state.prayerTimes, currentTime, prayerNames)
            val resolvedNext =
                resolveSimpleNextPrayer(prayerInfoList, state.nextPrayer, currentTime, state.nextDayFajr)
            val nextPrayerInfo = resolvedNext

            remember(nextPrayerInfo?.azanTime, nextPrayerInfo?.iqamahTime, currentTime) {
                if (nextPrayerInfo != null) {
                    computeScreenValues(nextPrayerInfo.azanTime, nextPrayerInfo.iqamahTime,
                        nextPrayerInfo.name, nextPrayerInfo.type, currentTime, settings)
                } else {
                    ComputedScreenValues(false, currentTime, false, false, null, false, null)
                }
            }
        }
        else -> ComputedScreenValues(false, globalCurrentTime, false, false, null, false, null)
    }

    val errorMessage = when (val state = uiState) {
        is MainUiState.Error -> mapErrorMessage(state.message)
        else -> null
    }

    return ScreenDisplayData(
        settings = settings,
        clock = clockData,
        mainState = uiState,
        weatherState = weatherState,
        currentTime = computed.currentTime,
        globalShowAzan = globalShowAzan,
        isCurrentPrayerGlobal = computed.isCurrentPrayer,
        isCountdownVisibleForWeight = computed.isCountdownVisibleForWeight,
        shouldShowFullScreenCountdown = computed.shouldShowFullScreenCountdown,
        fullScreenCountdownData = computed.fullScreenCountdownData,
        shouldShowDua = computed.shouldShowDua,
        jummahData = computed.jummahData,
        errorMessage = errorMessage,
    )
}

private data class SimplePrayerInfo(
    val type: PrayerType,
    val azanTime: String,
    val iqamahTime: String?,
    val name: String,
)

/** Localized prayer names, resolved once at the composable call site so [createPrayerInfoListSimple] —
 *  which runs inside a `remember` calculation block and so cannot call composables itself — can use them. */
private data class LocalizedPrayerNames(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val jummah: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
)

@Composable
private fun rememberLocalizedPrayerNames() = LocalizedPrayerNames(
    fajr = localizedStringResource(R.string.fajr),
    sunrise = localizedStringResource(R.string.sunrise),
    dhuhr = localizedStringResource(R.string.dhuhr),
    jummah = localizedStringResource(R.string.jummah),
    asr = localizedStringResource(R.string.asr),
    maghrib = localizedStringResource(R.string.maghrib),
    isha = localizedStringResource(R.string.isha),
)

private fun createPrayerInfoListSimple(
    prayerTimes: PrayerTimesWithIqamah,
    currentTime: Instant,
    names: LocalizedPrayerNames,
): List<SimplePrayerInfo> {
    val isFriday = currentTime.toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek == DayOfWeek.FRIDAY
    return listOf(
        SimplePrayerInfo(PrayerType.FAJR, prayerTimes.fajrAzan, prayerTimes.fajrIqamah, names.fajr),
        SimplePrayerInfo(PrayerType.SUNRISE, prayerTimes.sunrise, null, names.sunrise),
        SimplePrayerInfo(
            PrayerType.DHUHR,
            prayerTimes.dhuhrAzan,
            if (isFriday) null else prayerTimes.dhuhrIqamah,
            if (isFriday) names.jummah else names.dhuhr,
        ),
        SimplePrayerInfo(PrayerType.ASR, prayerTimes.asrAzan, prayerTimes.asrIqamah, names.asr),
        SimplePrayerInfo(PrayerType.MAGHRIB, prayerTimes.maghribAzan, prayerTimes.maghribIqamah, names.maghrib),
        SimplePrayerInfo(PrayerType.ISHA, prayerTimes.ishaAzan, prayerTimes.ishaIqamah, names.isha),
    )
}

/** Skips events whose time has already passed — mirrors [resolveDisplayNextPrayer] for non-composable data. */
private fun resolveSimpleNextPrayer(
    rows: List<SimplePrayerInfo>,
    nextPrayer: PrayerType?,
    currentTime: Instant,
    nextDayFajr: PrayerTimesWithIqamah? = null,
): SimplePrayerInfo? {
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
    val fajr = rows.firstOrNull { it.type == PrayerType.FAJR } ?: return null
    return if (nextDayFajr != null) {
        fajr.copy(azanTime = nextDayFajr.fajrAzan, iqamahTime = nextDayFajr.fajrIqamah)
    } else {
        fajr
    }
}

private fun computeScreenValues(
    azanTime: String,
    iqamahTime: String?,
    prayerName: String,
    prayerType: PrayerType,
    currentTime: Instant,
    settings: AppSettings,
): ComputedScreenValues {
    val now = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val currentTimeString = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"

    val isAfterAzan = TimeUtils.compareTimeStrings(azanTime, currentTimeString) <= 0
    val isBeforeIqamah = iqamahTime?.let {
        TimeUtils.compareTimeStrings(currentTimeString, it) < 0
    } ?: false
    val shouldShowIqamah = isAfterAzan && isBeforeIqamah && iqamahTime != null

    val targetTime = if (shouldShowIqamah) iqamahTime ?: azanTime else azanTime
    val countdownData = getCountdownData(targetTime, currentTime)

    val isCurrentPrayer = if (iqamahTime != null) {
        val bufferTime = TimeUtils.addMinutesToTime(iqamahTime, settings.duaDisplayDurationMinutes)
        isAfterAzan && TimeUtils.compareTimeStrings(currentTimeString, bufferTime) <= 0
    } else {
        val isFriday = now.dayOfWeek == DayOfWeek.FRIDAY
        val bufferMinutes = if (prayerType == PrayerType.DHUHR && isFriday) settings.jummahDurationMinutes else 60
        val extendedTime = TimeUtils.addMinutesToTime(azanTime, bufferMinutes)
        isAfterAzan && TimeUtils.compareTimeStrings(currentTimeString, extendedTime) <= 0
    }

    val isPastFinalTime = if (iqamahTime != null) {
        TimeUtils.compareTimeStrings(currentTimeString, iqamahTime) > 0
    } else {
        val isFriday = now.dayOfWeek == DayOfWeek.FRIDAY
        val bufferMinutes = if (prayerType == PrayerType.DHUHR && isFriday) settings.jummahDurationMinutes else 60
        val extendedTime = TimeUtils.addMinutesToTime(azanTime, bufferMinutes)
        TimeUtils.compareTimeStrings(currentTimeString, extendedTime) > 0
    }

    val isCountdownVisible = countdownData.totalSeconds >= 0 && countdownData.totalMinutes < 60 && !isPastFinalTime

    val shouldShowDua = if (iqamahTime != null && prayerType != PrayerType.SUNRISE) {
        val duaEndTime = TimeUtils.addMinutesToTime(iqamahTime, settings.duaDisplayDurationMinutes)
        val isAfterIqamah = TimeUtils.compareTimeStrings(currentTimeString, iqamahTime) >= 0
        val isBeforeDuaEnd = TimeUtils.compareTimeStrings(currentTimeString, duaEndTime) < 0
        isAfterIqamah && isBeforeDuaEnd
    } else false

    val maxCountdownMinutes = if (shouldShowIqamah && iqamahTime != null) {
        val azanParts = azanTime.split(":")
        val iqamahParts = iqamahTime.split(":")
        val azanMinutes = azanParts[0].toInt() * 60 + azanParts[1].toInt()
        val iqamahMinutes = iqamahParts[0].toInt() * 60 + iqamahParts[1].toInt()
        (iqamahMinutes - azanMinutes).toLong()
    } else 10L

    val shouldShowFullScreenCountdown = settings.fullScreenCountdownEnabled &&
        countdownData.totalMinutes in 0..maxCountdownMinutes &&
        countdownData.totalSeconds >= 0 && !isPastFinalTime

    val fullScreenCountdownData = if (shouldShowFullScreenCountdown) {
        val isFridayNightBayan = settings.jummaNightBayanEnabled &&
            now.dayOfWeek == DayOfWeek.THURSDAY &&
            prayerType == PrayerType.ISHA &&
            shouldShowIqamah
        FullScreenCountdownData(
            prayerName = prayerName,
            prayerType = prayerType,
            isIqamah = shouldShowIqamah,
            minutes = countdownData.minutes,
            seconds = countdownData.seconds,
            azanTime = azanTime,
            targetTime = targetTime,
            totalDurationSeconds = (maxCountdownMinutes * 60).coerceAtLeast(1),
            kind = when {
                isFridayNightBayan -> CountdownKind.FRIDAY_NIGHT_BAYAN
                shouldShowIqamah -> CountdownKind.IQAMAH
                else -> CountdownKind.AZAN
            },
        )
    } else null

    val jummahData = if (settings.showJummahScreen &&
        prayerType == PrayerType.DHUHR &&
        now.dayOfWeek == DayOfWeek.FRIDAY &&
        iqamahTime == null
    ) {
        val jummahEndTime = TimeUtils.addMinutesToTime(azanTime, settings.jummahDurationMinutes)
        val isAfterJummahAzan = TimeUtils.compareTimeStrings(azanTime, currentTimeString) <= 0
        val isBeforeJummahEnd = TimeUtils.compareTimeStrings(currentTimeString, jummahEndTime) < 0
        if (isAfterJummahAzan && isBeforeJummahEnd) {
            val jummahCountdown = getCountdownData(jummahEndTime, currentTime)
            JummahData(
                hoursRemaining = jummahCountdown.hours,
                minutesRemaining = jummahCountdown.minutes,
                secondsRemaining = jummahCountdown.seconds,
            )
        } else null
    } else null

    return ComputedScreenValues(
        isCountdownVisibleForWeight = isCountdownVisible,
        currentTime = currentTime,
        isCurrentPrayer = isCurrentPrayer,
        shouldShowFullScreenCountdown = shouldShowFullScreenCountdown,
        fullScreenCountdownData = fullScreenCountdownData,
        shouldShowDua = shouldShowDua,
        jummahData = jummahData,
    )
}

private fun mapErrorMessage(technicalMessage: String): String =
    when {
        technicalMessage.contains("failed to connect", ignoreCase = true) ||
            technicalMessage.contains("connection refused", ignoreCase = true) ||
            technicalMessage.contains("timeout", ignoreCase = true) ||
            technicalMessage.contains("unable to resolve host", ignoreCase = true) ->
            "Unable to connect to prayer time service.\nPlease check your internet connection."

        technicalMessage.contains("500", ignoreCase = true) ||
            technicalMessage.contains("502", ignoreCase = true) ||
            technicalMessage.contains("503", ignoreCase = true) ||
            technicalMessage.contains("server error", ignoreCase = true) ->
            "Prayer time service is temporarily unavailable.\nPlease try again later."

        technicalMessage.contains("404", ignoreCase = true) ||
            technicalMessage.contains("not found", ignoreCase = true) ||
            technicalMessage.contains("no data", ignoreCase = true) ->
            "Prayer times not available for your location.\nPlease check your settings."

        technicalMessage.contains("network", ignoreCase = true) ->
            "Network connection problem.\nPlease check your internet and try again."

        else -> "Unable to load prayer times.\nPlease check your internet connection and try again."
    }
