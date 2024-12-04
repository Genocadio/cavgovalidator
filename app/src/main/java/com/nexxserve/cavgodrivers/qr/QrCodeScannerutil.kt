package com.nexxserve.cavgodrivers.qr

import android.content.Context
import android.util.Log
import com.common.apiutil.decode.DecodeReader

class QRCodeScannerUtil(context: Context?) {
    private val decodeReader = DecodeReader(context)
    private var isScannerOpen = false
    private var qrCodeScanListener: QRCodeScanListener? = null

    /**
     * Opens the QR code scanner with a specified baud rate.
     *
     * @param baud Baud rate for communication (e.g., 9600).
     * @return boolean indicating if the scanner was opened successfully.
     */
    fun openScanner(baud: Int): Boolean {
        val resultCode = decodeReader.open(baud)
        if (resultCode == 0) {
            isScannerOpen = true
            Log.i("QRCodeScannerUtil", "Scanner opened successfully.")
            return true
        } else {
            Log.e("QRCodeScannerUtil", "Failed to open scanner. Code: $resultCode")
            return false
        }
    }

    /**
     * Closes the QR code scanner.
     */
    fun closeScanner() {
        if (isScannerOpen) {
            decodeReader.close()
            isScannerOpen = false
            Log.i("QRCodeScannerUtil", "Scanner closed.")
        }
    }

    /**
     * Sets the listener for handling scanned QR code data.
     *
     * @param listener QRCodeScanListener to receive scanned data.
     */
    fun setQRCodeScanListener(listener: QRCodeScanListener?) {
        this.qrCodeScanListener = listener
        // Set the listener to start receiving scanned data
        decodeReader.setDecodeReaderListener { data ->
            if (data != null && data.isNotEmpty()) {
                val scannedData = String(data)
                Log.i("QRCodeScannerUtil", "Scanned QR Code: $scannedData")

                // Notify the listener
                qrCodeScanListener?.onQRCodeScanned(scannedData)
            } else {
                Log.e("QRCodeScannerUtil", "Empty or null data received.")
            }
        }
    }

    /**
     * Sends a command to the QR code scanner.
     *
     * @param command Byte array representing the command.
     * @return boolean indicating if the command was sent successfully.
     */
    fun sendCommand(command: ByteArray?): Boolean {
        val resultCode = decodeReader.cmdSend(command)
        if (resultCode == 0) {
            Log.i("QRCodeScannerUtil", "Command sent successfully.")
            return true
        } else {
            Log.e("QRCodeScannerUtil", "Failed to send command. Code: $resultCode")
            return false
        }
    }

    /**
     * Interface for handling scanned QR code data.
     */
    interface QRCodeScanListener {
        fun onQRCodeScanned(data: String?)
    }
}
