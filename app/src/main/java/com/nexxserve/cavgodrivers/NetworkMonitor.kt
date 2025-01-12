package com.nexxserve.cavgodrivers

import NfcViewModel
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner



class NetworkMonitor(private val context: Context,
                     private val bookingViewModel: BookingViewModel,
                     private val nfcViewModel: NfcViewModel,
                     private val notificationHelper: NotificationHelper) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val lifecycleOwner = context as? LifecycleOwner

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        val tripId = CarIdStorage.getTripId()

        // Called when a network becomes available

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            if (tripId != null) {
                Log.d("MainActivity", "Trip ID found: $tripId")

                // Fetch bookings for the trip
                // Use a CoroutineScope to call the suspend function



//                var token = TokenRepository.getToken()
                nfcViewModel.setNetworkAvailable(true)

            }
            Toast.makeText(context, "Network Available", Toast.LENGTH_SHORT).show()
//            notificationHelper.showNotification("Network Available", "Your network is back online.", MainActivity::class.java)
            Log.d("NetworkMonitor", "Network available: $network")
        }

        // Called when network capabilities change
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isUnmetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            if(hasInternet) {
                Toast.makeText(context, "Net Internet Available", Toast.LENGTH_SHORT).show()
                Log.d("NetMonitoir", "has internet")
//                nfcViewModel.setNetworkAvailable(true)
            } else {
                Toast.makeText(context, "Net Internet Unavailable", Toast.LENGTH_SHORT).show()
                nfcViewModel.setNetworkAvailable(false)
            }
            Log.d("NetworkMonitor", "Network capabilities changed: $network, Unmetered: $isUnmetered hasint $hasInternet")
        }

        // Called when a network is lost
        override fun onLost(network: Network) {
            super.onLost(network)
            nfcViewModel.setNetworkAvailable(false)
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