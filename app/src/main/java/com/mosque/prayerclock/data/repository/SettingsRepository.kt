package com.mosque.prayerclock.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mosque.prayerclock.data.cache.PrayerTimesCacheInvalidator
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.AppTheme
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.FontSize
import com.mosque.prayerclock.data.model.HijriProvider
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.data.model.PrayerServiceType
import com.mosque.prayerclock.data.model.WeatherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class SettingsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        private val prayerTimesCacheInvalidatorProvider: Provider<PrayerTimesCacheInvalidator>,
    ) {
        private object PreferencesKeys {
            val LANGUAGE = stringPreferencesKey("language")
            val CITY = stringPreferencesKey("city")
            val COUNTRY = stringPreferencesKey("country")
            val LATITUDE = doublePreferencesKey("latitude")
            val LONGITUDE = doublePreferencesKey("longitude")
            val MOSQUE_NAME = stringPreferencesKey("mosque_name")
            val CLOCK_TYPE = stringPreferencesKey("clock_type")
            val THEME = stringPreferencesKey("theme")
            val FONT_SIZE = stringPreferencesKey("font_size")
            val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
            val SHOW_24_HOUR_FORMAT = booleanPreferencesKey("show_24_hour_format")
            val MANUAL_FAJR_AZAN = stringPreferencesKey("manual_fajr_azan")
            val MANUAL_SUNRISE = stringPreferencesKey("manual_sunrise")
            val MANUAL_DHUHR_AZAN = stringPreferencesKey("manual_dhuhr_azan")
            val MANUAL_ASR_AZAN = stringPreferencesKey("manual_asr_azan")
            val MANUAL_MAGHRIB_AZAN = stringPreferencesKey("manual_maghrib_azan")
            val MANUAL_ISHA_AZAN = stringPreferencesKey("manual_isha_azan")
            val FAJR_IQAMAH_GAP = intPreferencesKey("fajr_iqamah_gap")
            val DHUHR_IQAMAH_GAP = intPreferencesKey("dhuhr_iqamah_gap")
            val ASR_IQAMAH_GAP = intPreferencesKey("asr_iqamah_gap")
            val MAGHRIB_IQAMAH_GAP = intPreferencesKey("maghrib_iqamah_gap")
            val ISHA_IQAMAH_GAP = intPreferencesKey("isha_iqamah_gap")
            val REFRESH_INTERVAL = longPreferencesKey("refresh_interval")
            val SHOW_WEATHER = booleanPreferencesKey("show_weather")
            val WEATHER_CITY = stringPreferencesKey("weather_city")
            val WEATHER_COUNTRY = stringPreferencesKey("weather_country")
            val WEATHER_PROVIDER = stringPreferencesKey("weather_provider")
            val USE_API_FOR_HIJRI_DATE = booleanPreferencesKey("use_api_for_hijri_date")
            val HIJRI_PROVIDER = stringPreferencesKey("hijri_provider")
            val MANUAL_HIJRI_DAY = intPreferencesKey("manual_hijri_day")
            val MANUAL_HIJRI_MONTH = intPreferencesKey("manual_hijri_month")
            val MANUAL_HIJRI_YEAR = intPreferencesKey("manual_hijri_year")
            val LAST_UPDATED_GREGORIAN_DATE = stringPreferencesKey("last_updated_gregorian_date")
            val PRAYER_SERVICE_TYPE = stringPreferencesKey("prayer_service_type")
            val SELECTED_ZONE = intPreferencesKey("selected_zone")
            val SELECTED_REGION = stringPreferencesKey("selected_region")
            val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
            val FULL_SCREEN_COUNTDOWN_ENABLED = booleanPreferencesKey("full_screen_countdown_enabled")
            val COLOR_THEME = stringPreferencesKey("color_theme")
            val WEATHER_API_KEY = stringPreferencesKey("weather_api_key")
            val OPENWEATHERMAP_API_KEY = stringPreferencesKey("openweathermap_api_key")
            val MOSQUE_CLOCK_BACKEND_URL = stringPreferencesKey("mosque_clock_backend_url")
            val MOSQUE_CLOCK_BACKEND_API_KEY = stringPreferencesKey("mosque_clock_backend_api_key")
            val JUMMAH_DURATION_MINUTES = intPreferencesKey("jummah_duration_minutes")
            val DUA_DISPLAY_DURATION_MINUTES = intPreferencesKey("dua_display_duration_minutes")
            val SHOW_JUMMAH_SCREEN = booleanPreferencesKey("show_jummah_screen")
        }

        fun getSettings(): Flow<AppSettings> =
            dataStore.data.map { preferences ->
                AppSettings(
                    language =
                        Language.values().find {
                            it.code == preferences[PreferencesKeys.LANGUAGE]
                        }
                            ?: Language.TAMIL,
                    city = preferences[PreferencesKeys.CITY] ?: "Colombo",
                    country = preferences[PreferencesKeys.COUNTRY] ?: "Sri Lanka",
                    latitude = preferences[PreferencesKeys.LATITUDE] ?: 6.9271,
                    longitude = preferences[PreferencesKeys.LONGITUDE] ?: 79.8612,
                    mosqueName =
                        preferences[PreferencesKeys.MOSQUE_NAME]
                            ?: "Dickwella Jummah Mosque",
                    clockType =
                        ClockType.values().find {
                            it.name == preferences[PreferencesKeys.CLOCK_TYPE]
                        }
                            ?: ClockType.DIGITAL,
                    theme =
                        AppTheme.values().find {
                            it.name == preferences[PreferencesKeys.THEME]
                        }
                            ?: AppTheme.DARK,
                    fontSize =
                        FontSize.values().find {
                            it.name == preferences[PreferencesKeys.FONT_SIZE]
                        }
                            ?: FontSize.MEDIUM,
                    showSeconds = preferences[PreferencesKeys.SHOW_SECONDS] ?: true,
                    show24HourFormat =
                        preferences[PreferencesKeys.SHOW_24_HOUR_FORMAT]
                            ?: false,
                    colorTheme = preferences[PreferencesKeys.COLOR_THEME] ?: "classic_mosque",
                    manualFajrAzan = preferences[PreferencesKeys.MANUAL_FAJR_AZAN] ?: "05:30",
                    manualSunrise = preferences[PreferencesKeys.MANUAL_SUNRISE] ?: "06:15",
                    manualDhuhrAzan = preferences[PreferencesKeys.MANUAL_DHUHR_AZAN] ?: "12:15",
                    manualAsrAzan = preferences[PreferencesKeys.MANUAL_ASR_AZAN] ?: "15:30",
                    manualMaghribAzan =
                        preferences[PreferencesKeys.MANUAL_MAGHRIB_AZAN]
                            ?: "18:30",
                    manualIshaAzan = preferences[PreferencesKeys.MANUAL_ISHA_AZAN] ?: "19:45",
                    fajrIqamahGap = preferences[PreferencesKeys.FAJR_IQAMAH_GAP] ?: 20,
                    dhuhrIqamahGap = preferences[PreferencesKeys.DHUHR_IQAMAH_GAP] ?: 10,
                    asrIqamahGap = preferences[PreferencesKeys.ASR_IQAMAH_GAP] ?: 10,
                    maghribIqamahGap = preferences[PreferencesKeys.MAGHRIB_IQAMAH_GAP] ?: 10,
                    ishaIqamahGap = preferences[PreferencesKeys.ISHA_IQAMAH_GAP] ?: 10,
                    refreshInterval =
                        preferences[PreferencesKeys.REFRESH_INTERVAL]
                            ?: 24 * 60 * 60 * 1000L,
                    showWeather = preferences[PreferencesKeys.SHOW_WEATHER] ?: false,
                    weatherCity = preferences[PreferencesKeys.WEATHER_CITY] ?: "",
                    weatherCountry =
                        preferences[PreferencesKeys.WEATHER_COUNTRY]
                            ?: "Sri Lanka",
                    weatherProvider =
                        WeatherProvider.values().find {
                            it.name == preferences[PreferencesKeys.WEATHER_PROVIDER]
                        }
                            ?: WeatherProvider.WEATHER_API,
                    hijriProvider =
                        HijriProvider.values().find {
                            it.name == preferences[PreferencesKeys.HIJRI_PROVIDER]
                        }
                            ?: HijriProvider.MANUAL,
                    manualHijriDay = preferences[PreferencesKeys.MANUAL_HIJRI_DAY] ?: 7,
                    manualHijriMonth = preferences[PreferencesKeys.MANUAL_HIJRI_MONTH] ?: 2,
                    manualHijriYear = preferences[PreferencesKeys.MANUAL_HIJRI_YEAR] ?: 1447,
                    lastUpdatedGregorianDate =
                        preferences[PreferencesKeys.LAST_UPDATED_GREGORIAN_DATE]
                            ?: "2025-08-01",
                    prayerServiceType =
                        PrayerServiceType.values().find {
                            it.name == preferences[PreferencesKeys.PRAYER_SERVICE_TYPE]
                        }
                            ?: PrayerServiceType.MANUAL,
                    selectedZone = preferences[PreferencesKeys.SELECTED_ZONE] ?: 1,
                    selectedRegion = preferences[PreferencesKeys.SELECTED_REGION] ?: "Colombo",
                    soundEnabled = preferences[PreferencesKeys.SOUND_ENABLED] ?: true,
                    fullScreenCountdownEnabled = preferences[PreferencesKeys.FULL_SCREEN_COUNTDOWN_ENABLED] ?: true,
                    weatherApiKey = preferences[PreferencesKeys.WEATHER_API_KEY] ?: "",
                    openWeatherMapApiKey = preferences[PreferencesKeys.OPENWEATHERMAP_API_KEY] ?: "",
                    mosqueClockBackendUrl = preferences[PreferencesKeys.MOSQUE_CLOCK_BACKEND_URL] ?: "",
                    mosqueClockBackendApiKey = preferences[PreferencesKeys.MOSQUE_CLOCK_BACKEND_API_KEY] ?: "",
                    jummahDurationMinutes = preferences[PreferencesKeys.JUMMAH_DURATION_MINUTES] ?: 60,
                    duaDisplayDurationMinutes = preferences[PreferencesKeys.DUA_DISPLAY_DURATION_MINUTES] ?: 5,
                    showJummahScreen = preferences[PreferencesKeys.SHOW_JUMMAH_SCREEN] ?: true,
                )
            }

        suspend fun updateLanguage(language: Language) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.LANGUAGE] = language.code }
        }

        suspend fun updateCountry(country: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.COUNTRY] = country }
        }

        suspend fun updateCity(city: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.CITY] = city }
        }

        suspend fun updateMosqueName(mosqueName: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.MOSQUE_NAME] = mosqueName }
        }

        suspend fun updateClockType(clockType: ClockType) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.CLOCK_TYPE] = clockType.name }
        }

        suspend fun updateTheme(theme: AppTheme) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.THEME] = theme.name }
        }

        suspend fun updateFontSize(fontSize: FontSize) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.FONT_SIZE] = fontSize.name }
        }

        suspend fun updateShowSeconds(showSeconds: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.SHOW_SECONDS] = showSeconds }
        }

        suspend fun updateShow24HourFormat(show24Hour: Boolean) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.SHOW_24_HOUR_FORMAT] = show24Hour
            }
        }

        suspend fun updateRefreshInterval(refreshInterval: Long) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.REFRESH_INTERVAL] = refreshInterval
            }
        }

        suspend fun updateManualTime(
            timeType: String,
            time: String,
        ) {
            dataStore.edit { preferences ->
                when (timeType) {
                    "fajrAzan" -> preferences[PreferencesKeys.MANUAL_FAJR_AZAN] = time
                    "sunrise" -> preferences[PreferencesKeys.MANUAL_SUNRISE] = time
                    "dhuhrAzan" -> preferences[PreferencesKeys.MANUAL_DHUHR_AZAN] = time
                    "asrAzan" -> preferences[PreferencesKeys.MANUAL_ASR_AZAN] = time
                    "maghribAzan" -> preferences[PreferencesKeys.MANUAL_MAGHRIB_AZAN] = time
                    "ishaAzan" -> preferences[PreferencesKeys.MANUAL_ISHA_AZAN] = time
                }
            }
            // Invalidate prayer times cache when manual times are updated
            prayerTimesCacheInvalidatorProvider.get().invalidatePrayerTimesCache()
        }

        suspend fun updateIqamahGap(
            prayerType: String,
            gap: Int,
        ) {
            dataStore.edit { preferences ->
                when (prayerType) {
                    "fajr" -> preferences[PreferencesKeys.FAJR_IQAMAH_GAP] = gap
                    "dhuhr" -> preferences[PreferencesKeys.DHUHR_IQAMAH_GAP] = gap
                    "asr" -> preferences[PreferencesKeys.ASR_IQAMAH_GAP] = gap
                    "maghrib" -> preferences[PreferencesKeys.MAGHRIB_IQAMAH_GAP] = gap
                    "isha" -> preferences[PreferencesKeys.ISHA_IQAMAH_GAP] = gap
                }
            }
            // Invalidate prayer times cache when iqamah gaps are updated
            prayerTimesCacheInvalidatorProvider.get().invalidatePrayerTimesCache()
        }

        suspend fun updateHijriDate(
            day: Int,
            month: Int,
            year: Int,
            gregorianDate: String,
        ) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.MANUAL_HIJRI_DAY] = day
                preferences[PreferencesKeys.MANUAL_HIJRI_MONTH] = month
                preferences[PreferencesKeys.MANUAL_HIJRI_YEAR] = year
                preferences[PreferencesKeys.LAST_UPDATED_GREGORIAN_DATE] = gregorianDate
            }
        }

        suspend fun updateUseApiForHijriDate(useApi: Boolean) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.USE_API_FOR_HIJRI_DATE] = useApi
            }
        }

        suspend fun updateHijriProvider(provider: HijriProvider) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.HIJRI_PROVIDER] = provider.name
            }
        }

        suspend fun updateShowWeather(showWeather: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.SHOW_WEATHER] = showWeather }
        }

        suspend fun updateWeatherCity(city: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.WEATHER_CITY] = city }
        }

        suspend fun updateWeatherCountry(country: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.WEATHER_COUNTRY] = country }
        }

        suspend fun updateWeatherProvider(provider: WeatherProvider) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.WEATHER_PROVIDER] = provider.name
            }
        }

        suspend fun updatePrayerServiceType(serviceType: PrayerServiceType) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.PRAYER_SERVICE_TYPE] = serviceType.name
            }
            // Invalidate prayer times cache when prayer service type changes
            prayerTimesCacheInvalidatorProvider.get().invalidatePrayerTimesCache()
        }

        suspend fun updateSelectedZone(zone: Int) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.SELECTED_ZONE] = zone }
            // Invalidate prayer times cache when zone changes (affects cache key)
            prayerTimesCacheInvalidatorProvider.get().invalidatePrayerTimesCache()
        }

        suspend fun updateSelectedRegion(region: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.SELECTED_REGION] = region }
            // Invalidate prayer times cache when region changes (affects cache key)
            prayerTimesCacheInvalidatorProvider.get().invalidatePrayerTimesCache()
        }

        suspend fun updateSoundEnabled(soundEnabled: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.SOUND_ENABLED] = soundEnabled }
        }

        suspend fun updateFullScreenCountdownEnabled(enabled: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.FULL_SCREEN_COUNTDOWN_ENABLED] = enabled }
        }

        suspend fun updateColorTheme(themeId: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.COLOR_THEME] = themeId }
        }

        suspend fun updateWeatherApiKey(apiKey: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.WEATHER_API_KEY] = apiKey }
        }

        suspend fun updateOpenWeatherMapApiKey(apiKey: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.OPENWEATHERMAP_API_KEY] = apiKey }
        }

        suspend fun updateMosqueClockBackendUrl(url: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.MOSQUE_CLOCK_BACKEND_URL] = url }
        }

        suspend fun updateMosqueClockBackendApiKey(apiKey: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.MOSQUE_CLOCK_BACKEND_API_KEY] = apiKey }
        }

        suspend fun updateJummahDurationMinutes(minutes: Int) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.JUMMAH_DURATION_MINUTES] = minutes }
        }

        suspend fun updateDuaDisplayDurationMinutes(minutes: Int) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.DUA_DISPLAY_DURATION_MINUTES] = minutes }
        }

        suspend fun updateShowJummahScreen(show: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.SHOW_JUMMAH_SCREEN] = show }
        }
    }
