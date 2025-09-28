package com.mosque.prayerclock.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mosque.prayerclock.data.model.PrayerTimes
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTimesDao {
    // Provider-specific queries
    @Query("SELECT * FROM prayer_times WHERE date = :date AND providerKey = :providerKey")
    suspend fun getPrayerTimesByDateAndProvider(
        date: String,
        providerKey: String,
    ): PrayerTimes?

    @Query("SELECT * FROM prayer_times WHERE date = :date AND providerKey = :providerKey")
    fun getPrayerTimesByDateAndProviderFlow(
        date: String,
        providerKey: String,
    ): Flow<PrayerTimes?>

    // Legacy method for backward compatibility (gets first match by date)
    @Query("SELECT * FROM prayer_times WHERE date = :date LIMIT 1")
    suspend fun getPrayerTimesByDate(date: String): PrayerTimes?

    @Query("SELECT * FROM prayer_times WHERE date = :date")
    fun getPrayerTimesByDateFlow(date: String): Flow<PrayerTimes?>

    @Query("SELECT * FROM prayer_times ORDER BY date DESC LIMIT 30")
    fun getAllPrayerTimes(): Flow<List<PrayerTimes>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerTimes(prayerTimes: PrayerTimes)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerTimesList(prayerTimesList: List<PrayerTimes>)

    @Delete
    suspend fun deletePrayerTimes(prayerTimes: PrayerTimes)

    @Query("DELETE FROM prayer_times WHERE date < :date")
    suspend fun deleteOldPrayerTimes(date: String)

    @Query("SELECT COUNT(*) FROM prayer_times WHERE date >= :fromDate AND date <= :toDate")
    suspend fun getPrayerTimesCount(
        fromDate: String,
        toDate: String,
    ): Int
}
