package com.nexxserve.cavgodrivers

import android.graphics.Paint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.launch

@Composable
fun ExtraPage(
    nfcId: String,
    navController: NavController,



    onGoBack: () -> Unit,
    bookingViewModel: BookingViewModel,
    tripViewModel: TripViewModel
) {

    var cardData by remember { mutableStateOf<GetCardQuery. Data1?>(null) }
    val bookings by bookingViewModel.bookings.observeAsState(emptyList())
    val trips by tripViewModel.trips.observeAsState(emptyList())
    var message by remember { mutableStateOf("") }

    LaunchedEffect(bookings) {
        if (bookings.isEmpty()) {
            Log.d("ExtraPage", "No bookings found")
            navController.navigate("message") {
                popUpTo("extra") { inclusive = true }
            }
        } else {
            Log.d("ExtraPage", "Bookings found: ${bookings.size}")
        }
    }

    LaunchedEffect(nfcId) {
        if (nfcId.isEmpty()) {
            navController.navigate("message") {
                popUpTo("extra") { inclusive = true }
            }
        } else {
            // Directly call the suspend function within the LaunchedEffect scope
            val card = getCard(nfcId)

            // Access the data after the card is retrieved
            message = card?.message ?: ""
            val userDat = card?.data
            if (userDat != null) {
                Log.d("ExtraPage", "Card retrieved: ${userDat.wallet?.balance}")
                cardData = userDat
            } else {
                if (message.isNotEmpty() && message == "Card not found") {
                    Log.w("ExtraPage", "Card not found")
                    navController.navigate("message") {
                        popUpTo("extra") { inclusive = true }
                    }

                }
                Log.w("ExtraPage", "Failed to retrieve card data")
            }
        }
    }

    Log.d("ExtraPage", "NFC ID: $nfcId")

//    val trips = remember { mutableStateOf<List<TripData>>(emptyList()) }

    val noTripsMessage = remember { mutableStateOf("Loading trips...") }

    // Observe updates from the manager using the carId stored in CarIdStorage
    if(trips.isEmpty()) {
        TripListenerManager.startListeningForTrips(
            carId = CarIdStorage.getLinkedCarId() ?: "",
            onUpdate = { newTrips ->
                if (newTrips.isEmpty()) {
                    noTripsMessage.value = "No trips planned"
                }
                Log.d("Extra P", "trips n${newTrips.size}")
                tripViewModel.setTrips(newTrips)
            },
        )
    }



    // UI Content
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Spacer(modifier = Modifier.height(16.dp))

        cardData?.let { card ->
            CardDataDisplay(cardData = card)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (trips.isEmpty()) {
            Text(text = noTripsMessage.value)
        } else {
            val boardingTimeMillis = trips[0].boardingTime.toLong() // assuming `boardingTime` is a timestamp (in milliseconds)
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault()) // Format as per your requirement
            val formattedDate = dateFormat.format(Date(boardingTimeMillis))
            Text(text = "Seats: ${trips[0].availableSeats} Depart: $formattedDate")

            val rTrips = processTripsWithStopPoints(trips) // Use trips.value here
            val balance = cardData?.wallet?.balance ?: 0.0
            rTrips.forEach { trip ->
                TripItem(trip, nfcId, balance, onBookNow = { tripData, tickets ->
                    Log.d("ExtraPage", "Booking trip: ${tripData.id} with $tickets tickets  on Card $nfcId" )
                })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onGoBack, modifier = Modifier.fillMaxWidth()) {
            Text("Go Back")
        }
    }
}

@Composable
fun CardDataDisplay(cardData: GetCardQuery.Data1) {
    // Example UI for displaying card data (modify according to the fields in your data model)
    Column {
        Text("\uD83D\uDCB3: ${cardData.wallet?.balance?.toInt().toString().plus("Rwf") ?: "Not available"}, 👤${cardData.user?.firstName}" , style = MaterialTheme.typography.titleMedium, fontSize = 20.sp)
        // Add more fields to display here if necessary
    }
}


fun processTripsWithStopPoints(trips: List<TripData>): List<TripData> {
    return trips.flatMap { trip ->
        // Extract stop points from the trip or use an empty list
        val stopPoints = trip.stopPoints

        // Map stop points to new TripData objects
//        Log.d("ExtraPage", "Trips: $trips")
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
    nfcId: String,
    availableBalance: Double = 0.0,
    onBookNow: (TripData, Int) -> Unit // Updated callback to accept number of tickets
) {
    var showDialog by remember { mutableStateOf(false) } // To control dialog visibility
    var ticketCount by remember { mutableStateOf("1") } // To store user input for number of tickets
    var info by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val destination = trip.route.destination.name
    val maxTickets = 5
    val priceR = trip.route.price
    var totalPrice: Double = priceR * ticketCount.toInt()!!

    var isBalancSufficient = totalPrice <= availableBalance

    // Clickable Card for trip item
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { showDialog = true }, // Show dialog when clicked
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Route Text
            Text(
                text = "${trip.route.origin.name} to ${trip.route.destination.name}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                fontSize = 25.sp
            )

            Spacer(modifier = Modifier.height(5.dp))


            // If available seats are greater than 0, show "Book Now"
            if (trip.availableSeats > 0) {
                Text(
                    text = "Click to Book Now",
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .clickable {
                            // Open the booking dialog or action
                            showDialog = true
                        },
                    color = MaterialTheme.colorScheme.secondary,

                )
            } else {
                Text(
                    text = "No available seats",
                    modifier = Modifier.padding(top = 16.dp),
                    color = Color.Red
                )
            }
        }
    }

    // Dialog for entering ticket count
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {  Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Tickets Number", style = MaterialTheme.typography.titleLarge, fontSize = 20.sp)
            }},
            text = {
                Column {
                    Text("Seats: ${trip.availableSeats}")
                    Text(info, color = Color.Red, fontSize = 20.sp)
                    OutlinedTextField(
                        value = ticketCount,

                        onValueChange = {newValue ->
                            // Ensure the ticket count does not exceed maxTickets or cause invalid input
                            if (newValue.isNotEmpty() && newValue.toIntOrNull() != null) {
                                ticketCount = newValue
                            } else {
                                ticketCount = "1" // Set back to "1" if invalid input or empty
                            }
                            val newTicketCount = newValue.toIntOrNull()

                            if (newTicketCount != null && newTicketCount <= maxTickets) {
                                if (priceR * newTicketCount <= availableBalance) {
                                    totalPrice = priceR * newTicketCount
                                    ticketCount = newValue
                                }


                            } else if (newValue.isEmpty()) {
                                ticketCount = "1"
                            }
                        },
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
                        if (tickets > maxTickets) {
                            // Show error message if the number of tickets exceeds the limit
                            ticketCount = maxTickets.toString()
                            Log.d("TripItem", "Number of tickets exceeds limit")
                            return@Button
                        }
                        if (totalPrice > availableBalance) {
                            // Show error message if the balance is insufficient
                            Log.d("TripItem", "Insufficient balance")
                            return@Button
                        }
                        if(totalPrice == 0.0 || availableBalance == 0.0) {
                            Log.d("TripItem", "Insufficient balance")
                            info = "Insufficient balance ".plus(availableBalance.toString())
                            return@Button
                        }
                        Log.d("TripItem", "Number of tickets: $tickets availableBalance: $availableBalance totalPrice: $totalPrice")
                        onBookNow(trip, tickets) // Pass number of tickets to the booking function
//                        scope.launch {
//                            Log.d("TripItem", "Booking trip: ${trip.id} with $tickets tickets on Card $nfcId")
//                            val booked = addBooking(trip.id, destination, tickets, trip.route.price, nfcId)
//                            if (booked) {
//                                Log.d("TripItem", "Booking successful")
//                            } else {
//                                Log.d("TripItem", "Booking failed")
//                            }
//                        }
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
            },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            modifier = Modifier.fillMaxWidth()
        )

    }
}

suspend fun addBooking(tripId: String, destination: String, numberOfTickets: Int, price: Double, nfcId: String): Boolean {
    return try{
        val response = apolloClient.mutation(AddBookingMutation(tripId = tripId, destination = destination, numberOfTickets = numberOfTickets, price = price, nfcId = Optional.present(nfcId))).execute()
        when {
            response.exception != null -> {
                Log.w("AddBooking", "Failed to add booking ${response.exception!!.message}", response.exception)
                false
            }
            !response.data?.addBooking?.success!! -> {
                Log.w("AddBooking", "Failed to add booking: ${response.data?.addBooking?.message}")
                false
            }
            response.data?.addBooking != null -> {
                Log.d("AddBooking", "Booking added successfully")
                true
            }
            else -> {
                Log.w("AddBooking", "Failed to add booking")
                false
            }
        }
    } catch (e: Exception) {
        Log.e("AddBooking", "Exception during booking", e)
        false
    }
}

suspend fun getCard(nfcId: String): GetCardQuery. GetCard? {
    return try {
        val response = apolloClient.query(GetCardQuery(nfcId = nfcId)).execute()
        when {
            response.exception != null -> {
                Log.w("GetCard", "Failed to get card ${response.exception!!.message}", response.exception)
                null
            }
            response.data?.getCard != null -> {
                Log.d("GetCard", "Card retrieved successfully ${response.data?.getCard?.data?.wallet?.balance} ${response.data?.getCard?.message}")
                response.data?.getCard
            }
            else -> {
                Log.w("GetCard", "Failed to get card ${response.data?.getCard?.message}")
                null
            }
        }
    } catch (e: Exception) {
        Log.e("GetCard", "Exception during card retrieval", e)
        null
    }
}
