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
    // Get the linked carId from the storage
    val carId = CarIdStorage.getLinkedCarId()

    // If no carId is available, go back to the login page
    if (carId.isNullOrEmpty()) {
        CarIdStorage.removeTripId()
        onGoBack()
        return
    } else {
        Log.d("ExtraPage", "CarId: $carId")
    }

    // State to hold trips data
    var trips by remember { mutableStateOf<List<TripData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var noTripsMessage by remember { mutableStateOf("Loading trips...") }


    // Listen for trips associated with the carId
    LaunchedEffect(carId) {
        val firestore = FirebaseFirestore.getInstance()
        val tripsCollection = firestore.collection("trips")
        val tripsQuery = tripsCollection.whereEqualTo("car.id", carId).whereEqualTo("status", "Scheduled")

        // Set up the real-time listener
        val listener: ListenerRegistration = tripsQuery.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.e("ExtraPage", "Error getting trips: ", exception)
                isLoading = false
                noTripsMessage = "Error fetching trips"
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                Log.d("ExtraPage", "Trips f: ${snapshot.documents[0]}")
                val trip = snapshot.documents[0]
                trips = snapshot.documents.map { doc ->
                    val routeData = doc.get("route") as? Map<String, Any> ?: emptyMap()
                    Log.d("ExtraPage", "Route data: $routeData")
                    val carData = doc.get("car") as? Map<String, Any> ?: emptyMap()
                    val stopPointsData = doc.get("stopPoints") as? List<Map<String, Any>> ?: emptyList()
                    Log.d("ExtraPage", "StopPoints data: $stopPointsData")
                    CarIdStorage.saveTripId(doc.id)
                    TripData(
                        id = doc.id,

                        route = Route(
                            id = routeData["id"] as? String ?: "",
                            googleMapsRouteId = routeData["googleMapsRouteId"] as? String ?: "",
                            price = routeData["price"] as? Double ?: 0.0,
                            origin = Origin(
                                type = (routeData["origin"] as? Map<*, *>)?.get("type") as? String ?: "",
                                address = (routeData["origin"] as? Map<*, *>)?.get("address") as? String ?: "",
                                coordinates = Coordinates(
                                    lat = (routeData["origin"] as? Map<*, *>)?.let { origin ->
                                        (origin["coordinates"] as? Map<*, *>)?.get("lat") as? Double ?: 0.0
                                    } ?: 0.0,
                                    lng = (routeData["origin"] as? Map<*, *>)?.let { origin ->
                                        (origin["coordinates"] as? Map<*, *>)?.get("lng") as? Double ?: 0.0
                                    } ?: 0.0
                                ),
                                createdAt = (routeData["origin"] as? Map<*, *>)?.get("createdAt") as? String ?: "",
                                googlePlaceId = (routeData["origin"] as? Map<*, *>)?.get("googlePlaceId") as? String ?: "",
                                id = (routeData["origin"] as? Map<*, *>)?.get("id") as? String ?: "",
                                name = (routeData["origin"] as? Map<*, *>)?.get("name") as? String ?: ""
                            ),
                            destination = Destination(
                                type = (routeData["destination"] as? Map<*, *>)?.get("type") as? String ?: "",
                                address = (routeData["destination"] as? Map<*, *>)?.get("address") as? String ?: "",
                                coordinates = Coordinates(
                                    lat = (routeData["destination"] as? Map<*, *>)?.let { destination ->
                                        (destination["coordinates"] as? Map<*, *>)?.get("lat") as? Double ?: 0.0
                                    } ?: 0.0,
                                    lng = (routeData["destination"] as? Map<*, *>)?.let { destination ->
                                        (destination["coordinates"] as? Map<*, *>)?.get("lng") as? Double ?: 0.0
                                    } ?: 0.0
                                ),
                                createdAt = (routeData["destination"] as? Map<*, *>)?.get("createdAt") as? String ?: "",
                                googlePlaceId = (routeData["destination"] as? Map<*, *>)?.get("googlePlaceId") as? String ?: "",
                                id = (routeData["destination"] as? Map<*, *>)?.get("id") as? String ?: "",
                                name = (routeData["destination"] as? Map<*, *>)?.get("name") as? String ?: ""
                            )
                        ),
                        car = Car(
                            id = carData["id"] as? String ?: "",
                            plateNumber = carData["plateNumber"] as? String ?: "",
                            ownerCompany = OwnerCompany(
                                name = carData["ownerCompany.name"] as? String ?: ""
                            ),
                            driver = Driver(
                                name = carData["driver.name"] as? String ?: ""
                            )
                        ),
                        availableSeats = doc.getLong("availableSeats")?.toInt() ?: 0,
                        status = doc.get("status") as? String ?: "",
                        stopPointsData.map { stop ->
                            val location = stop["location"] as? Map<String, Any> ?: emptyMap()
                            val address = location["address"] as? String ?: ""
                            val coordinates = location["coordinates"] as? Map<String, Any> ?: emptyMap()
                            val lat = coordinates["lat"] as? Double ?: 0.0
                            val lng = coordinates["lng"] as? Double ?: 0.0
                            StopPoint(
                                price = stop["price"] as? Double ?: 0.0,
                                location = Location(
                                    address = address,
                                    coordinates = Coordinates(lat = lat, lng = lng),
                                    createdAt = location["createdAt"] as? String ?: "",
                                    googlePlaceId = location["googlePlaceId"] as? String ?: "",
                                    id = location["id"] as? String ?: "",
                                    name = location["name"] as? String ?: ""
                                )
                            )
                        }
                        ,
                        boardingTime = doc.get("boardingTime") as? String ?: "",
                        reverseRoute = doc.get("reverseRoute") as? Boolean ?: false,
                        createdAt = doc.get("createdAt") as? String,
                        user = User(
                            id = doc.get("user.id") as? String ?: "",
                            name = doc.get("user.name") as? String ?: ""
                        )
                    )
                }


                isLoading = false
            } else {
                trips = emptyList()
                isLoading = false
                noTripsMessage = "No trips planned"
            }
        }
    }

    // UI Content
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Extra Page",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Show loading message if still fetching trips
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            // Display the trips if they are available
            if (trips.isEmpty()) {
                Text(text = noTripsMessage) // Show the appropriate message when there are no trips
            } else {
                // Display the trips if available

                val rtrips = processTripsWithStopPoints(trips)

                rtrips.forEach { trip ->
                    TripItem(trip, onBookNow = { trip, tickets -> // Pass both trip and tickets
                        Log.d("ExtraPage", "Booking trip: $trip with $tickets tickets")
                    })
                }

            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Go back button
        Button(
            onClick = onGoBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Go Back")
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

