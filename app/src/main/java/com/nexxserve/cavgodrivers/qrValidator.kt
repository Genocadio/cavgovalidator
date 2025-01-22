package com.nexxserve.cavgodrivers
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject
import java.net.URLDecoder

/**
 * Verifies the QR code data extracted from a custom URL scheme.
 *
 * @param qrCodeUrl The full URL containing the QR code data (e.g., "myapp://ticket?data=BASE64_ENCODED_QR_CODE").
 * @param secretKey The secret key used to generate the hash.
 * @return True if the QR code is valid, false otherwise.
 */
@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.O)
fun verifyQrCodeDataFromUrl(qrCodeUrl: String, secretKey: String): Boolean {
    try {
        // Extract the base64-encoded data from the URL
        val prefix = "myapp://ticket?data="
        if (!qrCodeUrl.startsWith(prefix)) {
            throw IllegalArgumentException("Invalid URL format")
        }

        val encodedData = qrCodeUrl.removePrefix(prefix)
        val base64Data = URLDecoder.decode(encodedData, "UTF-8") // Decode any URL-encoded characters

        // Decode the base64-encoded QR code data
        val decodedData = String(Base64.getDecoder().decode(base64Data))

        // Parse the JSON data
        val qrCodeJson = JSONObject(decodedData)
        Log.d("QR Code Data", qrCodeJson.toString())
        val payload = qrCodeJson.getJSONObject("payload")
        val receivedHash = qrCodeJson.getString("hash")

        // Recreate the hash from the payload
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
        mac.init(secretKeySpec)
        val computedHash = mac.doFinal(payload.toString().toByteArray())
        val computedHashHex = computedHash.joinToString("") { "%02x".format(it) }

        // Compare the received hash with the computed hash
        return receivedHash == computedHashHex
    } catch (e: Exception) {
        // Handle errors (e.g., invalid QR code format or URL)
        println("Error verifying QR code: ${e.message}")
        return false
    }
}

