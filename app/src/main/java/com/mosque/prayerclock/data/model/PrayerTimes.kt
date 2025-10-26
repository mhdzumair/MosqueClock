package com.mosque.prayerclock.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.mosque.prayerclock.utils.TimeUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity(tableName = "prayer_times")
data class PrayerTimes(
    @PrimaryKey
    val id: String, // Composite ID: "2025-09-27_AL_ADHAN_API:Colombo"
    val date: String,
    val providerKey: String? = null, // "AL_ADHAN_API:Colombo", "MOSQUE_CLOCK_API:1", null for manual
    @SerializedName("fajr_azan")
    val fajrAzan: String,
    @SerializedName("dhuhr_azan")
    val dhuhrAzan: String,
    @SerializedName("asr_azan")
    val asrAzan: String,
    @SerializedName("maghrib_azan")
    val maghribAzan: String,
    @SerializedName("isha_azan")
    val ishaAzan: String,
    val sunrise: String,
    val hijriDate: String? = null,
    val location: String? = null,
) {
    /**
     * Calculate iqamah times dynamically based on settings
     * This ensures iqamah times always reflect current user preferences
     * Also handles special case for Jumma Night Bayan on Thursday nights
     */
    fun withIqamahTimes(settings: AppSettings): PrayerTimesWithIqamah {
        // Calculate standard iqamah time for Isha
        val standardIshaIqamah = TimeUtils.addMinutesToTime(ishaAzan, settings.ishaIqamahGap)
        
        // Check if Jumma Night Bayan should be applied (Thursday night)
        val ishaIqamah = if (settings.jummaNightBayanEnabled && isThursday()) {
            // Apply Jumma Night Bayan duration instead of standard iqamah gap
            TimeUtils.addMinutesToTime(ishaAzan, settings.jummaNightBayanMinutes)
        } else {
            standardIshaIqamah
        }
        
        return PrayerTimesWithIqamah(
            prayerTimes = this,
            fajrIqamah = TimeUtils.addMinutesToTime(fajrAzan, settings.fajrIqamahGap),
            dhuhrIqamah = TimeUtils.addMinutesToTime(dhuhrAzan, settings.dhuhrIqamahGap),
            asrIqamah = TimeUtils.addMinutesToTime(asrAzan, settings.asrIqamahGap),
            maghribIqamah = TimeUtils.addMinutesToTime(maghribAzan, settings.maghribIqamahGap),
            ishaIqamah = ishaIqamah,
        )
    }
    
    /**
     * Check if the prayer time date is a Thursday
     * Returns true if the date is Thursday
     */
    private fun isThursday(): Boolean {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val localDate = LocalDate.parse(date, formatter)
            localDate.dayOfWeek.value == 4 // Thursday is day 4 (Monday=1, Tuesday=2, Wednesday=3, Thursday=4, etc.)
        } catch (e: Exception) {
            false // If date parsing fails, don't apply Jumma Night Bayan
        }
    }
}

/**
 * Prayer times with dynamically calculated iqamah times
 * This is what the UI should use instead of PrayerTimes directly
 */
data class PrayerTimesWithIqamah(
    val prayerTimes: PrayerTimes,
    val fajrIqamah: String,
    val dhuhrIqamah: String,
    val asrIqamah: String,
    val maghribIqamah: String,
    val ishaIqamah: String,
) {
    // Delegate properties for easy access
    val id: String get() = prayerTimes.id
    val date: String get() = prayerTimes.date
    val providerKey: String? get() = prayerTimes.providerKey
    val fajrAzan: String get() = prayerTimes.fajrAzan
    val dhuhrAzan: String get() = prayerTimes.dhuhrAzan
    val asrAzan: String get() = prayerTimes.asrAzan
    val maghribAzan: String get() = prayerTimes.maghribAzan
    val ishaAzan: String get() = prayerTimes.ishaAzan
    val sunrise: String get() = prayerTimes.sunrise
    val hijriDate: String? get() = prayerTimes.hijriDate
    val location: String? get() = prayerTimes.location
}

data class PrayerTimeResponse(
    val success: Boolean,
    val data: List<PrayerTimes>,
    val message: String? = null,
)

enum class PrayerType {
    FAJR,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA,
    SUNRISE,
}

data class PrayerInfo(
    val type: PrayerType,
    val azanTime: String,
    val iqamahTime: String?,
    val name: String,
)
