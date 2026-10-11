package com.mosque.prayerclock.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.PrayerServiceType
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.data.model.PrayerTimesWithIqamah
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.data.model.WeatherInfo
import com.mosque.prayerclock.data.model.WeatherProvider
import com.mosque.prayerclock.data.network.NetworkResult
import com.mosque.prayerclock.data.repository.HijriDateRepository
import com.mosque.prayerclock.data.repository.PrayerTimesRepository
import com.mosque.prayerclock.data.repository.SettingsRepository
import com.mosque.prayerclock.data.repository.WeatherRepository
import com.mosque.prayerclock.utils.NetworkStatus
import com.mosque.prayerclock.utils.SystemChangeEvent
import com.mosque.prayerclock.utils.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import com.mosque.prayerclock.utils.calculateNextPrayer
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val prayerTimesRepository: PrayerTimesRepository,
        private val settingsRepository: SettingsRepository,
        private val weatherRepository: WeatherRepository,
        val hijriDateRepository: HijriDateRepository,
    ) : ViewModel() {
        // Add unique identifier to track ViewModel instances
        private val viewModelId = System.currentTimeMillis().toString().takeLast(4)

        private val _settingsLoaded = MutableStateFlow(false)
        val settingsLoaded: StateFlow<Boolean> = _settingsLoaded.asStateFlow()

        private val _settings = MutableStateFlow(AppSettings())
        val settings: StateFlow<AppSettings> = _settings.asStateFlow()

        private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
        // Calculate displayed iqamah times from persisted settings, including changes
        // made while prayer times are loading or while the settings screen is open.
        val uiState: StateFlow<MainUiState> =
            combine(_uiState, settingsRepository.getSettings(), flow {
                while (true) {
                    val now = Clock.System.now()
                    val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                    if (lastLoadedDate != null && lastLoadedDate != today) loadPrayerTimes()
                    emit(now)
                    delay(1000)
                }
            }) { state, currentSettings, currentTime ->
                when (state) {
                    is MainUiState.Success -> {
                        val prayerTimes = state.prayerTimes.prayerTimes.withIqamahTimes(currentSettings)
                        val nextPrayer = calculateNextPrayer(prayerTimes, currentSettings, currentTime)
                        val nextDayFajr = if (nextPrayer == PrayerType.FAJR && isAllPrayersCompleted(prayerTimes)) {
                            state.nextDayFajr?.prayerTimes?.withIqamahTimes(currentSettings)
                        } else null
                        state.copy(
                            prayerTimes = prayerTimes,
                            nextPrayer = nextPrayer,
                            nextDayFajr = nextDayFajr,
                        )
                    }
                    else -> state
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = MainUiState.Loading,
            )

        private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading())
        val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

        @Volatile private var isLoadingPrayerTimes: Boolean = false

        // Cache tomorrow's prayer times to avoid repeated fetching after Isha
        private var cachedTomorrowPrayerTimes: PrayerTimes? = null
        private var cachedTomorrowDate: String? = null
        private var lastTomorrowAttempt: kotlinx.datetime.Instant? = null

        // Network status tracking for auto-refresh after connectivity restored
        private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.Unavailable)
        val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

        // Track the date when prayer times were last successfully loaded
        // Used to detect if we need to refresh after a time sync
        @Volatile private var lastLoadedDate: String? = null

        // Event to trigger Hijri date refresh in UI components
        private val _hijriRefreshTrigger = MutableSharedFlow<Unit>(replay = 0)
        val hijriRefreshTrigger = _hijriRefreshTrigger.asSharedFlow()

        init {
            viewModelScope.launch {
                settingsRepository.getSettings().collect {
                    _settings.value = it
                    _settingsLoaded.value = true
                }
            }
            // Fetch tomorrow separately so a slow request cannot stop prayer transitions.
            viewModelScope.launch {
                uiState.collect { state ->
                    if (state is MainUiState.Success && state.nextPrayer == PrayerType.FAJR &&
                        state.nextDayFajr == null && isAllPrayersCompleted(state.prayerTimes)
                    ) {
                        val tomorrow = getTomorrowPrayerTimesOptimized(settings.value)
                        val latest = _uiState.value
                        if (tomorrow != null && latest is MainUiState.Success &&
                            latest.prayerTimes.id == state.prayerTimes.id
                        ) {
                            _uiState.value = latest.copy(nextDayFajr = tomorrow)
                        }
                    }
                }
            }
        }

        /**
         * Handle system change events (time/date/timezone changes).
         * Called from UI when SystemChangeMonitor emits events.
         */
        fun onSystemChangeEvent(event: SystemChangeEvent) {
            Log.i("MainViewModel", "System change event received: $event")

            when (event) {
                is SystemChangeEvent.TimeChanged,
                is SystemChangeEvent.DateChanged,
                is SystemChangeEvent.TimezoneChanged,
                is SystemChangeEvent.SignificantTimeJump -> {
                    // Check if the current date is different from when we last loaded
                    val currentDate = Clock.System
                        .now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                        .toString()

                    val needsRefresh = lastLoadedDate != currentDate || lastLoadedDate == null

                    Log.i(
                        "MainViewModel",
                        "Time sync detected - lastLoadedDate: $lastLoadedDate, currentDate: $currentDate, needsRefresh: $needsRefresh"
                    )

                    if (needsRefresh) {
                        refreshAllData()
                    }
                }
            }
        }

        /**
         * Handle network status changes.
         * Called from UI when NetworkMonitor emits events.
         */
        fun onNetworkStatusChange(status: NetworkStatus) {
            val previousStatus = _networkStatus.value
            _networkStatus.value = status

            Log.i("MainViewModel", "Network status changed: $previousStatus -> $status")

            // When network becomes available, check if we need to refresh data
            if (status == NetworkStatus.Available && previousStatus == NetworkStatus.Unavailable) {
                Log.i("MainViewModel", "Network restored - checking if data refresh needed")

                viewModelScope.launch(Dispatchers.IO) {
                    // Check if we have valid cached prayer times for today
                    val hasValidCache = prayerTimesRepository.hasCachedPrayerTimesForCurrentSettings()

                    if (!hasValidCache) {
                        Log.i("MainViewModel", "No valid cache found after network restore - refreshing data")
                        refreshAllData()
                    } else {
                        Log.d("MainViewModel", "Cache is valid - no refresh needed")
                    }
                }
            }
        }

        /**
         * Refresh all data: prayer times, Hijri date, and weather.
         * Invalidates caches and triggers fresh fetch.
         */
        private fun refreshAllData() {
            Log.i("MainViewModel", "Refreshing all data...")

            viewModelScope.launch(Dispatchers.IO) {
                // Invalidate prayer times cache to force fresh fetch
                prayerTimesRepository.invalidatePrayerTimesCache()

                // Also clear tomorrow's cache
                cachedTomorrowPrayerTimes = null
                cachedTomorrowDate = null
                lastTomorrowAttempt = null

                // Reset loading flag to allow new load
                isLoadingPrayerTimes = false

                // Trigger prayer times reload
                loadPrayerTimes()

                // Emit event to trigger Hijri date refresh in UI components
                _hijriRefreshTrigger.emit(Unit)

                Log.i("MainViewModel", "Data refresh triggered")
            }
        }

        /**
         * Force refresh of all data. Can be called from UI if needed.
         */
        fun forceRefresh() {
            Log.i("MainViewModel", "Force refresh requested")
            refreshAllData()
        }

        // Function to recalculate next prayer - can be called from UI when needed
        fun updateNextPrayer() {
            val currentState = uiState.value
            if (currentState is MainUiState.Success) {
                val newNextPrayer = calculateNextPrayer(currentState.prayerTimes, settings.value)
                if (newNextPrayer != currentState.nextPrayer) {
                    _uiState.value = currentState.copy(nextPrayer = newNextPrayer)
                }
            }
        }

        fun loadPrayerTimes() {
            if (isLoadingPrayerTimes) return

            isLoadingPrayerTimes = true
            cachedTomorrowPrayerTimes = null
            cachedTomorrowDate = null
            lastTomorrowAttempt = null

            viewModelScope.launch(Dispatchers.IO) {
                // Wait for saved settings instead of using the StateFlow's startup defaults.
                val currentSettings = settingsRepository.getSettings().first()

                // Use the centralized repository method that handles all prayer service types
                prayerTimesRepository.getTodayPrayerTimesFromSettings().collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> {
                            _uiState.value = MainUiState.Loading
                        }
                        is NetworkResult.Success -> {
                            // Track the date when prayer times were successfully loaded
                            lastLoadedDate = Clock.System
                                .now()
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                .date
                                .toString()

                            // Convert to PrayerTimesWithIqamah using current settings
                            // Iqamah times are calculated dynamically from settings
                            val prayerTimesWithIqamah = result.data.withIqamahTimes(currentSettings)

                            val nextPrayer = calculateNextPrayer(prayerTimesWithIqamah, currentSettings)

                            _uiState.value =
                                MainUiState.Success(
                                    prayerTimes = prayerTimesWithIqamah,
                                    nextPrayer = nextPrayer,
                                )

                            Log.d("MainViewModel", "Prayer times loaded successfully for date: $lastLoadedDate")
                        }
                        is NetworkResult.Error -> {
                            Log.e("MainViewModel", "Prayer times loading failed: ${result.message}")
                            _uiState.value = MainUiState.Error(result.message)
                        }
                    }
                }

                // Load weather data if enabled and start hourly refresh
                if (currentSettings.showWeather && currentSettings.weatherCity.isNotBlank()) {
                    loadWeatherData(
                        currentSettings.weatherCity,
                        currentSettings.weatherCountry,
                        currentSettings.weatherProvider,
                    )
                    weatherRepository.startHourlyWeatherRefresh(
                        currentSettings.weatherCity,
                        currentSettings.weatherCountry,
                        currentSettings.weatherProvider,
                    ) { result ->
                        // Update weather state when refresh occurs
                        when (result) {
                            is NetworkResult.Loading -> _weatherState.value = WeatherUiState.Loading()
                            is NetworkResult.Success -> _weatherState.value = WeatherUiState.Success(result.data)
                            is NetworkResult.Error -> _weatherState.value = WeatherUiState.Error(result.message)
                        }
                    }
                } else {
                    weatherRepository.stopAllWeatherRefreshJobs()
                    _weatherState.value = WeatherUiState.Error("Weather disabled")
                }
                isLoadingPrayerTimes = false
            }
        }

        private fun loadWeatherData(
            city: String,
            country: String,
            provider: WeatherProvider,
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                when (provider) {
                    WeatherProvider.WEATHER_API -> {
                        weatherRepository.getCurrentWeather(city, country).collect { result ->
                            handleWeatherResult(result)
                        }
                    }
                    WeatherProvider.OPEN_WEATHER_MAP -> {
                        weatherRepository.getCurrentWeather(city, country).collect { result ->
                            handleWeatherResult(result)
                        }
                    }
                }
            }
        }

        private fun handleWeatherResult(result: NetworkResult<WeatherInfo>) {
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

        // Weather job management moved to WeatherRepository

        override fun onCleared() {
            super.onCleared()
        }

        private fun isAllPrayersCompleted(prayerTimes: PrayerTimesWithIqamah): Boolean {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val currentTime = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"

            // Check if current time is after Isha Azan (all prayers for the day are done)
            return TimeUtils.compareTimeStrings(currentTime, prayerTimes.ishaAzan) > 0
        }

        private suspend fun getTomorrowPrayerTimesOptimized(settings: AppSettings): PrayerTimesWithIqamah? {
            val tomorrow =
                Clock.System.now().plus(
                    1,
                    DateTimeUnit.DAY,
                    TimeZone.currentSystemDefault(),
                )
            val tomorrowDate =
                tomorrow
                    .toLocalDateTime(
                        TimeZone.currentSystemDefault(),
                    ).date
                    .toString()

            // Check if we already have cached tomorrow's prayer times for today
            val cachedTimes = cachedTomorrowPrayerTimes
            if (cachedTimes != null && cachedTomorrowDate == tomorrowDate) {
                return cachedTimes.withIqamahTimes(settings)
            }

            val now = Clock.System.now()
            if (lastTomorrowAttempt?.let { (now - it).inWholeSeconds in 0..59 } == true) return null
            lastTomorrowAttempt = now
            return try {
                // Fetch tomorrow's prayer times
                val result = prayerTimesRepository.getTomorrowPrayerTimesFromSettings()

                // Wait for the first emission from the Flow
                var tomorrowTimes: PrayerTimesWithIqamah? = null
                result.collect { networkResult ->
                    when (networkResult) {
                        is NetworkResult.Success -> {
                            // Convert to PrayerTimesWithIqamah using current settings
                            tomorrowTimes = networkResult.data.withIqamahTimes(settings)

                            // Cache the result to avoid repeated fetching
                            cachedTomorrowPrayerTimes = networkResult.data
                            cachedTomorrowDate = tomorrowDate
                            return@collect
                        }
                        is NetworkResult.Error -> {
                            // Repository handles all modes, including manual, so errors are genuine
                            Log.e("MainViewModel", "Failed to fetch tomorrow's prayer times: ${networkResult.message}")
                            tomorrowTimes = null
                            return@collect
                        }
                        is NetworkResult.Loading -> {
                            // Continue waiting
                        }
                    }
                }
                tomorrowTimes
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching tomorrow's prayer times: ${e.message}")
                null
            }
        }
    }

sealed class MainUiState {
    object Loading : MainUiState()

    data class Success(
        val prayerTimes: PrayerTimesWithIqamah,
        val nextPrayer: PrayerType?,
        val nextDayFajr: PrayerTimesWithIqamah? = null, // For when next prayer is tomorrow's Fajr
    ) : MainUiState()

    data class Error(
        val message: String,
    ) : MainUiState()
}

sealed class WeatherUiState {
    class Loading : WeatherUiState()

    data class Success(
        val weatherInfo: WeatherInfo,
    ) : WeatherUiState()

    data class Error(
        val message: String,
    ) : WeatherUiState()
}
