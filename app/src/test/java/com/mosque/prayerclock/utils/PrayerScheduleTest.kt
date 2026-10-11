package com.mosque.prayerclock.utils

import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.ui.components.getCountdownData
import com.mosque.prayerclock.ui.components.screenfaces.shared.resolveTimetableNextPrayer
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerScheduleTest {
    private val settings = AppSettings(duaDisplayDurationMinutes = 5)
    private val times = PrayerTimes(
        id = "test", date = "2026-10-11", fajrAzan = "05:30", sunrise = "06:15",
        dhuhrAzan = "12:15", asrAzan = "15:30", maghribAzan = "17:58", ishaAzan = "19:15",
    ).withIqamahTimes(settings)

    private fun at(time: String, date: String = "2026-10-11") =
        LocalDateTime.parse("${date}T${time}:00").toInstant(TimeZone.currentSystemDefault())

    @Test
    fun `1825 selects Isha after Maghrib has completed`() {
        assertEquals(PrayerType.ISHA, calculateNextPrayer(times, settings, at("18:25")))
        // Also handle the reported iqamah earlier than azan without retaining Maghrib.
        assertEquals(
            PrayerType.ISHA,
            calculateNextPrayer(times.copy(maghribIqamah = "17:08"), settings, at("18:25")),
        )
    }

    @Test
    fun `Maghrib stays active through iqamah and dua then advances at the boundary`() {
        assertEquals(PrayerType.MAGHRIB, calculateNextPrayer(times, settings, at("17:58")))
        assertEquals(PrayerType.MAGHRIB, calculateNextPrayer(times, settings, at("18:07")))
        assertEquals(PrayerType.MAGHRIB, calculateNextPrayer(times, settings, at("18:08")))
        assertEquals(PrayerType.MAGHRIB, calculateNextPrayer(times, settings, at("18:12")))
        assertEquals(PrayerType.ISHA, calculateNextPrayer(times, settings, at("18:13")))
    }

    @Test
    fun `expired next-prayer hint cannot retain Maghrib on any face`() {
        val rows = listOf(
            PrayerInfo(PrayerType.FAJR, "05:30", "05:50", "Fajr"),
            PrayerInfo(PrayerType.MAGHRIB, "17:58", "18:08", "Maghrib"),
            PrayerInfo(PrayerType.ISHA, "19:15", "19:25", "Isha"),
        )
        val next = resolveTimetableNextPrayer(rows, PrayerType.MAGHRIB, at("18:25"))
        assertEquals(PrayerType.ISHA, next.type)
        assertEquals(50L, getCountdownData(next.azanTime, at("18:25")).totalMinutes)
    }

    @Test
    fun `sunrise advances immediately after its time`() {
        assertEquals(PrayerType.SUNRISE, calculateNextPrayer(times, settings, at("06:14")))
        assertEquals(PrayerType.DHUHR, calculateNextPrayer(times, settings, at("06:15")))
    }

    @Test
    fun `Friday bayan remains active until the configured end`() {
        assertEquals(PrayerType.DHUHR, calculateNextPrayer(times, settings, at("13:14", "2026-10-09")))
        assertEquals(PrayerType.ASR, calculateNextPrayer(times, settings, at("13:15", "2026-10-09")))
    }

    @Test
    fun `after Isha the Fajr countdown targets tomorrow instead of zero`() {
        assertEquals(PrayerType.FAJR, calculateNextPrayer(times, settings, at("20:00")))
        assertEquals(570L, getCountdownData("05:30", at("20:00"), rollToNextDay = true).totalMinutes)
        assertEquals(-1L, getCountdownData("17:58", at("18:25")).totalSeconds)
    }
}
