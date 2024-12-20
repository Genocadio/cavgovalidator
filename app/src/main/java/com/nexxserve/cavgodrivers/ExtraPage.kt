package com.nexxserve.cavgodrivers

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExtraPage(onGoBack: () -> Unit) {
    val trips = remember { mutableStateOf<List<TripData>>(emptyList()) }
    val noTripsMessage = remember { mutableStateOf("Loading trips...") }

    // Observe updates from the manager using the carId stored in CarIdStorage
    TripListenerManager.startListeningForTrips(
        carId = CarIdStorage.getLinkedCarId() ?: "",
        onUpdate = { newTrips ->
            if (newTrips.isEmpty()) {
                noTripsMessage.value = "No trips planned"
            }
            trips.value = newTrips
        }
    )

    // UI Content
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Extra Page", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        if (trips.value.isEmpty()) {
            Text(text = noTripsMessage.value)
        } else {
            val rtrips = processTripsWithStopPoints(trips.value) // Use trips.value here
            rtrips.forEach { trip ->
                TripItem(trip, onBookNow = { tripData, tickets ->
                    Log.d("ExtraPage", "Booking trip: ${tripData.id} with $tickets tickets")
                })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onGoBack, modifier = Modifier.fillMaxWidth()) {
            Text("Go Back")
        }
    }
}





fun processTripsWithStopPoints(trips: List<TripData>): List<TripData> {
    return trips.flatMap { trip ->
        // Extract stop points from the trip or use an empty list
        val stopPoints = trip.stopPoints

        // Map stop points to new TripData objects
        Log.d("ExtraPage", "Trips: $trips")
        val stopPointTrips = stopPoints.map { stopPoint ->
            Log.d("ExtraPage", "StopPoint to map: $stopPoint")
            TripData(
                id = trip.id,
                route = Route(
                    id = trip.route.id,
                    googleMapsRouteId = trip.route.googleMapsRouteId,
                    price = stopPoint.price, // Use stop point price
                    origin = trip.route.origin, // Original origin
                    destination = Destination(
                        type = trip.route.destination.type, // Maintain original destination type
                        address = stopPoint.location.address,
                        coordinates = stopPoint.location.coordinates,
                        createdAt = stopPoint.location.createdAt,
                        googlePlaceId = stopPoint.location.googlePlaceId,
                        id = stopPoint.location.id,
                        name = stopPoint.location.name
                    ),
                    stopPoints = true // Mark as a stop-point trip
                ),
                car = trip.car,
                availableSeats = trip.availableSeats,
                status = trip.status,
                stopPoints = emptyList(), // Clear stop points for stop-point trips
                boardingTime = trip.boardingTime,
                reverseRoute = trip.reverseRoute,
                createdAt = trip.createdAt,
                user = trip.user
            )
        }

        // Combine the original trip and the stop-point trips
        Log.d("ExtraPage", "StopPointTrips: $stopPointTrips")
        listOf(trip) + stopPointTrips
    }
}


// Composable function to display each trip
@Composable
fun TripItem(
    trip: TripData,
    onBookNow: (TripData, Int) -> Unit // Updated callback to accept number of tickets
) {
    var showDialog by remember { mutableStateOf(false) } // To control dialog visibility
    var ticketCount by remember { mutableStateOf("1") } // To store user input for number of tickets

    val boardingTimeMillis = trip.boardingTime.toLong() // assuming `boardingTime` is a timestamp (in milliseconds)
    val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault()) // Format as per your requirement
    val formattedDate = dateFormat.format(Date(boardingTimeMillis))

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = "Available Seats: ${trip.availableSeats}")
        Text(text = "Status: ${trip.status}")
        Text(text = "Boarding Time: $formattedDate")
        Text(text = "Route: ${trip.route.origin.name} to ${trip.route.destination.name}")

        Spacer(modifier = Modifier.height(16.dp))

        // Book Now Button
        Button(
            onClick = { showDialog = true }, // Show dialog when clicked
            enabled = trip.availableSeats > 0, // Disable button if no available seats
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Book Now")
        }

        // Dialog for entering ticket count
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Enter Number of Tickets") },
                text = {
                    Column {
                        Text("Available seats: ${trip.availableSeats}")
                        OutlinedTextField(
                            value = ticketCount,
                            onValueChange = { ticketCount = it },
                            label = { Text("Number of Tickets") },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Log the number of tickets entered
                            val tickets = ticketCount.toIntOrNull() ?: 1
                            Log.d("TripItem", "Number of tickets: $tickets")
                            onBookNow(trip, tickets) // Pass number of tickets to the booking function
                            showDialog = false // Close dialog
                        }
                    ) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDialog = false } // Close dialog if user clicks No
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

