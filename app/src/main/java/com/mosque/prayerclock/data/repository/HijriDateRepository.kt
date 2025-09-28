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
import kotlin.time.Duration.Companion.days
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HijriDateRepository
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val mosqueClockApi: MosqueClockApi,
        private val prayerTimesApi: com.mosque.prayerclock.data.network.PrayerTimesApi,
        private val hijriDateDao: HijriDateDao,
    ) {
        data class HijriDate(
            val day: Int,
            val month: Int,
            val year: Int,
        )

        suspend fun getCurrentHijriDate(): HijriDate {
            val settings = settingsRepository.getSettings().first()
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val todayString = today.toString()

            return when (settings.hijriProvider) {
                HijriProvider.MOSQUE_CLOCK_API -> {
                    getHijriDateWithCaching(
                        provider = "MOSQUE_CLOCK_API",
                        gregorianDate = todayString,
                        settings = settings
                    )
                }
                HijriProvider.AL_ADHAN_API -> {
                    getHijriDateWithCaching(
                        provider = "AL_ADHAN_API",
                        gregorianDate = todayString,
                        settings = settings,
                        region = settings.selectedRegion
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
            region: String? = null
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
                // Cache the calculated result
                cacheHijriDate(provider, gregorianDate, calculatedDate, region, isCalculated = true)
                return calculatedDate
            }

            // Step 3: Need to make API call
            Log.d("HijriDateRepository", "🌐 Making API call for Hijri date: $gregorianDate")
            return when (provider) {
                "MOSQUE_CLOCK_API" -> fetchFromMosqueClockApi(gregorianDate, settings)
                "AL_ADHAN_API" -> fetchFromAlAdhanApi(gregorianDate, settings, region!!)
                else -> calculateCurrentHijriDate(settings)
            }
        }

        /**
         * Try to calculate Hijri date from cached data instead of making API call
         * Only calculate if we have recent data and it's not near month-end
         */
        private suspend fun tryCalculateFromCache(
            provider: String,
            targetDate: String,
            region: String?
        ): HijriDate? {
            val targetLocalDate = LocalDate.parse(targetDate)
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            
            // Don't calculate if target date is in the future
            if (targetLocalDate > today) return null

            // Find the most recent cached date before target
            val recentCache = hijriDateDao.getLatestHijriDateBefore(provider, targetDate, region)
                ?: return null

            val cachedDate = LocalDate.parse(recentCache.gregorianDate)
            val daysDifference = targetLocalDate.toEpochDays() - cachedDate.toEpochDays()

            // Only calculate if:
            // 1. Difference is reasonable (< 7 days)
            // 2. We're not near month-end (day < 28) to avoid month transition errors
            if (daysDifference > 7 || recentCache.hijriDay >= 28) {
                Log.d("HijriDateRepository", "⚠️ Cannot calculate: daysDiff=$daysDifference, hijriDay=${recentCache.hijriDay}")
                return null
            }

            Log.d("HijriDateRepository", "🧮 Calculating Hijri date from cache: +$daysDifference days from ${recentCache.gregorianDate}")
            
            val baseHijriDate = HijriDate(recentCache.hijriDay, recentCache.hijriMonth, recentCache.hijriYear)
            return addDaysToHijriDate(baseHijriDate, daysDifference.toInt())
        }

        private suspend fun fetchFromMosqueClockApi(gregorianDate: String, settings: AppSettings): HijriDate {
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

        private suspend fun fetchFromAlAdhanApi(gregorianDate: String, settings: AppSettings, region: String): HijriDate {
            return try {
                val response = prayerTimesApi.getPrayerTimesByCity(
                    city = region,
                    country = getCountryForRegion(region)
                )
                if (response.isSuccessful && response.body()?.code == 200) {
                    val hijriData = response.body()?.data?.date?.hijri
                    if (hijriData != null) {
                        val hijriDate = HijriDate(
                            day = hijriData.day.toInt(),
                            month = hijriData.month.number,
                            year = hijriData.year.toInt()
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

        private suspend fun cacheHijriDate(
            provider: String,
            gregorianDate: String,
            hijriDate: HijriDate,
            region: String?,
            isCalculated: Boolean
        ) {
            val entity = HijriDateEntity(
                id = generateCacheId(provider, gregorianDate, region),
                gregorianDate = gregorianDate,
                hijriDay = hijriDate.day,
                hijriMonth = hijriDate.month,
                hijriYear = hijriDate.year,
                provider = provider,
                region = region,
                isCalculated = isCalculated
            )
            hijriDateDao.insertHijriDate(entity)
            Log.d("HijriDateRepository", "💾 Cached Hijri date: $gregorianDate -> ${hijriDate.day}/${hijriDate.month}/${hijriDate.year}")
        }

        private fun generateCacheId(provider: String, gregorianDate: String, region: String?): String {
            return if (region != null) {
                "${provider}_${region}_$gregorianDate"
            } else {
                "${provider}_$gregorianDate"
            }
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

    }
