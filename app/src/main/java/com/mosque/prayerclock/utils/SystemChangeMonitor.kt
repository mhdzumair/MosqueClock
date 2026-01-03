package com.mosque.prayerclock.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Events emitted when system time/date/timezone changes.
 * This helps detect when the device clock syncs after boot (e.g., when WiFi becomes available).
 */
sealed class SystemChangeEvent {
    /** System time was changed (manual or NTP sync) */
    data object TimeChanged : SystemChangeEvent()

    /** System date was changed */
    data object DateChanged : SystemChangeEvent()

    /** System timezone was changed */
    data object TimezoneChanged : SystemChangeEvent()

    /** Detected a significant time jump (e.g., clock corrected from old build time) */
    data class SignificantTimeJump(val jumpSeconds: Long) : SystemChangeEvent()
}

/**
 * Monitor for system time/date/timezone changes.
 * Uses BroadcastReceiver to detect ACTION_TIME_CHANGED, ACTION_DATE_CHANGED, and ACTION_TIMEZONE_CHANGED.
 * Also detects significant time jumps that indicate clock sync (e.g., after WiFi restored).
 */
object SystemChangeMonitor {
    private const val TAG = "SystemChangeMonitor"

    // Threshold for detecting significant time jumps (30 seconds)
    // If time jumps more than this between checks, we consider it a clock sync
    private const val TIME_JUMP_THRESHOLD_SECONDS = 30L

    /**
     * Creates a Flow that emits SystemChangeEvent when system time/date/timezone changes.
     * The flow will also periodically check for significant time jumps.
     *
     * @param context Application context
     * @return Flow of SystemChangeEvent
     */
    fun observeSystemChanges(context: Context): Flow<SystemChangeEvent> = callbackFlow {
        var lastKnownEpochSeconds = Clock.System.now().epochSeconds
        var lastKnownDate = Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val action = intent?.action ?: return

                Log.d(TAG, "Received broadcast: $action")

                when (action) {
                    Intent.ACTION_TIME_CHANGED -> {
                        Log.i(TAG, "System time changed detected")

                        // Check for significant time jump
                        val currentEpochSeconds = Clock.System.now().epochSeconds
                        val jump = kotlin.math.abs(currentEpochSeconds - lastKnownEpochSeconds)

                        if (jump > TIME_JUMP_THRESHOLD_SECONDS) {
                            Log.i(TAG, "Significant time jump detected: $jump seconds")
                            trySend(SystemChangeEvent.SignificantTimeJump(jump))
                        }

                        lastKnownEpochSeconds = currentEpochSeconds
                        trySend(SystemChangeEvent.TimeChanged)

                        // Also check if the date changed
                        val currentDate = Clock.System
                            .now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
                            .toString()

                        if (currentDate != lastKnownDate) {
                            Log.i(TAG, "Date also changed: $lastKnownDate -> $currentDate")
                            lastKnownDate = currentDate
                            trySend(SystemChangeEvent.DateChanged)
                        }
                    }

                    Intent.ACTION_DATE_CHANGED -> {
                        Log.i(TAG, "System date changed detected")
                        val currentDate = Clock.System
                            .now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
                            .toString()
                        lastKnownDate = currentDate
                        trySend(SystemChangeEvent.DateChanged)
                    }

                    Intent.ACTION_TIMEZONE_CHANGED -> {
                        Log.i(TAG, "System timezone changed detected")
                        trySend(SystemChangeEvent.TimezoneChanged)
                    }
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }

        Log.d(TAG, "Registering system change receiver")
        context.registerReceiver(receiver, intentFilter)

        awaitClose {
            Log.d(TAG, "Unregistering system change receiver")
            context.unregisterReceiver(receiver)
        }
    }
}





