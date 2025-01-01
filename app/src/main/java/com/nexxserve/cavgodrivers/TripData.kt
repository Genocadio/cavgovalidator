package com.nexxserve.cavgodrivers

// Data classes for each part of the TripData structure
data class Coordinates(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class Location(
    val address: String = "",
    val coordinates: Coordinates = Coordinates(),
    val createdAt: String = "",
    val googlePlaceId: String = "",
    val id: String = "",
    val name: String = ""
)

data class Origin(
    val type: String = "",
    val address: String = "",
    val coordinates: Coordinates = Coordinates(),
    val createdAt: String = "",
    val googlePlaceId: String = "",
    val id: String = "",
    val name: String = ""
)

data class Destination(
    val type: String = "",
    val address: String = "",
    val coordinates: Coordinates = Coordinates(),
    val createdAt: String = "",
    val googlePlaceId: String = "",
    val id: String = "",
    val name: String = ""
)

data class Route(
    val stopPoints: Boolean = false,
    val id: String = "",
    val googleMapsRouteId: String = "",
    val price: Double = 0.0,
    val origin: Origin = Origin(),
    val destination: Destination = Destination()
)

data class OwnerCompany(
    val name: String? = null
)

data class Driver(
    val name: String? = null
)

data class Car(
    val id: String = "",
    val plateNumber: String = "",
    val ownerCompany: OwnerCompany = OwnerCompany(),
    val driver: Driver? = null
)

data class StopPoint(
    val price: Double = 0.0,
    val location: Location = Location()
)

data class TripData(
    val id: String = "",
    val route: Route = Route(),
    val car: Car = Car(),
    val availableSeats: Int = 0,
    val status: String = "",
    val stopPoints: List<StopPoint> = emptyList(),
    val boardingTime: String = "",
    val reverseRoute: Boolean = false,
    val createdAt: String? = null,
    val user: User? = null
)

data class User(
    val id: String = "",
    val name: String = ""
)
data class Wallet(
    val balance: Double = 0.0
)
data class CardUser(
    val firstName: String = "",
    val lastName: String = ""
)
data class CardData(
    val wallet: Wallet? = null,
    val user: CardUser? = null
)
