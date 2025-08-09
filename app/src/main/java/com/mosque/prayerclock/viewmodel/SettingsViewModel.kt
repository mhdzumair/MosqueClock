package com.mosque.prayerclock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosque.prayerclock.data.model.*
import com.mosque.prayerclock.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    val settings = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )
    
    fun updateLanguage(language: Language) {
        viewModelScope.launch {
            settingsRepository.updateLanguage(language)
        }
    }
    
    fun updateCity(city: String) {
        viewModelScope.launch {
            settingsRepository.updateCity(city)
        }
    }
    
    fun updateCountry(country: String) {
        viewModelScope.launch {
            settingsRepository.updateCountry(country)
        }
    }
    
    fun updateMosqueName(mosqueName: String) {
        viewModelScope.launch {
            settingsRepository.updateMosqueName(mosqueName)
        }
    }
    
    fun updateClockType(clockType: ClockType) {
        viewModelScope.launch {
            settingsRepository.updateClockType(clockType)
        }
    }
    
    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.updateTheme(theme)
        }
    }
    
    fun updateShowSeconds(showSeconds: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowSeconds(showSeconds)
        }
    }
    
    fun updateShow24HourFormat(show24Hour: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShow24HourFormat(show24Hour)
        }
    }
    
    fun updateManualTime(timeType: String, time: String) {
        viewModelScope.launch {
            settingsRepository.updateManualTime(timeType, time)
        }
    }
    
    fun updateIqamahGap(prayerType: String, gap: Int) {
        viewModelScope.launch {
            settingsRepository.updateIqamahGap(prayerType, gap)
        }
    }
    
    fun updateWeatherCity(weatherCity: String) {
        viewModelScope.launch {
            settingsRepository.updateWeatherCity(weatherCity)
        }
    }
    
    fun updateWeatherCountry(weatherCountry: String) {
        viewModelScope.launch {
            settingsRepository.updateWeatherCountry(weatherCountry)
        }
    }
    
    fun updateWeatherProvider(provider: WeatherProvider) {
        viewModelScope.launch {
            settingsRepository.updateWeatherProvider(provider)
        }
    }
    
    fun updateShowWeather(showWeather: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowWeather(showWeather)
        }
    }
    
    fun updatePrayerServiceType(serviceType: PrayerServiceType) {
        viewModelScope.launch {
            settingsRepository.updatePrayerServiceType(serviceType)
        }
    }
    
    fun updateSelectedZone(zone: Int) {
        viewModelScope.launch {
            settingsRepository.updateSelectedZone(zone)
        }
    }
    
    fun updateSelectedRegion(region: String) {
        viewModelScope.launch {
            settingsRepository.updateSelectedRegion(region)
        }
    }
    
    fun updateUseApiForHijriDate(useApi: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUseApiForHijriDate(useApi)
        }
    }
    
    fun updateHijriDate(day: Int, month: Int, year: Int) {
        viewModelScope.launch {
            settingsRepository.updateHijriDate(day, month, year, java.time.LocalDate.now().toString())
        }
    }
}