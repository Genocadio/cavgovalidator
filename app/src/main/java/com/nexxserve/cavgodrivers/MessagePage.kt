package com.nexxserve.cavgodrivers

import NfcViewModel
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MessagePage(
    nfcViewModel: NfcViewModel,
    onGoToExtra: (String) -> Unit
) {
    // Observe the message state from the ViewModel
    val message = nfcViewModel.message.value
    val nfcId = nfcViewModel.nfcId.value
    val qrCodeData = nfcViewModel.qrcodeData.value

//    if (message == "invalid") {
//        androidx.compose.runtime.LaunchedEffect(key1 = message) {
//            if (nfcId != null) {
//                Log.d("MessagePage", "Navigating to ExtraPage with NFC ID: $nfcId")
//                onGoToExtra(nfcId)
//            }
//        }
//    }

    if (message == "valid" || message == "invalid") {
        nfcViewModel.resetMessage()
    }


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
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(backgroundColor, shape = RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Display icon if available
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
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
