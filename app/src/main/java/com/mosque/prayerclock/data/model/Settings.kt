package com.mosque.prayerclock.data.model

data class AppSettings(
    val language: Language = Language.TAMIL,
    val city: String = "Colombo",
    val country: String = "Sri Lanka",
    val latitude: Double = 6.9271,
    val longitude: Double = 79.8612,
    val mosqueName: String = "ஜமிஅத் அல்-இஸ்லாம் பள்ளிவாசல்",
    val clockType: ClockType = ClockType.DIGITAL,
    val theme: AppTheme = AppTheme.DEFAULT,
    val fontSize: FontSize = FontSize.MEDIUM,
    val showSeconds: Boolean = true,
    val show24HourFormat: Boolean = false,
    val useManualTimes: Boolean = false,
    val manualFajrAzan: String = "05:30",
    val manualFajrIqamah: String = "05:50",
    val manualDhuhrAzan: String = "12:15",
    val manualDhuhrIqamah: String = "12:25",
    val manualAsrAzan: String = "15:30",
    val manualAsrIqamah: String = "15:40",
    val manualMaghribAzan: String = "18:30",
    val manualMaghribIqamah: String = "18:35",
    val manualIshaAzan: String = "19:45",
    val manualIshaIqamah: String = "19:55",
    val fajrIqamahGap: Int = 20,
    val dhuhrIqamahGap: Int = 10,
    val asrIqamahGap: Int = 10,
    val maghribIqamahGap: Int = 5,
    val ishaIqamahGap: Int = 10,
    val refreshInterval: Long = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
)

enum class Language(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    TAMIL("ta", "தமிழ்")
}

enum class ClockType {
    DIGITAL, ANALOG
}

enum class AppTheme {
    DEFAULT, DARK, LIGHT, MOSQUE_GREEN, BLUE
}

enum class FontSize {
    SMALL, MEDIUM, LARGE, EXTRA_LARGE
}

data class LocationInfo(
    val region: String,
    val city: String,
    val displayNameEn: String,
    val displayNameTa: String
)