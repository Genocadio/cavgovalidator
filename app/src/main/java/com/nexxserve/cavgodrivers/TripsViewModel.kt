package com.nexxserve.cavgodrivers

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.State
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class TripViewModel : ViewModel() {
    private val _trips = MutableLiveData<List<TripData>>(emptyList())
    val trips: LiveData<List<TripData>> = _trips

    private val _noTripsMessage = mutableStateOf("Loading trips...")
    val noTripsMessage: State<String> get() = _noTripsMessage

    fun setTrips(newTrips: List<TripData>) {
        _trips.value = newTrips
    }
    fun clearTrips() {
        _trips.value = emptyList()
    }



}
