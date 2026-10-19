package com.mosque.prayerclock.service

import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.data.model.SoundType
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

internal enum class PrayerSoundEventType(val label: String) {
    AZAN("Azan"), IQAMAH("Iqamah"),
}

internal data class PrayerSoundEvent(
    val prayer: PrayerType,
    val time: String,
    val type: PrayerSoundEventType,
) {
    fun secondsUntil(now: LocalDateTime): Int? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val minute = parts[1].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        return (hour * 3600 + minute * 60 - now.hour * 3600 - now.minute * 60 - now.second + 86400) % 86400
    }

    fun shouldPlay(now: LocalDateTime, settings: AppSettings): Boolean {
        if (!settings.soundEnabled) return false
        val enabled = if (type == PrayerSoundEventType.AZAN) settings.azanSoundEnabled else settings.iqamahSoundEnabled
        if (!enabled) return false
        val sound = if (type == PrayerSoundEventType.AZAN) settings.azanSoundType else settings.iqamahSoundType
        return secondsUntil(now) == if (sound == SoundType.COUNTDOWN_TICKING) 5 else 0
    }
}

/** Audio follows the same iqamah schedule as the screen. Dua expiry is a silent UI transition. */
internal fun prayerSoundEvents(times: PrayerTimes, settings: AppSettings): List<PrayerSoundEvent> {
    val displayed = times.withIqamahTimes(settings)
    val friday = runCatching { LocalDate.parse(times.date).dayOfWeek == DayOfWeek.FRIDAY }.getOrDefault(false)
    return buildList {
        fun prayer(type: PrayerType, azan: String, iqamah: String?) {
            add(PrayerSoundEvent(type, azan, PrayerSoundEventType.AZAN))
            if (iqamah != null) add(PrayerSoundEvent(type, iqamah, PrayerSoundEventType.IQAMAH))
        }
        prayer(PrayerType.FAJR, displayed.fajrAzan, displayed.fajrIqamah)
        prayer(PrayerType.DHUHR, displayed.dhuhrAzan, displayed.dhuhrIqamah.takeUnless { friday })
        prayer(PrayerType.ASR, displayed.asrAzan, displayed.asrIqamah)
        prayer(PrayerType.MAGHRIB, displayed.maghribAzan, displayed.maghribIqamah)
        prayer(PrayerType.ISHA, displayed.ishaAzan, displayed.ishaIqamah)
    }
}
