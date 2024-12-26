package com.nexxserve.cavgodrivers

import NfcViewModel
import android.annotation.SuppressLint
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
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.nexxserve.cavgodrivers.fragment.BookingDetails
import com.nexxserve.cavgodrivers.nfc.NFCReaderHelper
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private lateinit var nfcReaderHelper: NFCReaderHelper
    private val nfcViewModel: NfcViewModel by viewModels()


    private fun isInternetAvailable(context: Context): Boolean {
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

    private fun isBookingValid(scannedData: String, bookings: List<BookingDetails?>): Boolean {
        // Ensure bookings data is not null
        if (bookings.isEmpty()) {
            return false
        }
        // Iterate through the fetched bookings to compare the scanned data
        for (booking in bookings) {
            // Check if the NFC data matches (if NFC is used)
            booking?.ticket?.nfcId?.let {
                if (scannedData == it) {
                    return true // NFC match found
                }
            }
            // Check if the QR code data matches (if QR code is used)
            booking?.ticket?.qrCodeData?.let {
                if (scannedData == it) {
                    return true // QR code match found
                }
            }
        }
        return false
    }

    private fun validateBooking(
        scannedData: String,
        bookings: List<BookingDetails>,
        callback: (Boolean) -> Unit
    ) {
        val isValid = isBookingValid(scannedData, bookings)
        callback(isValid)
    }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        var bookings by mutableStateOf<List<BookingDetails>>(emptyList())

        super.onCreate(savedInstanceState)
        Log.w("Main act", "starting")
        if (!isInternetAvailable(this)) {
            Log.e("MainActivity", "No internet connection. Cannot proceed.")
            // Optionally show a dialog or navigate to an error page
//            finish() // Close the activity if the internet is required
//            return
        }
        // Initialize necessary components
        TokenRepository.init(this)
        CarIdStorage.init(this)
        FirebaseApp.initializeApp(this)
        SDKUtil.getInstance(this).initSDK()




        val tripId = CarIdStorage.getTripId()
        if (tripId != null) {
            Log.d("MainActivity", "Trip ID found: $tripId")

            // Fetch bookings for the trip
            lifecycleScope.launch { BookingListenerManager.fetchBookingsForTrip(tripId) }
        }

        val carId = CarIdStorage.getLinkedCarId()
        if (carId == null) {
            Log.e("MainActivity", "Car ID is null. Cannot proceed.")
            // Optionally navigate to an error page or show a dialog
        }

        // Start the tracking service in the foreground
//        val serviceIntent = Intent(this, TrackingService::class.java)
//        startForegroundService(serviceIntent)

        nfcReaderHelper = NFCReaderHelper(
            context = this,
            onTagRead = { tag ->
                // Handle the NFC tag read here
                Log.d("NFC", "Tag read: ${tag.id}")
                val tagIdHex = tag.id.toHexString()
                nfcViewModel.setNfcId(tagIdHex)
                nfcViewModel.setMessage("invalid")
                if(bookings.isNotEmpty()){
                    validateBooking(tagIdHex, bookings) { isValid ->

//                    setColorLed(CommonConstants.LedType.COLOR_LED_1,  CommonConstants.LedColor.GREEN_LED, 255)
                        Log.d("NFC", "Validation status: $isValid $tagIdHex")



                        nfcViewModel.setNfcId(tagIdHex)
                    }
                }
            },
            onError = { errorMessage ->
                // Handle NFC error
                Log.e("NFC", errorMessage)
            }
        )

        setContent {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowInsetsController = window.insetsController
                windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                windowInsetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
            } else {
                // For lower versions, use the old way of hiding the status and navigation bar
                window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())

            }
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


                val nfcd = nfcViewModel.nfcId.value

                LaunchedEffect(carId) {
                    if (carId != null) {
                        TripListenerManager.startListeningForTrips(carId) { trips ->
                            // Update the isScanningAllowed state based on trips data
                            isScanningAllowed = trips.isNotEmpty()
                            Log.d("MainActivity", "Trips updated: $trips")

                        }
                    } else {
                        navController.navigate("helloWorld") {
                            popUpTo("message") { inclusive = true }
                            launchSingleTop = true
                        }
                        Log.e("MainActivity", "Car ID is null. TripListenerManager will not start.")
                    }
                }
                // Log NFC ID changes whenever it changes
                LaunchedEffect(nfcd) {
                    Log.d("NFC change", "NFC ID changed")
                    if (nfcd != null) {
                        Log.d("NFC change", "NFC ID change: $nfcd")
                        val currentRoute = navController.currentBackStackEntry?.destination?.route

                        if (currentRoute != "message") {
                            // If not already on the NFC scan page, navigate there
                            Log.d("NFC change", "Navigating to NFC Scan Page")
                            navController.navigate("message") {
                                // Optionally clear the back stack if necessary to prevent navigating back to this page
                                popUpTo("nfcScan") { inclusive = true }
                                launchSingleTop = true
                            }
                        }

                        nfcViewModel.clearNfcId()
                        nfcViewModel.clearMessage()
                    }
                }



                LaunchedEffect(tripId) {
                    tripId?.let { currentTripId ->
                        Log.d("GetBookings", "Fetching bookings for trip ID: $currentTripId")
                        try {
                            val fetchedBookings = getBookings(currentTripId)

                            val allBookings = BookingListenerManager.getCurrentBookings()
                            Log.d("Main Bookings", "all of them size ${allBookings.size}")
                            for (book in allBookings) {
                                Log.d("Main Booking ID", "Booking ID: ${book.id}")
                            }
                            bookings = allBookings
                            Log.d("Main Bookings", "Fetched bookings: ${fetchedBookings?.data}")
                        } catch (e: Exception) {
                            Log.e("GetBookings", "Error fetching bookings: ${e.message}")
                        }
                        BookingListenerManager.startListeningForBookings(currentTripId) { booking ->
                            Log.d("Main BookingListener", "New booking added: ${booking.bookingDetails.id}")
                        }
                    }
                }

                // Start listening for trips when the carId is available


                // Create NavHost to navigate between pages
                NavHost(
                    navController,
                    startDestination = if (loggedIn) "message" else "login" // Start at NFC Scan if logged in, else login page
                ) {

                    composable("message") {
                        MessagePage(
                            nfcViewModel = nfcViewModel
                        )
                    }


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
                    composable(
                        route = "nfcScan",

                    ) {

                        // Pass the isScanningAllowed state to NFCScanPage, and provide onGoToExtra function

                        NFCScanPage(
                            navController = navController,
                            isScanningAllowed = isScanningAllowed,

                            onGoToExtra = { nfcId ->
                                Log.d("NFCScanPageonGo", "Navigating to extra with nfcId: $nfcId")
                                runOnUiThread {
                                    navController.navigate("extra?nfcId=$nfcId")
                                }
                            }
                        )
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
                            onGoToExtra = { navController.navigate("nfcScan") },
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

                    composable(
                        route = "extra?nfcId={nfcId}",
                        arguments = listOf(navArgument("nfcId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val nfcId = backStackEntry.arguments?.getString("nfcId") ?: ""
                        ExtraPage(
                            nfcId = nfcId,
                            navController = navController,
                            onGoBack = {
                                navController.navigate("nfcScan") {
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

    @SuppressLint("NewApi")
    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsetsController = window.insetsController
                windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())

                windowInsetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
        } else {
            // For lower versions, use the old way of hiding the status and navigation bar
            window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())

        }
        if (nfcReaderHelper.isNfcSupported() && nfcReaderHelper.isNfcEnabled()) {
            nfcReaderHelper.enableNfcReader(this)
        } else {
            Log.e("MainActivity", "NFC is not enabled or supported.")
        }
    }

    override fun onPause() {
        super.onPause()
        nfcReaderHelper.disableNfcReader(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        TripListenerManager.stopListening() // Stop listening to trips when the activity is destroyed
    }
}