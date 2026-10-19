package com.mosque.prayerclock.data.model

import com.mosque.prayerclock.utils.calculateNextPrayer
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Test

class MazhabTest {
    private val times = PrayerTimes(
        id = "10-19_ACJU_DIRECT:1", date = "2026-10-19", providerKey = "ACJU_DIRECT:1",
        fajrAzan = "04:40", sunrise = "05:57", dhuhrAzan = "11:57", asrAzan = "15:17",
        maghribAzan = "17:56", ishaAzan = "19:05", asrShafiAzan = "15:17", asrHanafiAzan = "16:15",
    )

    @Test
    fun `Shafi is default and mazhab changes Asr azan and iqamah together`() {
        val defaults = AppSettings()
        assertEquals(Mazhab.SHAFI, defaults.mazhab)
        assertEquals("15:17", times.withIqamahTimes(defaults).asrAzan)
        val hanafi = times.withIqamahTimes(defaults.copy(mazhab = Mazhab.HANAFI, asrIqamahGap = 15))
        assertEquals("16:15", hanafi.asrAzan)
        assertEquals("16:30", hanafi.asrIqamah)
        assertEquals("17:56", hanafi.maghribAzan)
        assertEquals("19:05", hanafi.ishaAzan)
        assertEquals("15:17", hanafi.prayerTimes.withIqamahTimes(defaults).asrAzan)
        assertEquals("15:17", times.asrAzan) // Cached source remains unchanged.
    }

    @Test
    fun `next prayer follows selected Asr after Shafi prayer has finished`() {
        val now = LocalDateTime.parse("2026-10-19T15:40:00").toInstant(TimeZone.currentSystemDefault())
        val shafi = AppSettings(mazhab = Mazhab.SHAFI)
        val hanafi = shafi.copy(mazhab = Mazhab.HANAFI)
        assertEquals(PrayerType.MAGHRIB, calculateNextPrayer(times.withIqamahTimes(shafi), shafi, now))
        assertEquals(PrayerType.ASR, calculateNextPrayer(times.withIqamahTimes(hanafi), hanafi, now))
    }

    @Test
    fun `manual time remains unchanged when no alternative Asr is supplied`() {
        val manual = times.copy(providerKey = null, asrAzan = "15:45", asrShafiAzan = null, asrHanafiAzan = null)
        assertEquals("15:45", manual.withIqamahTimes(AppSettings(mazhab = Mazhab.HANAFI)).asrAzan)
    }
}
