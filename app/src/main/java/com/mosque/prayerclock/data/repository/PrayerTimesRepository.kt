package com.mosque.prayerclock.data.repository

import com.mosque.prayerclock.data.database.PrayerTimesDao
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.data.model.PrayerServiceType
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.network.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerTimesRepository @Inject constructor(
    private val api: PrayerTimesApi, // Keep for backward compatibility
    private val mosqueClockApi: MosqueClockApi, // New MosqueClock API
    private val dao: PrayerTimesDao,
    private val settingsRepository: SettingsRepository
) {
    
    // Main method that chooses service based on settings
    fun getTodayPrayerTimesFromSettings(): Flow<NetworkResult<PrayerTimes>> = flow {
        emit(NetworkResult.Loading())
        
        val settings = settingsRepository.getSettings().first()
        
        when (settings.prayerServiceType) {
            PrayerServiceType.MOSQUE_CLOCK_API -> {
                // Use MosqueClock API with selected zone
                emitAll(getTodayPrayerTimesByZone(settings.selectedZone))
            }
            PrayerServiceType.AL_ADHAN_API -> {
                // Use Al-Adhan API with selected region
                emitAll(getTodayPrayerTimesByRegion(settings.selectedRegion))
            }
            PrayerServiceType.MANUAL -> {
                // Manual handled in MainViewModel; return error to indicate manual mode here
                emit(NetworkResult.Error("Manual prayer times selected"))
            }
        }
    }
    
    fun getTodayPrayerTimesByRegion(region: String): Flow<NetworkResult<PrayerTimes>> = flow {
        emit(NetworkResult.Loading())
        
        val today = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString()
        
        val cachedPrayerTimes = dao.getPrayerTimesByDate(today)
        if (cachedPrayerTimes != null && cachedPrayerTimes.location?.contains(region) == true) {
            emit(NetworkResult.Success(cachedPrayerTimes))
            return@flow
        }
        
        try {
            val response = api.getPrayerTimesByCity(region, getCountryForRegion(region))
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
    
    fun getTodayPrayerTimes(city: String, country: String): Flow<NetworkResult<PrayerTimes>> = flow {
        emit(NetworkResult.Loading())
        
        val today = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString()
        
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
    
    fun getPrayerTimesByLocation(latitude: Double, longitude: Double): Flow<NetworkResult<PrayerTimes>> = flow {
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
        val thirtyDaysAgo = Clock.System.now()
            .minus(30, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
        dao.deleteOldPrayerTimes(thirtyDaysAgo)
    }
    
    // New methods using MosqueClock API for Sri Lankan prayer times
    fun getTodayPrayerTimesByZone(zone: Int): Flow<NetworkResult<PrayerTimes>> = flow {
        emit(NetworkResult.Loading())
        
        val today = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString()
        
        // Check cache first
        val cachedPrayerTimes = dao.getPrayerTimesByDate(today)
        if (cachedPrayerTimes != null && cachedPrayerTimes.location?.contains("Zone $zone") == true) {
            emit(NetworkResult.Success(cachedPrayerTimes))
            return@flow
        }
        
        try {
            val response = mosqueClockApi.getTodayPrayerTimes(zone, false)
            if (response.isSuccessful && response.body() != null) {
                val backendData = response.body()!!
                val prayerTimes = backendData.toPrayerTimes()
                dao.insertPrayerTimes(prayerTimes)
                emit(NetworkResult.Success(prayerTimes))
            } else {
                emit(NetworkResult.Error("Failed to fetch prayer times", response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }
    
    fun getPrayerTimesByZoneAndDate(zone: Int, date: String): Flow<NetworkResult<PrayerTimes>> = flow {
        emit(NetworkResult.Loading())
        
        // Check cache first
        val cachedPrayerTimes = dao.getPrayerTimesByDate(date)
        if (cachedPrayerTimes != null && cachedPrayerTimes.location?.contains("Zone $zone") == true) {
            emit(NetworkResult.Success(cachedPrayerTimes))
            return@flow
        }
        
        try {
            val response = mosqueClockApi.getPrayerTimesByZone(zone, date)
            if (response.isSuccessful && response.body() != null) {
                val mosqueDataList = response.body()?.data
                if (mosqueDataList != null && mosqueDataList.isNotEmpty()) {
                    // Take the first prayer time entry (should be for the requested date)
                    val prayerTimes = mosqueDataList.first().toPrayerTimes()
                    dao.insertPrayerTimes(prayerTimes)
                    emit(NetworkResult.Success(prayerTimes))
                } else {
                    emit(NetworkResult.Error("No prayer times data available"))
                }
            } else {
                emit(NetworkResult.Error("Failed to fetch prayer times", response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }
    
    fun getMonthPrayerTimes(zone: Int, year: Int, month: Int): Flow<NetworkResult<List<PrayerTimes>>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            // Convert month number to month name for backend API
            val monthName = getMonthName(month)
            val response = mosqueClockApi.getMonthPrayerTimes(zone, year, monthName)
            if (response.isSuccessful && response.body() != null) {
                // The backend returns a single response with a list of prayer times
                // We need to extract the prayer_times array from the metadata response
                // For now, let's handle this as an error since the API structure is complex
                emit(NetworkResult.Error("Month prayer times not implemented yet"))
            } else {
                emit(NetworkResult.Error("Failed to fetch month prayer times", response.code()))            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Network error: ${e.message}"))
        }
    }
    
    private fun convertAlAdhanToPrayerTimes(alAdhanData: AlAdhanData): PrayerTimes {
        val timings = alAdhanData.timings
        val date = alAdhanData.date.gregorian.date
        val hijriDate = alAdhanData.date.hijri.date
        
        return PrayerTimes(
            date = date,
            fajrAzan = formatTime(timings.fajr),
            fajrIqamah = calculateIqamahTime(timings.fajr, 20), // 20 minutes after Azan
            dhuhrAzan = formatTime(timings.dhuhr),
            dhuhrIqamah = calculateIqamahTime(timings.dhuhr, 10), // 10 minutes after Azan
            asrAzan = formatTime(timings.asr),
            asrIqamah = calculateIqamahTime(timings.asr, 10), // 10 minutes after Azan
            maghribAzan = formatTime(timings.maghrib),
            maghribIqamah = calculateIqamahTime(timings.maghrib, 5), // 5 minutes after Azan
            ishaAzan = formatTime(timings.isha),
            ishaIqamah = calculateIqamahTime(timings.isha, 10), // 10 minutes after Azan
            sunrise = formatTime(timings.sunrise),
            hijriDate = hijriDate,
            location = "${alAdhanData.meta.latitude}, ${alAdhanData.meta.longitude}"
        )
    }
    
    private fun formatTime(time: String): String {
        // AlAdhan returns time in HH:MM format, sometimes with timezone info
        return time.split(" ").first() // Remove timezone if present
    }
    
    private fun calculateIqamahTime(azanTime: String, minutesAfter: Int): String {
        try {
            val cleanTime = formatTime(azanTime)
            val parts = cleanTime.split(":")
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            
            val totalMinutes = (hours * 60) + minutes + minutesAfter
            val newHours = (totalMinutes / 60) % 24
            val newMinutes = totalMinutes % 60
            
            return String.format("%02d:%02d", newHours, newMinutes)
        } catch (e: Exception) {
            return azanTime // Return original time if calculation fails
        }
    }
    
    private fun getCountryForRegion(region: String): String {
        return when (region) {
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
    }
    
    private fun getMonthName(month: Int): String {
        return when (month) {
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
    }
}