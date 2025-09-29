package com.mosque.prayerclock.data.repository

import android.util.Log
import com.mosque.prayerclock.data.database.HijriDateDao
import com.mosque.prayerclock.data.database.HijriDateEntity
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.HijriProvider
import com.mosque.prayerclock.data.network.MosqueClockApi
import com.mosque.prayerclock.data.network.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

@Singleton
class HijriDateRepository
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val mosqueClockApi: MosqueClockApi,
        private val prayerTimesApi: com.mosque.prayerclock.data.network.PrayerTimesApi,
        private val hijriDateDao: HijriDateDao,
        private val hijriDateScraper: com.mosque.prayerclock.data.scraping.HijriDateScraper,
    ) {
        data class HijriDate(
            val day: Int,
            val month: Int,
            val year: Int,
        )

        suspend fun getCurrentHijriDate(): HijriDate {
            val settings = settingsRepository.getSettings().first()
            val today =
                Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            val todayString = today.toString()

            return when (settings.hijriProvider) {
                HijriProvider.MOSQUE_CLOCK_API -> {
                    getHijriDateWithCaching(
                        provider = "MOSQUE_CLOCK_API",
                        gregorianDate = todayString,
                        settings = settings,
                    )
                }
                HijriProvider.AL_ADHAN_API -> {
                    getHijriDateWithCaching(
                        provider = "AL_ADHAN_API",
                        gregorianDate = todayString,
                        settings = settings,
                        region = settings.selectedRegion,
                    )
                }
                HijriProvider.ACJU_DIRECT -> {
                    getHijriDateWithCaching(
                        provider = "ACJU_DIRECT",
                        gregorianDate = todayString,
                        settings = settings,
                    )
                }
                HijriProvider.MANUAL -> {
                    calculateCurrentHijriDate(settings)
                }
            }
        }

        /**
         * Intelligent caching strategy for Hijri dates:
         * 1. Check if we have cached data for today
         * 2. If not, check if we can calculate from recent cached data
         * 3. Only make API calls when necessary (month-end or no recent cache)
         */
        private suspend fun getHijriDateWithCaching(
            provider: String,
            gregorianDate: String,
            settings: AppSettings,
            region: String? = null,
        ): HijriDate {
            val cacheId = generateCacheId(provider, gregorianDate, region)

            // Step 1: Check if we have exact cached data for today
            val cachedToday = hijriDateDao.getHijriDateById(cacheId)
            if (cachedToday != null) {
                Log.d("HijriDateRepository", "📅 Using cached Hijri date for $gregorianDate")
                return HijriDate(cachedToday.hijriDay, cachedToday.hijriMonth, cachedToday.hijriYear)
            }

            // Step 2: Check if we can calculate from recent cached data
            val calculatedDate = tryCalculateFromCache(provider, gregorianDate, region)
            
            if (calculatedDate != null) {
                // Check if we need fresh data due to potential month transition
                val needsFreshData = shouldCheckForMonthTransition(gregorianDate, calculatedDate)
                
                if (!needsFreshData) {
                    Log.d("HijriDateRepository", "📊 Using calculated Hijri date from cache for $gregorianDate")
                    // Cache the calculated result
                    cacheHijriDate(provider, gregorianDate, calculatedDate, region, isCalculated = true)
                    return calculatedDate
                } else {
                    Log.d("HijriDateRepository", "🌙 Day ${calculatedDate.day} - checking for potential month transition")
                }
            } else {
                Log.d("HijriDateRepository", "🌐 No cached data available, fetching fresh Hijri date: $gregorianDate")
            }
            
            return when (provider) {
                "ACJU_DIRECT" -> fetchFromAcjuDirectWithMonthEndLogic(gregorianDate, settings, calculatedDate)
                "MOSQUE_CLOCK_API" -> fetchFromMosqueClockApi(gregorianDate, settings)
                "AL_ADHAN_API" -> fetchFromAlAdhanApi(gregorianDate, settings, region!!)
                else -> calculateCurrentHijriDate(settings)
            }
        }

        /**
         * Try to calculate Hijri date from cached data instead of making API call
         * Only calculate if we have recent data and it's not near month-end
         * This implements intelligent day-by-day calculation from a known cached date
         */
        private suspend fun tryCalculateFromCache(
            provider: String,
            targetDate: String,
            region: String?,
        ): HijriDate? {
            val targetLocalDate = LocalDate.parse(targetDate)
            val today =
                Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date

            // Don't calculate if target date is in the future
            if (targetLocalDate > today) return null

            // Find the most recent cached date before target
            val recentCache =
                hijriDateDao.getLatestHijriDateBefore(provider, targetDate, region)
                    ?: return null

            val cachedDate = LocalDate.parse(recentCache.gregorianDate)
            val daysDifference = targetLocalDate.toEpochDays() - cachedDate.toEpochDays()

            // Only calculate if:
            // 1. Difference is reasonable (< 7 days)
            // 2. We're not near month-end (day < 28) to avoid month transition errors
            if (daysDifference > 7 || recentCache.hijriDay >= 28) {
                Log.d(
                    "HijriDateRepository",
                    "⚠️ Cannot calculate: daysDiff=$daysDifference, hijriDay=${recentCache.hijriDay}",
                )
                return null
            }

            Log.d(
                "HijriDateRepository",
                "🧮 Calculating Hijri date from cache: +$daysDifference days from ${recentCache.gregorianDate}",
            )

            val baseHijriDate = HijriDate(recentCache.hijriDay, recentCache.hijriMonth, recentCache.hijriYear)
            return addDaysToHijriDate(baseHijriDate, daysDifference.toInt())
        }

        private suspend fun fetchFromMosqueClockApi(
            gregorianDate: String,
            settings: AppSettings,
        ): HijriDate {
            return try {
                val response = mosqueClockApi.getTodayBothCalendars()
                if (response.isSuccessful && response.body()?.success == true) {
                    val hijriInfo = response.body()?.data?.hijriDate
                    if (hijriInfo != null) {
                        val hijriDate = HijriDate(hijriInfo.day, hijriInfo.month, hijriInfo.year)
                        // Cache the API result
                        cacheHijriDate("MOSQUE_CLOCK_API", gregorianDate, hijriDate, null, isCalculated = false)
                        return hijriDate
                    }
                }
                // Fallback to manual calculation
                calculateCurrentHijriDate(settings)
            } catch (e: Exception) {
                Log.e("HijriDateRepository", "MosqueClock API error: ${e.message}")
                calculateCurrentHijriDate(settings)
            }
        }

        private suspend fun fetchFromAlAdhanApi(
            gregorianDate: String,
            settings: AppSettings,
            region: String,
        ): HijriDate {
            return try {
                val response =
                    prayerTimesApi.getPrayerTimesByCity(
                        city = region,
                        country = getCountryForRegion(region),
                    )
                if (response.isSuccessful && response.body()?.code == 200) {
                    val hijriData =
                        response
                            .body()
                            ?.data
                            ?.date
                            ?.hijri
                    if (hijriData != null) {
                        val hijriDate =
                            HijriDate(
                                day = hijriData.day.toInt(),
                                month = hijriData.month.number,
                                year = hijriData.year.toInt(),
                            )
                        // Cache the API result
                        cacheHijriDate("AL_ADHAN_API", gregorianDate, hijriDate, region, isCalculated = false)
                        return hijriDate
                    }
                }
                // Fallback to manual calculation
                calculateCurrentHijriDate(settings)
            } catch (e: Exception) {
                Log.e("HijriDateRepository", "Al-Adhan API error: ${e.message}")
                calculateCurrentHijriDate(settings)
            }
        }

        /**
         * Smart logic to determine if we should check for month transition
         * Only checks for fresh data when:
         * 1. We're on day 29 and need to know if month ends today or continues to day 30
         * 2. We're on day 30 and need to check if next month has started
         * 3. We're past the expected end date and might be in a new month
         */
        @Suppress("UNREACHABLE_CODE")
        private suspend fun shouldCheckForMonthTransition(gregorianDate: String, hijriDate: HijriDate): Boolean {
            return try {
                val targetDate = LocalDate.parse(gregorianDate)
                
                // Look for ACJU range data that covers this date
                val acjuEntries = hijriDateDao.getHijriDatesByProvider("ACJU_DIRECT")
                
                for (entry in acjuEntries) {
                    if (entry.hijriMonthStartDate != null && entry.hijriMonthEndDate != null) {
                        val startDate = LocalDate.parse(entry.hijriMonthStartDate)
                        val endDate = LocalDate.parse(entry.hijriMonthEndDate)
                        
                        // Check if target date is within this known range
                        if (targetDate >= startDate && targetDate <= endDate) {
                            // We have reliable range data for this date
                            val hijriDay = hijriDate.day
                            
                            // Only check for fresh data on critical days:
                            when (hijriDay) {
                                29 -> {
                                    // Day 29: Check if this is the last day or if month continues to day 30
                                    val isLastDayOfRange = targetDate == endDate
                                    if (isLastDayOfRange) {
                                        // This is day 29 and it's supposed to be the last day
                                        // But ACJU might update after Maghrib if moon is not sighted
                                        Log.d("HijriDateRepository", "📅 Day 29: Expected last day - checking for moon sighting updates")
                                        return shouldCheckForMoonSightingUpdate(entry)
                                    } else {
                                        Log.d("HijriDateRepository", "📅 Day 29: Month continues to day 30")
                                        return false // Month definitely continues, no need to check
                                    }
                                }
                                30 -> {
                                    // Day 30: This should be the last day, but double-check
                                    Log.d("HijriDateRepository", "📅 Day 30: Confirming month end")
                                    return false // We already know day 30 is the end, no need to fetch
                                }
                                else -> {
                                    // Days 1-28: Safe to use cached data
                                    Log.d("HijriDateRepository", "📅 Day $hijriDay: Safe to use cached data")
                                    return false
                                }
                            }
                        }
                    }
                }
                
                // If we're past the known range, we might be in a new month
                val latestEntry = acjuEntries.maxByOrNull { it.gregorianDate }
                if (latestEntry?.hijriMonthEndDate != null) {
                    val latestEndDate = LocalDate.parse(latestEntry.hijriMonthEndDate)
                    if (targetDate > latestEndDate) {
                        Log.d("HijriDateRepository", "📅 Date $gregorianDate is past known range (${latestEntry.hijriMonthEndDate}), checking for new month")
                        return true
                    }
                }
                
                // Conservative approach: if we don't have range data, check for fresh data
                Log.d("HijriDateRepository", "⚠️ No ACJU range data found for $gregorianDate, checking for fresh data")
                return true
                
            } catch (e: Exception) {
                Log.e("HijriDateRepository", "❌ Error checking month transition: ${e.message}")
                return true // Be conservative - check for fresh data if we can't determine
            }
        }

        /**
         * Check if we should look for moon sighting updates
         * This considers the time of day and when ACJU typically uploads new data
         */
        private fun shouldCheckForMoonSightingUpdate(@Suppress("UNUSED_PARAMETER") entry: HijriDateEntity): Boolean {
            return try {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val currentHour = now.hour
                
                // ACJU typically updates after Maghrib (around 6-8 PM) if moon is not sighted
                // If it's early in the day (before 6 PM), use cached data
                // If it's after 8 PM, check for updates (moon sighting would have happened)
                when {
                    currentHour < 18 -> {
                        Log.d("HijriDateRepository", "🌅 Early in day (${currentHour}:00) - using cached data, will check after Maghrib")
                        false
                    }
                    currentHour >= 20 -> {
                        Log.d("HijriDateRepository", "🌙 After Maghrib/Isha (${currentHour}:00) - checking for moon sighting updates")
                        true
                    }
                    else -> {
                        Log.d("HijriDateRepository", "🌆 Maghrib time (${currentHour}:00) - checking for potential updates")
                        true
                    }
                }
            } catch (e: Exception) {
                Log.e("HijriDateRepository", "❌ Error checking moon sighting timing: ${e.message}")
                true // Be conservative - check for updates if we can't determine time
            }
        }

        /**
         * Check if we're near the end of a Hijri month using ACJU date ranges
         * This checks against the actual end date provided by ACJU, not estimated days
         */
        @Suppress("UNREACHABLE_CODE")
        private suspend fun isNearMonthEnd(gregorianDate: String): Boolean {
            return try {
                val targetDate = LocalDate.parse(gregorianDate)
                
                // Look for any ACJU_DIRECT cache entry that contains this date in its range
                val acjuEntries = hijriDateDao.getHijriDatesByProvider("ACJU_DIRECT")
                
                for (entry in acjuEntries) {
                    if (entry.hijriMonthStartDate != null && entry.hijriMonthEndDate != null) {
                        val startDate = LocalDate.parse(entry.hijriMonthStartDate)
                        val endDate = LocalDate.parse(entry.hijriMonthEndDate)
                        
                        // Check if target date is within this range
                        if (targetDate >= startDate && targetDate <= endDate) {
                            // Calculate days remaining until end of month
                            val daysUntilEnd = (endDate.toEpochDays() - targetDate.toEpochDays()).toInt()
                            val isNearEnd = daysUntilEnd <= 2 // Within 2 days of month end
                            
                            Log.d("HijriDateRepository", "📅 Date $gregorianDate: $daysUntilEnd days until month end (${entry.hijriMonthEndDate})")
                            return isNearEnd
                        }
                    }
                }
                
                // If no range found, assume we might be near month-end to be safe
                Log.d("HijriDateRepository", "⚠️ No ACJU range data found for $gregorianDate, assuming potential month-end")
                return true
                
            } catch (e: Exception) {
                Log.e("HijriDateRepository", "❌ Error checking month-end status: ${e.message}")
                return true // Be conservative - assume near month-end if we can't determine
            }
        }

        /**
         * Enhanced ACJU fetching with month-end logic
         * If we're near month-end, always fetch fresh data to check for new calendar uploads
         */
        @Suppress("UNREACHABLE_CODE")
        private suspend fun fetchFromAcjuDirectWithMonthEndLogic(
            gregorianDate: String,
            settings: AppSettings,
            fallbackDate: HijriDate?
        ): HijriDate {
            return try {
                Log.d("HijriDateRepository", "🔍 Scraping Hijri date from ACJU for: $gregorianDate")
                val date = LocalDate.parse(gregorianDate)
                val hijriInfo = hijriDateScraper.getHijriDateForGregorian(date.year, date.monthNumber, date.dayOfMonth)
                
                if (hijriInfo != null) {
                    // Parse month string to number (hijriMonth is a string like "Rabee`unith Thaani")
                    val monthNumber = parseHijriMonthToNumber(hijriInfo.hijriMonth)
                    val hijriDate = HijriDate(hijriInfo.hijriDay, monthNumber, hijriInfo.hijriYear)
                    Log.d("HijriDateRepository", "✅ ACJU scraping successful: ${hijriDate.day}/${hijriDate.month}/${hijriDate.year}")
                    
                    // Always cache fresh ACJU data with range information
                    cacheHijriDate(
                        provider = "ACJU_DIRECT", 
                        gregorianDate = gregorianDate, 
                        hijriDate = hijriDate, 
                        region = null, 
                        isCalculated = false,
                        hijriMonthStartDate = hijriInfo.monthStartDate,
                        hijriMonthEndDate = hijriInfo.monthEndDate,
                        hijriMonthName = hijriInfo.hijriMonth
                    )
                    
                    // If we're near month-end, log additional info
                    if (isNearMonthEnd(gregorianDate)) {
                        Log.d("HijriDateRepository", "🌙 Fresh ACJU data shows day ${hijriDate.day} - monitoring for month transition")
                    }
                    
                    return hijriDate
                } else {
                    Log.w("HijriDateRepository", "⚠️ ACJU scraping returned null")
                    
                    // If we have fallback calculated date, use it
                    if (fallbackDate != null) {
                        Log.d("HijriDateRepository", "📊 Using fallback calculated date: ${fallbackDate.day}/${fallbackDate.month}/${fallbackDate.year}")
                        cacheHijriDate("ACJU_DIRECT", gregorianDate, fallbackDate, null, isCalculated = true)
                        return fallbackDate
                    } else {
                        Log.w("HijriDateRepository", "⚠️ No fallback available, using manual calculation")
                        return calculateCurrentHijriDate(settings)
                    }
                }
            } catch (e: Exception) {
                Log.e("HijriDateRepository", "❌ ACJU scraping error: ${e.message}")
                
                // If we have fallback calculated date, use it
                if (fallbackDate != null) {
                    Log.d("HijriDateRepository", "📊 Using fallback calculated date due to error: ${fallbackDate.day}/${fallbackDate.month}/${fallbackDate.year}")
                    cacheHijriDate("ACJU_DIRECT", gregorianDate, fallbackDate, null, isCalculated = true)
                    return fallbackDate
                } else {
                    return calculateCurrentHijriDate(settings)
                }
            }
        }

        private suspend fun fetchFromAcjuDirect(
            gregorianDate: String,
            settings: AppSettings,
        ): HijriDate {
            return try {
                Log.d("HijriDateRepository", "🔍 Scraping Hijri date from ACJU for: $gregorianDate")
                val date = LocalDate.parse(gregorianDate)
                val hijriInfo = hijriDateScraper.getHijriDateForGregorian(date.year, date.monthNumber, date.dayOfMonth)
                if (hijriInfo != null) {
                    // Parse month string to number (hijriMonth is a string like "Rabi' al-awwal")
                    val monthNumber = parseHijriMonthToNumber(hijriInfo.hijriMonth)
                    val hijriDate = HijriDate(hijriInfo.hijriDay, monthNumber, hijriInfo.hijriYear)
                    Log.d("HijriDateRepository", "✅ ACJU scraping successful: ${hijriDate.day}/${hijriDate.month}/${hijriDate.year}")
                    // Cache the scraping result
                    cacheHijriDate("ACJU_DIRECT", gregorianDate, hijriDate, null, isCalculated = false)
                    return hijriDate
                } else {
                    Log.w("HijriDateRepository", "⚠️ ACJU scraping returned null, falling back to manual calculation")
                    calculateCurrentHijriDate(settings)
                }
            } catch (e: Exception) {
                Log.e("HijriDateRepository", "❌ ACJU scraping error: ${e.message}")
                calculateCurrentHijriDate(settings)
            }
        }

        private suspend fun cacheHijriDate(
            provider: String,
            gregorianDate: String,
            hijriDate: HijriDate,
            region: String?,
            isCalculated: Boolean,
            // ACJU-specific range data
            hijriMonthStartDate: String? = null,
            hijriMonthEndDate: String? = null,
            hijriMonthName: String? = null,
        ) {
            val entity =
                HijriDateEntity(
                    id = generateCacheId(provider, gregorianDate, region),
                    gregorianDate = gregorianDate,
                    hijriDay = hijriDate.day,
                    hijriMonth = hijriDate.month,
                    hijriYear = hijriDate.year,
                    provider = provider,
                    region = region,
                    isCalculated = isCalculated,
                    hijriMonthStartDate = hijriMonthStartDate,
                    hijriMonthEndDate = hijriMonthEndDate,
                    hijriMonthName = hijriMonthName,
                )
            hijriDateDao.insertHijriDate(entity)
            Log.d(
                "HijriDateRepository",
                "💾 Cached Hijri date: $gregorianDate -> ${hijriDate.day}/${hijriDate.month}/${hijriDate.year}",
            )
        }

        private fun generateCacheId(
            provider: String,
            gregorianDate: String,
            region: String?,
        ): String =
            if (region != null) {
                "${provider}_${region}_$gregorianDate"
            } else {
                "${provider}_$gregorianDate"
            }

        /**
         * Clean up old cached data (older than 30 days)
         */
        suspend fun cleanOldHijriCache() {
            val thirtyDaysAgo = (Clock.System.now() - 30.days).toEpochMilliseconds()
            hijriDateDao.deleteOldHijriDates(thirtyDaysAgo)
            Log.d("HijriDateRepository", "🧹 Cleaned old Hijri cache data")
        }

        private fun getCountryForRegion(region: String): String =
            when (region.lowercase()) {
                "colombo", "kandy", "galle", "jaffna", "negombo" -> "Sri Lanka"
                "chennai", "mumbai", "delhi", "bangalore", "hyderabad" -> "India"
                "dhaka", "chittagong", "sylhet" -> "Bangladesh"
                "karachi", "lahore", "islamabad", "faisalabad" -> "Pakistan"
                "male" -> "Maldives"
                else -> "Sri Lanka" // Default fallback
            }

        fun getCurrentHijriDateFlow(): Flow<NetworkResult<HijriDate>> =
            flow {
                emit(NetworkResult.Loading())

                try {
                    val response = mosqueClockApi.getTodayBothCalendars()
                    if (response.isSuccessful && response.body()?.success == true) {
                        val hijriInfo = response.body()?.data?.hijriDate
                        if (hijriInfo != null) {
                            val hijriDate =
                                HijriDate(
                                    day = hijriInfo.day,
                                    month = hijriInfo.month,
                                    year = hijriInfo.year,
                                )
                            emit(NetworkResult.Success(hijriDate))
                        } else {
                            emit(NetworkResult.Error("No Hijri date data available"))
                        }
                    } else {
                        val errorMessage = response.body()?.message ?: "Failed to fetch Hijri date"
                        emit(NetworkResult.Error(errorMessage, response.code()))
                    }
                } catch (e: Exception) {
                    emit(NetworkResult.Error("Network error: ${e.message}"))
                }
            }

        fun getHijriDateForGregorian(
            year: Int,
            month: Int,
            day: Int,
        ): Flow<NetworkResult<HijriDate>> =
            flow {
                emit(NetworkResult.Loading())

                try {
                    val response = mosqueClockApi.getHijriDate(year, month, day)
                    if (response.isSuccessful && response.body()?.success == true) {
                        val hijriInfo = response.body()?.data
                        if (hijriInfo != null) {
                            val hijriDate =
                                HijriDate(
                                    day = hijriInfo.day,
                                    month = hijriInfo.month,
                                    year = hijriInfo.year,
                                )
                            emit(NetworkResult.Success(hijriDate))
                        } else {
                            emit(NetworkResult.Error("No Hijri date data available"))
                        }
                    } else {
                        val errorMessage = response.body()?.message ?: "Failed to fetch Hijri date"
                        emit(NetworkResult.Error(errorMessage, response.code()))
                    }
                } catch (e: Exception) {
                    emit(NetworkResult.Error("Network error: ${e.message}"))
                }
            }

        private fun calculateCurrentHijriDate(settings: AppSettings): HijriDate {
            val today =
                Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            val lastUpdatedDate = LocalDate.parse(settings.lastUpdatedGregorianDate)

            // Calculate how many days have passed since the manual date was set
            val daysPassed = today.toEpochDays() - lastUpdatedDate.toEpochDays()

            if (daysPassed == 0) {
                // Same day, return the manual date as is
                return HijriDate(
                    day = settings.manualHijriDay,
                    month = settings.manualHijriMonth,
                    year = settings.manualHijriYear,
                )
            }

            // Calculate the new Hijri date by adding the days
            return addDaysToHijriDate(
                HijriDate(
                    day = settings.manualHijriDay,
                    month = settings.manualHijriMonth,
                    year = settings.manualHijriYear,
                ),
                daysPassed.toInt(),
            )
        }

        private fun addDaysToHijriDate(
            hijriDate: HijriDate,
            daysToAdd: Int,
        ): HijriDate {
            var day = hijriDate.day
            var month = hijriDate.month
            var year = hijriDate.year
            var remainingDays = daysToAdd

            while (remainingDays > 0) {
                val daysInCurrentMonth = getDaysInHijriMonth(month, year)
                val daysLeftInMonth = daysInCurrentMonth - day + 1

                if (remainingDays < daysLeftInMonth) {
                    // Can add the remaining days within this month
                    day += remainingDays
                    remainingDays = 0
                } else {
                    // Move to next month
                    remainingDays -= daysLeftInMonth
                    day = 1
                    month++

                    if (month > 12) {
                        month = 1
                        year++
                    }
                }
            }

            return HijriDate(day, month, year)
        }

        private fun getDaysInHijriMonth(
            month: Int,
            year: Int,
        ): Int {
            // Islamic calendar months alternate between 29 and 30 days
            // with some variations based on lunar observations
            // This is a simplified approach - in reality, each month length
            // should be determined by lunar observations or precise calculations

            return when (month) {
                1, 3, 5, 7, 9, 11 -> 30 // Muharram, Rabi al-Awwal, Jumada al-Awwal, Rajab, Ramadan, Dhul Qidah
                2, 4, 6, 8, 10 -> 29 // Safar, Rabi al-Thani, Jumada al-Thani, Shaban, Shawwal
                12 -> if (isLeapYear(year)) 30 else 29 // Dhul Hijjah
                else -> 29
            }
        }

        private fun isLeapYear(hijriYear: Int): Boolean {
            // In the Islamic calendar, there are 11 leap years in every 30-year cycle
            // Years 2, 5, 7, 10, 13, 16, 18, 21, 24, 26, and 29 of each cycle are leap years
            val yearInCycle = hijriYear % 30
            return yearInCycle in listOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29)
        }

        /**
         * Convert Hijri month name to number (1-12)
         */
        private fun parseHijriMonthToNumber(monthName: String): Int {
            val normalizedName = monthName.lowercase()
                .replace("'", "")
                .replace("`", "")
                .replace("-", "")
                .replace(" ", "")
                .replace("\\", "")
            
            Log.d("HijriDateRepository", "🔍 Parsing month: '$monthName' → normalized: '$normalizedName'")
            
            return when (normalizedName) {
                "muharram" -> 1
                "safar" -> 2
                // Rabi' al-Awwal (3rd month) - ACJU format: "Rabi'ul Awwal"
                "rabialawwal", "rabiulawwal", "rabialawal" -> 3
                // Rabi' al-Thani (4th month) - ACJU format: "Rabee`unith Thaani"
                "rabialthani", "rabiulthani", "rabiathani", "rabeeuniththaani" -> 4
                // Jumada al-Awwal (5th month) - ACJU format: "Jumaadal Oola"
                "jumadaalawwal", "jumadaulawwal", "jumadalawal", "jumaaadaloola" -> 5
                // Jumada al-Thani (6th month) - ACJU format: "Jumaadal Aakhirah"
                "jumadaalthani", "jumadaulthani", "jumadathani", "jumaaadalaakhirah" -> 6
                "rajab" -> 7
                // Sha'ban - ACJU format: "Sha\\\'baan"
                "shaban", "shaaban", "shabaan" -> 8
                // Ramadan - ACJU format: "Ramadaan"
                "ramadan", "ramadhan", "ramadaan" -> 9
                // Shawwal - ACJU format: "Shawwaal"
                "shawwal", "shawwaal" -> 10
                // Dhul Qi'dah - ACJU format: "Dhul Qa\'dah"
                "dhulqidah", "dhulqadah", "zulqaidah" -> 11
                // Dhul Hijjah - ACJU format: "Dhul Hijjah"
                "dhulhijjah", "dhulhajjah", "zulhijjah" -> 12
                else -> {
                    Log.w("HijriDateRepository", "❌ Unknown Hijri month: '$monthName' (normalized: '$normalizedName'), defaulting to 1")
                    1
                }
            }.also { monthNumber ->
                Log.d("HijriDateRepository", "✅ Parsed '$monthName' → Month $monthNumber")
            }
        }
    }
