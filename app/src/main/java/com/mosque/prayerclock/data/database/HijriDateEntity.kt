package com.mosque.prayerclock.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hijri_dates")
data class HijriDateEntity(
    @PrimaryKey
    val id: String, // Format: "provider_gregorianDate" e.g., "AL_ADHAN_2025-09-28"
    val gregorianDate: String, // Format: "YYYY-MM-DD"
    val hijriDay: Int,
    val hijriMonth: Int,
    val hijriYear: Int,
    val provider: String, // "MOSQUE_CLOCK_API", "AL_ADHAN_API", "MANUAL", "ACJU_DIRECT"
    val region: String? = null, // For AL_ADHAN_API, store the region used
    val createdAt: Long = System.currentTimeMillis(),
    val isCalculated: Boolean = false, // true if calculated from cached data, false if from API
    // ACJU-specific fields for date range tracking
    val hijriMonthStartDate: String? = null, // Gregorian start date of Hijri month (e.g., "2025-09-24")
    val hijriMonthEndDate: String? = null, // Gregorian end date of Hijri month (e.g., "2025-10-23")
    val hijriMonthName: String? = null, // Original ACJU month name (e.g., "Rabee`unith Thaani")
)
