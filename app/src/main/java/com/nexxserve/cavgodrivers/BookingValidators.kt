package com.nexxserve.cavgodrivers

import android.content.Context
import android.util.Log
import com.nexxserve.cavgodrivers.fragment.BookingDetails
import android.R


class BookingValidator {

    private var soundManagement: SoundManagement? = null

    // Check if a booking is valid based on scanned data
    private fun isBookingValid(scannedData: String, bookings: List<BookingDetails?>): Boolean {
        if (bookings.isEmpty()) {
            return false
        }
        for (booking in bookings) {
            booking?.ticket?.nfcId?.let {
                Log.d("Booking Validator", it)
                if (scannedData == it) {
                    return true
                }
            }
            booking?.ticket?.qrCodeData?.let {
                if (scannedData == it) {
                    return true
                }
            }
        }
        return false
    }

    // Validate booking and invoke the callback with the result
    fun validateBooking(
        context: Context, // Add context parameter
        scannedData: String,
        bookings: List<BookingDetails>,
        callback: (Boolean) -> Unit
    ) {
        // Initialize SoundManagement with the provided context
        soundManagement = SoundManagement.getInstance(context)

        val isValid = isBookingValid(scannedData, bookings)

        // Play a sound based on whether the booking is valid or not
        if (isValid) {
            // Play a success sound
            soundManagement?.playSound(com.google.android.libraries.navigation.R.raw.test_sound)  // Replace with your success sound file
        } else {
            // Play an error sound
//            soundManagement?.playSound(com.google.android.libraries.navigation.R.raw.test_sound)  // Replace with your error sound file
        }

        callback(isValid)
    }
}
