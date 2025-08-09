package com.mosque.prayerclock.data.repository

import com.mosque.prayerclock.data.model.WeatherInfo
import com.mosque.prayerclock.data.network.NetworkResult
import com.mosque.prayerclock.data.network.WeatherApi
import com.mosque.prayerclock.data.network.MosqueClockApi
import com.mosque.prayerclock.data.network.toWeatherInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApi, // Keep for backward compatibility
    private val mosqueClockApi: MosqueClockApi // New MosqueClock API
) {
    
    // TODO: Add your WeatherAPI.com API key to local.properties file as:
    // weather_api_key=your_actual_api_key_here
    // You can get a free API key from: https://www.weatherapi.com/
    private val apiKey = com.mosque.prayerclock.BuildConfig.WEATHER_API_KEY
    
    fun getCurrentWeather(city: String, country: String): Flow<NetworkResult<WeatherInfo>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            // If no valid API key is provided, fall back to mock data
            if (apiKey.isBlank() || apiKey == "your_weatherapi_key_here" || apiKey == "demo_key") {
                val mockWeatherInfo = createMockWeatherData(city)
                emit(NetworkResult.Success(mockWeatherInfo))
                return@flow
            }
            
            // Make actual API call
            val response = weatherApi.getCurrentWeather(apiKey, "$city,$country")
            
            if (response.isSuccessful && response.body() != null) {
                val weatherInfo = response.body()!!.toWeatherInfo()
                emit(NetworkResult.Success(weatherInfo))
            } else {
                // Fallback to mock data if API call fails
                val mockWeatherInfo = createMockWeatherData(city)
                emit(NetworkResult.Success(mockWeatherInfo))
            }
        } catch (e: Exception) {
            // Fallback to mock data if there's an exception
            try {
                val mockWeatherInfo = createMockWeatherData(city)
                emit(NetworkResult.Success(mockWeatherInfo))
            } catch (mockException: Exception) {
                emit(NetworkResult.Error("Failed to fetch weather data: ${e.message}"))
            }
        }
    }
    
    private fun createMockWeatherData(city: String): WeatherInfo {
        // Mock data for demonstration
        return when (city.lowercase()) {
            "colombo" -> WeatherInfo(
                temperature = 28.5,
                description = "Partly Cloudy",
                icon = "//cdn.weatherapi.com/weather/64x64/day/116.png",
                humidity = 75,
                feelsLike = 32.1,
                visibility = 10.0,
                uvIndex = 6.0
            )
            "kuala lumpur" -> WeatherInfo(
                temperature = 31.2,
                description = "Sunny",
                icon = "//cdn.weatherapi.com/weather/64x64/day/113.png",
                humidity = 68,
                feelsLike = 35.4,
                visibility = 15.0,
                uvIndex = 8.0
            )
            else -> WeatherInfo(
                temperature = 25.0,
                description = "Clear",
                icon = "//cdn.weatherapi.com/weather/64x64/day/113.png",
                humidity = 60,
                feelsLike = 27.0,
                visibility = 12.0,
                uvIndex = 5.0
            )
        }
    }
    
    // New methods using MosqueClock API for accurate Sri Lankan weather
    
    fun getCurrentWeatherByCity(cityName: String): Flow<NetworkResult<WeatherInfo>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            val response = mosqueClockApi.getCurrentWeatherByCity(cityName)
            if (response.isSuccessful && response.body() != null) {
                val weatherData = response.body()!!
                val weatherInfo = weatherData.toWeatherInfo()
                emit(NetworkResult.Success(weatherInfo))
            } else {
                emit(NetworkResult.Error("Failed to fetch weather data", response.code()))
            }
        } catch (e: Exception) {
            // Fallback to mock data if API fails
            try {
                val mockWeatherInfo = createMockWeatherData(cityName)
                emit(NetworkResult.Success(mockWeatherInfo))
            } catch (mockException: Exception) {
                emit(NetworkResult.Error("Failed to fetch weather data: ${e.message}"))
            }
        }
    }
    
    fun getCurrentWeatherByCoordinates(latitude: Double, longitude: Double): Flow<NetworkResult<WeatherInfo>> = flow {
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
            // Fallback to mock data if API fails
            try {
                val mockWeatherInfo = createMockWeatherData("location")
                emit(NetworkResult.Success(mockWeatherInfo))
            } catch (mockException: Exception) {
                emit(NetworkResult.Error("Failed to fetch weather data: ${e.message}"))
            }
        }
    }
}