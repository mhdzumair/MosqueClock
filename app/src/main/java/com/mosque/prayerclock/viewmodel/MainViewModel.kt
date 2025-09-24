package com.mosque.prayerclock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosque.prayerclock.data.model.*
import com.mosque.prayerclock.data.network.NetworkResult
import com.mosque.prayerclock.data.repository.HijriDateRepository
import com.mosque.prayerclock.data.repository.PrayerTimesRepository
import com.mosque.prayerclock.data.repository.SettingsRepository
import com.mosque.prayerclock.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

@HiltViewModel
class MainViewModel
@Inject
constructor(
        private val prayerTimesRepository: PrayerTimesRepository,
        private val settingsRepository: SettingsRepository,
        private val weatherRepository: WeatherRepository,
        val hijriDateRepository: HijriDateRepository,
) : ViewModel() {
    val settings =
            settingsRepository
                    .getSettings()
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = AppSettings(),
                    )

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading())
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

    @Volatile private var isLoadingPrayerTimes: Boolean = false
    private var weatherRefreshJob: Job? = null

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
        if (isLoadingPrayerTimes) return
        isLoadingPrayerTimes = true
        viewModelScope.launch {
            val currentSettings = settings.value

            if (currentSettings.prayerServiceType == PrayerServiceType.MANUAL) {
                val manualPrayerTimes = createManualPrayerTimes(currentSettings)
                val nextPrayer = calculateNextPrayer(manualPrayerTimes)
                
                // If next prayer is Fajr and all today's prayers are done, get tomorrow's times
                val nextDayFajr = if (nextPrayer == PrayerType.FAJR && isAllPrayersCompleted(manualPrayerTimes)) {
                    createManualPrayerTimesForTomorrow(currentSettings)
                } else null
                
                _uiState.value =
                        MainUiState.Success(
                                prayerTimes = manualPrayerTimes,
                                nextPrayer = nextPrayer,
                                nextDayFajr = nextDayFajr,
                        )
            } else {
                // Use the new settings-based prayer times method
                prayerTimesRepository.getTodayPrayerTimesFromSettings().collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> {
                            _uiState.value = MainUiState.Loading
                        }
                        is NetworkResult.Success -> {
                            val adjustedPrayerTimes =
                                    adjustIqamahTimes(result.data, currentSettings)
                            val nextPrayer = calculateNextPrayer(adjustedPrayerTimes)
                            
                            // If next prayer is Fajr and all today's prayers are done, fetch tomorrow's times
                            val nextDayFajr = if (nextPrayer == PrayerType.FAJR && isAllPrayersCompleted(adjustedPrayerTimes)) {
                                fetchTomorrowPrayerTimes(currentSettings)
                            } else null
                            
                            _uiState.value =
                                    MainUiState.Success(
                                            prayerTimes = adjustedPrayerTimes,
                                            nextPrayer = nextPrayer,
                                            nextDayFajr = nextDayFajr,
                                    )
                        }
                        is NetworkResult.Error -> {
                            _uiState.value = MainUiState.Error(result.message)
                        }
                    }
                }
            }

            // Load weather data if enabled and start hourly refresh
            if (currentSettings.showWeather) {
                loadWeatherData(
                        currentSettings.weatherCity,
                        currentSettings.weatherCountry,
                        currentSettings.weatherProvider
                )
                startHourlyWeatherRefresh(
                        currentSettings.weatherCity,
                        currentSettings.weatherCountry,
                        currentSettings.weatherProvider
                )
            } else {
                stopWeatherRefresh()
            }
            isLoadingPrayerTimes = false
        }
    }

    private fun loadWeatherData(
            city: String,
            country: String,
            provider: WeatherProvider,
    ) {
        viewModelScope.launch {
            when (provider) {
                WeatherProvider.MOSQUE_CLOCK -> {
                    weatherRepository.getCurrentWeatherByCity(city).collect { result ->
                        if (result is NetworkResult.Error) {
                            weatherRepository.getCurrentWeather(city, country).collect {
                                    fallbackResult ->
                                handleWeatherResult(fallbackResult)
                            }
                        } else {
                            handleWeatherResult(result)
                        }
                    }
                }
                WeatherProvider.OPEN_WEATHER -> {
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

    private fun startHourlyWeatherRefresh(
        city: String,
        country: String,
        provider: WeatherProvider
    ) {
        stopWeatherRefresh() // Cancel any existing refresh job
        
        weatherRefreshJob = viewModelScope.launch {
            while (true) {
                delay(60 * 60 * 1000L) // Wait 1 hour (60 minutes * 60 seconds * 1000 milliseconds)
                loadWeatherData(city, country, provider)
            }
        }
    }

    private fun stopWeatherRefresh() {
        weatherRefreshJob?.cancel()
        weatherRefreshJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopWeatherRefresh()
    }

    private fun calculateNextPrayer(prayerTimes: PrayerTimes): PrayerType? {
        val now =
                Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val currentTime =
                "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
        val isFriday = now.dayOfWeek == DayOfWeek.FRIDAY

        // Check if we're in the 5-minute buffer after any Iqamah time
        val prayers =
                listOf(
                        PrayerType.FAJR to Pair(prayerTimes.fajrAzan, prayerTimes.fajrIqamah),
                        PrayerType.SUNRISE to Pair(prayerTimes.sunrise, null), // Sunrise has no Iqamah
                        PrayerType.DHUHR to
                                Pair(
                                        prayerTimes.dhuhrAzan,
                                        if (isFriday) null else prayerTimes.dhuhrIqamah
                                ), // No Iqamah on Friday
                        PrayerType.ASR to Pair(prayerTimes.asrAzan, prayerTimes.asrIqamah),
                        PrayerType.MAGHRIB to
                                Pair(prayerTimes.maghribAzan, prayerTimes.maghribIqamah),
                        PrayerType.ISHA to Pair(prayerTimes.ishaAzan, prayerTimes.ishaIqamah),
                )

        // First check if we're between Azan and Iqamah time OR within 5 minutes after Iqamah
        for ((prayerType, times) in prayers) {
            val azanTime = times.first
            val iqamahTime = times.second

            if (iqamahTime != null) { // Only check if Iqamah time exists
                val bufferTime = addMinutesToTime(iqamahTime, 5)

                // Check if we're between Azan and Iqamah + 5min buffer
                if (compareTimeStrings(azanTime, currentTime) <= 0 &&
                                compareTimeStrings(currentTime, bufferTime) < 0
                ) {
                    // We're in the current prayer period (from Azan to Iqamah + 5min)
                    return prayerType
                }
            } else if (prayerType == PrayerType.DHUHR && isFriday) {
                // For Friday, give 1 hour after Azan for Bayan
                val bayanEndTime = addMinutesToTime(times.first, 60)
                if (compareTimeStrings(times.first, currentTime) <= 0 &&
                                compareTimeStrings(currentTime, bayanEndTime) < 0
                ) {
                    return prayerType
                }
            }
        }

        // Regular logic: find next prayer that hasn't started yet
        val nextTodayPrayer = prayers
                .firstOrNull { (_, times) -> compareTimeStrings(currentTime, times.first) < 0 }
                ?.first
        
        // If no more prayers today, next prayer is Fajr of tomorrow
        return nextTodayPrayer ?: PrayerType.FAJR
    }

    private fun isAllPrayersCompleted(prayerTimes: PrayerTimes): Boolean {
        val now = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val currentTime = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
        
        // Check if current time is after Isha Azan (all prayers for the day are done)
        return compareTimeStrings(currentTime, prayerTimes.ishaAzan) > 0
    }

    private fun createManualPrayerTimesForTomorrow(settings: AppSettings): PrayerTimes {
        // For manual times, tomorrow's times are the same as today's
        // In a real implementation, you might want to calculate based on location
        return createManualPrayerTimes(settings)
    }

    private suspend fun fetchTomorrowPrayerTimes(settings: AppSettings): PrayerTimes? {
        return try {
            // Fetch tomorrow's prayer times from the same API source as today
            val result = prayerTimesRepository.getTomorrowPrayerTimesFromSettings()
            
            // Wait for the first emission from the Flow
            var tomorrowTimes: PrayerTimes? = null
            result.collect { networkResult ->
                when (networkResult) {
                    is NetworkResult.Success -> {
                        tomorrowTimes = adjustIqamahTimes(networkResult.data, settings)
                        return@collect // Exit the collect loop
                    }
                    is NetworkResult.Error -> {
                        // Fallback to manual times if API fails
                        tomorrowTimes = createManualPrayerTimes(settings)
                        return@collect
                    }
                    is NetworkResult.Loading -> {
                        // Continue waiting
                    }
                }
            }
            tomorrowTimes
        } catch (e: Exception) {
            // Fallback to manual times if API fails
            createManualPrayerTimes(settings)
        }
    }

    private fun compareTimeStrings(
            time1: String,
            time2: String,
    ): Int {
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
        val today =
                Clock.System.now()
                        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                        .date
                        .toString()

        return PrayerTimes(
                date = today,
                fajrAzan = settings.manualFajrAzan,
                fajrIqamah = calculateIqamahTime(settings.manualFajrAzan, settings.fajrIqamahGap),
                dhuhrAzan = settings.manualDhuhrAzan,
                dhuhrIqamah =
                        calculateIqamahTime(settings.manualDhuhrAzan, settings.dhuhrIqamahGap),
                asrAzan = settings.manualAsrAzan,
                asrIqamah = calculateIqamahTime(settings.manualAsrAzan, settings.asrIqamahGap),
                maghribAzan = settings.manualMaghribAzan,
                maghribIqamah =
                        calculateIqamahTime(settings.manualMaghribAzan, settings.maghribIqamahGap),
                ishaAzan = settings.manualIshaAzan,
                ishaIqamah = calculateIqamahTime(settings.manualIshaAzan, settings.ishaIqamahGap),
                sunrise = "06:00", // Default sunrise time
                hijriDate = null,
                location = "Manual Setup",
        )
    }

    private fun adjustIqamahTimes(
            prayerTimes: PrayerTimes,
            settings: AppSettings,
    ): PrayerTimes =
            prayerTimes.copy(
                    fajrIqamah = calculateIqamahTime(prayerTimes.fajrAzan, settings.fajrIqamahGap),
                    dhuhrIqamah =
                            calculateIqamahTime(prayerTimes.dhuhrAzan, settings.dhuhrIqamahGap),
                    asrIqamah = calculateIqamahTime(prayerTimes.asrAzan, settings.asrIqamahGap),
                    maghribIqamah =
                            calculateIqamahTime(prayerTimes.maghribAzan, settings.maghribIqamahGap),
                    ishaIqamah = calculateIqamahTime(prayerTimes.ishaAzan, settings.ishaIqamahGap),
            )

    private fun calculateIqamahTime(
            azanTime: String,
            minutesAfter: Int,
    ): String {
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

    private fun addMinutesToTime(
            timeString: String,
            minutesToAdd: Int,
    ): String {
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
        val nextPrayer: PrayerType?,
        val nextDayFajr: PrayerTimes? = null, // For when next prayer is tomorrow's Fajr
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
