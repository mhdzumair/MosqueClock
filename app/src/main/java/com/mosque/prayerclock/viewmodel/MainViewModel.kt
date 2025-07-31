package com.mosque.prayerclock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosque.prayerclock.data.model.*
import com.mosque.prayerclock.data.network.NetworkResult
import com.mosque.prayerclock.data.repository.PrayerTimesRepository
import com.mosque.prayerclock.data.repository.SettingsRepository
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
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    val settings = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )
    
    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
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
        }
    }
    
    private fun calculateNextPrayer(prayerTimes: PrayerTimes): PrayerType? {
        val now = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val currentTime = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
        
        val prayers = listOf(
            PrayerType.FAJR to prayerTimes.fajrAzan,
            PrayerType.DHUHR to prayerTimes.dhuhrAzan,
            PrayerType.ASR to prayerTimes.asrAzan,
            PrayerType.MAGHRIB to prayerTimes.maghribAzan,
            PrayerType.ISHA to prayerTimes.ishaAzan
        )
        
        return prayers.firstOrNull { (_, time) ->
            compareTimeStrings(currentTime, time) < 0
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
}

sealed class MainUiState {
    object Loading : MainUiState()
    data class Success(
        val prayerTimes: PrayerTimes,
        val nextPrayer: PrayerType?
    ) : MainUiState()
    data class Error(val message: String) : MainUiState()
}