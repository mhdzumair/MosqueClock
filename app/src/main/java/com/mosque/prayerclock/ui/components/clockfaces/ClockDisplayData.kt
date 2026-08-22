package com.mosque.prayerclock.ui.components.clockfaces

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.repository.HijriDateRepository
import com.mosque.prayerclock.ui.LocalLocalizedContext
import com.mosque.prayerclock.ui.localizedStringArrayResource
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.utils.LocaleManager
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Date

/**
 * All display-ready values needed to render any clock face.
 * Produced by [rememberClockDisplayData]; consumed by clock face [Render] implementations.
 * Clock face composables must not perform time computation, locale formatting, or repository
 * calls themselves — they receive this object and only draw.
 */
data class ClockDisplayData(
    // Pre-formatted time string (respects show24Hour and showSeconds user settings)
    val formattedTime: String,
    // Raw components — for faces that need custom layout (e.g. separate seconds ring)
    val hour12: Int,      // 0–11
    val hour24: Int,      // 0–23
    val minute: Int,
    val second: Int,
    val isAm: Boolean,
    // User settings forwarded so faces can make layout decisions
    val show24Hour: Boolean,
    val showSeconds: Boolean,
    // Hijri date
    val hijriDay: Int,
    val hijriMonthName: String,
    val hijriYear: Int,
    // Gregorian date
    val gregorianDay: Int,
    val gregorianMonthName: String,
    val gregorianYear: Int,
) {
    companion object {
        // Static snapshot at 10:10:30 used by face Preview composables.
        val PREVIEW = ClockDisplayData(
            formattedTime = "10:10",
            hour12 = 10,
            hour24 = 10,
            minute = 10,
            second = 30,
            isAm = true,
            show24Hour = false,
            showSeconds = false,
            hijriDay = 7,
            hijriMonthName = "Rajab",
            hijriYear = 1446,
            gregorianDay = 1,
            gregorianMonthName = "January",
            gregorianYear = 2025,
        )
    }
}

/**
 * Composable that owns all data-processing concerns for the clock:
 * - Self-ticking (1 s interval) when no external [currentTime] is provided
 * - Timezone / LocalDateTime decomposition
 * - Hijri date fetching from [hijriDateRepository]
 * - Time string formatting (12/24 h, seconds, AM/PM localisation)
 * - Gregorian month name localisation
 *
 * The returned [ClockDisplayData] is recomputed each second.
 * Clock face [Render] implementations receive this value and must perform no further
 * data processing.
 */
@Composable
fun rememberClockDisplayData(
    show24Hour: Boolean,
    showSeconds: Boolean,
    currentTime: Instant?,
    hijriDateRepository: HijriDateRepository?,
): ClockDisplayData {
    // Self-tick state — only read when currentTime is null (short-circuit ?: evaluation
    // means Compose does not track the read when currentTime is non-null, so no extra
    // recompositions happen while an external time source is active).
    var selfTickTime by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            selfTickTime = Clock.System.now()
        }
    }
    val timeToUse = currentTime ?: selfTickTime

    val localDateTime = timeToUse.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour24 = localDateTime.hour
    val hour12 = hour24 % 12
    val minute = localDateTime.minute
    val second = localDateTime.second
    val isAm = hour24 < 12

    // Hijri date — start from a sensible default so the UI never shows blank
    var hijriDate by remember {
        mutableStateOf(HijriDateRepository.HijriDate(7, 2, 1447))
    }
    LaunchedEffect(localDateTime.date) {
        hijriDate = hijriDateRepository?.getCurrentHijriDate()
            ?: HijriDateRepository.HijriDate(7, 2, 1447)
    }

    val hijriMonthNames = localizedStringArrayResource(R.array.hijri_months)
    val hijriMonthName = hijriMonthNames.getOrNull(hijriDate.month - 1) ?: hijriMonthNames[0]

    val currentLocale = LocaleManager.getCurrentLocale(LocalLocalizedContext.current)
    val date = Date(timeToUse.toEpochMilliseconds())
    val gregorianMonthName = SimpleDateFormat("MMMM", currentLocale).format(date)

    val ampm = if (isAm) localizedStringResource(R.string.am) else localizedStringResource(R.string.pm)
    val formattedTime = buildString {
        if (show24Hour) {
            append(String.format("%02d:%02d", hour24, minute))
            if (showSeconds) append(":${String.format("%02d", second)}")
        } else {
            val displayHour = if (hour12 == 0) 12 else hour12
            append(String.format("%d:%02d", displayHour, minute))
            if (showSeconds) append(":${String.format("%02d", second)}")
            append(" $ampm")
        }
    }

    return ClockDisplayData(
        formattedTime = formattedTime,
        hour12 = hour12,
        hour24 = hour24,
        minute = minute,
        second = second,
        isAm = isAm,
        show24Hour = show24Hour,
        showSeconds = showSeconds,
        hijriDay = hijriDate.day,
        hijriMonthName = hijriMonthName,
        hijriYear = hijriDate.year,
        gregorianDay = localDateTime.dayOfMonth,
        gregorianMonthName = gregorianMonthName,
        gregorianYear = localDateTime.year,
    )
}
