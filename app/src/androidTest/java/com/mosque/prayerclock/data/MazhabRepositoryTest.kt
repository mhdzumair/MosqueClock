package com.mosque.prayerclock.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.mosque.prayerclock.data.cache.PrayerTimesCacheInvalidator
import com.mosque.prayerclock.data.database.AppDatabase
import com.mosque.prayerclock.data.model.Mazhab
import com.mosque.prayerclock.data.model.PrayerServiceType
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.data.network.AlAdhanResponse
import com.mosque.prayerclock.data.network.NetworkResult
import com.mosque.prayerclock.data.network.PrayerTimesApi
import com.mosque.prayerclock.data.repository.PrayerTimesRepository
import com.mosque.prayerclock.data.repository.SettingsRepository
import com.mosque.prayerclock.data.scraping.ACJUScraper
import com.mosque.prayerclock.data.scraping.DirectScrapingService
import com.mosque.prayerclock.data.scraping.HijriDateScraper
import com.mosque.prayerclock.data.scraping.PdfParser
import java.io.File
import java.lang.reflect.Proxy
import javax.inject.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class MazhabRepositoryTest {
    @Test
    fun acjuFailureDoesNotContactAnotherProvider() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val settingsFile = File(context.cacheDir, "acju-failure-${System.nanoTime()}.preferences_pb")
        val job = Job()
        val requests = mutableListOf<String>()
        try {
            val settings = SettingsRepository(
                PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { settingsFile },
                Provider<PrayerTimesCacheInvalidator> { error("Unexpected invalidation") },
            )
            val client = OkHttpClient.Builder().addInterceptor {
                requests.add(it.request().url.toString())
                throw java.io.IOException("ACJU unavailable")
            }.build()
            val direct = DirectScrapingService(ACJUScraper(client), PdfParser(), HijriDateScraper(client, Gson()), database.prayerTimesDao())
            val repository = PrayerTimesRepository(noNetwork<PrayerTimesApi>(), direct, database.prayerTimesDao(), settings)
            val today = repository.getTodayPrayerTimesByDirectScraping(12).last()
            val tomorrow = repository.getTomorrowPrayerTimesByDirectScraping(12).last()
            assertTrue(today is NetworkResult.Error)
            assertTrue(tomorrow is NetworkResult.Error)
            assertEquals(2, requests.size)
            assertTrue(requests.all { it == "https://www.acju.lk/prayer-times/" })
            assertEquals(0, database.prayerTimesDao().getAllPrayerTimesCount())
        } finally {
            job.cancelAndJoin()
            database.close()
            settingsFile.delete()
        }
    }

    @Test
    fun newlyDownloadedOlderYearTimetableWorksOnFirstFetch() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val month = months[today.monthNumber - 1]
        val pdfBytes = java.io.ByteArrayOutputStream().also { output ->
            val document = com.itextpdf.text.Document()
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, output)
            document.open()
            document.add(com.itextpdf.text.Paragraph("Zone: 1 ${today.year - 1}"))
            document.add(com.itextpdf.text.Paragraph("${today.dayOfMonth}-${month.take(3)} 4:40 AM 5:57 AM 11:57 AM 3:17 PM 4:15 PM 5:56 PM 7:05 PM"))
            document.close()
        }.toByteArray()
        val html = """<details><div class="e-n-accordion-item-title-text">COLOMBO DISTRICT, GAMPAHA DISTRICT, KALUTARA DISTRICT</div><div><strong>$month</strong><a href="https://www.acju.lk/timetable.pdf">Download</a></div></details>"""
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val pdf = chain.request().url.encodedPath.endsWith(".pdf")
            okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("OK")
                .body(if (pdf) okhttp3.ResponseBody.create(null, pdfBytes) else okhttp3.ResponseBody.create(null, html)).build()
        }.build()
        try {
            val direct = DirectScrapingService(ACJUScraper(client), PdfParser(), HijriDateScraper(client, Gson()), database.prayerTimesDao())
            val result = direct.getTodayPrayerTimes(1)
            assertEquals(today.toString(), result?.date)
            assertEquals("15:17", result?.asrAzan)
            assertEquals("16:15", result?.asrHanafiAzan)
            assertEquals(today.toString(), database.prayerTimesDao().getPrayerTimesById("${today.toString().substring(5)}_ACJU_DIRECT:1")?.date)
        } finally {
            database.close()
        }
    }

    @Test
    fun existingDatabasesUpgradeWithoutLosingOtherProvidersOrHijriDates() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (version in listOf(4, 5)) {
            val name = "mazhab-migration-$version.db"
            context.deleteDatabase(name)
            try {
                context.openOrCreateDatabase(name, 0, null).use { db ->
                    db.execSQL("CREATE TABLE prayer_times (id TEXT NOT NULL PRIMARY KEY, date TEXT NOT NULL, providerKey TEXT, fajrAzan TEXT NOT NULL, dhuhrAzan TEXT NOT NULL, asrAzan TEXT NOT NULL, maghribAzan TEXT NOT NULL, ishaAzan TEXT NOT NULL, sunrise TEXT NOT NULL, hijriDate TEXT, location TEXT)")
                    db.execSQL("CREATE TABLE hijri_dates (id TEXT NOT NULL PRIMARY KEY, gregorianDate TEXT NOT NULL, hijriDay INTEGER NOT NULL, hijriMonth INTEGER NOT NULL, hijriYear INTEGER NOT NULL, provider TEXT NOT NULL, region TEXT, createdAt INTEGER NOT NULL, isCalculated INTEGER NOT NULL, hijriMonthStartDate TEXT, hijriMonthEndDate TEXT, hijriMonthName TEXT)")
                    for (provider in listOf("ACJU_DIRECT:1", "AL_ADHAN_API:Colombo")) {
                        db.execSQL("INSERT INTO prayer_times VALUES (?, '2026-10-19', ?, '04:40', '11:57', '15:17', '17:56', '19:05', '05:57', NULL, NULL)", arrayOf(provider, provider))
                    }
                    db.execSQL("INSERT INTO hijri_dates VALUES ('saved', '2026-10-19', 7, 5, 1448, 'MANUAL', NULL, 0, 0, NULL, NULL, NULL)")
                    db.version = version
                }
                val room = Room.databaseBuilder(context, AppDatabase::class.java, name)
                    .addMigrations(AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6).build()
                try {
                    assertEquals(1, room.prayerTimesDao().getAllPrayerTimesCount())
                    assertEquals("15:17", room.prayerTimesDao().getPrayerTimesById("AL_ADHAN_API:Colombo")!!.asrAzan)
                    room.openHelper.writableDatabase.query("SELECT COUNT(*) FROM hijri_dates").use { cursor ->
                        cursor.moveToFirst()
                        assertEquals(1, cursor.getInt(0))
                    }
                } finally {
                    room.close()
                }
            } finally {
                context.deleteDatabase(name)
            }
        }
    }

    private inline fun <reified T> noNetwork(): T = Proxy.newProxyInstance(
        T::class.java.classLoader, arrayOf(T::class.java),
    ) { _, method, _ -> error("Unexpected network call: ${method.name}") } as T

    @Test
    fun mazhabSelectionUsesOfflineAcjuAndSeparateAlAdhanCaches() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val job = Job()
        val settingsFile = File.createTempFile("mazhab", ".preferences_pb", context.cacheDir).apply { delete() }
        var repository: PrayerTimesRepository? = null
        val invalidator = object : PrayerTimesCacheInvalidator {
            override fun invalidatePrayerTimesCache() { repository?.invalidatePrayerTimesCache() }
        }
        try {
            val settings = SettingsRepository(
                PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { settingsFile },
                Provider<PrayerTimesCacheInvalidator> { invalidator },
            )
            val client = OkHttpClient.Builder().addInterceptor { error("Unexpected ACJU download") }.build()
            val direct = DirectScrapingService(
                ACJUScraper(client), PdfParser(), HijriDateScraper(client, Gson()), database.prayerTimesDao(),
            )
            var allowAlAdhan = false
            val schools = mutableListOf<Int>()
            val api = Proxy.newProxyInstance(PrayerTimesApi::class.java.classLoader, arrayOf(PrayerTimesApi::class.java)) { _, method, args ->
                check(allowAlAdhan && method.name == "getPrayerTimesByCity") { "Unexpected network call" }
                val school = args!![args.size - 2] as Int
                schools.add(school)
                val asr = if (school == 1) "16:15" else "15:17"
                Response.success(Gson().fromJson("""
                    {"code":200,"status":"OK","data":{
                    "timings":{"Fajr":"04:40","Sunrise":"05:57","Dhuhr":"11:57","Asr":"$asr","Maghrib":"17:56","Isha":"19:05"},
                    "date":{"gregorian":{"date":"19-10-2026"},"hijri":{"date":"07-05-1448"}},
                    "meta":{"latitude":6.9,"longitude":79.8}}}
                """.trimIndent(), AlAdhanResponse::class.java))
            } as PrayerTimesApi
            val repo = PrayerTimesRepository(api, direct, database.prayerTimesDao(), settings)
            repository = repo
            settings.updatePrayerServiceType(PrayerServiceType.ACJU_DIRECT)
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val tomorrow = today.plus(1, DateTimeUnit.DAY)
            for (date in listOf(today, tomorrow)) {
                database.prayerTimesDao().insertPrayerTimes(PrayerTimes(
                    id = "${date.toString().substring(5)}_ACJU_DIRECT:1", date = date.toString(), providerKey = "ACJU_DIRECT:1",
                    fajrAzan = "04:40", sunrise = "05:57", dhuhrAzan = "11:57", asrAzan = "15:17",
                    maghribAzan = "17:56", ishaAzan = "19:05", asrShafiAzan = "15:17", asrHanafiAzan = "16:15",
                ))
            }
            for (mazhab in listOf(Mazhab.SHAFI, Mazhab.HANAFI, Mazhab.SHAFI)) {
                settings.updateMazhab(mazhab)
                val expected = if (mazhab == Mazhab.HANAFI) "16:15" else "15:17"
                val current = repo.getTodayPrayerTimesFromSettings().last()
                assertTrue(current is NetworkResult.Success)
                assertEquals(expected, (current as NetworkResult.Success).data.asrAzan)
                assertEquals(expected, repo.getCachedPrayerTimes()!!.asrAzan)
                assertEquals(expected, (repo.getTomorrowPrayerTimesFromSettings().last() as NetworkResult.Success).data.asrAzan)
                assertEquals("15:17", database.prayerTimesDao().getPrayerTimesById("${today.toString().substring(5)}_ACJU_DIRECT:1")!!.asrAzan)
            }
            // A legacy timetable must not silently supply Shafi time for a Hanafi request.
            val id = "${today.toString().substring(5)}_ACJU_DIRECT:1"
            val legacy = database.prayerTimesDao().getPrayerTimesById(id)!!.copy(asrHanafiAzan = null)
            database.prayerTimesDao().insertPrayerTimes(legacy)
            settings.updateMazhab(Mazhab.HANAFI)
            assertTrue(repo.getTodayPrayerTimesFromSettings().last() is NetworkResult.Error)
            assertTrue(schools.isEmpty())

            allowAlAdhan = true
            settings.updatePrayerServiceType(PrayerServiceType.AL_ADHAN_API)
            for (mazhab in listOf(Mazhab.SHAFI, Mazhab.HANAFI, Mazhab.SHAFI)) {
                settings.updateMazhab(mazhab)
                val expected = if (mazhab == Mazhab.HANAFI) "16:15" else "15:17"
                assertEquals(expected, (repo.getTodayPrayerTimesFromSettings().last() as NetworkResult.Success).data.asrAzan)
                assertEquals(expected, (repo.getTomorrowPrayerTimesFromSettings().last() as NetworkResult.Success).data.asrAzan)
            }
            assertEquals(listOf(0, 0, 1, 1), schools) // Switching back reuses that school's cached days.
        } finally {
            job.cancelAndJoin()
            database.close()
            settingsFile.delete()
        }
    }
}
