package com.mosque.prayerclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.mosque.prayerclock.service.PrayerNotificationService
import com.mosque.prayerclock.utils.DeviceUtils

/**
 * Launches the display on TVs after boot, and only the background prayer service on mobile.
 */
class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        Log.i(TAG, "onReceive: boot received ${intent?.action}")
        if (intent?.action !in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                "android.intent.action.QUICKBOOT_POWERON",
                "com.htc.intent.action.QUICKBOOT_POWERON",
            )) return
        val serviceClass = if (DeviceUtils.isTelevision(context)) {
            AppStartService::class.java
        } else {
            PrayerNotificationService::class.java
        }
        val serviceIntent = Intent(context, serviceClass)
        ContextCompat.startForegroundService(context, serviceIntent)
        Log.i(TAG, "Started ${serviceClass.simpleName} from BootReceiver")
    }
}
