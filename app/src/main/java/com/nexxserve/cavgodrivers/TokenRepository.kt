package com.nexxserve.cavgodrivers

import NfcViewModel
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object TokenRepository {
    private const val KEY_TOKEN = "TOKEN"
    private const val REF_TOKEN = "REF_TOKEN"
    private const val KEY_TOKEN_TIMESTAMP = "TOKEN_TIMESTAMP"

    private lateinit var preferences: SharedPreferences
    private lateinit var nfcViewModel: NfcViewModel

    fun init(context: Context, nfcViewModel: NfcViewModel) {

        this.nfcViewModel = nfcViewModel

        val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        preferences = EncryptedSharedPreferences.create(
            context,
            "secret_shared_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Automatically checks and refreshes token if expired
    fun getToken(): String? {
        val reftoken = getRefresh()
        val token = preferences.getString(KEY_TOKEN, null)
        val netStatus = nfcViewModel.networkAvailable.value

        // Check network availability
        if (!netStatus) {
            Log.d("Auth", "Network is not available")
            return token
        }

        // Case 1: Both tokens are null
        if (token == null && reftoken == null) {
            Log.d("Auth", "Both token and refresh token are null")
            resetRepo()
            nfcViewModel.setLoggedIn(false)
            nfcViewModel.setIsRefreshing(false)
            return null
        }

        // Case 2: Token is null, but refresh token exists
        if (token == null && reftoken != null) {
            Log.d("Auth", "Token is null, refresh token exists")
            handleTokenRefresh()
            return null // Return null while the refresh process is in progress
        }

        // Case 3: Token exists but needs refresh based on expiration logic
        if (token != null) {
            val tokenTimestamp = getTokenTimestamp()
            val currentTime = System.currentTimeMillis()
            val tokenAge = currentTime - tokenTimestamp

            if (tokenAge > 60 * 1000 * 2) { // Token older than 50 minutes
                Log.d("Auth", "Token is expired, initiating refresh. Token age: $tokenAge ms")
                handleTokenRefresh()
                return null // Return null while refresh process is in progress
            } else {
                Log.d("Auth", "Token is valid. Age: ${tokenAge / 60000} minutes")
                return token
            }
        }

        // Case 4: Refresh token is null (unexpected edge case)
        Log.d("Auth", "Refresh token is null")
        handleMissingRefreshToken()

        Log.d("Auth", "Returning token")
        return preferences.getString(KEY_TOKEN, null) // Return existing or null token
    }

    /**
     * Handles token refresh logic ensuring only one refresh attempt at a time.
     */
    private fun handleTokenRefresh() {
        if (!nfcViewModel.isRefreshing.value!!) {
            nfcViewModel.setIsRefreshing(true)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val msg = refreshToken(nfcViewModel)
                    Log.d("AuthRet", "Token refresh message: $msg")
                } catch (e: Exception) {
                    Log.e("Auth", "Token refresh failed: ${e.message}")
                } finally {
                    nfcViewModel.setIsRefreshing(false) // Ensure resetting the state
                }
            }
        } else {
            Log.d("Auth", "Token refresh already in progress")
        }
    }

    /**
     * Handles cases where both token and refresh token are null.
     */
    private fun handleMissingRefreshToken() {
        val id = CarIdStorage.getId()
        if (id != null) {
            Log.d("Auth", "User ID is valid, setting logged in")
            CarIdStorage.removeSerial()
            CarIdStorage.removeLinkedCarId()
            nfcViewModel.setLoggedIn(true)
        } else {
            Log.d("Auth", "User ID is null, resetting repository and logging out")
            CarIdStorage.removeSerial()
            CarIdStorage.removeLinkedCarId()
            resetRepo()
        }
    }



    private fun getTokenTimestamp(): Long = preferences.getLong(KEY_TOKEN_TIMESTAMP, 0L)

    fun setToken(token: String) {
        nfcViewModel.setLoggedIn(true)
        preferences.edit().apply {
            putString(KEY_TOKEN, token)
            putLong(KEY_TOKEN_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
    }

    fun setRefresh(token: String) {
        Log.d("Auth", "Setting refresh token")
        preferences.edit().apply {
            putString(REF_TOKEN, token)
            apply()
        }
    }

    private fun removeRefresh() {
        preferences.edit().apply {
            remove(REF_TOKEN)
            apply()
        }
    }

    fun resetRepo() {
        preferences.edit().clear().apply()
    }

    fun getRefresh(): String? = preferences.getString(REF_TOKEN, null)

    fun removeToken() {
        nfcViewModel.setLoggedIn(false)
        preferences.edit().apply {
            remove(KEY_TOKEN)
            remove(KEY_TOKEN_TIMESTAMP)
            apply()
        }
    }

    // Refreshes the token
    suspend fun refreshToken(nfcViewModel: NfcViewModel): String {
        val refreshToken = getRefresh() ?: return "No refresh token available"
        Log.w("Auth query", "Refreshing token called")

        return withContext(Dispatchers.IO) { // Ensures it runs on a background thread
            try {
                // Execute the Apollo mutation
                val response = apolloClient.mutation(RegeneratePosTokenMutation(refreshToken)).execute()


                // Handle errors from the response
                if (response.hasErrors()) {
                    val errorMessage = response.errors?.firstOrNull()?.message ?: "Unknown error"
                    Log.d("Auth query", "Failed to refresh token: $errorMessage")
                    return@withContext "Failed to refresh token: $errorMessage"
                }

                // Check success and retrieve the new tokens
                val regenerateData = response.data?.regeneratePosToken
                if (regenerateData?.success != true) {
                    Log.d("Auth query", "Failed to refresh token: Refresh token invalid ${response.data?.regeneratePosToken?.message}")
                    nfcViewModel.setIsRefreshing(false)
                    removeToken()
                    removeRefresh()
                    return@withContext "Failed to refresh token: Refresh token invalid"
                }

                if (regenerateData?.success == true) {
                    regenerateData.token?.let { setToken(it) }
                    regenerateData.refreshToken?.let { setRefresh(it) }
                    nfcViewModel.clearNfcId()
                    if(getToken() != null && getRefresh() != null) {
                        CarIdStorage.removeId()
                        nfcViewModel.setIsRefreshing(false)
                        Log.d("Auth query", "Token refreshed")

                    }
                    Log.d("Auth", "Token refreshed")
                    "Token refreshed successfully"
                } else {
                    "Token refresh failed: Invalid response ${response}"
                }
            } catch (e: Exception) {
                // Handle exceptions during the network call
                "Failed to refresh token: ${e.message}"
            }
        }
    }
}
