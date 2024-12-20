package com.nexxserve.cavgodrivers

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.common.apiutil.util.SDKUtil
import com.google.firebase.FirebaseApp
import com.nexxserve.cavgodrivers.ui.theme.CavgodriversTheme
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Context

class MainActivity : ComponentActivity() {

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        Log.w("Main act", "starting")
        if (!isInternetAvailable(this)) {
            Log.e("MainActivity", "No internet connection. Cannot proceed.")
            // Optionally show a dialog or navigate to an error page
            finish() // Close the activity if the internet is required
            return
        }
        // Initialize necessary components
        TokenRepository.init(this)
        CarIdStorage.init(this)
        FirebaseApp.initializeApp(this)
        SDKUtil.getInstance(this).initSDK()

        val carId = CarIdStorage.getLinkedCarId()
        if (carId == null) {
            Log.e("MainActivity", "Car ID is null. Cannot proceed.")
            // Optionally navigate to an error page or show a dialog
        }


        // Start the tracking service in the foreground
        val serviceIntent = Intent(this, TrackingService::class.java)
        startForegroundService(serviceIntent)

        setContent {
            CavgodriversTheme {
                // Define the NavController to manage navigation between pages
                val navController = rememberNavController()

                // Manage login state
                var loggedIn by remember { mutableStateOf(true) }
                var username by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }

                // Check if token is available
                val token = TokenRepository.getToken()

                if (token != null) {
                    Log.d("MainActivity", "Token found: $token")
                    loggedIn = true
                    username = "yves" // This could be dynamic based on the token or other data
                } else {
                    Log.d("MainActivity", "Token not found. User is not logged in.")
                    loggedIn = false
                }

                // State for scanning allowed
                var isScanningAllowed by remember { mutableStateOf(false) }

                // Start listening for trips when the carId is available
                LaunchedEffect(carId) {
                    if (carId != null) {
                        TripListenerManager.startListeningForTrips(carId) { trips ->
                            // Update the isScanningAllowed state based on trips data
                            isScanningAllowed = trips.isNotEmpty()
                            Log.d("MainActivity", "Trips updated: $trips")
                        }
                    } else {
                        Log.e("MainActivity", "Car ID is null. TripListenerManager will not start.")
                    }
                }


                // Create NavHost to navigate between pages
                NavHost(
                    navController,
                    startDestination = if (loggedIn) "nfcScan" else "login" // Start at NFC Scan if logged in, else login page
                ) {
                    composable("login") {
                        LoginPage { user, pass ->
                            // Update state with login details
                            username = user
                            password = pass
                            loggedIn = true // Mark the user as logged in
//                            TokenRepository.setToken("your_generated_token_here") // Save token upon successful login

                            // Navigate to NFC Scan page
                            navController.navigate("nfcScan") {
                                popUpTo("login") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    composable("nfcScan") {
                        // Pass the isScanningAllowed state to NFCScanPage
                        NFCScanPage(navController, isScanningAllowed)
                    }

                    composable("helloWorld") {
                        HelloWorldPage(
                            username = username,
                            onLogout = {
                                loggedIn = false
                                TokenRepository.removeToken() // Remove the token on logout
                                navController.navigate("login") {
                                    popUpTo("helloWorld") { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onGoToExtra = { navController.navigate("extra") },
                            registerPosMachine = { serialNumber, carPlate, password ->
                                // Handle POS machine registration here
                                registerPosMachine(serialNumber, carPlate, password)
                            },
                            updateCarPlate = { serialNumber, plateNumber ->
                                // Handle updating car plate here
                                updatePosMachine(serialNumber, plateNumber)
                            }
                        )
                    }

                    composable("extra") {
                        ExtraPage(
                            onGoBack = {
                                // Navigate back to HelloWorld when going back
                                navController.navigate("helloWorld") {
                                    popUpTo("extra") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TripListenerManager.stopListening() // Stop listening to trips when the activity is destroyed
    }
}