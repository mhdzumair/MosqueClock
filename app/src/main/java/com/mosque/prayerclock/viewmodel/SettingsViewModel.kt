package com.mosque.prayerclock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.AppTheme
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.HijriProvider
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.data.model.PrayerServiceType
import com.mosque.prayerclock.data.model.SoundType
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

        fun updateFullScreenCountdownEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateFullScreenCountdownEnabled(enabled)
            }
        }

        fun updateColorTheme(themeId: String) {
            viewModelScope.launch {
                settingsRepository.updateColorTheme(themeId)
            }
        }

        fun updateWeatherApiKey(apiKey: String) {
            viewModelScope.launch {
                settingsRepository.updateWeatherApiKey(apiKey)
            }
        }

        fun updateOpenWeatherMapApiKey(apiKey: String) {
            viewModelScope.launch {
                settingsRepository.updateOpenWeatherMapApiKey(apiKey)
            }
        }

        fun updateMosqueClockBackendUrl(url: String) {
            viewModelScope.launch {
                settingsRepository.updateMosqueClockBackendUrl(url)
            }
        }

        fun updateMosqueClockBackendApiKey(apiKey: String) {
            viewModelScope.launch {
                settingsRepository.updateMosqueClockBackendApiKey(apiKey)
            }
        }

        fun updateJummahDurationMinutes(minutes: Int) {
            viewModelScope.launch {
                settingsRepository.updateJummahDurationMinutes(minutes)
            }
        }

        fun updateDuaDisplayDurationMinutes(minutes: Int) {
            viewModelScope.launch {
                settingsRepository.updateDuaDisplayDurationMinutes(minutes)
            }
        }

        fun updateShowJummahScreen(show: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateShowJummahScreen(show)
            }
        }

        fun updateAzanSoundEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateAzanSoundEnabled(enabled)
            }
        }

        fun updateIqamahSoundEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateIqamahSoundEnabled(enabled)
            }
        }

        fun updateAzanSoundType(soundType: SoundType) {
            viewModelScope.launch {
                settingsRepository.updateAzanSoundType(soundType)
            }
        }

        fun updateIqamahSoundType(soundType: SoundType) {
            viewModelScope.launch {
                settingsRepository.updateIqamahSoundType(soundType)
            }
        }

        fun updateAzanSoundUri(uri: String) {
            viewModelScope.launch {
                settingsRepository.updateAzanSoundUri(uri)
            }
        }

        fun updateIqamahSoundUri(uri: String) {
            viewModelScope.launch {
                settingsRepository.updateIqamahSoundUri(uri)
            }
        }
    }
