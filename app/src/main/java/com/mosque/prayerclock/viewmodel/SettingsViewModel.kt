package com.mosque.prayerclock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.AppTheme
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.HijriProvider
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.data.model.PrayerServiceType
import com.mosque.prayerclock.data.model.WeatherProvider
import com.mosque.prayerclock.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        val settings =
            settingsRepository
                .getSettings()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = AppSettings(),
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

        fun updateManualTime(
            timeType: String,
            time: String,
        ) {
            viewModelScope.launch {
                settingsRepository.updateManualTime(timeType, time)
            }
        }

        fun updateIqamahGap(
            prayerType: String,
            gap: Int,
        ) {
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

        fun updateHijriProvider(provider: HijriProvider) {
            viewModelScope.launch {
                settingsRepository.updateHijriProvider(provider)
            }
        }

        fun updateHijriDate(
            day: Int,
            month: Int,
            year: Int,
        ) {
            viewModelScope.launch {
                val currentDate =
                    Clock.System
                        .now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                settingsRepository.updateHijriDate(
                    day,
                    month,
                    year,
                    currentDate.toString(),
                )
            }
        }

        fun updateSoundEnabled(soundEnabled: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateSoundEnabled(soundEnabled)
            }
        }
    }
