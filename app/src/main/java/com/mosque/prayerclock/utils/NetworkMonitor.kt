package com.mosque.prayerclock.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Network connectivity status
 */
sealed class NetworkStatus {
    /** Network is available and has internet capability */
    data object Available : NetworkStatus()

    /** Network is not available */
    data object Unavailable : NetworkStatus()
}

/**
 * Monitor for network connectivity changes.
 * Uses ConnectivityManager.NetworkCallback to detect when network becomes available.
 * This is useful for triggering data refresh after WiFi reconnects post-boot.
 */
object NetworkMonitor {
    private const val TAG = "NetworkMonitor"

    /**
     * Creates a Flow that emits NetworkStatus when connectivity changes.
     * Emits Available when network with internet capability becomes available.
     * Emits Unavailable when network is lost.
     *
     * @param context Application context
     * @return Flow of NetworkStatus (distinctUntilChanged to avoid duplicate emissions)
     */
    fun observeNetworkStatus(context: Context): Flow<NetworkStatus> = callbackFlow {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Check current status immediately
        val currentStatus = getCurrentNetworkStatus(connectivityManager)
        Log.d(TAG, "Initial network status: $currentStatus")
        trySend(currentStatus)

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.i(TAG, "Network available: $network")
                trySend(NetworkStatus.Available)
            }

            override fun onLost(network: Network) {
                Log.i(TAG, "Network lost: $network")
                // Check if we still have any network available
                val status = getCurrentNetworkStatus(connectivityManager)
                trySend(status)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet =
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val hasValidated =
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                Log.d(TAG, "Network capabilities changed - hasInternet: $hasInternet, validated: $hasValidated")

                if (hasInternet && hasValidated) {
                    trySend(NetworkStatus.Available)
                }
            }

            override fun onUnavailable() {
                Log.i(TAG, "Network unavailable")
                trySend(NetworkStatus.Unavailable)
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        Log.d(TAG, "Registering network callback")

        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
            // Emit current status as fallback
            trySend(getCurrentNetworkStatus(connectivityManager))
        }

        awaitClose {
            Log.d(TAG, "Unregistering network callback")
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister network callback", e)
            }
        }
    }.distinctUntilChanged()

    /**
     * Get current network status synchronously
     */
    private fun getCurrentNetworkStatus(connectivityManager: ConnectivityManager): NetworkStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }

            if (capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            ) {
                NetworkStatus.Available
            } else {
                NetworkStatus.Unavailable
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo?.isConnected == true) {
                NetworkStatus.Available
            } else {
                NetworkStatus.Unavailable
            }
        }
    }

    /**
     * Check if network is currently available (synchronous check)
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return getCurrentNetworkStatus(connectivityManager) == NetworkStatus.Available
    }
}





