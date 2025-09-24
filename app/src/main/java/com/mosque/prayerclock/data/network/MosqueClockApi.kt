package com.mosque.prayerclock.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

interface MosqueClockApi {
    // Prayer Times endpoints
    @GET("api/v1/prayer-times/{zone}/")
    suspend fun getPrayerTimesByZone(
        @Path("zone") zone: Int,
        @Query("date") date: String? = null,
    ): Response<MosquePrayerTimesListResponse>

    @GET("api/v1/today/")
    suspend fun getTodayPrayerTimes(
        @Query("zone") zone: Int,
        @Query("apartment") apartment: Boolean = false,
    ): Response<BackendPrayerTime>

    @GET("api/v1/prayer-times/{zone}/{year}/{month}/")
    suspend fun getMonthPrayerTimes(
        @Path("zone") zone: Int,
        @Path("year") year: Int,
        @Path("month") month: String,
    ): Response<MosquePrayerTimesResponse>

    // Hijri Calendar endpoints
    @GET("api/v1/hijri-date/")
    suspend fun getHijriDate(
        @Query("year") year: Int,
        @Query("month") month: Int,
        @Query("day") day: Int,
    ): Response<HijriDateResponse>

    @GET("api/v1/today-both-calendars/")
    suspend fun getTodayBothCalendars(): Response<BothCalendarsResponse>

    @GET("api/v1/gregorian-date/")
    suspend fun getGregorianDate(
        @Query("hijri_year") hijriYear: Int,
        @Query("hijri_month") hijriMonth: Int,
        @Query("hijri_day") hijriDay: Int,
    ): Response<GregorianDateResponse>

    @GET("api/v1/hijri-months/")
    suspend fun getHijriMonths(): Response<HijriMonthsResponse>

    // Weather endpoints
    @GET("api/v1/weather/current/city/{city_name}/")
    suspend fun getCurrentWeatherByCity(
        @Path("city_name") cityName: String,
    ): Response<ProcessedCurrentWeather>

    @GET("api/v1/weather/current/coordinates/")
    suspend fun getCurrentWeatherByCoordinates(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
    ): Response<ProcessedCurrentWeather>

    @GET("api/v1/weather/forecast/")
    suspend fun getForecastDickwella(): Response<ProcessedForecastResponse>

    @GET("api/v1/weather/forecast/city/{city_name}/")
    suspend fun getForecastByCity(
        @Path("city_name") cityName: String,
    ): Response<ProcessedForecastResponse>

    @GET("api/v1/weather/locations/sri-lanka/")
    suspend fun getSriLankaLocations(): Response<SriLankaLocationsResponse>
}

// Prayer Times Data Classes
data class MosquePrayerTimesResponse(
    val success: Boolean,
    val data: MosquePrayerTimes,
    val message: String? = null,
)

data class MosquePrayerTimesListResponse(
    val success: Boolean,
    val data: List<MosquePrayerTimes>,
    val message: String? = null,
)

data class SinglePrayerTimeResponse(
    val success: Boolean,
    val data: MosquePrayerTimes,
    val message: String? = null,
)

// Backend prayer time format (matches PrayerTimeSchema from backend)
data class BackendPrayerTime(
    val date: String,
    val day: Int,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val magrib: String, // Note: backend uses "magrib" not "maghrib"
    val isha: String,
)

data class MosquePrayerTimes(
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
    @SerializedName("hijri_date")
    val hijriDate: String? = null,
    val location: String? = null,
    val zone: Int? = null,
)

// Hijri Calendar Data Classes
data class HijriDateResponse(
    val success: Boolean,
    val data: HijriDateInfo,
    val message: String? = null,
)

data class BothCalendarsResponse(
    val success: Boolean,
    val data: BothCalendarsInfo,
    val message: String? = null,
)

data class GregorianDateResponse(
    val success: Boolean,
    val data: GregorianDateInfo,
    val message: String? = null,
)

data class HijriMonthsResponse(
    val success: Boolean,
    val data: List<HijriMonthInfo>,
    val message: String? = null,
)

data class HijriDateInfo(
    val day: Int,
    val month: Int,
    val year: Int,
    val method: String,
    @SerializedName("source_data")
    val sourceData: Map<String, Any>? = null,
    @SerializedName("month_name_en")
    val monthNameEn: String,
    @SerializedName("month_name_ar")
    val monthNameAr: String,
    @SerializedName("month_name_ta")
    val monthNameTa: String,
    @SerializedName("month_name_si")
    val monthNameSi: String,
    val formatted: String,
    @SerializedName("formatted_ar")
    val formattedAr: String,
    @SerializedName("formatted_ta")
    val formattedTa: String,
    @SerializedName("formatted_si")
    val formattedSi: String,
)

data class BothCalendarsInfo(
    @SerializedName("gregorian_date")
    val gregorianDate: String,
    @SerializedName("hijri_date")
    val hijriDate: HijriDateInfo,
)

data class GregorianDateInfo(
    val year: Int,
    val month: Int,
    val day: Int,
    val formatted: String,
    @SerializedName("hijri_conversion")
    val hijriConversion: HijriDateInfo,
)

data class HijriMonthInfo(
    val number: Int,
    @SerializedName("name_en")
    val nameEn: String,
    @SerializedName("name_ar")
    val nameAr: String,
    @SerializedName("name_ta")
    val nameTa: String,
    @SerializedName("name_si")
    val nameSi: String,
)

// Weather Data Classes
data class ProcessedCurrentWeatherResponse(
    val success: Boolean,
    val data: ProcessedCurrentWeather,
    val message: String? = null,
)

data class ProcessedForecastResponse(
    val success: Boolean,
    val data: ProcessedForecast,
    val message: String? = null,
)

data class SriLankaLocationsResponse(
    val success: Boolean,
    val data: List<WeatherLocationInfo>,
    val message: String? = null,
)

data class WeatherLocationInfo(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("timezone_offset")
    val timezoneOffset: Int,
)

data class ProcessedCurrentWeather(
    val location: WeatherLocationInfo,
    val temperature: Double,
    @SerializedName("feels_like")
    val feelsLike: Double,
    val humidity: Int,
    val pressure: Int,
    @SerializedName("wind_speed")
    val windSpeed: Double,
    @SerializedName("wind_direction")
    val windDirection: Int,
    @SerializedName("wind_direction_text")
    val windDirectionText: String,
    val visibility: Int?,
    val cloudiness: Int,
    @SerializedName("weather_main")
    val weatherMain: String,
    @SerializedName("weather_description")
    val weatherDescription: String,
    @SerializedName("weather_icon")
    val weatherIcon: String,
    val sunrise: String,
    val sunset: String,
    @SerializedName("is_daytime")
    val isDaytime: Boolean,
)

data class ProcessedForecast(
    val location: WeatherLocationInfo,
    @SerializedName("current_weather")
    val currentWeather: ProcessedCurrentWeather,
    val forecast: List<ProcessedForecastItem>,
)

data class ProcessedForecastItem(
    @SerializedName("forecast_time")
    val forecastTime: String,
    val temperature: Double,
    @SerializedName("feels_like")
    val feelsLike: Double,
    @SerializedName("temp_min")
    val tempMin: Double,
    @SerializedName("temp_max")
    val tempMax: Double,
    val humidity: Int,
    val pressure: Int,
    @SerializedName("wind_speed")
    val windSpeed: Double,
    @SerializedName("wind_direction")
    val windDirection: Int,
    @SerializedName("wind_direction_text")
    val windDirectionText: String,
    val cloudiness: Int,
    @SerializedName("weather_main")
    val weatherMain: String,
    @SerializedName("weather_description")
    val weatherDescription: String,
    @SerializedName("weather_icon")
    val weatherIcon: String,
    @SerializedName("precipitation_probability")
    val precipitationProbability: Double,
    @SerializedName("rain_3h")
    val rain3h: Double?,
    @SerializedName("snow_3h")
    val snow3h: Double?,
    @SerializedName("is_daytime")
    val isDaytime: Boolean,
)

// Extension functions to convert to existing models
fun MosquePrayerTimes.toPrayerTimes(): com.mosque.prayerclock.data.model.PrayerTimes =
    com.mosque.prayerclock.data.model.PrayerTimes(
        date = this.date,
        fajrAzan = this.fajrAzan,
        fajrIqamah = this.fajrIqamah,
        dhuhrAzan = this.dhuhrAzan,
        dhuhrIqamah = this.dhuhrIqamah,
        asrAzan = this.asrAzan,
        asrIqamah = this.asrIqamah,
        maghribAzan = this.maghribAzan,
        maghribIqamah = this.maghribIqamah,
        ishaAzan = this.ishaAzan,
        ishaIqamah = this.ishaIqamah,
        sunrise = this.sunrise,
        hijriDate = this.hijriDate,
        location = this.location,
    )

fun ProcessedCurrentWeather.toWeatherInfo(): com.mosque.prayerclock.data.model.WeatherInfo =
    com.mosque.prayerclock.data.model.WeatherInfo(
        temperature = this.temperature,
        description = this.weatherDescription,
        icon = this.weatherIcon,
        humidity = this.humidity,
        feelsLike = this.feelsLike,
        visibility = this.visibility?.toDouble(),
        uvIndex = null, // UV index not provided by our API yet
    )

fun BackendPrayerTime.toPrayerTimes(): com.mosque.prayerclock.data.model.PrayerTimes =
    com.mosque.prayerclock.data.model.PrayerTimes(
        date = this.date,
        fajrAzan = this.fajr,
        fajrIqamah = this.fajr, // Backend doesn't provide separate iqamah times
        dhuhrAzan = this.dhuhr,
        dhuhrIqamah = this.dhuhr,
        asrAzan = this.asr,
        asrIqamah = this.asr,
        maghribAzan = this.magrib, // Backend uses "magrib"
        maghribIqamah = this.magrib,
        ishaAzan = this.isha,
        ishaIqamah = this.isha,
        sunrise = this.sunrise,
        hijriDate = null, // Backend doesn't include hijri date in prayer times
        location = "Zone from MosqueClock Backend",
    )
