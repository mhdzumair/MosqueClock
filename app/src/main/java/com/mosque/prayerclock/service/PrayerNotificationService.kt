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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.datetime.*
import javax.inject.Inject

@AndroidEntryPoint
class PrayerNotificationService : Service() {
    companion object {
        private const val TAG = "PrayerNotificationService"
        private const val CHECK_INTERVAL_MS = 1_000L // Check every 1 second for countdown beeps
    }

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var prayerTimesRepository: PrayerTimesRepository

    private lateinit var soundManager: SoundManager

    private val serviceScope = CoroutineScope(SupervisorJob())
    private var monitoringJob: Job? = null
    private var lastCheckedSecond = -1

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
     * Start monitoring for prayer times
     */
    private fun startPrayerMonitoring() {
        monitoringJob?.cancel()

        monitoringJob =
            serviceScope.launch {
                Log.d(TAG, "Starting prayer monitoring coroutine")

                // Start the monitoring loop that will check settings each iteration
                while (isActive) {
                    try {
                        // Get current settings
                        val settings = settingsRepository.getSettings().first()
                        Log.d(TAG, "Settings check: soundEnabled=${settings.soundEnabled}")

                        if (settings.soundEnabled) {
                            val now = Clock.System.now()
                            val currentDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
                            val currentSecond = currentDateTime.hour * 3600 + currentDateTime.minute * 60 + currentDateTime.second

                            // Check every second for countdown beeps
                            if (currentSecond != lastCheckedSecond) {
                                lastCheckedSecond = currentSecond

                                checkPrayerTime(currentDateTime, settings)
                            }
                        }

                        delay(CHECK_INTERVAL_MS)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in monitoring loop", e)
                        delay(CHECK_INTERVAL_MS)
                    }
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
        currentDateTime: kotlinx.datetime.LocalDateTime,
        settings: AppSettings,
    ) {
        if (!settings.soundEnabled) return

        try {
            // Get prayer times (repository now handles manual mode properly)
            prayerTimesRepository.getTodayPrayerTimesFromSettings().collect { result ->
                when (result) {
                    is com.mosque.prayerclock.data.network.NetworkResult.Success -> {
                        val prayerTimes = result.data

                        // Check all azan times for 5-second countdown ticking
                        checkAndPlayCountdownTicking(PrayerType.FAJR, prayerTimes.fajrAzan, currentDateTime, settings, "Azan")
                        checkAndPlayCountdownTicking(PrayerType.DHUHR, prayerTimes.dhuhrAzan, currentDateTime, settings, "Azan")
                        checkAndPlayCountdownTicking(PrayerType.ASR, prayerTimes.asrAzan, currentDateTime, settings, "Azan")
                        checkAndPlayCountdownTicking(PrayerType.MAGHRIB, prayerTimes.maghribAzan, currentDateTime, settings, "Azan")
                        checkAndPlayCountdownTicking(PrayerType.ISHA, prayerTimes.ishaAzan, currentDateTime, settings, "Azan")

                        // Check sunrise time for 5-second countdown ticking
                        checkAndPlayCountdownTicking(PrayerType.SUNRISE, prayerTimes.sunrise, currentDateTime, settings, "Sunrise")

                        // Check all iqamah times for 5-second countdown ticking
                        checkAndPlayCountdownTicking(PrayerType.FAJR, prayerTimes.fajrIqamah, currentDateTime, settings, "Iqamah")
                        checkAndPlayCountdownTicking(PrayerType.DHUHR, prayerTimes.dhuhrIqamah, currentDateTime, settings, "Iqamah")
                        checkAndPlayCountdownTicking(PrayerType.ASR, prayerTimes.asrIqamah, currentDateTime, settings, "Iqamah")
                        checkAndPlayCountdownTicking(PrayerType.MAGHRIB, prayerTimes.maghribIqamah, currentDateTime, settings, "Iqamah")
                        checkAndPlayCountdownTicking(PrayerType.ISHA, prayerTimes.ishaIqamah, currentDateTime, settings, "Iqamah")
                    }
                    is com.mosque.prayerclock.data.network.NetworkResult.Error -> {
                        Log.e(TAG, "Error loading prayer times: ${result.message}")
                    }
                    is com.mosque.prayerclock.data.network.NetworkResult.Loading -> {
                        // Don't log loading every second
                    }
                }
            }
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
        currentDateTime: kotlinx.datetime.LocalDateTime,
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
                Log.d(TAG, "Close to $prayerType $eventType: ${actualSecondsUntilPrayer}s remaining (time: $prayerTime)")
            }

            // Start 5-second countdown ticking when exactly 6 seconds remain
            // This ensures the 5-second audio finishes exactly when prayer time arrives
            if (actualSecondsUntilPrayer == 5) {
                Log.d(TAG, "TRIGGERING 5-second countdown for $prayerType $eventType at $prayerTime (5s remaining for perfect sync)")
                soundManager.playCountdownTicking(5, prayerType, settings)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating countdown for $prayerType $eventType", e)
        }
    }
}
