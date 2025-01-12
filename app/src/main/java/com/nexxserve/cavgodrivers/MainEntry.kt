package com.nexxserve.cavgodrivers


import NfcViewModel
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.common.CommonConstants.LedColor
import com.common.CommonConstants.LedType
import com.common.apiutil.pos.CommonUtil
import com.common.apiutil.system.SystemApiUtil
import com.common.apiutil.util.SDKUtil
import com.google.firebase.FirebaseApp
import com.nexxserve.cavgodrivers.fragment.BookingDetails
import com.nexxserve.cavgodrivers.nfc.NFCReaderHelper
import com.nexxserve.cavgodrivers.qr.QRCodeScannerUtil
import com.nexxserve.cavgodrivers.ui.theme.CavgodriversTheme
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
    private var mCommonUtil: CommonUtil? = null
    private var mSystemLib: SystemApiUtil? = null


    private fun observeRefreshingState() {
        var token = TokenRepository.getToken()
        nfcViewModel.isRefreshing.observe(this) { isRefreshing ->
            if (!isRefreshing) {
                Log.d("Xctest", "Refreshing has completed")
                lifecycleScope.launch {
                    // Wait for token to be fetched or refreshed
                    token = TokenRepository.getToken()
                    val carId = CarIdStorage.getLinkedCarId()
                    if (token != null) {
                        Log.d("Main", "Token is available: $token")
                        nfcViewModel.setLoggedIn(true)

                        // Proceed with the operations after ensuring the token is ready
                        if (carId != null) {
                            Log.d("Main", "Car ID: $carId")
                            TripListenerManager.startListeningForTrips(carId,
                                onUpdate = { newTrips ->
                                    if (newTrips.isNotEmpty()) {
                                        tripViewModel.setTrips(newTrips)
                                    }
                                })
                            // Fetch bookings for the trip only after ensuring the token is valid
                            val tripId = CarIdStorage.getTripId()
                            if (tripId != null) {
                                BookingListenerManager.fetchBookingsForTrip(tripId, bookingViewModel, nfcViewModel)
                            }
                        } else {
                            Log.d("MainActivity", "Car ID is null")
                        }
                    } else {
                        Log.d("Main", "No token found, unable to proceed with bookings.")
                        nfcViewModel.setLoggedIn(false)
                    }
                }
                // Perform additional actions when refreshing is done
            }
        }
    }


    @OptIn(ExperimentalStdlibApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TokenRepository.init(this, nfcViewModel)
        CarIdStorage.init(this)
        CarIdStorage.setNfcViewModel(nfcViewModel)
        FirebaseApp.initializeApp(this)
        SDKUtil.getInstance(this).initSDK()
        soundManagement = SoundManagement.getInstance(this)
        notificationHelper = NotificationHelper.getInstance(this)
        networkMonitor = NetworkMonitor(this, bookingViewModel, nfcViewModel, notificationHelper)
        TripListenerManager.initialize(this)
        qrCodeScannerUtil = QRCodeScannerUtil(this)
        mCommonUtil = CommonUtil(this)
        var bookings by mutableStateOf(emptyList<BookingDetails>())
        mSystemLib = SystemApiUtil(this)
        mSystemLib!!.registerWakeUpAppBroadcast()

        mSystemLib!!.showStatusBar()
        mSystemLib!!.showNavigationBar()

//        observeRefreshingState()

        nfcViewModel.networkAvailable.observe(this) { networkAvailable ->
            Log.d("Main",  "Mornitoring network")
            if(networkAvailable) {
                Log.d("MAin", "Network good")
                observeRefreshingState()
            }
            else {
                Log.w("Main", "NetworkLost")
            }

        }



        bookingViewModel.bookings.observe(this) { bookngs ->
            // Update your UI with the new bookings list
            Log.d("MainActivity", "Bookings updated size: ${bookngs.size}")
            if (bookngs.isNotEmpty()) {
                bookings = bookngs
                Log.d("MainActivity", "Bookings var updated size: ${bookings.size}")

            } else {
                bookings = emptyList()
                Log.d("MainActivity", "No bookings available yet")
                nfcViewModel.setMessage("No bookings available yet")
            }
        }

        notificationHelper.createNotificationChannel()

        soundManagement.loadSound(com.google.android.libraries.navigation.R.raw.test_sound)

        mCommonUtil!!.setColorLed(LedType.COLOR_LED_1, LedColor.GREEN_LED, 0)

        fun turnOnLights(led: String) {
            // Determine the color and brightness
            val color = when (led) {
                "green" -> LedColor.GREEN_LED
                else -> LedColor.RED_LED
            }
            val bright = if (led == "off") 0 else 255
            Log.d("MainLed", "color$color bright$bright")

            // List of LED types
            val ledTypes = listOf(
                LedType.COLOR_LED_1,
                LedType.COLOR_LED_2,
                LedType.COLOR_LED_3,
                LedType.COLOR_LED_4
            )

            // Turn off all LEDs if the input is "off"
            if (led == "off") {
                // Turn off both green and red LEDs
                val colorsToTurnOff = listOf(LedColor.GREEN_LED, LedColor.RED_LED)
                for (ledColor in colorsToTurnOff) {
                    ledTypes.forEach { ledType ->
                        mCommonUtil!!.setColorLed(ledType, ledColor, 0) // Pass zero brightness
                    }
                }
                Log.d("MainLed", "All LEDs turned off")
            } else {
                // Handle the case for specific LED color and brightness
                val isOn = ledTypes.map { ledType ->
                    mCommonUtil!!.setColorLed(ledType, color, bright)
                }.firstOrNull() // Assuming you're interested only in the first LED status
                Log.d("MainLed", "isOn$isOn")
            }
        }


        val token = TokenRepository.getToken()
        if (token != null) {
            nfcViewModel.setLoggedIn(true)
            Log.d("Main", "Token is not null")
        } else {
            val isRefreshing = nfcViewModel.isRefreshing.value
            val retokenise = TokenRepository.getRefresh()
            if(retokenise == null) {
                Log.d("Main", "Token is null and not refreshing")
                nfcViewModel.setLoggedIn(false)
            } else {
                Log.d("Main", "Token is null and refreshing")
                nfcViewModel.setLoggedIn(true)
            }
        }


        nfcReaderHelper = NFCReaderHelper(
            context = this,
            onTagRead = { tag ->
                // Handle the NFC tag read here
                Log.d("NFC", "Tag read: ${tag.id}")
                val tagIdHex = tag.id.toHexString()
//                nfcViewModel.setNfcId("")
                nfcViewModel.clearNfcId()
                nfcViewModel.clearMessage()
                nfcViewModel.clearNfcId()
//                soundManagement.playSound(com.google.android.libraries.navigation.R.raw.test_sound)

                bookingValidator.validateBooking(this,tagIdHex, bookings, nfcViewModel) { isValid ->

                    Log.d("NFC", "Validation status: $isValid $tagIdHex")
                    if (isValid) {
                        nfcViewModel.setMessage("valid")
                    } else {
                        nfcViewModel.setNfcId(tagIdHex)
                        nfcViewModel.setMessage("invalid")
                    }

                }
                nfcViewModel.setNfcId(tagIdHex)
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
                    if(bookings.isNotEmpty()){
                        bookingValidator.validateBooking(this@MainActivity,data, bookings, nfcViewModel) { isValid ->

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





        // Set content based on login state
        setContent {
            val isLoggedIn = nfcViewModel.isLoggedIn.value
            val isRefreshing = nfcViewModel.isRefreshing.value

            LaunchedEffect(isLoggedIn, isRefreshing) {
                if (isLoggedIn) {
                    // User is logged in, navigate to the main app
                    Log.d("Main", "User is logged in")
                    if(isRefreshing == true) {
                        Log.d("Main", "User is logged in and refreshing")
                    }
                } else {
                    // User is not logged in, stay on the login page
                    Log.d("Auth", "User is not logged in")
                }
            }

            CavgodriversTheme {
                val navController = rememberNavController() // Initialize NavController

                if (isLoggedIn) {
                    MainApp(
                        navController = navController,
                        nfcViewModel = nfcViewModel,
                        bookingViewModel = bookingViewModel,
                        tripViewModel = tripViewModel,
                        soundManagement = soundManagement,
                        turnOnLights = { led ->
                            turnOnLights(led)
                        }
                    )
                } else {
                    val retokenise = TokenRepository.getRefresh()
                    if(retokenise != null) {
                        LoadingDots()
                    } else {
                        Log.d("MAinissue", "$retokenise")
                        LoginPage(
                            onLoginSuccess = {
                                // Perform login logic
                                nfcViewModel.setLoggedIn(true) // Mark the user as logged in
                                // Navigate to HelloWorldPage

                            }

                        )
                    }
                }
            }
        }
    }
    override fun onStart() {
        super.onStart()
        // Start monitoring network changes
        TokenRepository.getToken()
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

        // Ensure Token is refreshed before proceeding with other actions
        Log.d("Resuming", "resume")
        nfcViewModel.networkAvailable.observe(this) { networkAvailable ->
            Log.d("Main",  "Mornitoring network")
            if(networkAvailable) {
                Log.d("MAin", "Network good")
                observeRefreshingState()
            }
            else {
                Log.w("Main", "NetworkLost")
            }

        }

//        observeRefreshingState()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            val windowInsetsController = window.insetsController
//            windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
//            windowInsetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
//        } else {
//            // For lower versions, use the old way of hiding the status and navigation bar
//            window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
//        }

        if (nfcReaderHelper.isNfcSupported() && nfcReaderHelper.isNfcEnabled()) {
            nfcReaderHelper.enableNfcReader(this)
        } else {
            Log.e("MainActivity", "NFC is not enabled or supported.")
        }
    }

    override fun onPause() {
        super.onPause()
        TripListenerManager.stopListening()
        nfcViewModel.setNetworkAvailable(false)
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
