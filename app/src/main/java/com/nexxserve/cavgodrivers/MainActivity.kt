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
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.nexxserve.cavgodrivers.fragment.BookingDetails
import com.nexxserve.cavgodrivers.nfc.NFCReaderHelper
import com.nexxserve.cavgodrivers.qr.QRCodeScannerUtil
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private lateinit var nfcReaderHelper: NFCReaderHelper
    private lateinit var networkMonitor: NetworkMonitor
    private val nfcViewModel: NfcViewModel by viewModels()
    private val bookingViewModel: BookingViewModel by viewModels()
    private val bookingValidator = BookingValidator()
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var soundManagement: SoundManagement
    private lateinit var qrCodeScannerUtil: QRCodeScannerUtil
    private  val tripViewModel: TripViewModel by viewModels()


    @OptIn(ExperimentalStdlibApi::class)
    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var Bookings by mutableStateOf(emptyList<BookingDetails>())

        // Initialize necessary components
        TokenRepository.init(this, nfcViewModel)
        CarIdStorage.init(this)
        CarIdStorage.setNfcViewModel(nfcViewModel)
        FirebaseApp.initializeApp(this)
        SDKUtil.getInstance(this).initSDK()
        soundManagement = SoundManagement.getInstance(this)
        notificationHelper = NotificationHelper.getInstance(this)
        networkMonitor = NetworkMonitor(this, bookingViewModel, notificationHelper)
        TripListenerManager.initialize(this)
        qrCodeScannerUtil = QRCodeScannerUtil(this)


        notificationHelper.createNotificationChannel()



        soundManagement.loadSound(com.google.android.libraries.navigation.R.raw.test_sound)



        val carId = CarIdStorage.getLinkedCarId()
        if (carId == null) {
            Log.e("MainActivity", "Car ID is null. Cannot proceed.")
            // Optionally navigate to an error page or show a dialog
        }
        tripViewModel.trips.observe(this) { trips ->
            if (trips.isEmpty()) {
                println("No trips available.")

            } else {
                println("Trips loaded: ${trips.size}")
            }
        }

        bookingViewModel.bookings.observe(this) { bookings ->
            // Update your UI with the new bookings list
            Log.d("MainActivity", "Bookings updated size: ${bookings.size}")
            if (bookings.isNotEmpty()) {
                Bookings = bookings
                Log.d("MainActivity", "Bookings var updated size: ${Bookings.size}")

            } else {
                Bookings = emptyList()
                Log.d("MainActivity", "No bookings available yet")
                nfcViewModel.setMessage("No bookings available yet")
            }
        }


        nfcReaderHelper = NFCReaderHelper(
            context = this,
            onTagRead = { tag ->
                // Handle the NFC tag read here
                Log.d("NFC", "Tag read: ${tag.id}")
                val tagIdHex = tag.id.toHexString()
                nfcViewModel.clearNfcId()
                nfcViewModel.clearMessage()
//                soundManagement.playSound(com.google.android.libraries.navigation.R.raw.test_sound)

                bookingValidator.validateBooking(this,tagIdHex, Bookings, nfcViewModel) { isValid ->

                    Log.d("NFC", "Validation status: $isValid $tagIdHex")
                    if (isValid) {
                        nfcViewModel.setMessage("valid")
                    } else {
                        nfcViewModel.setNfcId(tagIdHex)
                        nfcViewModel.setMessage("invalid")
                    }
                    nfcViewModel.setNfcId(tagIdHex)
                }
            },
            onError = { errorMessage ->
                // Handle NFC error
                Log.e("NFC", errorMessage)
            }
        )

        qrCodeScannerUtil.setQRCodeScanListener(object : QRCodeScannerUtil.QRCodeScanListener {
            override fun onQRCodeScanned(data: String?) {
                // Handle the scanned QR code data here
                // For example, show a Toast with the scanned data
                Log.d("MainActivity", "Scanned QR Code: $data")
                if (data != null && data.length > 3) {
                    // Process the scanned QR code data
                    if(Bookings.isNotEmpty()){
                        bookingValidator.validateBooking(this@MainActivity,data, Bookings, nfcViewModel) { isValid ->

                            Log.d("QR Code", "Validation status: $isValid $data")
                            if (isValid) {
                                nfcViewModel.setMessage("valid")

                            } else {
                                nfcViewModel.setMessage("invalid")
                                nfcViewModel.setQrCodeData(data)
                            }
                        }
                    }
                }
            }
        })


        var tripId by mutableStateOf(CarIdStorage.getTripId())

        if (carId != null) {
            Log.d("MainActivity", "Trip ID found: $carId")
            TripListenerManager.startListeningForTrips(
                carId = CarIdStorage.getLinkedCarId() ?: "",
                onUpdate = { newTrips ->
                    // Handle the updated trips list here
                    if (newTrips.isNotEmpty()) {
                        tripViewModel.setTrips(newTrips)
                        tripId = CarIdStorage.getTripId()
                    }
                }
            )
        }
        lifecycleScope.launch {
            if (tripId != null) {
                BookingListenerManager.fetchBookingsForTrip(tripId!!, bookingViewModel)
//                BookingListenerManager.startListeningForBookings(tripId!!, bookingViewModel)
            }
        }



        // Start the tracking service in the foreground
//        val serviceIntent = Intent(this, TrackingService::class.java)
//        startForegroundService(serviceIntent)


        setContent {
            val token = TokenRepository.getToken()
            if (token != null) {
                Log.d("MainActivity", "Token found: $token")
                nfcViewModel.setLoggedIn(true)
            } else {
                Log.d("MainActivity", "Token not found. User is not logged in.")
                nfcViewModel.setLoggedIn(false)
            }

            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
                val loggedIn = nfcViewModel.isLoggedIn.value
                val isRefreshing = nfcViewModel.isRefreshing.value
                var username by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                // Check if token is available
                val token = TokenRepository.getToken()

                if (token != null) {
                    Log.d("MainActivity", "Token found: $token")
                    nfcViewModel.setLoggedIn(true)

                    username = "yves" // This could be dynamic based on the token or other data
                } else {
                    Log.d("MainActivity", "Token not found. User is not logged in.")
                    if (isRefreshing) {
                        Log.d("MainActivity", "Refreshing token...")
                        Toast.makeText(this, "Refreshing token...", Toast.LENGTH_SHORT).show()
                        nfcViewModel.setLoggedIn(true)
                    } else {
                        Log.d("MainActivity", "User is not logged in.")
                        nfcViewModel.setLoggedIn(false)
                    }
                }

                // State for scanning allowed
                var isScanningAllowed by remember { mutableStateOf(false) }


                val nfcd = nfcViewModel.nfcId.value
                val message = nfcViewModel.message.value
                val tripId2 = nfcViewModel.tripId.value
                val loading = bookingViewModel.loading.value
                val qrCodeData = nfcViewModel.qrcodeData.value

                LaunchedEffect(loading) {
                    if (loading) {
                        Log.d("MainActivity", "Loading bookings...")
                        nfcViewModel.setMessage("No bookings available yet")
                    } else {
                        Log.d("MainActivity", "Bookings loaded.")
                        nfcViewModel.setMessage("Tap Your Card or Scan QR Code")
                    }
                }

                LaunchedEffect(tripId2) {
                    if (tripId2 != null) {
                        Log.d("MainActivity", "Trip ID Changed: $tripId2")
                        BookingListenerManager.fetchBookingsForTrip(tripId2, bookingViewModel)
                        BookingListenerManager.startListeningForBookings(tripId2, bookingViewModel)
                    } else {
                        Log.e("MainActivity", "Trip ID is null. Cannot fetch bookings.")
                        bookingViewModel.clearBookings()
                    }
                }
//                LaunchedEffect(Bookings) {
//                    qrCodeScannerUtil.setQRCodeScanListener(object :
//                        QRCodeScannerUtil.QRCodeScanListener {
//                        override fun onQRCodeScanned(data: String?) {
//                            // Handle the scanned data
//                            data?.let {
//                                Log.d("MainActivity", "Scanned QR Code: $data")
//                                if (data != null && data.length > 3) {
//                                    // Process the scanned QR code data
//                                    bookingValidator.validateBooking(this@MainActivity, it, Bookings) { isValid ->
//                                        Log.d("NFC", "Validation status: $isValid $it")
//                                        if (isValid) {
//                                            nfcViewModel.setMessage("valid")
//                                        } else {
//                                            nfcViewModel.setNfcId(it)
//                                            nfcViewModel.setMessage("invalid")
//                                        }
//                                    }
//                                    Log.d("MainActivity", "Scanned QR Code: $it")
//                                }
//
//
//                            }
//                        }
//                    })
//                    Log.d("MainActivity", "NFC ID Changed: $nfcd")
//                }


                LaunchedEffect(carId) {
                    if (carId != null) {
                        TripListenerManager.startListeningForTrips(carId) { trips ->
                            // Update the isScanningAllowed state based on trips data
                            isScanningAllowed = trips.isNotEmpty()
                            Log.d("MainActivity", "Trips updated: $trips")
                            tripViewModel.setTrips(trips)

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
                LaunchedEffect(nfcd ) {
                    Log.d("NFC change", "NFC ID changed")
                    if (nfcd != null) {
                        nfcViewModel.clearQrCodeData()
                        Log.d("NFC change", "NFC ID change: $nfcd message: $message")
                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                        if (message == "invalid" && currentRoute != "extra") {
                            // If not already on the Extra page, navigate there
                            Log.d("NFC change", "Navigating to Extra Page")
                            navController.navigate("extra?nfcId=$nfcd") {
                                // Optionally clear the back stack if necessary to prevent navigating back to this page
                                popUpTo("extra") { inclusive = true }
                                launchSingleTop = true
                            }
//                            nfcViewModel.clearNfcId()
                        } else {
                            if (currentRoute != "message" ) {
                                nfcViewModel.clearNfcId()
                                // If not already on the NFC scan page, navigate there
                                Log.d("NFC change", "Navigating to NFC Scan Page")
                                navController.navigate("message") {
                                    // Optionally clear the back stack if necessary to prevent navigating back to this page
                                    popUpTo("message") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }
                LaunchedEffect(qrCodeData) {
                    if (qrCodeData != null) {
                        Log.d("NFC change", "QR Code Data change: $qrCodeData")
                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                        if (currentRoute != "message") {
                            // If not already on the NFC scan page, navigate there
                            Log.d("NFC change", "Navigating to message Page")
                            navController.navigate("message") {
                                // Optionally clear the back stack if necessary to prevent navigating back to this page
                                popUpTo("message") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }
                // Create NavHost to navigate between pages
                NavHost(
                    navController,
                    startDestination = if (loggedIn) "message" else "login" // Start at NFC Scan if logged in, else login page
                ) {

                    composable("message") {
                        MessagePage(
                            nfcViewModel = nfcViewModel,
                            bookingViewModel = bookingViewModel
                        )
                    }


                    composable("login") {
                        LoginPage { user, pass ->
                            // Update state with login details
                            username = user
                            password = pass
                            nfcViewModel.setLoggedIn(true) // Mark the user as logged in
                            // Navigate to NFC Scan page
                            navController.navigate("message") {
                                popUpTo("login") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }


                    composable("helloWorld") {
                        HelloWorldPage(
                            username = username,
                            onLogout = {
                                nfcViewModel.setLoggedIn(false) // Mark the user as logged out
                                TokenRepository.removeToken() // Remove the token on logout
                                navController.navigate("login") {
                                    popUpTo("helloWorld") { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onGoToExtra = { navController.navigate("message") },
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
                                navController.navigate("message") {
                                    popUpTo("extra") { inclusive = true }
                                    launchSingleTop = true
                                }

                            },
                            bookingViewModel = bookingViewModel,
                            nfcViewModel = nfcViewModel,
                            onBookingSuccess = {
                                soundManagement.playSound(com.google.android.libraries.navigation.R.raw.test_sound)
                                nfcViewModel.setMessage("valid")
                                navController.navigate("message") {
                                    popUpTo("extra") { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            tripViewModel = tripViewModel
                        )
                    }
                }
            }
        }
    }



    override fun onStart() {
        super.onStart()
        // Start monitoring network changes
        networkMonitor.startMonitoring()
        val baudRate = 9600
        val isScannerOpen = qrCodeScannerUtil.openScanner(baudRate)
        if (isScannerOpen) {
            Log.i("QRCodeScanner", "Scanner opened successfully.")
        } else {
            Log.e("QRCodeScanner", "Failed to open scanner.")
        }
    }
    @SuppressLint("NewApi", "SuspiciousIndentation")
    override fun onResume() {
        super.onResume()
        soundManagement.loadSound(com.google.android.libraries.navigation.R.raw.test_sound)
        networkMonitor.startMonitoring()
        qrCodeScannerUtil.openScanner(9600)

        TripListenerManager.startListeningForTrips(CarIdStorage.getLinkedCarId() ?: "",
            onUpdate = { newTrips ->
                if(newTrips.isNotEmpty()) {
                    tripViewModel.setTrips(newTrips)
                }

            }) // Start listening to trips when the activity is resumed

        val tripId = CarIdStorage.getTripId()
        if (tripId != null) {
            lifecycleScope.launch {
                BookingListenerManager.fetchBookingsForTrip(tripId, bookingViewModel)
            }
        }
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
        TripListenerManager.stopListening()
        networkMonitor.stopMonitoring()
        nfcReaderHelper.disableNfcReader(this)
        qrCodeScannerUtil.closeScanner()
    }
    override fun onDestroy() {
        super.onDestroy()
        soundManagement.release()
        networkMonitor.stopMonitoring()
        qrCodeScannerUtil.closeScanner()
        TripListenerManager.stopListening() // Stop listening to trips when the activity is destroyed
    }
}