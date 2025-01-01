package com.nexxserve.cavgodrivers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class NetworkMonitor(private val context: Context,
                     private val bookingViewModel: BookingViewModel,
                     private val notificationHelper: NotificationHelper) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        val tripId = CarIdStorage.getTripId()

        // Called when a network becomes available

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            if (tripId != null) {
                Log.d("MainActivity", "Trip ID found: $tripId")

                // Fetch bookings for the trip
                // Use a CoroutineScope to call the suspend function
                val scope = (context as? LifecycleOwner)?.lifecycleScope
                scope?.launch {
                    BookingListenerManager.fetchBookingsForTrip(tripId, bookingViewModel)
                    BookingListenerManager.startListeningForBookings(tripId, bookingViewModel)
                }

            }
            Toast.makeText(context, "Network Available", Toast.LENGTH_SHORT).show()
//            notificationHelper.showNotification("Network Available", "Your network is back online.", MainActivity::class.java)
            Log.d("NetworkMonitor", "Network available: $network")
        }

        // Called when network capabilities change
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            val isUnmetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            Log.d("NetworkMonitor", "Network capabilities changed: $network, Unmetered: $isUnmetered")
        }

        // Called when a network is lost
        override fun onLost(network: Network) {
            super.onLost(network)
            Toast.makeText(context, "Network Unavailable", Toast.LENGTH_LONG).show()
//            notificationHelper.showNotification("Network Lost", "Your network connection is lost.", MainActivity::class.java)
            Log.d("NetworkMonitor", "Network lost: $network")
        }
    }

    // Start monitoring network changes
    fun startMonitoring() {
        val networkRequest = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        Log.d("NetworkMonitor", "Started monitoring network changes")
    }

    // Stop monitoring network changes
    fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)

        Log.d("NetworkMonitor", "Stopped monitoring network changes")
    }
}
