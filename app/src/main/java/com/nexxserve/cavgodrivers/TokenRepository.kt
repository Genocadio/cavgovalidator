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
        if (token == null) {
            val  refreshing = nfcViewModel.isRefreshing.value
            if (reftoken != null && !refreshing) {
                Log.d("Auth", "Token is null, refreshing")
                nfcViewModel.setIsRefreshing(true)
                CoroutineScope(Dispatchers.IO).launch{
                    refreshToken(nfcViewModel)
                }
            } else {
                Log.d("Auth", "Token is null, but refresh token is null, refreshing = $refreshing" )
            }
        } else {
            val timestamp = getTokenTimestamp()
            if (System.currentTimeMillis() - timestamp > 3000000) {
                val  refreshing = nfcViewModel.isRefreshing.value
                if (reftoken != null && !refreshing) {
                    Log.d("Auth", "Token expired, refreshing")
                    CoroutineScope(Dispatchers.IO).launch{
                        nfcViewModel.setIsRefreshing(true)
                        refreshToken(nfcViewModel)
                    }
                } else {
                    Log.d("Auth", "Token expired, but refresh token is null, refreshing = $refreshing")
                }

            } else {
                Log.d("Auth", "Token is not null, not expired user loged in $reftoken")
            }
        }
        return preferences.getString(KEY_TOKEN, null) // Return existing or null token
    }


    private fun getTokenTimestamp(): Long = preferences.getLong(KEY_TOKEN_TIMESTAMP, 0L)

    fun setToken(token: String) {
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

        return withContext(Dispatchers.IO) { // Ensures it runs on a background thread
            try {
                // Execute the Apollo mutation
                val response = apolloClient.mutation(RegeneratePosTokenMutation(refreshToken)).execute()


                // Handle errors from the response
                if (response.hasErrors()) {
                    val errorMessage = response.errors?.firstOrNull()?.message ?: "Unknown error"
                    return@withContext "Failed to refresh token: $errorMessage"
                }

                // Check success and retrieve the new tokens
                val regenerateData = response.data?.regeneratePosToken
                if (regenerateData?.success != true) {
                    nfcViewModel.setIsRefreshing(false)
                    removeToken()
                    removeRefresh()
                    return@withContext "Failed to refresh token: Refresh token invalid"
                }

                if (regenerateData?.success == true) {
                    regenerateData.token?.let { setToken(it) }
                    regenerateData.refreshToken?.let { setRefresh(it) }
                    nfcViewModel.clearNfcId()
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
