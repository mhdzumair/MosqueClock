package com.mosque.prayerclock.service

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.PrayerType

class SoundManager(
    private val context: Context,
) {
    companion object {
        private const val TAG = "SoundManager"
    }

    private var beepPlayer: MediaPlayer? = null

    /**
     * Play 5-second countdown ticking sound when exactly 5 seconds remain
     */
    fun playCountdownTicking(
        secondsRemaining: Int,
        prayerType: PrayerType,
        settings: AppSettings,
    ) {
        Log.d(
            TAG,
            "playCountdownTicking called: secondsRemaining=$secondsRemaining, prayerType=$prayerType, soundEnabled=${settings.soundEnabled}",
        )

        if (!settings.soundEnabled) {
            Log.d(TAG, "Countdown ticking disabled in settings")
            return
        }

        // Only start the 5-second countdown when triggered (now at 6 seconds remaining for perfect sync)
        if (secondsRemaining != 5) {
            Log.d(TAG, "Not triggering countdown - secondsRemaining=$secondsRemaining (expecting 5 from service)")
            return
        }

        // Don't start if already playing
        if (beepPlayer != null && beepPlayer?.isPlaying == true) {
            Log.d(TAG, "Countdown ticking already playing")
            return
        }

        try {
            stopAllSounds() // Stop any currently playing sounds

            // Use the 5-second countdown ticking audio
            val uri = getCountdownTickingUri()
            Log.d(TAG, "Using audio URI: $uri")

            beepPlayer =
                MediaPlayer().apply {
                    setDataSource(context, uri)
                    setOnCompletionListener {
                        Log.d(TAG, "5-second countdown ticking completed for $prayerType")
                        release()
                        beepPlayer = null
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "Error playing countdown ticking: what=$what, extra=$extra")
                        release()
                        beepPlayer = null
                        true
                    }
                    setOnPreparedListener {
                        Log.d(TAG, "MediaPlayer prepared, starting playback")
                        start()
                    }
                    // Use async prepare for better error handling
                    prepareAsync()
                }

            Log.d(TAG, "Started preparing 5-second countdown ticking: $prayerType")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play countdown ticking", e)
            beepPlayer?.release()
            beepPlayer = null
        }
    }

    /**
     * Stop all currently playing sounds
     */
    fun stopAllSounds() {
        try {
            beepPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                beepPlayer = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping sounds", e)
        }
    }

    /**
     * Get the URI for 5-second countdown ticking sound
     */
    private fun getCountdownTickingUri(): Uri =
        try {
            // Use the 5-second countdown ticking audio file
            Uri.parse("android.resource://${context.packageName}/raw/countdown_ticking_5s")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting countdown ticking URI, falling back to system sound", e)
            // Fallback to system notification sound
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopAllSounds()
    }
}
