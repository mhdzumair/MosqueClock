package com.mosque.prayerclock.data.model

data class AppSettings(
    val language: Language = Language.TAMIL,
    val city: String = "Colombo",
    val country: String = "Sri Lanka",
    val latitude: Double = 6.9271,
    val longitude: Double = 79.8612,
    val mosqueName: String = "Dickwella Jummah Mosque",
    val clockType: ClockType = ClockType.DIGITAL,
    val theme: AppTheme = AppTheme.DARK,
    val fontSize: FontSize = FontSize.MEDIUM,
    val showSeconds: Boolean = true,
    val show24HourFormat: Boolean = false,
    val manualFajrAzan: String = "05:30",
    val manualDhuhrAzan: String = "12:15",
    val manualAsrAzan: String = "15:30",
    val manualMaghribAzan: String = "18:30",
    val manualIshaAzan: String = "19:45",
    val fajrIqamahGap: Int = 20,
    val dhuhrIqamahGap: Int = 10,
    val asrIqamahGap: Int = 10,
    val maghribIqamahGap: Int = 5,
    val ishaIqamahGap: Int = 10,
    val refreshInterval: Long = 24 * 60 * 60 * 1000L, // 24 hours in milliseconds
    val showWeather: Boolean = true,
    val weatherCity: String = "Colombo",
    val weatherCountry: String = "Sri Lanka",
    val weatherProvider: WeatherProvider = WeatherProvider.MOSQUE_CLOCK,
    val hijriProvider: HijriProvider = HijriProvider.MANUAL,
    val manualHijriDay: Int = 7,
    val manualHijriMonth: Int = 2, // Safar
    val manualHijriYear: Int = 1447,
    val lastUpdatedGregorianDate: String = "2025-08-01", // Track when Hijri date was last set
    val prayerServiceType: PrayerServiceType = PrayerServiceType.MOSQUE_CLOCK_API,
    val selectedZone: Int = 1, // For MosqueClock backend zones
    val selectedRegion: String = "Colombo", // For third-party prayer time services
)

enum class Language(
    val code: String,
    val displayName: String,
) {
    MULTI("multi", "Multi Language"),
    ENGLISH("en", "English"),
    TAMIL("ta", "தமிழ்"),
    SINHALA("si", "Sinhala"),
}

enum class WeatherProvider {
    MOSQUE_CLOCK,
    OPEN_WEATHER,
}

enum class HijriProvider {
    MOSQUE_CLOCK_API,
    AL_ADHAN_API,
    MANUAL,
}

enum class ClockType {
    DIGITAL,
    ANALOG,
}

enum class AppTheme {
    DEFAULT,
    DARK,
    LIGHT,
    MOSQUE_GREEN,
    BLUE,
}

enum class FontSize {
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE,
}

enum class PrayerServiceType(
    val displayName: String,
) {
    MOSQUE_CLOCK_API("MosqueClock API"),
    AL_ADHAN_API("Al-Adhan API"),
    MANUAL("Manual Prayer Times"),
}

data class LocationInfo(
    val region: String,
    val city: String,
    val displayNameEn: String,
    val displayNameTa: String,
)

data class WeatherInfo(
    val temperature: Double,
    val description: String,
    val icon: String,
    val humidity: Int,
    val feelsLike: Double,
    val visibility: Double? = null,
    val uvIndex: Double? = null,
)

data class PrayerZone(
    val id: Int,
    val name: String,
    val description: String,
)

// Predefined zones for MosqueClock backend (all 13 Sri Lankan zones as per ACJU)
object PrayerZones {
    val zones =
        listOf(
            PrayerZone(
                1,
                "Zone 1",
                "Colombo District, Gampaha District, Kalutara District",
            ),
            PrayerZone(2, "Zone 2", "Jaffna District, Nallur"),
            PrayerZone(
                3,
                "Zone 3",
                "Mullaitivu District (except Nallur), Kilinochchi District, Vavuniya District",
            ),
            PrayerZone(4, "Zone 4", "Mannar District, Puttalam District"),
            PrayerZone(5, "Zone 5", "Anuradhapura District, Polonnaruwa District"),
            PrayerZone(6, "Zone 6", "Kurunegala District"),
            PrayerZone(
                7,
                "Zone 7",
                "Kandy District, Matale District, Nuwara Eliya District",
            ),
            PrayerZone(8, "Zone 8", "Batticaloa District, Ampara District"),
            PrayerZone(9, "Zone 9", "Trincomalee District"),
            PrayerZone(
                10,
                "Zone 10",
                "Badulla District, Monaragala District, Padiyatalawa, Dehiaththakandiya",
            ),
            PrayerZone(11, "Zone 11", "Ratnapura District, Kegalle District"),
            PrayerZone(12, "Zone 12", "Galle District, Matara District"),
            PrayerZone(13, "Zone 13", "Hambantota District"),
        )
}
