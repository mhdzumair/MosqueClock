package com.mosque.prayerclock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosque.prayerclock.data.model.*
import com.mosque.prayerclock.data.network.NetworkResult
import com.mosque.prayerclock.data.repository.PrayerTimesRepository
import com.mosque.prayerclock.data.repository.SettingsRepository
import com.mosque.prayerclock.data.repository.WeatherRepository
import com.mosque.prayerclock.data.repository.HijriDateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val settingsRepository: SettingsRepository,
    private val weatherRepository: WeatherRepository,
    val hijriDateRepository: HijriDateRepository
) : ViewModel() {
    
    val settings = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )
    
    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading())
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()
    
    // Function to recalculate next prayer - can be called from UI when needed
    fun updateNextPrayer() {
        val currentState = _uiState.value
        if (currentState is MainUiState.Success) {
            val newNextPrayer = calculateNextPrayer(currentState.prayerTimes)
            if (newNextPrayer != currentState.nextPrayer) {
                _uiState.value = currentState.copy(nextPrayer = newNextPrayer)
            }
        }
    }
    
    fun loadPrayerTimes() {
        viewModelScope.launch {
            val currentSettings = settings.value
            
            if (currentSettings.useManualTimes) {
                val manualPrayerTimes = createManualPrayerTimes(currentSettings)
                val nextPrayer = calculateNextPrayer(manualPrayerTimes)
                _uiState.value = MainUiState.Success(
                    prayerTimes = manualPrayerTimes,
                    nextPrayer = nextPrayer
                )
            } else {
                prayerTimesRepository.getTodayPrayerTimes(
                    currentSettings.city,
                    currentSettings.country
                ).collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> {
                            _uiState.value = MainUiState.Loading
                        }
                        is NetworkResult.Success -> {
                            val adjustedPrayerTimes = adjustIqamahTimes(result.data, currentSettings)
                            val nextPrayer = calculateNextPrayer(adjustedPrayerTimes)
                            _uiState.value = MainUiState.Success(
                                prayerTimes = adjustedPrayerTimes,
                                nextPrayer = nextPrayer
                            )
                        }
                        is NetworkResult.Error -> {
                            _uiState.value = MainUiState.Error(result.message)
                        }
                    }
                }
            }
            
            // Load weather data if enabled
            if (currentSettings.showWeather) {
                loadWeatherData(currentSettings.weatherCity, currentSettings.weatherCountry)
            }
        }
    }
    
    private fun loadWeatherData(city: String, country: String) {
        viewModelScope.launch {
            weatherRepository.getCurrentWeather(city, country).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _weatherState.value = WeatherUiState.Loading()
                    }
                    is NetworkResult.Success -> {
                        _weatherState.value = WeatherUiState.Success(result.data)
                    }
                    is NetworkResult.Error -> {
                        _weatherState.value = WeatherUiState.Error(result.message)
                    }
                }
            }
        }
    }
    
    private fun calculateNextPrayer(prayerTimes: PrayerTimes): PrayerType? {
        val now = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val currentTime = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
        val isFriday = now.dayOfWeek == DayOfWeek.FRIDAY
        
        // Check if we're in the 5-minute buffer after any Iqamah time
        val prayers = listOf(
            PrayerType.FAJR to Pair(prayerTimes.fajrAzan, prayerTimes.fajrIqamah),
            PrayerType.DHUHR to Pair(prayerTimes.dhuhrAzan, if (isFriday) null else prayerTimes.dhuhrIqamah), // No Iqamah on Friday
            PrayerType.ASR to Pair(prayerTimes.asrAzan, prayerTimes.asrIqamah),
            PrayerType.MAGHRIB to Pair(prayerTimes.maghribAzan, prayerTimes.maghribIqamah),
            PrayerType.ISHA to Pair(prayerTimes.ishaAzan, prayerTimes.ishaIqamah)
        )
        
        // First check if we're between Azan and Iqamah time OR within 5 minutes after Iqamah
        for ((prayerType, times) in prayers) {
            val azanTime = times.first
            val iqamahTime = times.second
            
            if (iqamahTime != null) { // Only check if Iqamah time exists
                val bufferTime = addMinutesToTime(iqamahTime, 5)
                
                // Check if we're between Azan and Iqamah + 5min buffer
                if (compareTimeStrings(azanTime, currentTime) <= 0 && 
                    compareTimeStrings(currentTime, bufferTime) < 0) {
                    // We're in the current prayer period (from Azan to Iqamah + 5min)
                    return prayerType
                }
            } else if (prayerType == PrayerType.DHUHR && isFriday) {
                // For Friday, give 1 hour after Azan for Bayan
                val bayanEndTime = addMinutesToTime(times.first, 60)
                if (compareTimeStrings(times.first, currentTime) <= 0 && 
                    compareTimeStrings(currentTime, bayanEndTime) < 0) {
                    return prayerType
                }
            }
        }
        
        // Regular logic: find next prayer that hasn't started yet
        // Skip prayers that have already started (past their Azan time)
        return prayers.firstOrNull { (_, times) ->
            compareTimeStrings(currentTime, times.first) < 0
        }?.first
    }
    
    private fun compareTimeStrings(time1: String, time2: String): Int {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return try {
            val date1 = format.parse(time1)
            val date2 = format.parse(time2)
            date1?.compareTo(date2) ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    private fun createManualPrayerTimes(settings: AppSettings): PrayerTimes {
        val today = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString()
        
        return PrayerTimes(
            date = today,
            fajrAzan = settings.manualFajrAzan,
            fajrIqamah = settings.manualFajrIqamah,
            dhuhrAzan = settings.manualDhuhrAzan,
            dhuhrIqamah = settings.manualDhuhrIqamah,
            asrAzan = settings.manualAsrAzan,
            asrIqamah = settings.manualAsrIqamah,
            maghribAzan = settings.manualMaghribAzan,
            maghribIqamah = settings.manualMaghribIqamah,
            ishaAzan = settings.manualIshaAzan,
            ishaIqamah = settings.manualIshaIqamah,
            sunrise = "06:00", // Default sunrise time
            hijriDate = null,
            location = "Manual Setup"
        )
    }
    
    private fun adjustIqamahTimes(prayerTimes: PrayerTimes, settings: AppSettings): PrayerTimes {
        return prayerTimes.copy(
            fajrIqamah = calculateIqamahTime(prayerTimes.fajrAzan, settings.fajrIqamahGap),
            dhuhrIqamah = calculateIqamahTime(prayerTimes.dhuhrAzan, settings.dhuhrIqamahGap),
            asrIqamah = calculateIqamahTime(prayerTimes.asrAzan, settings.asrIqamahGap),
            maghribIqamah = calculateIqamahTime(prayerTimes.maghribAzan, settings.maghribIqamahGap),
            ishaIqamah = calculateIqamahTime(prayerTimes.ishaAzan, settings.ishaIqamahGap)
        )
    }
    
    private fun calculateIqamahTime(azanTime: String, minutesAfter: Int): String {
        try {
            val parts = azanTime.split(":")
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            
            val totalMinutes = (hours * 60) + minutes + minutesAfter
            val newHours = (totalMinutes / 60) % 24
            val newMinutes = totalMinutes % 60
            
            return String.format("%02d:%02d", newHours, newMinutes)
        } catch (e: Exception) {
            return azanTime // Return original time if calculation fails
        }
    }
    
    private fun addMinutesToTime(timeString: String, minutesToAdd: Int): String {
        try {
            val parts = timeString.split(":")
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            
            val totalMinutes = (hours * 60) + minutes + minutesToAdd
            val newHours = (totalMinutes / 60) % 24
            val newMinutes = totalMinutes % 60
            
            return String.format("%02d:%02d", newHours, newMinutes)
        } catch (e: Exception) {
            return timeString
        }
    }
}

sealed class MainUiState {
    object Loading : MainUiState()
    data class Success(
        val prayerTimes: PrayerTimes,
        val nextPrayer: PrayerType?
    ) : MainUiState()
    data class Error(val message: String) : MainUiState()
}

sealed class WeatherUiState {
    class Loading : WeatherUiState()
    data class Success(val weatherInfo: WeatherInfo) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}