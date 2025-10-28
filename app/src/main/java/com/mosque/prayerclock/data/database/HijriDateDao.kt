package com.mosque.prayerclock.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HijriDateDao {
    @Query("SELECT * FROM hijri_dates WHERE id = :id")
    suspend fun getHijriDateById(id: String): HijriDateEntity?

    @Query("SELECT * FROM hijri_dates WHERE gregorianDate = :gregorianDate AND provider = :provider")
    suspend fun getHijriDateByDateAndProvider(
        gregorianDate: String,
        provider: String,
    ): HijriDateEntity?

    @Query(
        """
        SELECT * FROM hijri_dates 
        WHERE provider = :provider 
        AND (:region IS NULL OR region = :region)
        ORDER BY gregorianDate DESC 
        LIMIT 1
    """,
    )
    suspend fun getLatestHijriDateByProvider(
        provider: String,
        region: String? = null,
    ): HijriDateEntity?

    @Query(
        """
        SELECT * FROM hijri_dates 
        WHERE provider = :provider 
        AND (:region IS NULL OR region = :region)
        AND gregorianDate < :beforeDate
        ORDER BY gregorianDate DESC 
        LIMIT 1
    """,
    )
    suspend fun getLatestHijriDateBefore(
        provider: String,
        beforeDate: String,
        region: String? = null,
    ): HijriDateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHijriDate(hijriDate: HijriDateEntity)

    @Query("DELETE FROM hijri_dates WHERE createdAt < :cutoffTime")
    suspend fun deleteOldHijriDates(cutoffTime: Long)

    @Query(
        """
        DELETE FROM hijri_dates 
        WHERE provider = :provider 
        AND (:region IS NULL OR region = :region)
        AND gregorianDate < :beforeDate
    """,
    )
    suspend fun deleteHijriDatesBefore(
        provider: String,
        beforeDate: String,
        region: String? = null,
    )

    @Query("SELECT COUNT(*) FROM hijri_dates WHERE provider = :provider AND (:region IS NULL OR region = :region)")
    suspend fun getHijriDateCountByProvider(
        provider: String,
        region: String? = null,
    ): Int

    @Query("SELECT * FROM hijri_dates WHERE provider = :provider ORDER BY gregorianDate DESC")
    suspend fun getHijriDatesByProvider(provider: String): List<HijriDateEntity>

    @Query("DELETE FROM hijri_dates")
    suspend fun deleteAllHijriDates()

    @Query("SELECT COUNT(*) FROM hijri_dates")
    suspend fun getAllHijriDatesCount(): Int
}
