package com.mosque.prayerclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * BroadcastReceiver that handles device boot completion and starts the app automatically. This
 * enables the mosque clock app to launch when the Android TV device starts up.
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
        val serviceIntent = Intent(context, AppStartService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
        Log.i(TAG, "Started AppStartService from BootReceiver")
    }
}
