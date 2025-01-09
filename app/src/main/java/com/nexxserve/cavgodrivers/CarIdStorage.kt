package com.nexxserve.cavgodrivers

import NfcViewModel
import android.content.Context
import androidx.compose.runtime.rememberCoroutineScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object CarIdStorage {

    private const val PREFS_NAME = "secure_prefs"
    private const val LINKED_CAR_ID_KEY = "linkedCarId"
    private const val SERIAL= "serial"

    private lateinit var sharedPreferences: EncryptedSharedPreferences
    private var nfcViewModel: NfcViewModel? = null
    private var bookingViewModel: BookingViewModel? = null


    /**
     * Initialize SharedPreferences (must be called once in the application lifecycle).
     */
    fun init(context: Context) {
        // Create or retrieve the MasterKey for encryption
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    /**
     * Save the linked car ID.
     */

    fun setNfcViewModel(viewModel: NfcViewModel) {
        nfcViewModel = viewModel
    }


    fun saveLinkedCarId(linkedCarId: String) {
        sharedPreferences.edit().putString(LINKED_CAR_ID_KEY, linkedCarId).apply()
    }

    fun saveTripId(tripId: String) {
        nfcViewModel?.setTripId(tripId)
        sharedPreferences.edit().putString("tripId", tripId).apply()
    }

    fun getTripId(): String? {
        return sharedPreferences.getString("tripId", null)
    }

    fun saveId(nfcId: String) {
        sharedPreferences.edit().putString("nfcId", nfcId).apply()
    }


    fun getId(): String? {
        return sharedPreferences.getString("nfcId", null)
    }
    fun removeId() {
        sharedPreferences.edit().remove("nfcId").apply()
    }

    fun removeTripId() {
        nfcViewModel?.clearTripId()
        sharedPreferences.edit().remove("tripId").apply()
    }

    /**
     * Retrieve the linked car ID.
     */
    fun getLinkedCarId(): String? {
        return sharedPreferences.getString(LINKED_CAR_ID_KEY, null)
    }

    /**
     * Remove the linked car ID.
     */
    fun removeLinkedCarId() {
        sharedPreferences.edit().remove(LINKED_CAR_ID_KEY).apply()
    }
    fun saveSerial(serial: String){
        sharedPreferences.edit().putString(SERIAL, serial).apply()
    }
    fun getSerial(): String? {
        return sharedPreferences.getString(SERIAL, null)
    }
    fun removeSerial() {
        sharedPreferences.edit().remove(SERIAL).apply()
    }
}
