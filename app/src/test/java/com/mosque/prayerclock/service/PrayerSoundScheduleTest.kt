package com.mosque.prayerclock.service

import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.data.model.SoundType
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.*
import org.junit.Test

class PrayerSoundScheduleTest {
    private val settings = AppSettings(duaDisplayDurationMinutes = 5, fajrIqamahGap = 10)
    private val times = PrayerTimes(
        id = "test", date = "2026-10-19", fajrAzan = "05:00", sunrise = "06:00",
        dhuhrAzan = "12:00", asrAzan = "15:00", maghribAzan = "18:00", ishaAzan = "19:00",
    )

    private fun at(time: String) = LocalDateTime.parse("2026-10-19T$time")

    @Test fun `dua expiry and last five seconds stay silent for every prayer and audio type`() {
        for (sound in SoundType.values()) {
            val config = settings.copy(azanSoundType = sound, iqamahSoundType = sound)
            val events = prayerSoundEvents(times, config)
            for (hour in listOf(5, 12, 15, 18, 19)) {
                // Iqamah is ten minutes after azan; dua ends five minutes later.
                for (second in 0..5) {
                    val now = at("${hour.toString().padStart(2, '0')}:${if (second == 5) "15:00" else "14:${55 + second}"}")
                    assertTrue("Unexpected audio at $now ($sound)", events.none { it.shouldPlay(now, config) })
                }
            }
        }
    }

    @Test fun `azan and iqamah still trigger at configured audio boundary`() {
        for (sound in SoundType.values()) {
            val config = settings.copy(azanSoundType = sound, iqamahSoundType = sound)
            val events = prayerSoundEvents(times, config)
            val azan = at(if (sound == SoundType.COUNTDOWN_TICKING) "11:59:55" else "12:00:00")
            val iqamah = at(if (sound == SoundType.COUNTDOWN_TICKING) "12:09:55" else "12:10:00")
            assertEquals(PrayerSoundEventType.AZAN, events.single { it.shouldPlay(azan, config) }.type)
            assertEquals(PrayerSoundEventType.IQAMAH, events.single { it.shouldPlay(iqamah, config) }.type)
            assertTrue(events.none { it.shouldPlay(azan, config.copy(soundEnabled = false)) })
            assertTrue(events.none { it.shouldPlay(azan, config.copy(azanSoundEnabled = false)) })
            assertTrue(events.none { it.shouldPlay(iqamah, config.copy(iqamahSoundEnabled = false)) })
        }
    }

    @Test fun `Thursday Isha uses displayed bayan iqamah instead of standard gap`() {
        val config = settings.copy(jummaNightBayanEnabled = true, jummaNightBayanMinutes = 5)
        val thursday = times.copy(date = "2026-10-22")
        val events = prayerSoundEvents(thursday, config)
        assertEquals("19:05", events.single { it.prayer == PrayerType.ISHA && it.type == PrayerSoundEventType.IQAMAH }.time)
        // The old service sounded at 19-10, exactly when this five-minute dua ended.
        assertTrue(events.none { it.shouldPlay(at("19:09:55"), config) })
        assertTrue(events.none { it.shouldPlay(at("19:10:00"), config) })
        assertEquals("19:05", thursday.withIqamahTimes(config).ishaIqamah)
    }

    @Test fun `sunrise and Friday bayan have no iqamah audio event`() {
        val events = prayerSoundEvents(times.copy(date = "2026-10-23"), settings)
        assertTrue(events.none { it.prayer == PrayerType.SUNRISE })
        assertTrue(events.none { it.prayer == PrayerType.DHUHR && it.type == PrayerSoundEventType.IQAMAH })
    }

    @Test fun `Hanafi audio uses the same Asr time as display`() {
        val config = settings.copy(mazhab = com.mosque.prayerclock.data.model.Mazhab.HANAFI)
        val events = prayerSoundEvents(times.copy(asrShafiAzan = "15:00", asrHanafiAzan = "16:00"), config)
        assertEquals(listOf("16:00", "16:10"), events.filter { it.prayer == PrayerType.ASR }.map { it.time })
    }
}
