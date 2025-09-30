package com.mosque.prayerclock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class AppStartService : Service() {
    companion object {
        private const val TAG = "AppStartService"
        private const val CHANNEL_ID = "app-start-service"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int = START_STICKY

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(1, createNotification())
        }

        // Display management handled by activities through window flags
        Log.i(TAG, "Starting mosque clock app from boot...")

        // Start SplashActivity immediately without coroutine delay
        try {
            val intent =
                Intent(this, SplashActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("started_from_boot", true)
                }
            startActivity(intent)
            Log.i(TAG, "Successfully started SplashActivity from AppStartService")

            // Stop the service after starting the activity
            stopSelf()
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to start SplashActivity from AppStartService", ex)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {
        val serviceChannel =
            NotificationChannel(
                CHANNEL_ID,
                "Mosque Clock Service",
                NotificationManager.IMPORTANCE_LOW,
            )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)

        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("Mosque Clock Service")
            .setSilent(true)
            .setContentText("Starting mosque prayer clock application")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }
}
