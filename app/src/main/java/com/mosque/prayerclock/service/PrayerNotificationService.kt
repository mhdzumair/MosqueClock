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
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mosque.prayerclock.R
import com.mosque.prayerclock.SplashActivity
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.utils.DeviceUtils
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.data.model.SoundType
import com.mosque.prayerclock.data.repository.PrayerTimesRepository
import com.mosque.prayerclock.data.repository.SettingsRepository
import com.mosque.prayerclock.ui.notifications.PrayerTimetableNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import java.util.Locale

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
        private const val SERVICE_CHANNEL_ID = "prayer_times_service"
        private const val SERVICE_NOTIFICATION_ID = 1002
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
    private var lastPrayerTimesUpdate = 0L
    private val cacheValidityMs = 60_000L // Cache for 1 minute
    private var cachedPrayerDate: String? = null
    private var lastServiceNotification: List<String>? = null
    private var lastServiceNotificationUpdate = 0L

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

    override fun onCreate() {
        super.onCreate()

        // Initialize SoundManager manually
        soundManager = SoundManager(applicationContext)

        // Initialize NotificationManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        // Promote immediately, before loading settings or making network requests at boot.
        startForeground(SERVICE_NOTIFICATION_ID, createServiceNotification(null, null))

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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Prayer notification service started")
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
        cancelNotification()
    }

    override fun onBind(intent: Intent): IBinder? = null

    /**
     * Get settings with caching to reduce database queries
     */
    private suspend fun getCachedSettings(): AppSettings {
        return cachedSettings ?: settingsRepository.getSettings().first()
    }

    /**
     * Get prayer times with caching to reduce API calls
     */
    private suspend fun getCachedPrayerTimes(): PrayerTimes? {
        val currentTime = System.currentTimeMillis()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        if (cachedPrayerDate != today) {
            cachedPrayerDate = today
            cachedPrayerTimes = null
            lastPrayerTimesUpdate = 0L
            updateServiceNotification()
        }

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
                settingsRepository.getSettings().collectLatest { settings ->
                    val previousSettings = cachedSettings
                    cachedSettings = settings
                    if (previousSettings == null || prayerSourceKey(previousSettings) != prayerSourceKey(settings)) {
                        cachedPrayerTimes = null
                        lastPrayerTimesUpdate = 0L
                        if (previousSettings != null) prayerTimesRepository.invalidatePrayerTimesCache()
                    }
                    updateServiceNotification()
                    // Monitoring loop with smart polling
                    while (isActive) {
                        try {
                            if (settings.soundEnabled || shouldShowPrayerTimes(settings)) {
                                getCachedPrayerTimes()
                                updateServiceNotification()
                            }

                            if (settings.soundEnabled) {
                                val now = Clock.System.now()
                                val currentDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
                                val prayerTimes = cachedPrayerTimes

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
    }

    private fun prayerSourceKey(settings: AppSettings): List<Any> = listOf(
        settings.prayerServiceType, settings.city, settings.country, settings.latitude, settings.longitude,
        settings.selectedZone, settings.selectedRegion, settings.mazhab,
        settings.manualFajrAzan, settings.manualSunrise, settings.manualDhuhrAzan,
        settings.manualAsrAzan, settings.manualMaghribAzan, settings.manualIshaAzan,
    )

    private fun shouldShowPrayerTimes(settings: AppSettings): Boolean =
        settings.showPrayerTimesNotification && !DeviceUtils.isTelevision(this)

    private fun notificationContext(settings: AppSettings?): Context {
        val language = settings?.language?.takeUnless { it == Language.MULTI } ?: Language.ENGLISH
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(Locale(language.code))
        return createConfigurationContext(configuration)
    }

    private fun createServiceNotification(settings: AppSettings?, times: PrayerTimes?): Notification {
        val context = notificationContext(settings)
        val showTimes = settings != null && shouldShowPrayerTimes(settings)
        val title = context.getString(if (showTimes) R.string.prayer_times_notification_title else R.string.app_name)
        val text = context.getString(
            if (showTimes) R.string.prayer_times_notification_unavailable else R.string.prayer_service_running,
        )
        val openApp = PendingIntent.getActivity(
            this, 1, Intent(this, SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_prayer_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (showTimes && settings != null && times != null) {
            PrayerTimetableNotification.apply(builder, context, settings, times)
        }
        return builder.build()
    }

    private fun updateServiceNotification() {
        val settings = cachedSettings
        val key = listOf(
            settings?.language.toString(), settings?.showPrayerTimesNotification.toString(),
            settings?.show24HourFormat.toString(), cachedPrayerTimes.toString(),
            cachedPrayerTimes?.let { PrayerTimetableNotification.nextPrayerIndex(it) }.toString(),
        )
        val now = System.currentTimeMillis()
        // Repost occasionally if notification permission was restored or Android allowed dismissal.
        if (key != lastServiceNotification || now - lastServiceNotificationUpdate >= cacheValidityMs) {
            notificationManager.notify(SERVICE_NOTIFICATION_ID, createServiceNotification(settings, cachedPrayerTimes))
            lastServiceNotification = key
            lastServiceNotificationUpdate = now
        }
    }

    /**
     * Check if current time is approaching any prayer time (within COUNTDOWN_START_MINUTES)
     * This includes BOTH azan times AND iqamah times
     */
    private suspend fun isApproachingAnyPrayerTime(
        currentDateTime: LocalDateTime,
        prayerTimes: PrayerTimes,
    ): Boolean = prayerSoundEvents(prayerTimes, getCachedSettings()).any {
        it.secondsUntil(currentDateTime)?.let { seconds -> seconds in 0..(COUNTDOWN_START_MINUTES * 60) } == true
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
    private fun checkPrayerTime(
        currentDateTime: LocalDateTime,
        settings: AppSettings,
        prayerTimes: PrayerTimes,
    ) {
        if (!settings.soundEnabled) return

        for (event in prayerSoundEvents(prayerTimes, settings)) {
            val seconds = event.secondsUntil(currentDateTime) ?: continue
            if (seconds in 1..60) acquireWakeLock()
            if (!event.shouldPlay(currentDateTime, settings)) continue

            val isAzan = event.type == PrayerSoundEventType.AZAN
            val soundType = if (isAzan) settings.azanSoundType else settings.iqamahSoundType
            Log.i(TAG, "Playing sound: ${event.prayer} ${event.type.label} at ${event.time}")
            val soundPlayed = if (soundType == SoundType.COUNTDOWN_TICKING) {
                soundManager.playCountdownTicking(5, event.prayer, settings)
                true
            } else if (isAzan) {
                soundManager.playAzanSound(settings, event.prayer)
            } else {
                soundManager.playIqamahSound(settings, event.prayer)
            }
            if (soundPlayed) showPrayerNotification(event.prayer, event.type.label)
            if (seconds == 0) releaseWakeLock()
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
            notificationManager.createNotificationChannel(
                NotificationChannel(SERVICE_CHANNEL_ID, getString(R.string.prayer_times_notification_channel), NotificationManager.IMPORTANCE_LOW).apply {
                    description = getString(R.string.prayer_times_notification_description)
                    setShowBadge(false)
                },
            )
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
                // SoundManager owns prayer audio; notification updates must stay silent.
                .setSilent(true)
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
