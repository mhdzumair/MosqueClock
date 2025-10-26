package com.mosque.prayerclock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.data.model.SoundType
import com.mosque.prayerclock.data.repository.PrayerTimesRepository
import com.mosque.prayerclock.data.repository.SettingsRepository
import com.mosque.prayerclock.utils.TimeUtils
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
        private const val TAG = "PrayerNotifService" // Max 23 chars for Android log tag
        private const val CHECK_INTERVAL_MS = 10_000L // Check every 10 seconds (much more efficient)
        private const val COUNTDOWN_CHECK_INTERVAL_MS = 1_000L // Only use 1-second interval during countdown
        private const val COUNTDOWN_START_MINUTES = 1 // Start countdown 1 minute before prayer
        
        // Notification constants
        private const val NOTIFICATION_CHANNEL_ID = "prayer_notification_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SOUND = "com.mosque.prayerclock.STOP_SOUND"
    }

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var prayerTimesRepository: PrayerTimesRepository

    private lateinit var soundManager: SoundManager
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var notificationManager: NotificationManager

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
    
    // Broadcast receiver for power button and stop action
    private val stopSoundReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        // Power button pressed
                        if (soundManager.isPlaying()) {
                            Log.i(TAG, "Screen off - stopping sound")
                            soundManager.stopAllSounds()
                            cancelNotification()
                        }
                    }
                    ACTION_STOP_SOUND -> {
                        // Stop button clicked in notification
                        Log.i(TAG, "Stop sound action received")
                        soundManager.stopAllSounds()
                        cancelNotification()
                    }
                }
            }
        }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)

        Log.d(TAG, "Prayer notification service started")

        // Initialize SoundManager manually
        soundManager = SoundManager(applicationContext)

        // Initialize NotificationManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        // Initialize WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock =
            powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MosqueClock:PrayerNotificationWakeLock",
            )

        // Register broadcast receiver for power button and stop action
        val intentFilter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(ACTION_STOP_SOUND)
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopSoundReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stopSoundReceiver, intentFilter)
        }

        startPrayerMonitoring()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Prayer notification service stopped")
        stopPrayerMonitoring()
        releaseWakeLock()
        
        // Unregister broadcast receiver
        try {
            unregisterReceiver(stopSoundReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        
        serviceScope.cancel()
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
                // Monitoring loop with smart polling
                while (isActive) {
                    try {
                        val settings = getCachedSettings()

                        if (settings.soundEnabled) {
                            val now = Clock.System.now()
                            val currentDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
                            val prayerTimes = getCachedPrayerTimes()

                            if (prayerTimes != null) {
                                val isApproachingPrayer = isApproachingAnyPrayerTime(currentDateTime, prayerTimes)

                                if (isApproachingPrayer && !isInCountdownMode) {
                                    Log.i(TAG, "Prayer approaching - high-frequency mode")
                                    isInCountdownMode = true
                                    countdownStartTime = System.currentTimeMillis()
                                } else if (!isApproachingPrayer && isInCountdownMode) {
                                    isInCountdownMode = false
                                }

                                // Check prayer times every second when in countdown mode
                                if (isInCountdownMode) {
                                    val currentSecond =
                                        currentDateTime.hour * 3600 + currentDateTime.minute * 60 +
                                            currentDateTime.second

                                    if (currentSecond != lastCheckedSecond) {
                                        lastCheckedSecond = currentSecond
                                        checkPrayerTime(currentDateTime, settings, prayerTimes)
                                    }
                                }
                            } else {
                                Log.w(TAG, "Prayer times unavailable")
                            }
                        } else {
                            isInCountdownMode = false
                        }

                        // Smart delay with millisecond precision
                        val delayMs =
                            if (isInCountdownMode) {
                                // Precise delay to next second boundary
                                val currentMillis = System.currentTimeMillis()
                                val millisToNextSecond = 1000 - (currentMillis % 1000)
                                millisToNextSecond + 10 // Small buffer
                            } else {
                                CHECK_INTERVAL_MS
                            }

                        delay(delayMs)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e // Re-throw to properly cancel
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in monitoring: ${e.message}")
                        delay(CHECK_INTERVAL_MS)
                    }
                }
            }
    }

    /**
     * Check if current time is approaching any prayer time (within COUNTDOWN_START_MINUTES)
     * This includes BOTH azan times AND iqamah times
     */
    private suspend fun isApproachingAnyPrayerTime(
        currentDateTime: LocalDateTime,
        prayerTimes: PrayerTimes,
    ): Boolean {
        // Get settings to calculate iqamah times dynamically
        val settings = getCachedSettings()

        // Include BOTH azan and iqamah times
        val allPrayerTimeStrings =
            listOf(
                // Azan times
                prayerTimes.fajrAzan,
                prayerTimes.dhuhrAzan,
                prayerTimes.asrAzan,
                prayerTimes.maghribAzan,
                prayerTimes.ishaAzan,
                // Iqamah times (calculated dynamically from settings)
                TimeUtils.addMinutesToTime(prayerTimes.fajrAzan, settings.fajrIqamahGap),
                TimeUtils.addMinutesToTime(prayerTimes.dhuhrAzan, settings.dhuhrIqamahGap),
                TimeUtils.addMinutesToTime(prayerTimes.asrAzan, settings.asrIqamahGap),
                TimeUtils.addMinutesToTime(prayerTimes.maghribAzan, settings.maghribIqamahGap),
                TimeUtils.addMinutesToTime(prayerTimes.ishaAzan, settings.ishaIqamahGap),
            )

        return allPrayerTimeStrings.any { prayerTimeString ->
            try {
                val parts = prayerTimeString.split(":")
                if (parts.size >= 2) {
                    val prayerHour = parts[0].toInt()
                    val prayerMinute = parts[1].toInt()
                    val currentMinutes = currentDateTime.hour * 60 + currentDateTime.minute
                    val prayerMinutes = prayerHour * 60 + prayerMinute
                    val timeDiff = prayerMinutes - currentMinutes
                    timeDiff in 0..COUNTDOWN_START_MINUTES
                } else {
                    false
                }
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
        monitoringJob = null
        soundManager.cleanup()
    }

    /**
     * Acquire WakeLock to ensure service stays awake during critical periods
     */
    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(2 * 60 * 1000L) // 2 minutes timeout
                Log.d(TAG, "WakeLock acquired")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring WakeLock", e)
        }
    }

    /**
     * Release WakeLock
     */
    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock", e)
        }
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
            // Calculate all iqamah times dynamically from settings
            val fajrIqamah = TimeUtils.addMinutesToTime(prayerTimes.fajrAzan, settings.fajrIqamahGap)
            val dhuhrIqamah = TimeUtils.addMinutesToTime(prayerTimes.dhuhrAzan, settings.dhuhrIqamahGap)
            val asrIqamah = TimeUtils.addMinutesToTime(prayerTimes.asrAzan, settings.asrIqamahGap)
            val maghribIqamah = TimeUtils.addMinutesToTime(prayerTimes.maghribAzan, settings.maghribIqamahGap)
            val ishaIqamah = TimeUtils.addMinutesToTime(prayerTimes.ishaAzan, settings.ishaIqamahGap)

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
            // Iqamah times are calculated dynamically from settings
            checkAndPlayCountdownTicking(
                PrayerType.FAJR,
                fajrIqamah,
                currentDateTime,
                settings,
                "Iqamah",
            )
            checkAndPlayCountdownTicking(
                PrayerType.DHUHR,
                dhuhrIqamah,
                currentDateTime,
                settings,
                "Iqamah",
            )
            checkAndPlayCountdownTicking(
                PrayerType.ASR,
                asrIqamah,
                currentDateTime,
                settings,
                "Iqamah",
            )
            checkAndPlayCountdownTicking(
                PrayerType.MAGHRIB,
                maghribIqamah,
                currentDateTime,
                settings,
                "Iqamah",
            )
            checkAndPlayCountdownTicking(
                PrayerType.ISHA,
                ishaIqamah,
                currentDateTime,
                settings,
                "Iqamah",
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking prayer times", e)
        }
    }

    /**
     * Check if we should play sound for prayer event (countdown ticking or immediate sound)
     */
    private fun checkAndPlayCountdownTicking(
        prayerType: PrayerType,
        prayerTime: String,
        currentDateTime: LocalDateTime,
        settings: AppSettings,
        eventType: String,
    ) {
        if (!settings.soundEnabled || prayerTime.isBlank()) return

        // Check if sound is enabled for this event type
        val isAzan = eventType == "Azan" || eventType == "Sunrise"
        val isIqamah = eventType == "Iqamah"

        if (isAzan && !settings.azanSoundEnabled) return
        if (isIqamah && !settings.iqamahSoundEnabled) return

        try {
            val timeParts = prayerTime.split(":")
            if (timeParts.size != 2) return

            val prayerHour = timeParts[0].toIntOrNull() ?: return
            val prayerMinute = timeParts[1].toIntOrNull() ?: return

            val currentTotalSeconds = currentDateTime.hour * 3600 + currentDateTime.minute * 60 + currentDateTime.second
            val prayerTotalSeconds = prayerHour * 3600 + prayerMinute * 60
            val secondsUntilPrayer = prayerTotalSeconds - currentTotalSeconds

            // Handle day rollover
            val actualSecondsUntilPrayer =
                if (secondsUntilPrayer < 0) {
                    secondsUntilPrayer + (24 * 3600)
                } else {
                    secondsUntilPrayer
                }

            // Get sound type for this event
            val soundType =
                if (isAzan) settings.azanSoundType else settings.iqamahSoundType

            // Acquire WakeLock when approaching prayer time (60 seconds before)
            if (actualSecondsUntilPrayer <= 60 && actualSecondsUntilPrayer > 0) {
                acquireWakeLock()
            }

            when (soundType) {
                SoundType.COUNTDOWN_TICKING -> {
                    // Play 5-second countdown ticking at exactly 5 seconds before
                    if (actualSecondsUntilPrayer == 5) {
                        Log.i(TAG, "Countdown ticking: $prayerType $eventType at $prayerTime")
                        soundManager.playCountdownTicking(5, prayerType, settings)
                        // Show notification with stop action
                        showPrayerNotification(prayerType, eventType)
                    }
                }
                SoundType.TRADITIONAL_BEEP, SoundType.CUSTOM -> {
                    // Play sound at exact prayer time (0 seconds)
                    if (actualSecondsUntilPrayer == 0) {
                        Log.i(TAG, "Playing sound: $prayerType $eventType at $prayerTime")
                        val soundPlayed =
                            if (isAzan) {
                                soundManager.playAzanSound(settings, prayerType)
                            } else {
                                soundManager.playIqamahSound(settings, prayerType)
                            }
                        
                        // Show notification
                        if (soundPlayed) {
                            showPrayerNotification(prayerType, eventType)
                        }
                        
                        // Release WakeLock after playing sound
                        releaseWakeLock()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in prayer time check", e)
        }
    }

    /**
     * Create notification channel for prayer notifications
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Prayer Notifications",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Notifications for Azan and Iqamah times"
                    enableVibration(true)
                    setShowBadge(true)
                }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show notification for prayer time with stop button
     */
    private fun showPrayerNotification(
        prayerType: PrayerType,
        eventType: String,
    ) {
        val prayerName =
            when (prayerType) {
                PrayerType.FAJR -> "Fajr"
                PrayerType.DHUHR -> "Dhuhr"
                PrayerType.ASR -> "Asr"
                PrayerType.MAGHRIB -> "Maghrib"
                PrayerType.ISHA -> "Isha"
                PrayerType.SUNRISE -> "Sunrise"
            }

        val title = "$prayerName $eventType"
        val content = "It's time for $prayerName $eventType"

        // Create stop sound action
        val stopIntent =
            Intent(ACTION_STOP_SOUND).apply {
                setPackage(packageName)
            }
        val stopPendingIntent =
            PendingIntent.getBroadcast(
                this,
                0,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notification =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Use system icon
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setOngoing(soundManager.isPlaying()) // Keep notification while sound is playing
                .addAction(
                    android.R.drawable.ic_delete, // Use system stop/delete icon
                    "Stop Sound",
                    stopPendingIntent,
                )
                .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Prayer notification shown: $title")
    }

    /**
     * Cancel prayer notification
     */
    private fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        Log.d(TAG, "Prayer notification cancelled")
    }
}
