package com.mosque.prayerclock.data.network

import com.mosque.prayerclock.data.model.WeatherInfo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("current.json")
    suspend fun getCurrentWeather(
        @Query("key") apiKey: String,
        @Query("q") location: String,
        @Query("aqi") includeAirQuality: String = "no",
    ): Response<WeatherResponse>
}

data class WeatherResponse(
    val location: WeatherLocation,
    val current: CurrentWeather,
)

data class WeatherLocation(
    val name: String,
    val region: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val localtime: String,
)

data class CurrentWeather(
    val temp_c: Double,
    val temp_f: Double,
    val is_day: Int,
    val condition: WeatherCondition,
    val humidity: Int,
    val feelslike_c: Double,
    val feelslike_f: Double,
    val vis_km: Double,
    val uv: Double,
    val wind_kph: Double,
    val wind_mph: Double,
)

data class WeatherCondition(
    val text: String,
    val icon: String,
    val code: Int,
)

fun WeatherResponse.toWeatherInfo(): WeatherInfo =
    WeatherInfo(
        temperature = current.temp_c,
        description = current.condition.text,
        icon = current.condition.icon,
        humidity = current.humidity,
        feelsLike = current.feelslike_c,
        visibility = current.vis_km,
        uvIndex = current.uv,
        windSpeed = current.wind_kph,
    )
