package com.nexxserve.cavgodrivers

import android.annotation.SuppressLint
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import android.util.Log

object TripSeatManager {
    @SuppressLint("StaticFieldLeak")
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Reduces the availableSeats for a given trip document by a specified value.
     *
     * @param tripId The ID of the trip document to update.
     * @param seatsToReduce The number of seats to deduct.
     * @param onComplete Callback to indicate success or failure.
     */
    fun reduceAvailableSeats(tripId: String, seatsToReduce: Int, onComplete: (Boolean, String?) -> Unit) {
        val tripRef = firestore.collection("trips").document(tripId)

        // Perform an atomic decrement
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(tripRef)
            val currentSeats = snapshot.getLong("availableSeats")?.toInt() ?: 0

            if (currentSeats < seatsToReduce) {
                throw IllegalArgumentException("Not enough available seats")
            }

            // Deduct seats
            transaction.update(tripRef, "availableSeats", FieldValue.increment(-seatsToReduce.toLong()))
        }.addOnSuccessListener {
            Log.d("TripSeatManager", "Successfully reduced seats by $seatsToReduce for trip $tripId")
            onComplete(true, null)
        }.addOnFailureListener { exception ->
            Log.e("TripSeatManager", "Failed to reduce seats for trip $tripId", exception)
            onComplete(false, exception.message)
        }
    }
}
