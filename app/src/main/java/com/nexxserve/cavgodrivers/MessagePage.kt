package com.nexxserve.cavgodrivers

import NfcViewModel
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nexxserve.cavgodrivers.fragment.BookingDetails

@Composable
fun MessagePage(
    nfcViewModel: NfcViewModel,
    bookingViewModel: BookingViewModel,
    navController: NavController,
) {
    // Observe the message state from the ViewModel
    val message = nfcViewModel.message.value
    val bookings by bookingViewModel.bookings.observeAsState(initial = emptyList())
    val bookingId = nfcViewModel.bookingid.value
    var booking: BookingDetails? = null
    var totalTickets by remember { mutableIntStateOf(0) }
    val isloggedin = nfcViewModel.isLoggedIn.value
    val CarId = CarIdStorage.getLinkedCarId()

    if(CarId == null) {
        Log.d("MessagePage", "Navigating to CarId Page")
        CarIdStorage.removeSerial()
        CarIdStorage.removeLinkedCarId()
        navController.navigate("helloworld") {
            popUpTo("message") {
                inclusive = true
            }
        }
    }

    LaunchedEffect(isloggedin) {
        if (!isloggedin) {
            Log.d("MessagePage", "Navigating to Login Page")
            TokenRepository.getToken()
        }
    }

    LaunchedEffect(bookings) {
        Log.d("MessagePage", "Bookings M: ${bookings.size}")
        totalTickets = bookings.sumOf { it.numberOfTickets }
    }


//    if (message == "invalid") {
//        androidx.compose.runtime.LaunchedEffect(key1 = message) {
//            if (nfcId != null) {
//                Log.d("MessagePage", "Navigating to ExtraPage with NFC ID: $nfcId")
//                onGoToExtra(nfcId)
//            }
//        }
//    }

    if (message == "valid" && bookingId != null && bookings.isNotEmpty())  {

        booking = bookings.find { it.id == bookingId }
        if (booking != null) {
            Log.d("MessagePage", "Found booking: ${booking.user?.firstName}")
        }

    }

    if (message == "valid" || message == "invalid") {
        nfcViewModel.resetMessage()
    }

    Text(
        text = "Bookings: $totalTickets",
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = Color.Cyan.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp)
            )
            .border(2.dp, Color.Blue, RoundedCornerShape(8.dp))
            .padding(4.dp), // Additional padding inside
        textAlign = TextAlign.Center,
        fontSize = 30.sp,
//            style = MaterialTheme.typography.displayLarge,
        color = Color.Black
    )
    // Define a default, valid, and invalid state
    when {
        message.isNullOrEmpty() -> {
            // Default message state: "Tap card"
            MessageCard(message = "Tap Your Card or SCan Qr Code", backgroundColor = Color.Gray.copy(alpha = 0.8f), icon = null)
        }
        message == "valid" -> {
            // Valid message: Show tick and valid ticket message
            MessageCard(
                message = "Valid Ticket",
                backgroundColor = Color.Green.copy(alpha = 0.8f),
                booking = booking,
                icon = Icons.Filled.Check

            )
        }
        message == "invalid" -> {
            // Invalid message: Show error and invalid ticket message
            MessageCard(
                message = "Invalid Ticket",
                backgroundColor = Color.Red.copy(alpha = 0.8f),
                icon = Icons.Filled.Clear
            )

        }
        else -> {
            // For other messages, display the default behavior
            MessageCard(message = message, backgroundColor = Color.Gray.copy(alpha = 0.8f), icon = null)
        }
    }
}

@Composable
fun MessageCard(
    message: String,
    backgroundColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    booking: BookingDetails? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()

            .padding(16.dp, 70.dp, 16.dp, 16.dp)
            .background(backgroundColor, shape = RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Display icon if available
            if (booking != null) {
                Text(
                    text = "👤 ${booking.user?.firstName} ${booking.user?.lastName}",
                    style = TextStyle(fontSize = 20.sp, color = Color.White),
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = "🚎${booking.destination}",
                    style = TextStyle(fontSize = 20.sp, color = Color.White),
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = "Tickets",
                    style = TextStyle(fontSize = 20.sp, color = Color.White),
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = "${booking.numberOfTickets}",
                    style = TextStyle(fontSize = 180.sp, color = Color.Black),
                    modifier = Modifier.padding(1.dp)
                )
            }
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(booking?.let { 40.dp } ?: 400.dp)
                        .padding(bottom = 8.dp),
                    tint = Color.White
                )
            }
            // Display message text
            Text(
                text = message,
                style = TextStyle(fontSize = 20.sp, color = Color.White),
                modifier = Modifier.padding(16.dp)
            )

        }
    }
}
