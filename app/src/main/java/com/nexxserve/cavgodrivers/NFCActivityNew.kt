package com.nexxserve.cavgodrivers

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexxserve.cavgodrivers.nfc.NFCReaderHelper
import com.nexxserve.cavgodrivers.qr.QRCodeScannerUtil

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCScanPage() {
    val context = LocalContext.current
    var tagData by remember { mutableStateOf("") }
    var qrData by remember { mutableStateOf("") }
    var combinedScanData by remember { mutableStateOf("") }
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTagId by remember { mutableStateOf("") }
    val activity = context as ComponentActivity

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
                val techList = tag.techList.joinToString(", ")
                tagData = """
                    NFC Tag Detected:
                    Tag ID: $tagIdHex
                    Technologies: $techList
                    Tap Count: $tapCount
                """.trimIndent()
                combinedScanData = "$tagData\n\n$qrData"
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
                        qrData = """
                            QR Code Detected:
                            Data: $it
                        """.trimIndent()
                        combinedScanData = "$tagData\n\n$qrData"
                    }
                }
            })
        }
    }

    LaunchedEffect(nfcReaderHelper, qrCodeScanner) {
        // NFC Initialization
        if (!nfcReaderHelper.isNfcSupported()) {
            Toast.makeText(context, "NFC not supported on this device.", Toast.LENGTH_LONG).show()
        } else if (!nfcReaderHelper.isNfcEnabled()) {
            Toast.makeText(context, "Please enable NFC.", Toast.LENGTH_LONG).show()
            context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        } else {
            nfcReaderHelper.enableNfcReader(activity)
        }

        // Open QR Scanner
        qrCodeScanner.openScanner(9600)
    }

    DisposableEffect(Unit) {
        onDispose {
            nfcReaderHelper.disableNfcReader(activity)
            qrCodeScanner.closeScanner()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("NFC & QR Scan") })
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // QR Code Scanner Preview - takes top half of the screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp) // Adjust this value as needed for your layout
                ) {
                    CameraPreviewView()  // Placeholder for the camera preview
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Display QR scan data or NFC info
                if (combinedScanData.isNotEmpty()) {
                    Text(
                        text = combinedScanData,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Place your NFC tag near the device or scan a QR code.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

// This would be a placeholder for actual camera preview logic
@Composable
fun CameraPreviewView() {
    // Here you would integrate your CameraPreview logic
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)  // Use 'primary' instead of 'primaryVariant'
    ) {
        // Actual camera view goes here, possibly using a library like CameraX or custom CameraPreview composable.
        Text(text = "QR Camera Preview", color = MaterialTheme.colorScheme.onPrimary)
    }
}


fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
