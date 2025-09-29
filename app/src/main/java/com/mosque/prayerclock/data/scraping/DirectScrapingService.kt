package com.mosque.prayerclock.data.scraping

import android.util.Log
import com.mosque.prayerclock.data.database.PrayerTimesDao
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectScrapingService @Inject constructor(
    private val acjuScraper: ACJUScraper,
    private val pdfParser: PdfParser,
    private val hijriDateScraper: HijriDateScraper,
    private val prayerTimesDao: PrayerTimesDao
) {

    companion object {
        private const val TAG = "DirectScrapingService"
    }

    /**
     * Get today's prayer times with monthly caching
     */
    suspend fun getTodayPrayerTimes(zone: Int, apartment: Boolean = false): PrayerTimes? {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "🔍 Getting today's prayer times with caching - Zone: $zone, Apartment: $apartment")
            try {
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                Log.d(TAG, "📅 Today's date: $today")
                
                // First try to get from cache
                val cachedResult = getCachedPrayerTimesForDate(zone, today)
                if (cachedResult != null) {
                    Log.d(TAG, "✅ Found cached prayer times for $today")
                    return@withContext cachedResult
                }
                
                // If not cached, scrape the entire month and cache it
                Log.d(TAG, "📥 No cached data found, scraping entire month...")
                val monthlyData = scrapeAndCacheMonthlyPrayerTimes(zone, today.year, today.monthNumber)
                
                // Return today's data from the scraped monthly data
                val result = monthlyData?.find { it.date == today.toString() }
                if (result != null) {
                    Log.d(TAG, "✅ Successfully got today's prayer times from monthly cache: Fajr=${result.fajrAzan}, Dhuhr=${result.dhuhrAzan}")
                } else {
                    Log.w(TAG, "⚠️ Failed to find today's prayer times in monthly data")
                }
                return@withContext result
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting today's prayer times", e)
                null
            }
        }
    }

    /**
     * Get tomorrow's prayer times with monthly caching
     */
    suspend fun getTomorrowPrayerTimes(zone: Int, apartment: Boolean = false): PrayerTimes? {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "🔍 Getting tomorrow's prayer times with caching - Zone: $zone, Apartment: $apartment")
            try {
                val tomorrow = Clock.System.now()
                    .plus(1, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
                Log.d(TAG, "📅 Tomorrow's date: $tomorrow")
                
                // First try to get from cache
                val cachedResult = getCachedPrayerTimesForDate(zone, tomorrow)
                if (cachedResult != null) {
                    Log.d(TAG, "✅ Found cached prayer times for $tomorrow")
                    return@withContext cachedResult
                }
                
                // If not cached, scrape the month and cache it
                Log.d(TAG, "📥 No cached data found, scraping entire month...")
                val monthlyData = scrapeAndCacheMonthlyPrayerTimes(zone, tomorrow.year, tomorrow.monthNumber)
                
                // Return tomorrow's data from the scraped monthly data
                val result = monthlyData?.find { it.date == tomorrow.toString() }
                if (result != null) {
                    Log.d(TAG, "✅ Successfully got tomorrow's prayer times from monthly cache: Fajr=${result.fajrAzan}, Dhuhr=${result.dhuhrAzan}")
                } else {
                    Log.w(TAG, "⚠️ Failed to find tomorrow's prayer times in monthly data")
                }
                return@withContext result
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting tomorrow's prayer times", e)
                null
            }
        }
    }

    /**
     * Scrape prayer times for a specific date and zone
     */
    suspend fun getPrayerTimesForDate(
        zone: Int, 
        date: LocalDate, 
        apartment: Boolean = false
    ): PrayerTimes? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔍 Scraping prayer times for zone $zone, date $date, apartment: $apartment")

                // Scrape prayer times data
                Log.d(TAG, "📥 Requesting PDF data from ACJU scraper...")
                val prayerTimesData = acjuScraper.scrapePrayerTimes(
                    zone = zone,
                    year = date.year,
                    month = date.monthNumber,
                    pdfParser = pdfParser
                )

                if (prayerTimesData == null) {
                    Log.e(TAG, "❌ Failed to scrape prayer times data from ACJU")
                    return@withContext null
                }
                
                Log.d(TAG, "✅ Successfully received prayer times data. Zone: ${prayerTimesData.metadata.zone}, Month: ${prayerTimesData.metadata.month}/${prayerTimesData.metadata.year}")
                Log.d(TAG, "📊 Total prayer time entries: ${prayerTimesData.prayerTimes.size}")

                // Find the specific day's prayer times
                val dateString = date.toString()
                val dailyPrayerTime = prayerTimesData.prayerTimes.find { it.date == dateString }

                if (dailyPrayerTime == null) {
                    Log.e(TAG, "Prayer times not found for date $dateString")
                    return@withContext null
                }

                // Get Hijri date
                val hijriDate = hijriDateScraper.getHijriDateForGregorian(
                    date.year, 
                    date.monthNumber, 
                    date.dayOfMonth
                )

                // Apply apartment adjustments if requested
                val adjustedTimes = if (apartment) {
                    applyApartmentAdjustments(dailyPrayerTime, prayerTimesData.apartmentAdjustments)
                } else {
                    dailyPrayerTime
                }

                // Convert to PrayerTimes model
                val prayerTimes = convertToPrayerTimes(
                    adjustedTimes,
                    prayerTimesData.metadata,
                    hijriDate?.let { "${it.hijriDay} ${it.hijriMonth} ${it.hijriYear}" }
                )

                Log.d(TAG, "Successfully scraped prayer times for $dateString")
                return@withContext prayerTimes

            } catch (e: Exception) {
                Log.e(TAG, "Error scraping prayer times for date $date", e)
                null
            }
        }
    }

    /**
     * Scrape prayer times for an entire month
     */
    suspend fun getMonthPrayerTimes(
        zone: Int, 
        year: Int, 
        month: Int, 
        apartment: Boolean = false
    ): List<PrayerTimes>? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Scraping month prayer times for zone $zone, $year-$month")

                val prayerTimesData = acjuScraper.scrapePrayerTimes(
                    zone = zone,
                    year = year,
                    month = month,
                    pdfParser = pdfParser
                )

                if (prayerTimesData == null) {
                    Log.e(TAG, "Failed to scrape month prayer times data")
                    return@withContext null
                }

                val monthPrayerTimes = mutableListOf<PrayerTimes>()

                for (dailyPrayerTime in prayerTimesData.prayerTimes) {
                    try {
                        // Parse date to get Hijri date
                        val dateParts = dailyPrayerTime.date.split("-")
                        val hijriDate = hijriDateScraper.getHijriDateForGregorian(
                            dateParts[0].toInt(),
                            dateParts[1].toInt(),
                            dateParts[2].toInt()
                        )

                        // Apply apartment adjustments if requested
                        val adjustedTimes = if (apartment) {
                            applyApartmentAdjustments(dailyPrayerTime, prayerTimesData.apartmentAdjustments)
                        } else {
                            dailyPrayerTime
                        }

                        // Convert to PrayerTimes model
                        val prayerTimes = convertToPrayerTimes(
                            adjustedTimes,
                            prayerTimesData.metadata,
                            hijriDate?.let { "${it.hijriDay} ${it.hijriMonth} ${it.hijriYear}" }
                        )

                        monthPrayerTimes.add(prayerTimes)

                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing daily prayer time: ${dailyPrayerTime.date}", e)
                        // Continue with other days even if one fails
                    }
                }

                Log.d(TAG, "Successfully scraped ${monthPrayerTimes.size} days of prayer times")
                return@withContext monthPrayerTimes

            } catch (e: Exception) {
                Log.e(TAG, "Error scraping month prayer times", e)
                null
            }
        }
    }

    /**
     * Get current Hijri date
     */
    suspend fun getCurrentHijriDate(): HijriDateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                return@withContext hijriDateScraper.getCurrentHijriDate()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting current Hijri date", e)
                null
            }
        }
    }

    /**
     * Apply apartment adjustments to prayer times
     */
    private fun applyApartmentAdjustments(
        dailyPrayerTime: DailyPrayerTime,
        adjustments: ApartmentAdjustments
    ): DailyPrayerTime {
        return dailyPrayerTime.copy(
            fajr = TimeUtils.addMinutesToTime(dailyPrayerTime.fajr, adjustments.adjustments.fajr),
            sunrise = TimeUtils.addMinutesToTime(dailyPrayerTime.sunrise, adjustments.adjustments.sunrise),
            maghrib = TimeUtils.addMinutesToTime(dailyPrayerTime.maghrib, adjustments.adjustments.maghrib),
            isha = TimeUtils.addMinutesToTime(dailyPrayerTime.isha, adjustments.adjustments.isha)
        )
    }

    /**
     * Convert scraped data to PrayerTimes model
     */
    private fun convertToPrayerTimes(
        dailyPrayerTime: DailyPrayerTime,
        metadata: PrayerTimesMetadata,
        hijriDate: String?
    ): PrayerTimes {
        // Calculate Iqamah times (using standard gaps)
        val fajrIqamah = TimeUtils.calculateIqamahTime(dailyPrayerTime.fajr, 20) // 20 minutes after Azan
        val dhuhrIqamah = TimeUtils.calculateIqamahTime(dailyPrayerTime.dhuhr, 10) // 10 minutes after Azan
        val asrIqamah = TimeUtils.calculateIqamahTime(dailyPrayerTime.asr, 10) // 10 minutes after Azan
        val maghribIqamah = TimeUtils.calculateIqamahTime(dailyPrayerTime.maghrib, 5) // 5 minutes after Azan
        val ishaIqamah = TimeUtils.calculateIqamahTime(dailyPrayerTime.isha, 10) // 10 minutes after Azan

        return PrayerTimes(
            id = "", // Will be set by repository with proper composite ID
            date = dailyPrayerTime.date,
            providerKey = null, // Will be set by repository
            fajrAzan = dailyPrayerTime.fajr,
            fajrIqamah = fajrIqamah,
            dhuhrAzan = dailyPrayerTime.dhuhr,
            dhuhrIqamah = dhuhrIqamah,
            asrAzan = dailyPrayerTime.asr,
            asrIqamah = asrIqamah,
            maghribAzan = dailyPrayerTime.maghrib,
            maghribIqamah = maghribIqamah,
            ishaAzan = dailyPrayerTime.isha,
            ishaIqamah = ishaIqamah,
            sunrise = dailyPrayerTime.sunrise,
            hijriDate = hijriDate,
            location = "Zone ${metadata.zone} - ${metadata.districts.joinToString(", ")}"
        )
    }

    /**
     * Check if direct scraping is available for a zone
     */
    fun isZoneSupported(zone: Int): Boolean {
        return acjuScraper.getZoneDescription(zone) != null
    }

    /**
     * Get cached prayer times for a specific date
     */
    private suspend fun getCachedPrayerTimesForDate(zone: Int, date: LocalDate): PrayerTimes? {
        return try {
            val providerKey = "ACJU_DIRECT:$zone"
            val dateStr = date.toString()
            val cached = prayerTimesDao.getPrayerTimesByDateAndProvider(dateStr, providerKey)
            if (cached != null) {
                Log.d(TAG, "📋 Found cached prayer times for $dateStr")
            }
            cached
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting cached prayer times", e)
            null
        }
    }

    /**
     * Scrape and cache entire month's prayer times
     */
    private suspend fun scrapeAndCacheMonthlyPrayerTimes(zone: Int, year: Int, month: Int): List<PrayerTimes>? {
        return try {
            Log.d(TAG, "📥 Scraping monthly prayer times for Zone $zone, $year-$month")
            
            // Get PDF URL for the month
            val pdfUrl = acjuScraper.getPrayerTimesPdfUrl(zone, year, month)
            if (pdfUrl == null) {
                Log.e(TAG, "❌ No prayer times PDF found for zone $zone, $year-$month")
                return null
            }

            // Download and parse PDF
            val pdfBytes = acjuScraper.downloadPdf(pdfUrl)
            if (pdfBytes == null) {
                Log.e(TAG, "❌ Failed to download PDF from $pdfUrl")
                return null
            }
            val text = pdfParser.extractTextFromPdf(pdfBytes)
            val prayerTimesData = pdfParser.parsePrayerTimes(text)

            // Convert to PrayerTimes entities
            val prayerTimesEntities = pdfParser.convertToPrayerTimesEntities(prayerTimesData, zone)
            
            // Cache all prayer times for the month
            prayerTimesDao.insertPrayerTimesList(prayerTimesEntities)
            
            Log.d(TAG, "✅ Successfully cached ${prayerTimesEntities.size} prayer times for Zone $zone, $year-$month")
            prayerTimesEntities
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error scraping and caching monthly prayer times", e)
            null
        }
    }

    /**
     * Check if we have complete monthly cache for a given month
     */
    private suspend fun hasCompleteMonthlyCache(zone: Int, year: Int, month: Int): Boolean {
        return try {
            val providerKey = "ACJU_DIRECT:$zone"
            val startDate = LocalDate(year, month, 1).toString()
            val firstDayOfNextMonth = LocalDate(year, month, 1).plus(1, DateTimeUnit.MONTH)
            val lastDayOfMonth = firstDayOfNextMonth.plus(-1, DateTimeUnit.DAY)
            val endDate = lastDayOfMonth.toString()
            
            val cachedCount = prayerTimesDao.getMonthlyPrayerTimesCount(providerKey, startDate, endDate)
            val expectedDays = lastDayOfMonth.dayOfMonth
            
            Log.d(TAG, "📊 Monthly cache check for Zone $zone, $year-$month: $cachedCount/$expectedDays days")
            cachedCount >= expectedDays
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking monthly cache", e)
            false
        }
    }

    /**
     * Get zone description
     */
    fun getZoneDescription(zone: Int): String? {
        return acjuScraper.getZoneDescription(zone)
    }

    /**
     * Get all supported zones
     */
    fun getSupportedZones(): List<Int> {
        return acjuScraper.getAvailableZones()
    }
}
