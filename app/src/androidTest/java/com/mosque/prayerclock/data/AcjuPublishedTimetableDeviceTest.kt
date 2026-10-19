package com.mosque.prayerclock.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mosque.prayerclock.data.scraping.ACJUScraper
import com.mosque.prayerclock.data.scraping.PdfParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** Opt-in live-source diagnostic: am instrument -e acjuLive true -e class ...AcjuPublishedTimetableDeviceTest */
@RunWith(AndroidJUnit4::class)
class AcjuPublishedTimetableDeviceTest {
    @Test
    fun zone12OctoberLinkLoadsOctoberTimetableOnDevice() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("acjuLive") == "true")
        val client = OkHttpClient.Builder().callTimeout(60, TimeUnit.SECONDS).build()
        val scraper = ACJUScraper(client)
        val url = scraper.getPrayerTimesPdfUrl(zone = 12, year = 2026, month = 10)
        assertNotNull("ACJU Zone 12 October PDF URL", url)
        val pdf = scraper.downloadPdf(url!!)
        assertNotNull("October PDF must download using this device's connection", pdf)
        val parser = PdfParser()
        val timetable = parser.parsePrayerTimes(parser.extractTextFromPdf(pdf!!), expectedMonth = 10)
        assertEquals((1..31).toList(), timetable.prayerTimes.map { it.day })
        val today = timetable.prayerTimes.single { it.day == 19 }
        assertEquals("04:38", today.fajr)
        assertEquals("05:55", today.sunrise)
    }
}
