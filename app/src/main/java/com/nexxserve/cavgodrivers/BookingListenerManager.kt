package com.nexxserve.cavgodrivers

import android.util.Log
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.MutableStateFlow

object BookingListenerManager {

    private var tripId: String? = null
    private val bookingsList = mutableListOf<BookingAddedSubscription.BookingAdded>()
    private val _currentBookingsFlow = MutableStateFlow<List<BookingAddedSubscription.BookingAdded>>(emptyList())

    // Method to start actively monitoring new bookings for the provided tripId
    suspend fun startListeningForBookings(tripId: String, onBookingAdded: (BookingAddedSubscription.BookingAdded) -> Unit) {
        stopListening() // Stop any previous listeners if there were any
        this.tripId = tripId // Set the trip ID to track bookings for
        bookingsList.clear() // Clear previous bookings

        // Start the subscription to monitor new bookings
        Log.d("BookingListenerManager", "Starting to listen for bookings for trip ID: $tripId")
        apolloClient.subscription(BookingAddedSubscription(tripId = tripId))
            .toFlow()
            .onStart {
                Log.d("BookingListenerManager", "Subscription started for trip ID: $tripId")
            }
            .catch { e ->
                Log.e("BookingListenerManager", "Error in booking subscription for trip ID: $tripId", e)
                stopListening() // Stop listening on error
            }
            .collect { response ->
                response.data?.bookingAdded?.let {
                    Log.d("BookingListenerManager", "New booking added: ${it.id} for trip ID: $tripId")
                    // Call the onBookingAdded callback to handle the new booking
                    try {
                        onBookingAdded(it)
                    } catch (e: Exception) {
                        Log.e("BookingListenerManager", "Error processing new booking: ${it.id}", e)
                    }

                    // Add the new booking to the existing list and update the flow
                    addBookingToList(it)
                } ?: run {
                    Log.w("BookingListenerManager", "Received empty booking data for trip ID: $tripId")
                }
            }
    }

    // Stop actively listening for bookings
    private fun stopListening() {
        Log.d("BookingListenerManager", "Stopped listening for bookings for trip ID: $tripId")
        tripId = null
        bookingsList.clear()
        _currentBookingsFlow.value = emptyList() // Reset the current bookings flow when stopped
    }

    // Add the new booking to the existing bookings list and update the StateFlow
    private fun addBookingToList(booking: BookingAddedSubscription.BookingAdded) {
        bookingsList.add(booking)
        _currentBookingsFlow.value = bookingsList // Update the flow with the latest bookings
        Log.d("BookingListenerManager", "Booking added to list: ${booking.id}")
    }

    // Optional: Method to get all current bookings (for UI updates)
    fun getCurrentBookings(): List<BookingAddedSubscription.BookingAdded> {
        return bookingsList
    }
}
