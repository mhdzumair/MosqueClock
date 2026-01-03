package com.mosque.prayerclock.viewmodel

import android.util.Log
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
import com.mosque.prayerclock.data.repository.HijriDateRepository
import com.mosque.prayerclock.data.repository.PrayerTimesRepository
import com.mosque.prayerclock.data.repository.SettingsRepository
import com.mosque.prayerclock.data.scraping.DirectScrapingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        private val prayerTimesRepository: PrayerTimesRepository,
        private val hijriDateRepository: HijriDateRepository,
    ) : ViewModel() {
        val settings =
            settingsRepository
                .getSettings()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = AppSettings(),
                )

        // Offline data prefetch state
        private val _prefetchState = MutableStateFlow<PrefetchState>(PrefetchState.Idle)
        val prefetchState: StateFlow<PrefetchState> = _prefetchState.asStateFlow()

        private val _cacheStatus = MutableStateFlow<DirectScrapingService.CacheStatus?>(null)
        val cacheStatus: StateFlow<DirectScrapingService.CacheStatus?> = _cacheStatus.asStateFlow()

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

        fun updateJummaNightBayanEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateJummaNightBayanEnabled(enabled)
            }
        }

        fun updateJummaNightBayanMinutes(minutes: Int) {
            viewModelScope.launch {
                settingsRepository.updateJummaNightBayanMinutes(minutes)
            }
        }

        fun updateAutoUpdateCheckEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateAutoUpdateCheckEnabled(enabled)
            }
        }

        suspend fun updateSkippedUpdateVersion(version: String) {
            settingsRepository.updateSkippedUpdateVersion(version)
        }

        /**
         * Prefetch all remaining months of the year for offline use
         */
        fun prefetchOfflineData(zone: Int) {
            viewModelScope.launch {
                _prefetchState.value = PrefetchState.Loading
                Log.d("SettingsViewModel", "🔄 Starting prefetch for zone $zone")
                
                try {
                    val result = prayerTimesRepository.prefetchRemainingYearPrayerTimes(zone)
                    _prefetchState.value = PrefetchState.Success(result)
                    Log.d("SettingsViewModel", "✅ Prefetch complete: ${result.successfulMonths}/${result.totalMonths} months")
                    
                    // Update cache status after prefetch
                    checkCacheStatus(zone)
                } catch (e: Exception) {
                    _prefetchState.value = PrefetchState.Error(e.message ?: "Unknown error")
                    Log.e("SettingsViewModel", "❌ Prefetch failed", e)
                }
            }
        }

        /**
         * Check offline cache status
         */
        fun checkCacheStatus(zone: Int) {
            viewModelScope.launch {
                try {
                    val status = prayerTimesRepository.checkOfflineCacheStatus(zone)
                    _cacheStatus.value = status
                    Log.d("SettingsViewModel", "📊 Cache status: ${status.cachedMonths}/${status.totalMonths} months cached")
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "❌ Failed to check cache status", e)
                }
            }
        }

        /**
         * Reset prefetch state
         */
        fun resetPrefetchState() {
            _prefetchState.value = PrefetchState.Idle
        }

        /**
         * Clear all cached data (both prayer times and hijri dates)
         */
        suspend fun clearAllCachedData() {
            try {
                Log.d("SettingsViewModel", "🧹 Starting cache cleanup...")
                
                // Clear prayer times cache
                prayerTimesRepository.clearAllPrayerTimesCache()
                
                // Clear hijri dates cache
                hijriDateRepository.clearAllHijriCache()
                
                Log.d("SettingsViewModel", "✅ All cached data cleared successfully")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Failed to clear cached data", e)
                throw e // Re-throw so the UI can handle the error
            }
        }

        /**
         * Get cache statistics
         */
        suspend fun getCacheStats(): CacheStats {
            return try {
                CacheStats(
                    prayerTimesCount = prayerTimesRepository.getPrayerTimesCacheCount(),
                    hijriDatesCount = hijriDateRepository.getHijriCacheCount()
                )
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Failed to get cache stats", e)
                CacheStats(0, 0)
            }
        }
    }

/**
 * Cache statistics
 */
data class CacheStats(
    val prayerTimesCount: Int,
    val hijriDatesCount: Int,
)

/**
 * State for offline data prefetch operation
 */
sealed class PrefetchState {
    object Idle : PrefetchState()
    object Loading : PrefetchState()
    data class Success(val result: DirectScrapingService.PrefetchResult) : PrefetchState()
    data class Error(val message: String) : PrefetchState()
}

