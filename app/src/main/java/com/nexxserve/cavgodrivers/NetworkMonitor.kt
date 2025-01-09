package com.nexxserve.cavgodrivers

import NfcViewModel
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class NetworkMonitor(
    private val context: Context,
    private val bookingViewModel: BookingViewModel,
    private val notificationHelper: NotificationHelper,
    nfcViewModel: NfcViewModel
) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        // Existing callback implementation
        // ...
    }

    private var isMonitoring = false // Flag to track registration status

    // Start monitoring network changes
    fun startMonitoring() {
        if (!isMonitoring) {
            val networkRequest = android.net.NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            isMonitoring = true // Mark as registered
            Log.d("NetworkMonitor", "Started monitoring network changes")
        }
    }

    // Stop monitoring network changes
    fun stopMonitoring() {
        if (isMonitoring) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
                isMonitoring = false // Mark as unregistered
                Log.d("NetworkMonitor", "Stopped monitoring network changes")
            } catch (e: IllegalArgumentException) {
                Log.e("NetworkMonitor", "Failed to unregister NetworkCallback: ${e.message}")
            }
        }
    }
}

