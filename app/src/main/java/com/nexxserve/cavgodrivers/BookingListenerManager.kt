package com.nexxserve.cavgodrivers

import NfcViewModel
import android.util.Log
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.flow.*

object BookingListenerManager {
    private var tripId: String? = null


    // Fetch bookings for the provided tripId
    suspend fun fetchBookingsForTrip(tripId: String, viewModel: BookingViewModel, nfcViewModel: NfcViewModel) {
        viewModel.setLoading(true)
        TokenRepository.getToken()
        val isrefreshing = nfcViewModel.isRefreshing.value
        val isloggedin = nfcViewModel.isLoggedIn.value
        try {
            if (!isrefreshing!! && isloggedin) {
                val response = apolloClient.query(GetBookingsQuery(Optional.Present(tripId))).execute()
                val bookingsData = response.data?.getBookings?.data ?: emptyList()
                Log.d("BookingListenerManager", "Query status ${response.data?.getBookings?.message} bookings for trip ID: $tripId")

                val bookings = bookingsData.mapNotNull { booking ->
                    try {
                        booking.bookingDetails
                    } catch (e: Exception) {
                        Log.e("BookingListenerManager", "Error processing booking: ${e.message}")
                        null
                    }
                }
                Log.d(
                    "BookingListenerManager",
                    "Fetched ${bookings.size} bookings for trip ID: $tripId "
                )
                if(bookings.isEmpty()) {
                    Log.d("BookingListenerManager", "No bookings found for trip ID: $tripId")
                    if (response.data?.getBookings?.message == "Unauthorized access") {
                        Log.d("BookingListenerManager", "Unauthorized access for trip ID: $tripId")
//                        TokenRepository.removeToken()
                        TokenRepository.getToken()

                        Log.e("BookingListenerManager", "Error fetching bookings for trip ID: $tripId: ${response.errors}")
                    }
                }
                viewModel.setBookings(bookings)
            } else {
                Log.d("BookingListenerManager", "Already refreshing token ")
            }



        } catch (e: Exception) {
            Log.e("BookingListenerManager", "Error fetching bookings for trip ID: $tripId $isloggedin, $isrefreshing", e)

        } finally {
            viewModel.setLoading(false)
        }
    }


    // Start listening for new bookings
    suspend fun startListeningForBookings(tripId: String, viewModel: BookingViewModel) {
        Log.d("BookingListenerManager", "Start listening for bookings for trip ID: $tripId")
        try {
            apolloClient.subscription(BookingAddedSubscription(tripId))
                .toFlow()
                .catch { e ->
                    Log.e("BookingListenerManager", "Error in subscription for trip ID: $tripId", e)
                    stopListening()
                }
                .collect { response ->
                    Log.d("BookingListenerManager", "New response received with ${response.data?.bookingAdded?.bookingDetails} ")
                    response.data?.bookingAdded?.let { booking ->
                        // Dispatch the action to add a new booking


                        Log.d("BookingListenerManager", "New booking received for trip ID: $tripId")
                        viewModel.addBooking(booking.bookingDetails)
                    } ?: Log.w("BookingListenerManager", "Empty booking data received for trip ID: $tripId")
                }
        } catch (e: Exception) {
            Log.e("BookingListenerManager", "Unexpected error in subscription: $e")
        }
    }

    // Stop listening for bookings
    fun stopListening() {
        tripId = null
        // Clear the current bookings when stopping
    }
}
