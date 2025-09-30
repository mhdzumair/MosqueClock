package com.mosque.prayerclock.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.mosque.prayerclock.utils.TimeUtils

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
     */
    fun withIqamahTimes(settings: AppSettings): PrayerTimesWithIqamah =
        PrayerTimesWithIqamah(
            prayerTimes = this,
            fajrIqamah = TimeUtils.addMinutesToTime(fajrAzan, settings.fajrIqamahGap),
            dhuhrIqamah = TimeUtils.addMinutesToTime(dhuhrAzan, settings.dhuhrIqamahGap),
            asrIqamah = TimeUtils.addMinutesToTime(asrAzan, settings.asrIqamahGap),
            maghribIqamah = TimeUtils.addMinutesToTime(maghribAzan, settings.maghribIqamahGap),
            ishaIqamah = TimeUtils.addMinutesToTime(ishaAzan, settings.ishaIqamahGap),
        )
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
