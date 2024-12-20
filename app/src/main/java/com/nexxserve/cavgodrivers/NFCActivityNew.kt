package com.nexxserve.cavgodrivers

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexxserve.cavgodrivers.nfc.NFCReaderHelper
import com.nexxserve.cavgodrivers.qr.QRCodeScannerUtil
import androidx.navigation.NavController
import com.apollographql.apollo.api.Optional
import androidx.compose.material.icons.filled.Close

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCScanPage(navController: NavController, isScanningAllowed: Boolean) {
    val context = LocalContext.current
    var tagData by remember { mutableStateOf("") }
    var qrData by remember { mutableStateOf("") }
    var combinedScanData by remember { mutableStateOf("") }
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTagId by remember { mutableStateOf("") }
    var bookings by remember { mutableStateOf<GetBookingsQuery.GetBookings?>(null) }
    var validationStatus by remember { mutableStateOf<Boolean?>(null) } // null means no validation yet
    val activity = context as ComponentActivity

    // Get the linked car ID and trip ID
    val linkedCarId = CarIdStorage.getLinkedCarId()
    val tripId by remember { mutableStateOf(CarIdStorage.getTripId()) }

    // Navigate away if no linked car ID
    if (linkedCarId == null) {
        LaunchedEffect(Unit) {
            navController.navigate("helloWorld") // Replace with your actual route
        }
    }

    // Fetch bookings when the page loads
    LaunchedEffect(tripId) {
        tripId?.let { currentTripId ->
            try {
                val fetchedBookings = getBookings(currentTripId)
                bookings = fetchedBookings
                Log.d("Bookings", "Fetched bookings: ${fetchedBookings?.data}")
            } catch (e: Exception) {
                Log.e("GetBookings", "Error fetching bookings: ${e.message}")
            }

            BookingListenerManager.startListeningForBookings(currentTripId) { booking ->
                Log.d("BookingListener", "New booking added: ${booking.id}")
            }
        }
    }

    // Initialize NFC Reader Helper
    val nfcReaderHelper = remember {
        NFCReaderHelper(
            context,
            onTagRead = { tag ->
                val tagIdHex = tag.id.toHexString()
                if (tagIdHex == lastTagId) {
                    tapCount++
                } else {
                    tapCount = 1
                    lastTagId = tagIdHex
                }
                validateBooking(tagIdHex, bookings) { isValid ->
                    validationStatus = isValid
                }
            },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Initialize QR Code Scanner Helper
    val qrCodeScanner = remember {
        QRCodeScannerUtil(context).apply {
            setQRCodeScanListener(object : QRCodeScannerUtil.QRCodeScanListener {
                override fun onQRCodeScanned(data: String?) {
                    data?.let {
                        validateBooking(it, bookings) { isValid ->
                            validationStatus = isValid
                        }
                    }
                }
            })
        }
    }

    // Enable scanning based on conditions
    LaunchedEffect(isScanningAllowed) {
        if (!isScanningAllowed || tripId == null) {
            Toast.makeText(context, "This car has no active trip or scanning is disabled.", Toast.LENGTH_LONG).show()
        } else {
            if (!nfcReaderHelper.isNfcSupported()) {
                Toast.makeText(context, "NFC not supported on this device.", Toast.LENGTH_LONG).show()
            } else if (!nfcReaderHelper.isNfcEnabled()) {
                Toast.makeText(context, "Please enable NFC.", Toast.LENGTH_LONG).show()
                context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            } else {
                nfcReaderHelper.enableNfcReader(activity)
            }
            qrCodeScanner.openScanner(9600)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            nfcReaderHelper.disableNfcReader(activity)
            qrCodeScanner.closeScanner()
        }
    }

    // UI Layout
    Scaffold(
        topBar = { TopAppBar(title = { Text("NFC & QR Scan") }) },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (validationStatus) {
                    true -> {
                        // Show green screen with a tick
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(128.dp)
                            )
                        }
                    }
                    false -> {
                        // Show red screen with a cross
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector =  Icons.Default.Close,
                                contentDescription = "Invalid",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(128.dp)
                            )
                        }
                    }
                    null -> {
                        // Default UI
                        Text(
                            text = "Place your NFC tag near the device or scan a QR code.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
}

fun validateBooking(
    scannedData: String,
    bookings: GetBookingsQuery.GetBookings?,
    callback: (Boolean) -> Unit
) {
    val isValid = isBookingValid(scannedData, bookings)
    callback(isValid)
}

fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

suspend fun getBookings(tripId: String): GetBookingsQuery.GetBookings? {
    val response = apolloClient.query(GetBookingsQuery(Optional.Present(tripId))).execute()
    return if (response.hasErrors()) {
        Log.e("GetBookings", "Failed to fetch bookings for trip ID: $tripId. Errors: ${response.errors?.joinToString()}")
        null
    } else {
        response.data?.getBookings
    }
}

fun isBookingValid(scannedData: String, bookings: GetBookingsQuery.GetBookings?): Boolean {
    // Ensure bookings data is not null
    if (bookings?.data == null) {
        return false
    }

    // Iterate through the fetched bookings to compare the scanned data
    for (booking in bookings.data) {
        // Check if the NFC data matches (if NFC is used)
        booking.ticket?.nfcId?.let {
            if (scannedData == it) {
                return true // NFC match found
            }
        }

        // Check if the QR code data matches (if QR code is used)
        booking.ticket?.qrCodeData?.let {
            if (scannedData == it) {
                return true // QR code match found
            }
        }
    }

    // Return false if no match is found
    return false
}
