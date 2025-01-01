package com.nexxserve.cavgodrivers

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration


object TripListenerManager {
    private var listener: ListenerRegistration? = null
    @SuppressLint("StaticFieldLeak")
    private val firestore = FirebaseFirestore.getInstance()
    private var notificationShown = false
    // Callback for trip updates
    private var onTripsUpdated: ((List<TripData>) -> Unit)? = null
    @SuppressLint("StaticFieldLeak")
    private lateinit var notificationHelper: NotificationHelper

    fun initialize(context: Context) {
        notificationHelper = NotificationHelper.getInstance(context)
        notificationHelper.createNotificationChannel()
    }
    // Initialize listener with carId and trip status
    fun startListeningForTrips(carId: String, status: String = "Scheduled", onUpdate: (List<TripData>) -> Unit) {

        stopListening() // Prevent multiple listeners
        onTripsUpdated = onUpdate

        val tripsQuery = firestore.collection("trips")
            .whereEqualTo("car.id", carId)
            .whereEqualTo("status", status)

        listener = tripsQuery.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.e("TripListenerManager", "Error fetching trips", exception)
                onTripsUpdated?.invoke(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                if (!notificationShown) {
                    notificationHelper.showNotification(
                        "Trips Updated",
                        "Your trips have been updated.",
                        MainActivity::class.java
                    )
                    notificationShown = true // Set the flag to true so it doesn't trigger again
                }
                val trips = snapshot.documents.map { doc ->
                    val routeData = doc.get("route") as? Map<String, Any> ?: emptyMap()
                    Log.d("ExtraPage", "Route data: $routeData")
                    val carData = doc.get("car") as? Map<String, Any> ?: emptyMap()
                    val stopPointsData = doc.get("stopPoints") as? List<Map<String, Any>> ?: emptyList()
                    Log.d("ExtraPage", "StopPoints data: $stopPointsData")
                    CarIdStorage.saveTripId(doc.id)
//                    val tripId = doc.id
//                    CoroutineScope(Dispatchers.IO).launch {
//                        BookingListenerManager.fetchBookingsForTrip(tripId)
//                        BookingListenerManager.startListeningForBookings(tripId)
//                    }

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
                        stopPoints = stopPointsData.map { stop ->
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
                        },
                        boardingTime = doc.get("boardingTime") as? String ?: "",
                        reverseRoute = doc.get("reverseRoute") as? Boolean ?: false,
                        createdAt = doc.get("createdAt") as? String,
                        user = User(
                            id = doc.get("user.id") as? String ?: "",
                            name = doc.get("user.name") as? String ?: ""
                        )
                    )
                }
                Log.d("TripListenerManager", "Fetched trips: $trips")
                onTripsUpdated?.invoke(trips)
            } else {
                Log.d("TripListenerManager", "No trips found")
                onTripsUpdated?.invoke(emptyList())
                CarIdStorage.removeTripId()
            }
        }
    }
    // Stop the listener when no longer needed
    fun stopListening() {
        listener?.remove()
        listener = null
    }
}
