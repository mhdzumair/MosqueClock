package com.mosque.prayerclock.data.service

import android.util.Log
import com.mosque.prayerclock.BuildConfig
import com.mosque.prayerclock.data.model.WeatherInfo
import com.mosque.prayerclock.data.network.NetworkResult
import com.mosque.prayerclock.data.network.OpenWeatherMapApi
import com.mosque.prayerclock.data.network.toWeatherInfo
import com.mosque.prayerclock.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenWeatherMapService
    @Inject
    constructor(
        private val openWeatherMapApi: OpenWeatherMapApi,
        private val settingsRepository: SettingsRepository,
    ) {
        companion object {
            private const val TAG = "OpenWeatherMapService"

            // Sri Lankan city coordinates
            val CITY_COORDINATES =
                mapOf(
                    // Southern Province
                    "dickwella" to Pair(5.95983, 80.68578),
                    "matara" to Pair(5.948313, 80.535217),
                    "hambantota" to Pair(6.124593, 81.101074),
                    "tangalle" to Pair(6.024, 80.794),
                    // Western Province
                    "colombo" to Pair(6.927079, 79.861244),
                    "negombo" to Pair(7.189464, 79.858734),
                    "kalutara" to Pair(6.583, 79.961),
                    "panadura" to Pair(6.713, 79.905),
                    // Central Province
                    "kandy" to Pair(7.290572, 80.633728),
                    "nuwara eliya" to Pair(6.949, 80.790),
                    "matale" to Pair(7.469, 80.623),
                    // Southern Coast
                    "galle" to Pair(6.053519, 80.220978),
                    "hikkaduwa" to Pair(6.141, 80.102),
                    "unawatuna" to Pair(6.010, 80.248),
                    // Eastern Province
                    "trincomalee" to Pair(8.592200, 81.196793),
                    "batticaloa" to Pair(7.717, 81.700),
                    // Northern Province
                    "jaffna" to Pair(9.661, 80.025),
                    // North Western Province
                    "kurunegala" to Pair(7.487, 80.365),
                    "puttalam" to Pair(8.036, 79.828),
                    // Uva Province
                    "badulla" to Pair(6.989, 81.055),
                    // Sabaragamuwa Province
                    "ratnapura" to Pair(6.683, 80.400),
                    // North Central Province
                    "anuradhapura" to Pair(8.311, 80.403),
                    "polonnaruwa" to Pair(7.940, 81.000),
                )

            // Default coordinates (Dickwella)
            private const val DEFAULT_LAT = 5.95983
            private const val DEFAULT_LON = 80.68578
        }

        // Get API key from settings at runtime
        private suspend fun getApiKey(): String {
            val settings = settingsRepository.getSettings().first()
            return settings.openWeatherMapApiKey
        }

        suspend fun getCurrentWeather(
            city: String,
            country: String = "LK",
        ): NetworkResult<WeatherInfo> =
            withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "🌤️ Fetching weather for $city, $country using OpenWeatherMap")

                    // Get coordinates for the city
                    val coordinates = getCoordinatesForCity(city)
                    val lat = coordinates.first
                    val lon = coordinates.second

                    return@withContext getCurrentWeatherByCoordinates(lat, lon)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Exception fetching weather from OpenWeatherMap", e)
                    NetworkResult.Error("Failed to fetch weather: ${e.message}")
                }
            }

        suspend fun getCurrentWeatherByCoordinates(
            latitude: Double,
            longitude: Double,
        ): NetworkResult<WeatherInfo> =
            withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "🌤️ Fetching weather by coordinates: $latitude, $longitude using OpenWeatherMap")

                    val apiKey = getApiKey()

                    if (apiKey.isBlank()) {
                        Log.e(TAG, "❌ OpenWeatherMap API key is missing")
                        return@withContext NetworkResult.Error("OpenWeatherMap API key not configured")
                    }

                    val response =
                        openWeatherMapApi.getCurrentWeather(
                            latitude = latitude,
                            longitude = longitude,
                            apiKey = apiKey,
                        )

                    if (response.isSuccessful) {
                        val weatherData = response.body()
                        if (weatherData != null) {
                            Log.d(TAG, "✅ Weather data received: ${weatherData.weather.firstOrNull()?.description}")
                            val weatherInfo = weatherData.toWeatherInfo()
                            NetworkResult.Success(weatherInfo)
                        } else {
                            Log.e(TAG, "❌ Weather response body is null")
                            NetworkResult.Error("Empty response from OpenWeatherMap")
                        }
                    } else {
                        val errorMsg = "OpenWeatherMap API error: ${response.code()} - ${response.message()}"
                        Log.e(TAG, "❌ $errorMsg")
                        NetworkResult.Error(errorMsg)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Exception fetching weather from OpenWeatherMap", e)
                    NetworkResult.Error("Failed to fetch weather: ${e.message}")
                }
            }

        private fun getCoordinatesForCity(city: String): Pair<Double, Double> {
            val normalizedCity = city.lowercase().trim()

            return CITY_COORDINATES[normalizedCity] ?: run {
                // Try partial matching for compound city names
                val matchingCity =
                    CITY_COORDINATES.keys.find { key ->
                        key.contains(normalizedCity) || normalizedCity.contains(key)
                    }

                if (matchingCity != null) {
                    Log.d(TAG, "📍 Found partial match: $matchingCity for $city")
                    CITY_COORDINATES[matchingCity]!!
                } else {
                    Log.w(TAG, "⚠️ City $city not found in coordinates map, using default (Dickwella)")
                    Pair(DEFAULT_LAT, DEFAULT_LON)
                }
            }
        }

        suspend fun isConfigured(): Boolean {
            val apiKey = getApiKey()
            return apiKey.isNotBlank()
        }
    }
