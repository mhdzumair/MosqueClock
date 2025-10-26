package com.mosque.prayerclock.data.scraping

import android.util.Log
import com.mosque.prayerclock.data.database.PrayerTimesDao
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
class DirectScrapingService
    @Inject
    constructor(
        private val acjuScraper: ACJUScraper,
        private val pdfParser: PdfParser,
        private val hijriDateScraper: HijriDateScraper,
        private val prayerTimesDao: PrayerTimesDao,
    ) {
        companion object {
            private const val TAG = "DirectScrapingService"
        }

        /**
         * Get today's prayer times with monthly caching
         */
        suspend fun getTodayPrayerTimes(
            zone: Int,
            apartment: Boolean = false,
        ): PrayerTimes? {
            return withContext(Dispatchers.IO) {
                Log.d(TAG, "🔍 Getting today's prayer times with caching - Zone: $zone, Apartment: $apartment")
                try {
                    val today =
                        Clock.System
                            .now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
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
                        Log.d(
                            TAG,
                            "✅ Successfully got today's prayer times from monthly cache: Fajr=${result.fajrAzan}, Dhuhr=${result.dhuhrAzan}",
                        )
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
        suspend fun getTomorrowPrayerTimes(
            zone: Int,
            apartment: Boolean = false,
        ): PrayerTimes? {
            return withContext(Dispatchers.IO) {
                Log.d(TAG, "🔍 Getting tomorrow's prayer times with caching - Zone: $zone, Apartment: $apartment")
                try {
                    val tomorrow =
                        Clock.System
                            .now()
                            .plus(1, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
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
                        Log.d(
                            TAG,
                            "✅ Successfully got tomorrow's prayer times from monthly cache: Fajr=${result.fajrAzan}, Dhuhr=${result.dhuhrAzan}",
                        )
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
            apartment: Boolean = false,
        ): PrayerTimes? {
            return withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "🔍 Scraping prayer times for zone $zone, date $date, apartment: $apartment")

                    // Scrape prayer times data
                    Log.d(TAG, "📥 Requesting PDF data from ACJU scraper...")
                    val prayerTimesData =
                        acjuScraper.scrapePrayerTimes(
                            zone = zone,
                            year = date.year,
                            month = date.monthNumber,
                            pdfParser = pdfParser,
                        )

                    if (prayerTimesData == null) {
                        Log.e(TAG, "❌ Failed to scrape prayer times data from ACJU")
                        return@withContext null
                    }

                    Log.d(
                        TAG,
                        "✅ Successfully received prayer times data. Zone: ${prayerTimesData.metadata.zone}, Month: ${prayerTimesData.metadata.month}/${prayerTimesData.metadata.year}",
                    )
                    Log.d(TAG, "📊 Total prayer time entries: ${prayerTimesData.prayerTimes.size}")

                    // Find the specific day's prayer times
                    val dateString = date.toString()
                    val dailyPrayerTime = prayerTimesData.prayerTimes.find { it.date == dateString }

                    if (dailyPrayerTime == null) {
                        Log.e(TAG, "Prayer times not found for date $dateString")
                        return@withContext null
                    }

                    // Get Hijri date
                    val hijriDate =
                        hijriDateScraper.getHijriDateForGregorian(
                            date.year,
                            date.monthNumber,
                            date.dayOfMonth,
                        )

                    // Apply apartment adjustments if requested
                    val adjustedTimes =
                        if (apartment) {
                            applyApartmentAdjustments(dailyPrayerTime, prayerTimesData.apartmentAdjustments)
                        } else {
                            dailyPrayerTime
                        }

                    // Convert to PrayerTimes model
                    val prayerTimes =
                        convertToPrayerTimes(
                            adjustedTimes,
                            prayerTimesData.metadata,
                            hijriDate?.let { "${it.hijriDay} ${it.hijriMonth} ${it.hijriYear}" },
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
            apartment: Boolean = false,
        ): List<PrayerTimes>? {
            return withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "Scraping month prayer times for zone $zone, $year-$month")

                    val prayerTimesData =
                        acjuScraper.scrapePrayerTimes(
                            zone = zone,
                            year = year,
                            month = month,
                            pdfParser = pdfParser,
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
                            val hijriDate =
                                hijriDateScraper.getHijriDateForGregorian(
                                    dateParts[0].toInt(),
                                    dateParts[1].toInt(),
                                    dateParts[2].toInt(),
                                )

                            // Apply apartment adjustments if requested
                            val adjustedTimes =
                                if (apartment) {
                                    applyApartmentAdjustments(dailyPrayerTime, prayerTimesData.apartmentAdjustments)
                                } else {
                                    dailyPrayerTime
                                }

                            // Convert to PrayerTimes model
                            val prayerTimes =
                                convertToPrayerTimes(
                                    adjustedTimes,
                                    prayerTimesData.metadata,
                                    hijriDate?.let { "${it.hijriDay} ${it.hijriMonth} ${it.hijriYear}" },
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
            adjustments: ApartmentAdjustments,
        ): DailyPrayerTime =
            dailyPrayerTime.copy(
                fajr = TimeUtils.addMinutesToTime(dailyPrayerTime.fajr, adjustments.adjustments.fajr),
                sunrise = TimeUtils.addMinutesToTime(dailyPrayerTime.sunrise, adjustments.adjustments.sunrise),
                maghrib = TimeUtils.addMinutesToTime(dailyPrayerTime.maghrib, adjustments.adjustments.maghrib),
                isha = TimeUtils.addMinutesToTime(dailyPrayerTime.isha, adjustments.adjustments.isha),
            )

        /**
         * Convert scraped data to PrayerTimes model
         * Note: Iqamah times are no longer stored, they will be calculated dynamically from settings
         */
        private fun convertToPrayerTimes(
            dailyPrayerTime: DailyPrayerTime,
            metadata: PrayerTimesMetadata,
            hijriDate: String?,
        ): PrayerTimes =
            PrayerTimes(
                id = "", // Will be set by repository with proper composite ID
                date = dailyPrayerTime.date,
                providerKey = null, // Will be set by repository
                fajrAzan = dailyPrayerTime.fajr,
                dhuhrAzan = dailyPrayerTime.dhuhr,
                asrAzan = dailyPrayerTime.asr,
                maghribAzan = dailyPrayerTime.maghrib,
                ishaAzan = dailyPrayerTime.isha,
                sunrise = dailyPrayerTime.sunrise,
                hijriDate = hijriDate,
                location = "Zone ${metadata.zone} - ${metadata.districts.joinToString(", ")}",
            )

        /**
         * Check if direct scraping is available for a zone
         */
        fun isZoneSupported(zone: Int): Boolean = acjuScraper.getZoneDescription(zone) != null

        /**
         * Get cached prayer times for a specific date
         */
        private suspend fun getCachedPrayerTimesForDate(
            zone: Int,
            date: LocalDate,
        ): PrayerTimes? =
            try {
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

        /**
         * Scrape and cache entire month's prayer times
         */
        private suspend fun scrapeAndCacheMonthlyPrayerTimes(
            zone: Int,
            year: Int,
            month: Int,
        ): List<PrayerTimes>? {
            return try {
                Log.d(TAG, "📥 Scraping monthly prayer times for Zone $zone, $year-$month")

                // Get PDF URL for the month
                val pdfUrl = acjuScraper.getPrayerTimesPdfUrl(zone, year, month)
                if (pdfUrl == null) {
                    Log.d(TAG, "ℹ️ No PDF available yet for zone $zone, $year-$month (likely future month)")
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

                if (prayerTimesEntities.isEmpty()) {
                    Log.e(TAG, "⚠️ No prayer times extracted from PDF for Zone $zone, $year-$month")
                    return null
                }

                // Cache all prayer times for the month
                prayerTimesDao.insertPrayerTimesList(prayerTimesEntities)

                Log.d(
                    TAG,
                    "✅ Cached ${prayerTimesEntities.size} prayer times for Zone $zone (${prayerTimesEntities.first().date} to ${prayerTimesEntities.last().date})",
                )
                prayerTimesEntities
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error scraping and caching monthly prayer times", e)
                null
            }
        }

        /**
         * Check if we have complete monthly cache for a given month
         */
        private suspend fun hasCompleteMonthlyCache(
            zone: Int,
            year: Int,
            month: Int,
        ): Boolean =
            try {
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

        /**
         * Get zone description
         */
        fun getZoneDescription(zone: Int): String? = acjuScraper.getZoneDescription(zone)

        /**
         * Get all supported zones
         */
        fun getSupportedZones(): List<Int> = acjuScraper.getAvailableZones()

        /**
         * Prefetch and cache prayer times for all remaining months of the year
         * This enables full offline functionality
         */
        suspend fun prefetchRemainingYearPrayerTimes(zone: Int): PrefetchResult {
            return withContext(Dispatchers.IO) {
                Log.d(TAG, "🔄 Starting prefetch of remaining year's prayer times for Zone $zone")
                
                val today = Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                
                val currentYear = today.year
                val currentMonth = today.monthNumber
                val monthsToFetch = mutableListOf<Pair<Int, Int>>() // List of (year, month) pairs
                
                // Add remaining months of current year
                for (month in currentMonth..12) {
                    monthsToFetch.add(Pair(currentYear, month))
                }
                
                // Add first few months of next year (January to March)
                val nextYear = currentYear + 1
                for (month in 1..3) {
                    monthsToFetch.add(Pair(nextYear, month))
                }
                
                Log.d(TAG, "📋 Planning to fetch ${monthsToFetch.size} months")
                
                val results = PrefetchResult(
                    totalMonths = monthsToFetch.size,
                    successfulMonths = 0,
                    failedMonths = 0,
                    skippedMonths = 0,
                    unavailableMonths = 0,
                    cachedDays = 0
                )
                
                for ((year, month) in monthsToFetch) {
                    try {
                        // Check if month is already cached
                        val isComplete = hasCompleteMonthlyCache(zone, year, month)
                        if (isComplete) {
                            Log.d(TAG, "⏭️ Skipping $year-$month (already fully cached)")
                            results.skippedMonths++
                            continue
                        } else {
                            Log.d(TAG, "📥 Fetching prayer times for $year-$month (incomplete or not cached)...")
                        }
                        
                        val monthData = scrapeAndCacheMonthlyPrayerTimes(zone, year, month)
                        
                        if (monthData != null && monthData.isNotEmpty()) {
                            results.successfulMonths++
                            results.cachedDays += monthData.size
                            Log.d(TAG, "✅ Successfully cached ${monthData.size} days for $year-$month")
                        } else {
                            // Check if this is a future month (PDF not published yet) vs actual failure
                            val today = Clock.System
                                .now()
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                .date
                            val monthDate = LocalDate(year, month, 1)
                            
                            // If the month is in the future (next year), treat as unavailable, not failed
                            if (year > today.year) {
                                results.unavailableMonths++
                                Log.d(TAG, "ℹ️ $year-$month not available yet (future month)")
                            } else {
                                results.failedMonths++
                                Log.w(TAG, "⚠️ Failed to fetch $year-$month")
                            }
                        }
                        
                        // Add a small delay between requests to be respectful to the server
                        kotlinx.coroutines.delay(1000)
                        
                    } catch (e: Exception) {
                        results.failedMonths++
                        Log.e(TAG, "❌ Error fetching $year-$month: ${e.message}", e)
                    }
                }
                
                Log.d(TAG, "🎉 Prefetch complete: ${results.successfulMonths}/${results.totalMonths} months cached (${results.cachedDays} days total)")
                results
            }
        }
        
        /**
         * Check cache status for remaining months of the year
         */
        suspend fun checkCacheStatus(zone: Int): CacheStatus {
            return withContext(Dispatchers.IO) {
                val today = Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                
                val currentYear = today.year
                val currentMonth = today.monthNumber
                
                var totalMonths = 0
                var cachedMonths = 0
                var totalDays = 0
                var cachedDays = 0
                
                // Check remaining months of current year
                for (month in currentMonth..12) {
                    totalMonths++
                    val providerKey = "ACJU_DIRECT:$zone"
                    val startDate = LocalDate(currentYear, month, 1).toString()
                    val firstDayOfNextMonth = LocalDate(currentYear, month, 1).plus(1, DateTimeUnit.MONTH)
                    val lastDayOfMonth = firstDayOfNextMonth.plus(-1, DateTimeUnit.DAY)
                    val endDate = lastDayOfMonth.toString()
                    val expectedDays = lastDayOfMonth.dayOfMonth
                    val cached = prayerTimesDao.getMonthlyPrayerTimesCount(providerKey, startDate, endDate)
                    
                    totalDays += expectedDays
                    cachedDays += cached
                    
                    if (cached >= expectedDays) {
                        cachedMonths++
                    }
                }
                
                // Check first few months of next year
                val nextYear = currentYear + 1
                for (month in 1..3) {
                    totalMonths++
                    val providerKey = "ACJU_DIRECT:$zone"
                    val startDate = LocalDate(nextYear, month, 1).toString()
                    val firstDayOfNextMonth = LocalDate(nextYear, month, 1).plus(1, DateTimeUnit.MONTH)
                    val lastDayOfMonth = firstDayOfNextMonth.plus(-1, DateTimeUnit.DAY)
                    val endDate = lastDayOfMonth.toString()
                    val expectedDays = lastDayOfMonth.dayOfMonth
                    val cached = prayerTimesDao.getMonthlyPrayerTimesCount(providerKey, startDate, endDate)
                    
                    totalDays += expectedDays
                    cachedDays += cached
                    
                    if (cached >= expectedDays) {
                        cachedMonths++
                    }
                }
                
                CacheStatus(
                    totalMonths = totalMonths,
                    cachedMonths = cachedMonths,
                    totalDays = totalDays,
                    cachedDays = cachedDays,
                    isFullyCached = cachedMonths == totalMonths
                )
            }
        }

        /**
         * Result of prefetch operation
         */
        data class PrefetchResult(
            val totalMonths: Int,
            var successfulMonths: Int,
            var failedMonths: Int,
            var skippedMonths: Int,
            var unavailableMonths: Int = 0,
            var cachedDays: Int
        )

        /**
         * Cache status information
         */
        data class CacheStatus(
            val totalMonths: Int,
            val cachedMonths: Int,
            val totalDays: Int = 0,
            val cachedDays: Int = 0,
            val isFullyCached: Boolean
        )
    }
