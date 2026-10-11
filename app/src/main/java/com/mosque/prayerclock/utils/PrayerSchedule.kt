package com.mosque.prayerclock.utils

import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.PrayerTimesWithIqamah
import com.mosque.prayerclock.data.model.PrayerType
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Select the active prayer (including dua or Friday bayan), then the next event. */
fun calculateNextPrayer(
    prayerTimes: PrayerTimesWithIqamah,
    currentSettings: AppSettings,
    currentTime: kotlinx.datetime.Instant = Clock.System.now(),
): PrayerType? {
    val now =
        currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val currentTimeString =
        "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
    val isFriday = now.dayOfWeek == DayOfWeek.FRIDAY

    // Timetable order also determines the next upcoming event.
    val prayers =
        listOf(
            PrayerType.FAJR to Pair(prayerTimes.fajrAzan, prayerTimes.fajrIqamah),
            PrayerType.SUNRISE to Pair(prayerTimes.sunrise, null), // Sunrise has no Iqamah
            PrayerType.DHUHR to
                Pair(
                    prayerTimes.dhuhrAzan,
                    if (isFriday) null else prayerTimes.dhuhrIqamah,
                ), // No Iqamah on Friday
            PrayerType.ASR to Pair(prayerTimes.asrAzan, prayerTimes.asrIqamah),
            PrayerType.MAGHRIB to
                Pair(prayerTimes.maghribAzan, prayerTimes.maghribIqamah),
            PrayerType.ISHA to Pair(prayerTimes.ishaAzan, prayerTimes.ishaIqamah),
        )

    // Active salah window (azan → iqamah + dua). Sunrise is informational only — never a salah period.
    for ((prayerType, times) in prayers) {
        if (prayerType == PrayerType.SUNRISE) continue

        val azanTime = times.first
        val iqamahTime = times.second?.takeIf { it.isNotBlank() }

        if (iqamahTime != null) {
            val bufferTime = TimeUtils.addMinutesToTime(iqamahTime, currentSettings.duaDisplayDurationMinutes)

            if (TimeUtils.compareTimeStrings(azanTime, currentTimeString) <= 0 &&
                TimeUtils.compareTimeStrings(currentTimeString, bufferTime) < 0
            ) {
                return prayerType
            }
        } else if (prayerType == PrayerType.DHUHR && isFriday) {
            val bayanEndTime = TimeUtils.addMinutesToTime(azanTime, currentSettings.jummahDurationMinutes)
            if (TimeUtils.compareTimeStrings(azanTime, currentTimeString) <= 0 &&
                TimeUtils.compareTimeStrings(currentTimeString, bayanEndTime) < 0
            ) {
                return prayerType
            }
        }
    }

    // Next upcoming event — skip sunrise once it has passed
    val nextTodayPrayer =
        prayers
            .firstOrNull { (type, times) ->
                if (type == PrayerType.SUNRISE &&
                    TimeUtils.compareTimeStrings(times.first, currentTimeString) <= 0
                ) {
                    false
                } else {
                    TimeUtils.compareTimeStrings(currentTimeString, times.first) < 0
                }
            }
            ?.first

    // If no more prayers today, next prayer is Fajr of tomorrow
    return nextTodayPrayer ?: PrayerType.FAJR
}
