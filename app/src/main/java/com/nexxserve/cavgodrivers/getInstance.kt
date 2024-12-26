package com.nexxserve.cavgodrivers


import android.annotation.SuppressLint
import android.content.Context
import com.common.apiutil.nfc.NfcUtil

class NfcUtil private constructor(private val context: Context) {

    companion object {
        // Volatile ensures that the instance is correctly initialized even in a multi-threaded environment
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: NfcUtil? = null

        // Synchronized ensures only one thread can call this at a time
        @Synchronized
        fun getInstance(context: Context): NfcUtil {
            // Check if an instance already exists
            return INSTANCE ?: run {
                val instance = NfcUtil(context)  // Create a new instance if none exists
                INSTANCE = instance  // Store the instance for future calls
                instance
            }
        }
    }

    // Other methods of NfcUtil
}
