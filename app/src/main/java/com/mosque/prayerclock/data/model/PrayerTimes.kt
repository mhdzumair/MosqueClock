package com.mosque.prayerclock.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "prayer_times")
data class PrayerTimes(
    @PrimaryKey
    val date: String,
    @SerializedName("fajr_azan")
    val fajrAzan: String,
    @SerializedName("fajr_iqamah")
    val fajrIqamah: String,
    @SerializedName("dhuhr_azan")
    val dhuhrAzan: String,
    @SerializedName("dhuhr_iqamah")
    val dhuhrIqamah: String,
    @SerializedName("asr_azan")
    val asrAzan: String,
    @SerializedName("asr_iqamah")
    val asrIqamah: String,
    @SerializedName("maghrib_azan")
    val maghribAzan: String,
    @SerializedName("maghrib_iqamah")
    val maghribIqamah: String,
    @SerializedName("isha_azan")
    val ishaAzan: String,
    @SerializedName("isha_iqamah")
    val ishaIqamah: String,
    val sunrise: String,
    val hijriDate: String? = null,
    val location: String? = null
)

data class PrayerTimeResponse(
    val success: Boolean,
    val data: List<PrayerTimes>,
    val message: String? = null
)

enum class PrayerType {
    FAJR, DHUHR, ASR, MAGHRIB, ISHA, SUNRISE
}

data class PrayerInfo(
    val type: PrayerType,
    val azanTime: String,
    val iqamahTime: String?,
    val name: String
)