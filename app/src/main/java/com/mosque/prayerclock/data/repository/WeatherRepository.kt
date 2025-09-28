package com.mosque.prayerclock.data.repository

import android.util.Log
import com.mosque.prayerclock.data.model.CityCoordinatesMap
import com.mosque.prayerclock.data.model.WeatherInfo
import com.mosque.prayerclock.data.model.WeatherProvider
import com.mosque.prayerclock.data.network.MosqueClockApi
import com.mosque.prayerclock.data.network.NetworkResult
import com.mosque.prayerclock.data.network.WeatherApi
import com.mosque.prayerclock.data.network.toWeatherInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository
    @Inject
    constructor(
        private val weatherApi: WeatherApi, // Keep for backward compatibility
        private val mosqueClockApi: MosqueClockApi, // New MosqueClock API
    ) {
    // Weather refresh job management - survives ViewModel recreation since Repository is Singleton
    private val weatherRefreshJobs = mutableMapOf<Triple<String, String, WeatherProvider>, Job>()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // TODO: Add your WeatherAPI.com API key to local.properties file as:
    // weather_api_key=your_actual_api_key_here
    // You can get a free API key from: https://www.weatherapi.com/
    private val apiKey = com.mosque.prayerclock.BuildConfig.WEATHER_API_KEY

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
                        Log.d("WeatherRepository", "Using coordinates for $city: ${coordinates.latitude}, ${coordinates.longitude}")
                        // Use coordinates-based weather API call for better accuracy
                        getCurrentWeatherByCoordinatesInternal(coordinates.latitude, coordinates.longitude).collect { result ->
                            emit(result)
                        }
                        return@flow
                    }

                    // If no valid API key is provided, fall back to mock data
                    if (apiKey.isBlank() || apiKey == "your_weatherapi_key_here" || apiKey == "demo_key") {
                        Log.d("WeatherRepository", "No valid API key, using mock data")
                        val mockWeatherInfo = createMockWeatherData(city)
                        emit(NetworkResult.Success(mockWeatherInfo))
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
                    when {
                        e.message?.contains("CertificateException") == true || 
                        e.message?.contains("Trust anchor") == true -> {
                            Log.w("WeatherRepository", "SSL Certificate error detected - falling back to mock data for limited connection")
                            val mockWeatherInfo = createMockWeatherData(city)
                            emit(NetworkResult.Success(mockWeatherInfo))
                            return@flow
                        }
                        e.message?.contains("UnknownHostException") == true -> {
                            Log.w("WeatherRepository", "DNS resolution failed - falling back to mock data for limited connection")
                            val mockWeatherInfo = createMockWeatherData(city)
                            emit(NetworkResult.Success(mockWeatherInfo))
                            return@flow
                        }
                        e.message?.contains("SocketTimeoutException") == true -> {
                            Log.w("WeatherRepository", "Connection timeout - falling back to mock data for limited connection")
                            val mockWeatherInfo = createMockWeatherData(city)
                            emit(NetworkResult.Success(mockWeatherInfo))
                            return@flow
                        }
                        e.message?.contains("ConnectException") == true -> {
                            Log.w("WeatherRepository", "Connection failed - falling back to mock data for limited connection")
                            val mockWeatherInfo = createMockWeatherData(city)
                            emit(NetworkResult.Success(mockWeatherInfo))
                            return@flow
                        }
                        else -> {
                            val errorMessage = "Failed to fetch weather data: ${e.message}"
                            emit(NetworkResult.Error(errorMessage))
                        }
                    }
                }
            }

        private fun createMockWeatherData(city: String): WeatherInfo {
            // Mock data for demonstration
            return when (city.lowercase()) {
                "colombo" ->
                    WeatherInfo(
                        temperature = 28.5,
                        description = "Partly Cloudy",
                        icon = "//cdn.weatherapi.com/weather/64x64/day/116.png",
                        humidity = 75,
                        feelsLike = 32.1,
                        visibility = 10.0,
                        uvIndex = 6.0,
                    )
                "kuala lumpur" ->
                    WeatherInfo(
                        temperature = 31.2,
                        description = "Sunny",
                        icon = "//cdn.weatherapi.com/weather/64x64/day/113.png",
                        humidity = 68,
                        feelsLike = 35.4,
                        visibility = 15.0,
                        uvIndex = 8.0,
                    )
                else ->
                    WeatherInfo(
                        temperature = 25.0,
                        description = "Clear",
                        icon = "//cdn.weatherapi.com/weather/64x64/day/113.png",
                        humidity = 60,
                        feelsLike = 27.0,
                        visibility = 12.0,
                        uvIndex = 5.0,
                    )
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
                        Log.e("WeatherRepository", "API call failed with code: ${response.code()}, message: ${response.message()}")
                        emit(NetworkResult.Error("Failed to fetch weather data", response.code()))
                    }
                } catch (e: Exception) {
                    Log.e("WeatherRepository", "Exception in getCurrentWeatherByCity: ${e.message}", e)
                    val errorMessage = when {
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

                    // If no valid API key is provided, fall back to mock data
                    if (apiKey.isBlank() || apiKey == "your_weatherapi_key_here" || apiKey == "demo_key") {
                        Log.d("WeatherRepository", "No valid API key, using mock data")
                        val mockWeatherInfo = createMockWeatherData("location")
                        emit(NetworkResult.Success(mockWeatherInfo))
                        return@flow
                    }
                    
                    Log.d("WeatherRepository", "API Key available: ${apiKey.take(8)}... (${apiKey.length} chars)")

                    // Make API call using coordinates
                    val response = weatherApi.getCurrentWeather(apiKey, "$latitude,$longitude")

                    if (response.isSuccessful && response.body() != null) {
                        Log.d("WeatherRepository", "Coordinates API call successful")
                        val rawResponse = response.body()!!
                        Log.d("WeatherRepository", "Full API Response: $rawResponse")
                        val weatherInfo = rawResponse.toWeatherInfo()
                        Log.d(
                            "WeatherRepository",
                            "Weather info from coordinates: Icon=${weatherInfo.icon}, Description=${weatherInfo.description}, Temp=${weatherInfo.temperature}",
                        )
                        emit(NetworkResult.Success(weatherInfo))
                    } else {
                        Log.e("WeatherRepository", "Coordinates API call failed. Code: ${response.code()}")
                        emit(NetworkResult.Error("Failed to fetch weather data: ${response.code()}", response.code()))
                    }
                } catch (e: Exception) {
                    Log.e("WeatherRepository", "Exception in coordinates weather fetch: ${e.message}", e)
                    val errorMessage = when {
                        e.message?.contains("CertificateException") == true || 
                        e.message?.contains("Trust anchor") == true -> {
                            Log.w("WeatherRepository", "SSL Certificate error detected - falling back to mock data for limited connection")
                            val mockWeatherInfo = createMockWeatherData("location")
                            emit(NetworkResult.Success(mockWeatherInfo))
                            return@flow
                        }
                        e.message?.contains("UnknownHostException") == true -> {
                            Log.w("WeatherRepository", "DNS resolution failed - falling back to mock data for limited connection")
                            val mockWeatherInfo = createMockWeatherData("location")
                            emit(NetworkResult.Success(mockWeatherInfo))
                            return@flow
                        }
                        e.message?.contains("SocketTimeoutException") == true -> {
                            Log.w("WeatherRepository", "Connection timeout - falling back to mock data for limited connection")
                            val mockWeatherInfo = createMockWeatherData("location")
                            emit(NetworkResult.Success(mockWeatherInfo))
                            return@flow
                        }
                        e.message?.contains("ConnectException") == true -> {
                            Log.w("WeatherRepository", "Connection failed - falling back to mock data for limited connection")
                            val mockWeatherInfo = createMockWeatherData("location")
                            emit(NetworkResult.Success(mockWeatherInfo))
                            return@flow
                        }
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
        onWeatherUpdate: suspend (NetworkResult<WeatherInfo>) -> Unit
    ) {
        val params = Triple(city, country, provider)
        
        // Check if job already exists and is active
        val existingJob = weatherRefreshJobs[params]
        if (existingJob?.isActive == true) {
            Log.d("WeatherRepository", "Weather refresh job already running for params $params - skipping")
            return
        }
        
        // Stop any existing jobs with different parameters
        weatherRefreshJobs.entries.removeAll { (existingParams, job) ->
            if (existingParams != params && job.isActive) {
                job.cancel()
                true
            } else {
                false
            }
        }
        
        Log.d("WeatherRepository", "Starting hourly weather refresh for $city with $provider")
        
        val job = repositoryScope.launch {
            try {
                while (isActive) {
                    // Fetch weather data based on provider
                    when (provider) {
                        WeatherProvider.OPEN_WEATHER -> {
                            getCurrentWeather(city, country).collect { result ->
                                onWeatherUpdate(result)
                            }
                        }
                        WeatherProvider.MOSQUE_CLOCK -> {
                            getCurrentWeatherByCity(city).collect { result ->
                                if (result is NetworkResult.Error) {
                                    // Fallback to OpenWeather
                                    getCurrentWeather(city, country).collect { fallbackResult ->
                                        onWeatherUpdate(fallbackResult)
                                    }
                                } else {
                                    onWeatherUpdate(result)
                                }
                            }
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