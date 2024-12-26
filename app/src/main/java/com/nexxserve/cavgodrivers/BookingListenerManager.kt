package com.nexxserve.cavgodrivers

import android.util.Log
import com.apollographql.apollo.api.Optional
import com.nexxserve.cavgodrivers.fragment.BookingDetails
import kotlinx.coroutines.flow.*

object BookingListenerManager {

    private var tripId: String? = null
    private val bookingsList = mutableListOf<BookingDetails>()
    private val _currentBookingsFlow = MutableStateFlow<List<BookingDetails>>(emptyList())

//    val currentBookingsFlow: StateFlow<List<BookingDetails>> = _currentBookingsFlow


    // Fetch bookings for the provided tripId
// Fetch bookings for the provided tripId
    suspend fun fetchBookingsForTrip(tripId: String) {
        try {
            // Fetch the bookings data from GraphQL
            val response = apolloClient.query(GetBookingsQuery(Optional.Present(tripId))).execute()
            val bookingsData = response.data?.getBookings?.data ?: emptyList()

            // Log the size of the fetched bookings
            Log.d("BookingListenerManager", "Fetched ${bookingsData.size} bookings for trip ID: $tripId")

            // Safely map the bookingsData to BookingDetails
            val bookings = bookingsData.mapNotNull { booking ->
                try {
                    // Extract booking details and map it to BookingDetails
                    booking.bookingDetails.let { bookingDetail ->
                        // Now bookingDetail is of type BookingDetails
                        Log.d("BookingListenerManager", "Converted booking ID: ${bookingDetail.id}")
                        bookingDetail
                    }
                } catch (e: Exception) {
                    Log.e("BookingListenerManager", "Error processing booking: ${e.message}")
                    null
                }
            }

            // Log the number of successfully converted bookings
            Log.d("BookingListenerManager", "Converted ${bookings.size} bookings for trip ID: $tripId")

            // Add the bookings to the list
            addBookings(bookings)
        } catch (e: Exception) {
            Log.e("BookingListenerManager", "Error fetching bookings for trip ID: $tripId", e)
            addBookings(emptyList())
        }
    }


    // Add multiple bookings to the list and update the StateFlow
    private fun addBookings(bookings: List<BookingDetails>) {
        synchronized(bookingsList) {
            bookingsList.addAll(bookings)
            _currentBookingsFlow.value = bookingsList
            Log.d("BookingListenerManager", "Added ${bookings.size} bookings to the list.")
        }

    }

    // Add a single booking to the list and update the StateFlow
    private fun addBooking(booking: BookingDetails) {
        synchronized(bookingsList) {
            bookingsList.add(booking)
            _currentBookingsFlow.value = bookingsList
            Log.d("BookingListenerManager", "Added booking to the list: ${booking.id}")
        }
    }

    // Start listening for new bookings
    suspend fun startListeningForBookings(
        tripId: String,
        onBookingAdded: (BookingAddedSubscription.BookingAdded) -> Unit
    ) {
        stopListening() // Stop any previous listener
        this.tripId = tripId
        bookingsList.clear()
        _currentBookingsFlow.value = emptyList()

        Log.d("BookingListenerManager", "Starting subscription for trip ID: $tripId")
        try {
            apolloClient.subscription(BookingAddedSubscription(tripId))
                .toFlow()
                .onStart {
                    Log.d("BookingListenerManager", "Subscription started for trip ID: $tripId")
                }
                .catch { e ->
                    Log.e("BookingListenerManager", "Error in subscription for trip ID: $tripId", e)
                    stopListening()
                }
                .collect { response ->
                    response.data?.bookingAdded?.let { booking ->


                        onBookingAdded(booking)
                        addBooking(booking.bookingDetails )
                    } ?: Log.w("BookingListenerManager", "Empty booking data received for trip ID: $tripId")
                }
        } catch (e: Exception) {
            Log.e("BookingListenerManager", "Unexpected error in subscription: $e")
        }
    }

    // Stop listening for bookings
    private fun stopListening() {
        Log.d("BookingListenerManager", "Stopped listening for bookings for trip ID: $tripId")
        tripId = null
        bookingsList.clear()
        _currentBookingsFlow.value = emptyList()
    }

    // Get the current bookings list (for UI or debugging)
    fun getCurrentBookings(): List<BookingDetails> = synchronized(bookingsList) { bookingsList.toList() }
}
