package com.mosque.prayerclock.data.repository

import android.util.Log
import com.mosque.prayerclock.data.cache.PrayerTimesCacheInvalidator
import com.mosque.prayerclock.data.database.PrayerTimesDao
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.Mazhab
import com.mosque.prayerclock.data.model.PrayerServiceType
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.data.network.AlAdhanData
import com.mosque.prayerclock.data.network.NetworkResult
import com.mosque.prayerclock.data.network.PrayerTimesApi
import com.mosque.prayerclock.data.scraping.DirectScrapingService
import com.mosque.prayerclock.utils.TimeUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

@Singleton
class PrayerTimesRepository
    @Inject
    constructor(
        private val api: PrayerTimesApi, // Keep for backward compatibility
        private val directScrapingService: DirectScrapingService, // Direct PDF scraping
        private val dao: PrayerTimesDao,
        private val settingsRepository: SettingsRepository,
    ) : PrayerTimesCacheInvalidator {
        // Smart caching to prevent unnecessary API calls
        // Tracks what was fetched for today to avoid refetching on non-prayer-related settings changes
        // Repository-level in-memory cache for instant access (no DB queries)
        @Volatile private var cachedPrayerTimes: PrayerTimes? = null

        @Volatile private var cacheDate: String? = null

        @Volatile private var cacheProviderKey: String? = null

        // "MANUAL", "AL_ADHAN_API:Colombo:SHAFI", "ACJU_DIRECT:1"
        @Volatile private var isCurrentlyFetching: Boolean = false

        /**
         * Get cached prayer times instantly without any DB queries
         * Returns null if cache is invalid (different date/provider)
         */
        suspend fun getCachedPrayerTimes(): PrayerTimes? {
            val today =
                Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                    .toString()
            val currentProviderKey = getCurrentProviderKey()

            return if (cacheDate == today && cacheProviderKey == currentProviderKey) {
                cachedPrayerTimes?.forMazhab(settingsRepository.getSettings().first().mazhab)
            } else {
                null
            }
        }

        /**
         * Generate provider-specific cache key
         */
        private suspend fun getCurrentProviderKey(): String {
            val settings = settingsRepository.getSettings().first()
            return when (settings.prayerServiceType) {
                PrayerServiceType.MANUAL -> "MANUAL"
                PrayerServiceType.AL_ADHAN_API -> "AL_ADHAN_API:${settings.selectedRegion}:${settings.mazhab.name}"
                PrayerServiceType.ACJU_DIRECT -> "ACJU_DIRECT:${settings.selectedZone}"
            }
        }

        /**
         * Generate composite database ID: "date_providerKey"
         */
        private fun generateDatabaseId(
            date: String,
            providerKey: String,
        ): String = "${date}_$providerKey"

        /**
         * Get prayer times from database using provider-specific lookup
         */
        private suspend fun getPrayerTimesFromDatabase(
            date: String,
            providerKey: String,
        ): PrayerTimes? = dao.getPrayerTimesByDateAndProvider(date, providerKey)

        /**
         * Manually invalidate the prayer times cache
         * Call this when you want to force a fresh fetch regardless of cache state
         */
        override fun invalidatePrayerTimesCache() {
            Log.d("PrayerTimesRepository", "Prayer times cache manually invalidated")
            cachedPrayerTimes = null
            cacheDate = null
            cacheProviderKey = null
        }

        /**
         * Check if we have valid cached data for the current settings
         */
        suspend fun hasCachedPrayerTimesForCurrentSettings(): Boolean = getCachedPrayerTimes() != null

        // Main method that chooses service based on settings with intelligent caching
        fun getTodayPrayerTimesFromSettings(): Flow<NetworkResult<PrayerTimes>> =
            flow<NetworkResult<PrayerTimes>> {
                Log.d(
                    "PrayerTimesRepository",
                    "🚀 getTodayPrayerTimesFromSettings() called - checking repository cache",
                )
                emit(NetworkResult.Loading())

                try {
                    // First check: Repository-level instant cache (no DB queries)
                    val cachedData = getCachedPrayerTimes()
                    if (cachedData != null) {
                        emit(NetworkResult.Success(cachedData))
                        return@flow
                    }

                    val settings = settingsRepository.getSettings().first()
                    val today =
                        Clock.System
                            .now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
                            .toString()
                    val currentProviderKey = getCurrentProviderKey()

                    // Check if another thread is already fetching the same data
                    if (isCurrentlyFetching) {
                        while (isCurrentlyFetching) {
                            kotlinx.coroutines.delay(100) // Wait 100ms
                            val freshCache = getCachedPrayerTimes()
                            if (freshCache != null) {
                                emit(NetworkResult.Success(freshCache))
                                return@flow
                            }
                        }
                    }

                    // Set fetching flag
                    isCurrentlyFetching = true

                    // Fetch data based on service type and emit results
                    when (settings.prayerServiceType) {
                        PrayerServiceType.ACJU_DIRECT -> {
                            Log.d(
                                "PrayerTimesRepository",
                                "🔄 Using ACJU DIRECT SCRAPING for prayer times (Zone: ${settings.selectedZone})",
                            )
                            // Use direct PDF scraping
                            getTodayPrayerTimesByDirectScraping(settings.selectedZone).collect { result ->
                                emit(result)
                                // Cache successful results
                                if (result is NetworkResult.Success) {
                                    Log.d("PrayerTimesRepository", "💾 Caching ACJU_DIRECT data")
                                    Log.d(
                                        "PrayerTimesRepository",
                                        "📋 Original scraping data: date='${result.data.date}', fajr='${result.data.fajrAzan}', dhuhr='${result.data.dhuhrAzan}'",
                                    )

                                    // FIX: Normalize date format and add provider context
                                    val databaseId = generateDatabaseId(today, currentProviderKey)
                                    val normalizedPrayerTimes =
                                        result.data.copy(
                                            id = databaseId,
                                            date = today,
                                            providerKey = currentProviderKey,
                                        )
                                    Log.d(
                                        "PrayerTimesRepository",
                                        "🔧 Normalized: '${result.data.date}' -> '$today', Provider: '$currentProviderKey', ID: '$databaseId'",
                                    )

                                    // Update repository cache for instant access
                                    cachedPrayerTimes = normalizedPrayerTimes
                                    cacheDate = today
                                    cacheProviderKey = currentProviderKey
                                    Log.d(
                                        "PrayerTimesRepository",
                                        "⚡ Repository cache updated - instant access enabled",
                                    )

                                    // Save scraping data to database for persistence across app restarts (provider-specific)
                                    try {
                                        dao.insertPrayerTimes(normalizedPrayerTimes)
                                        Log.d("PrayerTimesRepository", "💾 ACJU_DIRECT data saved to database")
                                    } catch (e: Exception) {
                                        Log.e(
                                            "PrayerTimesRepository",
                                            "❌ Failed to save ACJU_DIRECT data to database",
                                            e,
                                        )
                                    }
                                }
                            }
                        }
                        PrayerServiceType.AL_ADHAN_API -> {
                            // Use Al-Adhan API with selected region
                            getTodayPrayerTimesByRegion(settings.selectedRegion).collect { result ->
                                emit(result)
                                // Cache successful results
                                if (result is NetworkResult.Success) {
                                    Log.d("PrayerTimesRepository", "💾 Caching AL_ADHAN_API data")
                                    Log.d(
                                        "PrayerTimesRepository",
                                        "📋 Original API data: date='${result.data.date}', fajr='${result.data.fajrAzan}', dhuhr='${result.data.dhuhrAzan}'",
                                    )

                                    // FIX: Normalize date format and add provider context
                                    val databaseId = generateDatabaseId(today, currentProviderKey)
                                    val normalizedPrayerTimes =
                                        result.data.copy(
                                            id = databaseId,
                                            date = today,
                                            providerKey = currentProviderKey,
                                        )
                                    Log.d(
                                        "PrayerTimesRepository",
                                        "🔧 Normalized: '${result.data.date}' -> '$today', Provider: '$currentProviderKey', ID: '$databaseId'",
                                    )

                                    // Update repository cache for instant access
                                    cachedPrayerTimes = normalizedPrayerTimes
                                    cacheDate = today
                                    cacheProviderKey = currentProviderKey
                                    Log.d(
                                        "PrayerTimesRepository",
                                        "⚡ Repository cache updated - instant access enabled",
                                    )

                                    // Save API data to database for persistence across app restarts (provider-specific)
                                    try {
                                        dao.insertPrayerTimes(normalizedPrayerTimes)
                                        Log.d(
                                            "PrayerTimesRepository",
                                            "💾 AL_ADHAN_API data saved to database with provider context",
                                        )
                                    } catch (e: Exception) {
                                        Log.e("PrayerTimesRepository", "❌ Database save failed: ${e.message}", e)
                                    }
                                }
                            }
                        }
                        PrayerServiceType.MANUAL -> {
                            // Create manual prayer times from settings
                            val basePrayerTimes = createManualPrayerTimes(settings, today)
                            val databaseId = generateDatabaseId(today, currentProviderKey)
                            val manualPrayerTimes =
                                basePrayerTimes.copy(
                                    id = databaseId,
                                    providerKey = null, // Manual entries don't have provider context
                                )
                            Log.d("PrayerTimesRepository", "⚡ Caching MANUAL data (memory only - no database)")
                            Log.d(
                                "PrayerTimesRepository",
                                "📋 Manual data: date='${manualPrayerTimes.date}', fajr='${manualPrayerTimes.fajrAzan}', dhuhr='${manualPrayerTimes.dhuhrAzan}'",
                            )

                            // Update repository cache for instant access (MANUAL entries stay in memory only)
                            cachedPrayerTimes = manualPrayerTimes
                            cacheDate = today
                            cacheProviderKey = currentProviderKey
                            Log.d(
                                "PrayerTimesRepository",
                                "⚡ Repository cache updated - MANUAL data cached in memory only",
                            )

                            emit(NetworkResult.Success(manualPrayerTimes))
                        }
                    }

                    // Clear fetching flag
                    isCurrentlyFetching = false
                    Log.d("PrayerTimesRepository", "Prayer times fetch completed - repository cache updated")
                    Unit
                } catch (e: Exception) {
                    Log.e("PrayerTimesRepository", "Error in getTodayPrayerTimesFromSettings", e)
                    isCurrentlyFetching = false // Clear flag on error
                    emit(NetworkResult.Error("Failed to get prayer times: ${e.message}"))
                }
            }.withSelectedMazhab()

        private fun Flow<NetworkResult<PrayerTimes>>.withSelectedMazhab(): Flow<NetworkResult<PrayerTimes>> = map { result ->
            if (result is NetworkResult.Success) {
                val settings = settingsRepository.getSettings().first()
                if (settings.prayerServiceType == PrayerServiceType.ACJU_DIRECT &&
                    settings.mazhab == Mazhab.HANAFI && result.data.asrHanafiAzan == null
                ) {
                    NetworkResult.Error("Hanafi Asr is unavailable in this timetable. Try a newer ACJU timetable or select Shafi.")
                } else {
                    NetworkResult.Success(result.data.forMazhab(settings.mazhab))
                }
            } else result
        }

        // Method to get tomorrow's prayer times based on settings
        fun getTomorrowPrayerTimesFromSettings(): Flow<NetworkResult<PrayerTimes>> =
            flow<NetworkResult<PrayerTimes>> {
                emit(NetworkResult.Loading())

                val settings = settingsRepository.getSettings().first()
                val tomorrow =
                    Clock.System.now().plus(
                        1,
                        DateTimeUnit.DAY,
                        TimeZone.currentSystemDefault(),
                    )
                val tomorrowDate =
                    tomorrow
                        .toLocalDateTime(
                            TimeZone.currentSystemDefault(),
                        ).date
                        .toString()

                when (settings.prayerServiceType) {
                    PrayerServiceType.ACJU_DIRECT -> {
                        // Use direct scraping for tomorrow
                        emitAll(getTomorrowPrayerTimesByDirectScraping(settings.selectedZone))
                    }
                    PrayerServiceType.AL_ADHAN_API -> {
                        // Use Al-Adhan API with selected region for tomorrow
                        emitAll(getTomorrowPrayerTimesByRegion(settings.selectedRegion))
                    }
                    PrayerServiceType.MANUAL -> {
                        // Create manual prayer times from settings (same for tomorrow in manual mode)
                        val manualPrayerTimes = createManualPrayerTimes(settings, tomorrowDate)
                        emit(NetworkResult.Success(manualPrayerTimes))
                    }
                }
            }.withSelectedMazhab()

        fun getTodayPrayerTimesByRegion(region: String): Flow<NetworkResult<PrayerTimes>> =
            flow {
                emit(NetworkResult.Loading())

                val today =
                    Clock.System
                        .now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                        .toString()

                val mazhab = settingsRepository.getSettings().first().mazhab
                val providerKey = "AL_ADHAN_API:$region:${mazhab.name}"
                val cachedPrayerTimes = dao.getPrayerTimesByDateAndProvider(today, providerKey)
                if (cachedPrayerTimes != null) {
                    emit(NetworkResult.Success(cachedPrayerTimes))
                    return@flow
                }

                try {
                    val response = api.getPrayerTimesByCity(region, getCountryForRegion(region), school = mazhab.alAdhanSchool)
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val alAdhanData = response.body()?.data
                        if (alAdhanData != null) {
                            val prayerTimes = convertAlAdhanToPrayerTimes(alAdhanData).copy(
                                id = generateDatabaseId(today, providerKey), date = today, providerKey = providerKey,
                            )
                            dao.insertPrayerTimes(prayerTimes)
                            emit(NetworkResult.Success(prayerTimes))
                        } else {
                            emit(NetworkResult.Error("No prayer times data available"))
                        }
                    } else {
                        val errorMessage = response.body()?.status ?: "Failed to fetch prayer times"
                        emit(NetworkResult.Error(errorMessage, response.code()))
                    }
                } catch (e: Exception) {
                    emit(NetworkResult.Error("Network error: ${e.message}"))
                }
            }

        fun getTomorrowPrayerTimesByRegion(region: String): Flow<NetworkResult<PrayerTimes>> =
            flow {
                emit(NetworkResult.Loading())

                val tomorrow =
                    Clock.System.now().plus(
                        1,
                        DateTimeUnit.DAY,
                        TimeZone.currentSystemDefault(),
                    )
                val tomorrowDate =
                    tomorrow
                        .toLocalDateTime(
                            TimeZone.currentSystemDefault(),
                        ).date
                        .toString()

                // Check cache first for tomorrow's date
                val mazhab = settingsRepository.getSettings().first().mazhab
                val providerKey = "AL_ADHAN_API:$region:${mazhab.name}"
                val cachedPrayerTimes = dao.getPrayerTimesByDateAndProvider(tomorrowDate, providerKey)
                if (cachedPrayerTimes != null) {
                    emit(NetworkResult.Success(cachedPrayerTimes))
                    return@flow
                }

                try {
                    // Format tomorrow's date for Al-Adhan API (dd-MM-yyyy format)
                    val tomorrowLocalDate =
                        tomorrow
                            .toLocalDateTime(
                                TimeZone.currentSystemDefault(),
                            ).date
                    val tomorrowFormatted = "${tomorrowLocalDate.dayOfMonth.toString().padStart(
                        2,
                        '0',
                    )}-${tomorrowLocalDate.monthNumber.toString().padStart(2, '0')}-${tomorrowLocalDate.year}"

                    val response = api.getPrayerTimesByCity(region, getCountryForRegion(region), tomorrowFormatted, school = mazhab.alAdhanSchool)
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val alAdhanData = response.body()?.data
                        if (alAdhanData != null) {
                            val prayerTimes = convertAlAdhanToPrayerTimes(alAdhanData).copy(
                                id = generateDatabaseId(tomorrowDate, providerKey), date = tomorrowDate, providerKey = providerKey,
                            )
                            dao.insertPrayerTimes(prayerTimes)
                            emit(NetworkResult.Success(prayerTimes))
                        } else {
                            emit(NetworkResult.Error("No prayer times data available"))
                        }
                    } else {
                        val errorMessage = response.body()?.status ?: "Failed to fetch prayer times"
                        emit(NetworkResult.Error(errorMessage, response.code()))
                    }
                } catch (e: Exception) {
                    emit(NetworkResult.Error("Network error: ${e.message}"))
                }
            }

        fun getTodayPrayerTimes(
            city: String,
            country: String,
        ): Flow<NetworkResult<PrayerTimes>> =
            flow {
                emit(NetworkResult.Loading())

                val today =
                    Clock.System
                        .now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                        .toString()

                val cachedPrayerTimes = dao.getPrayerTimesByDate(today)
                if (cachedPrayerTimes != null) {
                    emit(NetworkResult.Success(cachedPrayerTimes))
                    return@flow
                }

                try {
                    val response = api.getPrayerTimesByCity(city, country)
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val alAdhanData = response.body()?.data
                        if (alAdhanData != null) {
                            val prayerTimes = convertAlAdhanToPrayerTimes(alAdhanData)
                            dao.insertPrayerTimes(prayerTimes)
                            emit(NetworkResult.Success(prayerTimes))
                        } else {
                            emit(NetworkResult.Error("No prayer times data available"))
                        }
                    } else {
                        val errorMessage = response.body()?.status ?: "Failed to fetch prayer times"
                        emit(NetworkResult.Error(errorMessage, response.code()))
                    }
                } catch (e: Exception) {
                    emit(NetworkResult.Error("Network error: ${e.message}"))
                }
            }

        fun getPrayerTimesByLocation(
            latitude: Double,
            longitude: Double,
        ): Flow<NetworkResult<PrayerTimes>> =
            flow {
                emit(NetworkResult.Loading())

                try {
                    val response = api.getPrayerTimes(latitude, longitude)
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val alAdhanData = response.body()?.data
                        if (alAdhanData != null) {
                            val prayerTimes = convertAlAdhanToPrayerTimes(alAdhanData)
                            dao.insertPrayerTimes(prayerTimes)
                            emit(NetworkResult.Success(prayerTimes))
                        } else {
                            emit(NetworkResult.Error("No prayer times data available"))
                        }
                    } else {
                        val errorMessage = response.body()?.status ?: "Failed to fetch prayer times"
                        emit(NetworkResult.Error(errorMessage, response.code()))
                    }
                } catch (e: Exception) {
                    emit(NetworkResult.Error("Network error: ${e.message}"))
                }
            }

        fun getPrayerTimesByDateFlow(date: String): Flow<PrayerTimes?> = dao.getPrayerTimesByDateFlow(date)

        suspend fun cleanOldPrayerTimes() {
            val thirtyDaysAgo =
                (Clock.System.now() - 30.days)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                    .toString()
            dao.deleteOldPrayerTimes(thirtyDaysAgo)
        }

        // ACJU failures are surfaced directly; no backend or provider substitution.
        fun getTodayPrayerTimesByDirectScraping(zone: Int): Flow<NetworkResult<PrayerTimes>> = flow {
            emit(NetworkResult.Loading())
            try {
                val times = directScrapingService.getTodayPrayerTimes(zone, false)
                emit(if (times != null) NetworkResult.Success(times)
                    else NetworkResult.Error("No ACJU prayer times available for today"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emit(NetworkResult.Error("Unable to load ACJU prayer times: ${e.message}"))
            }
        }

        fun getTomorrowPrayerTimesByDirectScraping(zone: Int): Flow<NetworkResult<PrayerTimes>> = flow {
            emit(NetworkResult.Loading())
            try {
                val times = directScrapingService.getTomorrowPrayerTimes(zone, false)
                emit(if (times != null) NetworkResult.Success(times)
                    else NetworkResult.Error("No ACJU prayer times available for tomorrow"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emit(NetworkResult.Error("Unable to load ACJU prayer times: ${e.message}"))
            }
        }

        private fun convertAlAdhanToPrayerTimes(alAdhanData: AlAdhanData): PrayerTimes {
            val timings = alAdhanData.timings
            val date = alAdhanData.date.gregorian.date
            val hijriDate = alAdhanData.date.hijri.date

            // Note: Iqamah times are no longer stored, they will be calculated dynamically from settings
            return PrayerTimes(
                id = "", // Will be set later with proper composite ID
                date = date,
                providerKey = null, // Will be set later with provider context
                fajrAzan = formatTime(timings.fajr),
                dhuhrAzan = formatTime(timings.dhuhr),
                asrAzan = formatTime(timings.asr),
                maghribAzan = formatTime(timings.maghrib),
                ishaAzan = formatTime(timings.isha),
                sunrise = formatTime(timings.sunrise),
                hijriDate = hijriDate,
                location = "${alAdhanData.meta.latitude}, ${alAdhanData.meta.longitude}",
            )
        }

        private fun formatTime(time: String): String {
            // AlAdhan returns time in HH:MM format, sometimes with timezone info
            return time.split(" ").first() // Remove timezone if present
        }

        private fun getCountryForRegion(region: String): String =
            when (region) {
                "Colombo", "Kandy", "Galle", "Jaffna" -> "Sri Lanka"
                "Kuala Lumpur", "Penang" -> "Malaysia"
                "Singapore" -> "Singapore"
                "Jakarta" -> "Indonesia"
                "Chennai", "Mumbai", "Delhi" -> "India"
                "Dubai" -> "UAE"
                "Riyadh" -> "Saudi Arabia"
                "Doha" -> "Qatar"
                "London" -> "UK"
                "New York" -> "USA"
                "Toronto" -> "Canada"
                else -> "Sri Lanka" // Default fallback
            }

        private fun getMonthName(month: Int): String =
            when (month) {
                1 -> "January"
                2 -> "February"
                3 -> "March"
                4 -> "April"
                5 -> "May"
                6 -> "June"
                7 -> "July"
                8 -> "August"
                9 -> "September"
                10 -> "October"
                11 -> "November"
                12 -> "December"
                else -> "January"
            }

        /**
         * Create manual prayer times from settings
         */
        private fun createManualPrayerTimes(
            settings: AppSettings,
            date: String? = null,
        ): PrayerTimes {
            // Note: Iqamah times are no longer stored, they will be calculated dynamically from settings
            val currentDate =
                date ?: Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                    .toString()

            return PrayerTimes(
                id = "", // Will be set later with proper composite ID
                date = currentDate,
                providerKey = null, // Manual entries don't have provider context
                fajrAzan = settings.manualFajrAzan,
                dhuhrAzan = settings.manualDhuhrAzan,
                asrAzan = settings.manualAsrAzan,
                maghribAzan = settings.manualMaghribAzan,
                ishaAzan = settings.manualIshaAzan,
                sunrise = "06:00", // Default sunrise time for manual mode
                hijriDate = null,
                location = "Manual",
            )
        }

        /**
         * Prefetch all remaining months of the year for offline use
         * This is only applicable when using ACJU.
         */
        suspend fun prefetchRemainingYearPrayerTimes(zone: Int): DirectScrapingService.PrefetchResult {
            Log.d("PrayerTimesRepository", "🔄 Starting prefetch for zone $zone")
            return directScrapingService.prefetchRemainingYearPrayerTimes(zone)
        }

        /**
         * Check offline cache status
         */
        suspend fun checkOfflineCacheStatus(zone: Int): DirectScrapingService.CacheStatus {
            return directScrapingService.checkCacheStatus(zone)
        }

        /**
         * Clear all cached prayer times
         */
        suspend fun clearAllPrayerTimesCache() {
            val count = dao.getAllPrayerTimesCount()
            dao.deleteAllPrayerTimes()
            // Clear in-memory cache as well
            cachedPrayerTimes = null
            cacheDate = null
            cacheProviderKey = null
            Log.d("PrayerTimesRepository", "🧹 Cleared all prayer times cache ($count entries)")
        }

        /**
         * Get total count of cached prayer times
         */
        suspend fun getPrayerTimesCacheCount(): Int = dao.getAllPrayerTimesCount()
    }
