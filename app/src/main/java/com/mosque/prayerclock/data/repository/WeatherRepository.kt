package com.mosque.prayerclock.data.repository

import android.util.Log
import com.mosque.prayerclock.BuildConfig
import com.mosque.prayerclock.data.model.CityCoordinatesMap
import com.mosque.prayerclock.data.model.WeatherInfo
import com.mosque.prayerclock.data.model.WeatherProvider
import com.mosque.prayerclock.data.network.MosqueClockApi
import com.mosque.prayerclock.data.network.NetworkResult
import com.mosque.prayerclock.data.network.WeatherApi
import com.mosque.prayerclock.data.network.toWeatherInfo
import com.mosque.prayerclock.data.service.OpenWeatherMapService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository
    @Inject
    constructor(
        private val weatherApi: WeatherApi, // Primary weather provider (WeatherAPI.com)
        private val mosqueClockApi: MosqueClockApi, // MosqueClock API (for backend fallback)
        private val openWeatherMapService: OpenWeatherMapService, // Secondary weather provider
        private val settingsRepository: com.mosque.prayerclock.data.repository.SettingsRepository, // For runtime API key
    ) {
        // Weather refresh job management - survives ViewModel recreation since Repository is Singleton
        private val weatherRefreshJobs = mutableMapOf<Triple<String, String, WeatherProvider>, Job>()
        private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        // Get API key from settings at runtime
        private suspend fun getWeatherApiKey(): String {
            val settings = settingsRepository.getSettings().first()
            return settings.weatherApiKey
        }

        fun getCurrentWeather(
            city: String,
            country: String,
        ): Flow<NetworkResult<WeatherInfo>> =
            flow {
                emit(NetworkResult.Loading())

                try {
                    Log.d("WeatherRepository", "Fetching weather for city: $city, country: $country")

                    // Check if we have coordinates for this city
                    val coordinates = CityCoordinatesMap.getCoordinates(city)
                    if (coordinates != null) {
                        Log.d(
                            "WeatherRepository",
                            "Using coordinates for $city: ${coordinates.latitude}, ${coordinates.longitude}",
                        )
                        // Use coordinates-based weather API call for better accuracy
                        getCurrentWeatherByCoordinatesInternal(
                            coordinates.latitude,
                            coordinates.longitude,
                        ).collect { result ->
                            emit(result)
                        }
                        return@flow
                    }

                    // Get API key from settings
                    val apiKey = getWeatherApiKey()

                    // If no valid API key is provided, return error
                    if (apiKey.isBlank()) {
                        Log.w("WeatherRepository", "No API key configured for WeatherAPI.com")
                        emit(NetworkResult.Error("Weather API key not configured. Please add your API key in Settings."))
                        return@flow
                    }

                    // Make actual API call using city name
                    Log.d("WeatherRepository", "Making API call for city: $city")
                    val response = weatherApi.getCurrentWeather(apiKey, "$city,$country")

                    if (response.isSuccessful && response.body() != null) {
                        Log.d("WeatherRepository", "API call successful")
                        val rawResponse = response.body()!!
                        Log.d("WeatherRepository", "Full API Response: $rawResponse")
                        val weatherInfo = rawResponse.toWeatherInfo()
                        Log.d(
                            "WeatherRepository",
                            "Weather info: Icon=${weatherInfo.icon}, Description=${weatherInfo.description}, Temp=${weatherInfo.temperature}",
                        )
                        emit(NetworkResult.Success(weatherInfo))
                    } else {
                        Log.e("WeatherRepository", "API call failed. Code: ${response.code()}")
                        emit(NetworkResult.Error("Failed to fetch weather data: ${response.code()}", response.code()))
                    }
                } catch (e: Exception) {
                    Log.e("WeatherRepository", "Exception in getCurrentWeather: ${e.message}", e)
                    val errorMessage = when {
                        e.message?.contains("CertificateException") == true ||
                            e.message?.contains("Trust anchor") == true ->
                            "SSL Certificate error. Please check your internet connection."
                        e.message?.contains("UnknownHostException") == true ->
                            "DNS resolution failed. Please check your internet connection."
                        e.message?.contains("SocketTimeoutException") == true ->
                            "Connection timeout. Please check your internet connection."
                        e.message?.contains("ConnectException") == true ->
                            "Connection failed. Please check your internet connection."
                        else -> "Failed to fetch weather data: ${e.message}"
                    }
                    emit(NetworkResult.Error(errorMessage))
                }
            }

        // New methods using MosqueClock API for accurate Sri Lankan weather

        fun getCurrentWeatherByCity(cityName: String): Flow<NetworkResult<WeatherInfo>> =
            flow {
                emit(NetworkResult.Loading())

                try {
                    Log.d("WeatherRepository", "Fetching weather for city: $cityName")
                    val response = mosqueClockApi.getCurrentWeatherByCity(cityName)
                    if (response.isSuccessful && response.body() != null) {
                        val weatherData = response.body()!!
                        Log.d("WeatherRepository", "Raw weather data received: $weatherData")
                        Log.d(
                            "WeatherRepository",
                            "Weather main: ${weatherData.weatherMain}, Description: ${weatherData.weatherDescription}, Icon: ${weatherData.weatherIcon}",
                        )
                        val weatherInfo = weatherData.toWeatherInfo()
                        Log.d(
                            "WeatherRepository",
                            "Converted weather info - Icon: ${weatherInfo.icon}, Description: ${weatherInfo.description}",
                        )
                        emit(NetworkResult.Success(weatherInfo))
                    } else {
                        Log.e(
                            "WeatherRepository",
                            "API call failed with code: ${response.code()}, message: ${response.message()}",
                        )
                        emit(NetworkResult.Error("Failed to fetch weather data", response.code()))
                    }
                } catch (e: Exception) {
                    Log.e("WeatherRepository", "Exception in getCurrentWeatherByCity: ${e.message}", e)
                    val errorMessage =
                        when {
                            e.message?.contains("CertificateException") == true ||
                                e.message?.contains("Trust anchor") == true ->
                                "SSL Certificate error with MosqueClock API. Please switch to OpenWeather provider in settings."
                            e.message?.contains("UnknownHostException") == true ->
                                "Cannot connect to MosqueClock API. Please check your network or switch to OpenWeather provider."
                            e.message?.contains("ConnectException") == true ->
                                "MosqueClock API server is not running. Please switch to OpenWeather provider."
                            else -> "Failed to fetch weather data from MosqueClock API: ${e.message}"
                        }
                    emit(NetworkResult.Error(errorMessage))
                }
            }

        fun getCurrentWeatherByCoordinates(
            latitude: Double,
            longitude: Double,
        ): Flow<NetworkResult<WeatherInfo>> =
            flow {
                emit(NetworkResult.Loading())

                try {
                    val response = mosqueClockApi.getCurrentWeatherByCoordinates(latitude, longitude)
                    if (response.isSuccessful && response.body() != null) {
                        val weatherData = response.body()!!
                        val weatherInfo = weatherData.toWeatherInfo()
                        emit(NetworkResult.Success(weatherInfo))
                    } else {
                        emit(NetworkResult.Error("Failed to fetch weather data", response.code()))
                    }
                } catch (e: Exception) {
                    emit(NetworkResult.Error("Failed to fetch weather data: ${e.message}"))
                }
            }

        // Internal method for coordinates-based weather using WeatherAPI.com
        private fun getCurrentWeatherByCoordinatesInternal(
            latitude: Double,
            longitude: Double,
        ): Flow<NetworkResult<WeatherInfo>> =
            flow {
                emit(NetworkResult.Loading())

                try {
                    Log.d("WeatherRepository", "Fetching weather by coordinates: $latitude, $longitude")

                    // Get API key from settings
                    val apiKey = getWeatherApiKey()

                    // If no valid API key is provided, try secondary provider
                    if (apiKey.isBlank()) {
                        Log.w("WeatherRepository", "No API key configured for WeatherAPI.com, trying secondary provider")
                        // Try secondary weather provider (OpenWeatherMap)
                        if (openWeatherMapService.isConfigured()) {
                            Log.d("WeatherRepository", "🌤️ Trying secondary weather provider (OpenWeatherMap)")
                            val openWeatherResult =
                                openWeatherMapService.getCurrentWeatherByCoordinates(
                                    latitude,
                                    longitude,
                                )

                            when (openWeatherResult) {
                                is NetworkResult.Success -> {
                                    Log.d("WeatherRepository", "✅ Secondary weather provider successful")
                                    emit(openWeatherResult)
                                    return@flow
                                }
                                is NetworkResult.Error -> {
                                    Log.w(
                                        "WeatherRepository",
                                        "⚠️ Secondary weather provider failed: ${openWeatherResult.message}",
                                    )
                                    emit(NetworkResult.Error("No weather API keys configured. Please add API keys in Settings."))
                                    return@flow
                                }
                                else -> {
                                    Log.w("WeatherRepository", "⚠️ Secondary weather provider returned unexpected result")
                                }
                            }
                        } else {
                            Log.w("WeatherRepository", "⚠️ No weather API keys configured")
                            emit(NetworkResult.Error("Weather API key not configured. Please add your API key in Settings."))
                            return@flow
                        }
                    }

                    Log.d("WeatherRepository", "API Key available: ${apiKey.take(8)}... (${apiKey.length} chars)")

                    // Try primary weather provider (WeatherAPI.com) first
                    Log.d("WeatherRepository", "🌤️ Trying primary weather provider (WeatherAPI.com)")
                    val response = weatherApi.getCurrentWeather(apiKey, "$latitude,$longitude")

                    if (response.isSuccessful && response.body() != null) {
                        Log.d("WeatherRepository", "✅ Primary weather provider successful")
                        val rawResponse = response.body()!!
                        Log.d("WeatherRepository", "Full API Response: $rawResponse")
                        val weatherInfo = rawResponse.toWeatherInfo()
                        Log.d(
                            "WeatherRepository",
                            "Weather info from coordinates: Icon=${weatherInfo.icon}, Description=${weatherInfo.description}, Temp=${weatherInfo.temperature}",
                        )
                        emit(NetworkResult.Success(weatherInfo))
                        return@flow
                    } else {
                        Log.w("WeatherRepository", "⚠️ Primary weather provider failed. Code: ${response.code()}")
                    }

                    // Try secondary weather provider (OpenWeatherMap)
                    if (openWeatherMapService.isConfigured()) {
                        Log.d("WeatherRepository", "🌤️ Trying secondary weather provider (OpenWeatherMap)")
                        val openWeatherResult =
                            openWeatherMapService.getCurrentWeatherByCoordinates(
                                latitude,
                                longitude,
                            )

                        when (openWeatherResult) {
                            is NetworkResult.Success -> {
                                Log.d("WeatherRepository", "✅ Secondary weather provider successful")
                                emit(openWeatherResult)
                                return@flow
                            }
                            is NetworkResult.Error -> {
                                Log.w(
                                    "WeatherRepository",
                                    "⚠️ Secondary weather provider failed: ${openWeatherResult.message}",
                                )
                            }
                            else -> {
                                Log.w("WeatherRepository", "⚠️ Secondary weather provider returned unexpected result")
                            }
                        }
                    } else {
                        Log.w("WeatherRepository", "⚠️ OpenWeatherMap key not configured")
                    }

                    // All weather providers failed
                    emit(NetworkResult.Error("All weather providers failed", response.code()))
                } catch (e: Exception) {
                    Log.e("WeatherRepository", "Exception in coordinates weather fetch: ${e.message}", e)
                    val errorMessage = when {
                        e.message?.contains("CertificateException") == true ||
                            e.message?.contains("Trust anchor") == true ->
                            "SSL Certificate error. Please check your internet connection."
                        e.message?.contains("UnknownHostException") == true ->
                            "DNS resolution failed. Please check your internet connection."
                        e.message?.contains("SocketTimeoutException") == true ->
                            "Connection timeout. Please check your internet connection."
                        e.message?.contains("ConnectException") == true ->
                            "Connection failed. Please check your internet connection."
                        else -> "Failed to fetch weather data: ${e.message}"
                    }
                    emit(NetworkResult.Error(errorMessage))
                }
            }

        // Weather refresh job management functions
        fun startHourlyWeatherRefresh(
            city: String,
            country: String,
            provider: WeatherProvider,
            onWeatherUpdate: suspend (NetworkResult<WeatherInfo>) -> Unit,
        ) {
            val params = Triple(city, country, provider)

            // Check if job already exists and is active for the same parameters
            val existingJob = weatherRefreshJobs[params]
            if (existingJob?.isActive == true) {
                Log.d("WeatherRepository", "Weather refresh job already running for params $params - skipping")
                return
            }

            // Stop any existing jobs (including different providers for same city or different cities)
            val stoppedJobs =
                weatherRefreshJobs.entries.removeAll { (existingParams, job) ->
                    if (job.isActive) {
                        Log.d("WeatherRepository", "Stopping existing weather job for $existingParams")
                        job.cancel()
                        true
                    } else {
                        false
                    }
                }

            if (stoppedJobs) {
                Log.d("WeatherRepository", "Stopped ${if (stoppedJobs) "existing" else "no"} weather refresh jobs")
            }

            Log.d("WeatherRepository", "Starting hourly weather refresh for $city with $provider")

            // Immediately fetch weather data when starting the job (don't wait 30 minutes)
            repositoryScope.launch {
                Log.d("WeatherRepository", "Immediate fetch for new provider: $provider")
                try {
                    when (provider) {
                        WeatherProvider.WEATHER_API -> {
                            getCurrentWeather(city, country).collect { result ->
                                onWeatherUpdate(result)
                            }
                        }
                        WeatherProvider.OPEN_WEATHER_MAP -> {
                            val openWeatherResult = openWeatherMapService.getCurrentWeather(city, country)
                            onWeatherUpdate(openWeatherResult)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WeatherRepository", "Error in immediate weather fetch", e)
                    onWeatherUpdate(NetworkResult.Error("Failed to fetch weather: ${e.message}"))
                }
            }

            val job =
                repositoryScope.launch {
                    try {
                        while (isActive) {
                            // Fetch weather data based on provider
                            when (provider) {
                                WeatherProvider.WEATHER_API -> {
                                    getCurrentWeather(city, country).collect { result ->
                                        onWeatherUpdate(result)
                                    }
                                }
                                WeatherProvider.OPEN_WEATHER_MAP -> {
                                    // Use OpenWeatherMap service directly
                                    val openWeatherResult = openWeatherMapService.getCurrentWeather(city, country)
                                    onWeatherUpdate(openWeatherResult)
                                }
                            }

                            Log.d("WeatherRepository", "Weather refresh completed. Next refresh in 30 minutes...")
                            delay(1800000) // 30 minutes
                        }
                    } catch (e: CancellationException) {
                        Log.d("WeatherRepository", "Weather refresh job ($params) was cancelled")
                        throw e
                    } catch (e: Exception) {
                        Log.e("WeatherRepository", "Error in weather refresh job ($params)", e)
                    } finally {
                        weatherRefreshJobs.remove(params)
                        Log.d("WeatherRepository", "Weather refresh job ($params) finished and removed")
                    }
                }

            weatherRefreshJobs[params] = job
        }

        fun stopAllWeatherRefreshJobs() {
            Log.d("WeatherRepository", "Stopping all weather refresh jobs. Count: ${weatherRefreshJobs.size}")
            weatherRefreshJobs.values.forEach { job ->
                if (job.isActive) {
                    job.cancel()
                }
            }
            weatherRefreshJobs.clear()
            Log.d("WeatherRepository", "All weather refresh jobs stopped")
        }
    }
