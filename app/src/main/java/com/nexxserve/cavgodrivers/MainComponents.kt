package com.nexxserve.cavgodrivers

import NfcViewModel
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kotlinx.coroutines.launch

@Composable
fun MainApp(navController: NavHostController, nfcViewModel: NfcViewModel, bookingViewModel: BookingViewModel, tripViewModel: TripViewModel, soundManagement: SoundManagement, turnOnLights: (String) ->  Unit) {
    val userId = CarIdStorage.getId()
    val isLoggedIn = nfcViewModel.isLoggedIn.value
    val carId = CarIdStorage.getLinkedCarId()
    val message = nfcViewModel.message.value
    val tripId = nfcViewModel.tripId.value
    val loading = bookingViewModel.loading.value
    val qrCodeData = nfcViewModel.qrcodeData.value
    val nfcd = nfcViewModel.nfcId.value
    var scope = rememberCoroutineScope()


    val startPage = if(userId == null && carId != null) {
        "message"
    } else if(userId != null && carId == null) {
        "helloWorld"
    } else if(userId == null && carId == null) {
        "message"
    } else {
        CarIdStorage.removeId()
        Log.d("ElseMain", "User ID: $userId, Car ID: $carId")
        "message"
    }
    LaunchedEffect(isLoggedIn ) {
        if(isLoggedIn) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            Log.d("MainComp", "Current route: $currentRoute, Start page: $startPage")
            if(startPage == "message" && tripId != null) {
                scope.launch{
                    BookingListenerManager.fetchBookingsForTrip(tripId, bookingViewModel, nfcViewModel)
                    BookingListenerManager.startListeningForBookings(tripId, bookingViewModel)
                }
            }
            if(currentRoute == startPage) {
                navController.navigate(startPage) {
                    popUpTo("login") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
    if (isLoggedIn && carId != null) {
        LaunchedEffect(loading) {
            if (loading) {
                Log.d("MainActivity", "Loading bookings...")
                nfcViewModel.setMessage("No bookings available yet")
            } else {
                Log.d("MainActivity", "Bookings loaded.")
                nfcViewModel.setMessage("Tap Your Card or Scan QR Code")
            }
        }

        LaunchedEffect(tripId) {
            if (tripId != null) {
                Log.d("MainActivity", "Trip ID Changed: $tripId")
                BookingListenerManager.fetchBookingsForTrip(tripId, bookingViewModel, nfcViewModel)
                BookingListenerManager.startListeningForBookings(tripId, bookingViewModel)
            } else {
                Log.e("MainActivity", "Trip ID is null. Cannot fetch bookings.")
                bookingViewModel.clearBookings()
            }
        }
        LaunchedEffect(nfcd ) {
            Log.d("NFC change", "NFC ID changed 2 $nfcd")
            if (nfcd != null) {
                nfcViewModel.clearQrCodeData()
                Log.d("NFC change", "NFC ID change 1: $nfcd message: $message")
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (message == "invalid" && currentRoute != "extra") {
                    // If not already on the Extra page, navigate there
                    Log.d("NFC change", "Navigating to Extra Page")
                    navController.navigate("extra?nfcId=$nfcd") {
                        // Optionally clear the back stack if necessary to prevent navigating back to this page
                        popUpTo("extra") { inclusive = true }
                        launchSingleTop = true
                    }
                    nfcViewModel.clearMessage()

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
//        LaunchedEffect(message) {
//            Log.d("NFC change", "Message changed: $message")
//
//            if (message == "valid") {
//                turnOnLights("off")
//                turnOnLights("green")
//            } else if (message == "invalid") {
//                turnOnLights("off")
//                turnOnLights("red")
//            } else {
//                Log.d("NFC change", "Message changed to swich: $message")
//                turnOnLights("off")
//            }
//        }

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

    } else {
        Log.d("Main", "User is not logged in or car is not linked. NFC ID change: $nfcd")
    }



    // Log NFC ID changes whenever it changes


    NavHost(
        navController = navController,
        startDestination = startPage
    ) {
        // Home Page
        composable("message") {
            MessagePage(
                nfcViewModel = nfcViewModel,
                bookingViewModel = bookingViewModel,
                navController
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

        // Profile Page
        composable("helloWorld") {
            HelloWorldPage(
                onLogout = {
                    nfcViewModel.setLoggedIn(false) // Mark the user as logged out
                    TokenRepository.resetRepo()
                    CarIdStorage.removeSerial()
                    CarIdStorage.removeLinkedCarId()
                },
                onGoToExtra = {
                    val id = CarIdStorage.getId()
                    if (id != null) {
                        nfcViewModel.setRefreshData(true)
                        TripListenerManager.startListeningForTrips(id,
                            onUpdate = { newTrips ->
                                if(newTrips.isNotEmpty()) {
                                    tripViewModel.setTrips(newTrips)
                                }
                            })

                        navController.navigate("message") {
                            popUpTo("helloWorld") { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        Log.d("Main", "No car linked")
                    }
                },
                tripViewModel = tripViewModel,
                bookingViewModel = bookingViewModel,
                nfcViewModel = nfcViewModel,
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
    }
}