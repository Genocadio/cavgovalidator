package com.nexxserve.cavgodrivers

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nexxserve.cavgodrivers.fragment.BookingDetails

class BookingViewModel : ViewModel() {
    // Use MutableLiveData to manage the state and LiveData to expose it
    private val _bookings = MutableLiveData<List<BookingDetails>>()
    val bookings: LiveData<List<BookingDetails>> = _bookings

    private val _loading = mutableStateOf(false) // Add loading state
    val loading: State<Boolean> get() = _loading

    // Function to update bookings
    fun setBookings(newBookings: List<BookingDetails>) {
        _bookings.value = newBookings
    }

    // Function to add a new booking
    fun addBooking(booking: BookingDetails) {
        val currentBookings = _bookings.value.orEmpty().toMutableList()

        // Check for an existing booking with the same ID
        val existingIndex = currentBookings.indexOfFirst { it.id== booking.id }

        if (existingIndex != -1) {
            // Replace the existing booking with the new one
            currentBookings[existingIndex] = booking
            Log.d("BookingViewModel", "Updated existing booking with ID: ${booking.id}")
        } else {
            // Add the new booking to the list
            currentBookings.add(booking)
            Log.d("BookingViewModel", "Added new booking with ID: ${booking.id}")
        }

        _bookings.value = currentBookings
    }

    // Function to remove a booking
    fun removeBooking(bookingId: String) {
        val currentBookings = _bookings.value.orEmpty().filterNot { it.id == bookingId }
        _bookings.value = currentBookings
    }
    fun clearBookings() {
        _bookings.value = emptyList()
        Log.d("BookingViewModel", "Cleared bookings")
    }

    fun setLoading(isLoading: Boolean) {
        _loading.value = isLoading
    }
}
