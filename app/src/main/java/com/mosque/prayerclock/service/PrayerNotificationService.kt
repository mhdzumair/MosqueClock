package com.mosque.prayerclock.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.data.repository.PrayerTimesRepository
import com.mosque.prayerclock.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class PrayerNotificationService : Service() {
    companion object {
        private const val TAG = "PrayerNotificationService"
        private const val CHECK_INTERVAL_MS = 10_000L // Check every 10 seconds (much more efficient)
        private const val COUNTDOWN_CHECK_INTERVAL_MS = 1_000L // Only use 1-second interval during countdown
        private const val COUNTDOWN_START_MINUTES = 1 // Start countdown 1 minute before prayer
    }

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var prayerTimesRepository: PrayerTimesRepository

    private lateinit var soundManager: SoundManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitoringJob: Job? = null
    private var lastCheckedSecond = -1
    
    // Caching to reduce expensive operations
    private var cachedSettings: AppSettings? = null
    private var cachedPrayerTimes: PrayerTimes? = null
    private var lastSettingsUpdate = 0L
    private var lastPrayerTimesUpdate = 0L
    private val cacheValidityMs = 60_000L // Cache for 1 minute
    
    // Smart polling state
    private var isInCountdownMode = false
    private var countdownStartTime = 0L

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)

        Log.d(TAG, "Prayer notification service started")

        // Initialize SoundManager manually
        soundManager = SoundManager(applicationContext)

        startPrayerMonitoring()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Prayer notification service destroyed")
        stopPrayerMonitoring()
        serviceScope.cancel()
        soundManager.cleanup()
    }

    override fun onBind(intent: Intent): IBinder? = null

    /**
     * Get settings with caching to reduce database queries
     */
    private suspend fun getCachedSettings(): AppSettings {
        val currentTime = System.currentTimeMillis()
        
        if (cachedSettings == null || currentTime - lastSettingsUpdate > cacheValidityMs) {
            cachedSettings = settingsRepository.getSettings().first()
            lastSettingsUpdate = currentTime
        }
        
        return cachedSettings!!
    }

    /**
     * Get prayer times with caching to reduce API calls
     */
    private suspend fun getCachedPrayerTimes(): PrayerTimes? {
        val currentTime = System.currentTimeMillis()
        
        if (cachedPrayerTimes == null || currentTime - lastPrayerTimesUpdate > cacheValidityMs) {
            prayerTimesRepository.getTodayPrayerTimesFromSettings().collect { result ->
                if (result is com.mosque.prayerclock.data.network.NetworkResult.Success) {
                    cachedPrayerTimes = result.data
                    lastPrayerTimesUpdate = currentTime
                }
            }
        }
        
        return cachedPrayerTimes
    }

    /**
     * Start monitoring for prayer times
     */
    private fun startPrayerMonitoring() {
        monitoringJob?.cancel()

        monitoringJob =
            serviceScope.launch {
                Log.d(TAG, "Starting prayer monitoring coroutine")

                // Start the monitoring loop with smart polling
                while (isActive) {
                    try {
                        // Get cached settings (reduces database queries)
                        val settings = getCachedSettings()
                        
                        if (settings.soundEnabled) {
                            val now = Clock.System.now()
                            val currentDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
                            
                            // Get cached prayer times (reduces API calls)
                            val prayerTimes = getCachedPrayerTimes()
                            
                            if (prayerTimes != null) {
                                // Check if we're approaching prayer time (within 1 minute)
                                val isApproachingPrayer = isApproachingAnyPrayerTime(currentDateTime, prayerTimes)
                                
                                if (isApproachingPrayer && !isInCountdownMode) {
                                    // Switch to high-frequency polling for countdown
                                    isInCountdownMode = true
                                    countdownStartTime = System.currentTimeMillis()
                                } else if (!isApproachingPrayer && isInCountdownMode) {
                                    // Switch back to low-frequency polling
                                    isInCountdownMode = false
                                }
                                
                                // Only do expensive checks if needed
                                if (isInCountdownMode) {
                                    val currentSecond =
                                        currentDateTime.hour * 3600 + currentDateTime.minute * 60 + currentDateTime.second
                                    
                                    if (currentSecond != lastCheckedSecond) {
                                        lastCheckedSecond = currentSecond
                                        checkPrayerTime(currentDateTime, settings, prayerTimes)
                                    }
                                }
                            }
                        } else {
                            // If sound is disabled, use longer intervals
                            isInCountdownMode = false
                        }

                        // Use smart delay based on current mode
                        val delayMs = if (isInCountdownMode) COUNTDOWN_CHECK_INTERVAL_MS else CHECK_INTERVAL_MS
                        delay(delayMs)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in monitoring loop", e)
                        delay(CHECK_INTERVAL_MS)
                    }
                }
            }
    }

    /**
     * Check if current time is approaching any prayer time (within COUNTDOWN_START_MINUTES)
     */
    private fun isApproachingAnyPrayerTime(currentDateTime: LocalDateTime, prayerTimes: PrayerTimes): Boolean {
        val prayerTimeStrings = listOf(
            prayerTimes.fajrAzan,
            prayerTimes.dhuhrAzan,
            prayerTimes.asrAzan,
            prayerTimes.maghribAzan,
            prayerTimes.ishaAzan
        )
        
        return prayerTimeStrings.any { prayerTimeString ->
            try {
                val parts = prayerTimeString.split(":")
                if (parts.size >= 2) {
                    val prayerHour = parts[0].toInt()
                    val prayerMinute = parts[1].toInt()
                    
                    val currentMinutes = currentDateTime.hour * 60 + currentDateTime.minute
                    val prayerMinutes = prayerHour * 60 + prayerMinute
                    
                    // Check if we're within COUNTDOWN_START_MINUTES of prayer time
                    val timeDiff = prayerMinutes - currentMinutes
                    timeDiff in 0..COUNTDOWN_START_MINUTES
                } else false
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Stop monitoring prayer times
     */
    private fun stopPrayerMonitoring() {
        monitoringJob?.cancel()
        soundManager.cleanup()
    }

    /**
     * Check if current time matches any prayer time and trigger sound
     */
    private suspend fun checkPrayerTime(
        currentDateTime: LocalDateTime,
        settings: AppSettings,
        prayerTimes: PrayerTimes,
    ) {
        if (!settings.soundEnabled) return

        try {
            // Check all azan times for 5-second countdown ticking
                        checkAndPlayCountdownTicking(
                            PrayerType.FAJR,
                            prayerTimes.fajrAzan,
                            currentDateTime,
                            settings,
                            "Azan",
                        )
                        checkAndPlayCountdownTicking(
                            PrayerType.DHUHR,
                            prayerTimes.dhuhrAzan,
                            currentDateTime,
                            settings,
                            "Azan",
                        )
                        checkAndPlayCountdownTicking(
                            PrayerType.ASR,
                            prayerTimes.asrAzan,
                            currentDateTime,
                            settings,
                            "Azan",
                        )
                        checkAndPlayCountdownTicking(
                            PrayerType.MAGHRIB,
                            prayerTimes.maghribAzan,
                            currentDateTime,
                            settings,
                            "Azan",
                        )
                        checkAndPlayCountdownTicking(
                            PrayerType.ISHA,
                            prayerTimes.ishaAzan,
                            currentDateTime,
                            settings,
                            "Azan",
                        )

                        // Check sunrise time for 5-second countdown ticking
                        checkAndPlayCountdownTicking(
                            PrayerType.SUNRISE,
                            prayerTimes.sunrise,
                            currentDateTime,
                            settings,
                            "Sunrise",
                        )

                        // Check all iqamah times for 5-second countdown ticking
                        checkAndPlayCountdownTicking(
                            PrayerType.FAJR,
                            prayerTimes.fajrIqamah,
                            currentDateTime,
                            settings,
                            "Iqamah",
                        )
                        checkAndPlayCountdownTicking(
                            PrayerType.DHUHR,
                            prayerTimes.dhuhrIqamah,
                            currentDateTime,
                            settings,
                            "Iqamah",
                        )
                        checkAndPlayCountdownTicking(
                            PrayerType.ASR,
                            prayerTimes.asrIqamah,
                            currentDateTime,
                            settings,
                            "Iqamah",
                        )
                        checkAndPlayCountdownTicking(
                            PrayerType.MAGHRIB,
                            prayerTimes.maghribIqamah,
                            currentDateTime,
                            settings,
                            "Iqamah",
                        )
                        checkAndPlayCountdownTicking(
                            PrayerType.ISHA,
                            prayerTimes.ishaIqamah,
                            currentDateTime,
                            settings,
                            "Iqamah",
                        )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking prayer times", e)
        }
    }

    /**
     * Check if we should play 5-second countdown ticking (exactly 5 seconds before prayer event)
     */
    private fun checkAndPlayCountdownTicking(
        prayerType: PrayerType,
        prayerTime: String,
        currentDateTime: LocalDateTime,
        settings: AppSettings,
        eventType: String,
    ) {
        if (!settings.soundEnabled) return

        // Skip empty times (some iqamah times might be empty)
        if (prayerTime.isBlank()) return

        try {
            // Parse prayer time
            val timeParts = prayerTime.split(":")
            if (timeParts.size != 2) return

            val prayerHour = timeParts[0].toIntOrNull() ?: return
            val prayerMinute = timeParts[1].toIntOrNull() ?: return

            // Calculate seconds until prayer time with precise timing
            val currentTotalSeconds = currentDateTime.hour * 3600 + currentDateTime.minute * 60 + currentDateTime.second
            val prayerTotalSeconds = prayerHour * 3600 + prayerMinute * 60

            val secondsUntilPrayer = prayerTotalSeconds - currentTotalSeconds

            // Handle day rollover (if prayer is tomorrow)
            val actualSecondsUntilPrayer =
                if (secondsUntilPrayer < 0) {
                    secondsUntilPrayer + (24 * 3600) // Add 24 hours
                } else {
                    secondsUntilPrayer
                }

            // Debug: Log timing for prayers that are close
            if (actualSecondsUntilPrayer <= 10 && actualSecondsUntilPrayer > 0) {
                Log.d(
                    TAG,
                    "Close to $prayerType $eventType: ${actualSecondsUntilPrayer}s remaining (time: $prayerTime)",
                )
            }

            // Start 5-second countdown ticking when exactly 6 seconds remain
            // This ensures the 5-second audio finishes exactly when prayer time arrives
            if (actualSecondsUntilPrayer == 5) {
                Log.d(
                    TAG,
                    "TRIGGERING 5-second countdown for $prayerType $eventType at $prayerTime (5s remaining for perfect sync)",
                )
                soundManager.playCountdownTicking(5, prayerType, settings)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating countdown for $prayerType $eventType", e)
        }
    }
}
