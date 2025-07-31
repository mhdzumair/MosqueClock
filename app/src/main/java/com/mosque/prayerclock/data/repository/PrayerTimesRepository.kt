package com.mosque.prayerclock.data.repository

import com.mosque.prayerclock.data.database.PrayerTimesDao
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.data.network.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerTimesRepository @Inject constructor(
    private val api: PrayerTimesApi,
    private val dao: PrayerTimesDao
) {
    
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
        
        val today = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString()
        
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
}