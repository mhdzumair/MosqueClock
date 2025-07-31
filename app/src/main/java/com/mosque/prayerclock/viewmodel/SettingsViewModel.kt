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
    
    fun updateUseManualTimes(useManualTimes: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUseManualTimes(useManualTimes)
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
}