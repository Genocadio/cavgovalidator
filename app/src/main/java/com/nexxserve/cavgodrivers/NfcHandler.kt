package com.nexxserve.cavgodrivers

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import android.app.PendingIntent
import android.content.IntentFilter
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

class NfcHandler(private val activity: Activity) {

    private var nfcAdapter: NfcAdapter? = null
    private val _tagData = mutableStateOf("")
    val tagData: State<String> get() = _tagData

    init {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        if (nfcAdapter == null) {
            Log.e("NFC", "NFC is not supported on this device.")
        } else if (!nfcAdapter!!.isEnabled) {
            Log.e("NFC", "NFC is disabled. Please enable it.")
        } else {
            Log.d("NFC", "NFC is supported and enabled.")
        }

        if (nfcAdapter == null) {
            // NFC is not available on this device
            Toast.makeText(activity, "NFC is not supported on this device.", Toast.LENGTH_LONG).show()
        }
    }

    // Enabling NFC foreground dispatch
    fun enableForegroundDispatch() {
        val pendingIntent = PendingIntent.getActivity(
            activity,
            0,
            Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE // Add this flag
        )
        val filters = arrayOfNulls<IntentFilter>(1)
        filters[0] = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        val techLists = arrayOf(arrayOf<String>(Tag::class.java.name))

        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, filters, techLists)
        Log.d("NFC", "Foreground dispatch enabled")
    }


    // Disabling NFC foreground dispatch
    fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    // Handling new NFC tag
    fun handleNewIntent(intent: Intent) {
        Log.d("NFC", "Received Intent action: ${intent.action}")  // Log the action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            Log.d("NFC", "Tag discovered!")
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let {
                val tagInfo = "Tag ID: ${it.id.joinToString()}"
                _tagData.value = tagInfo
                Log.d("NFC", "Tag ID: ${it.id.joinToString()}")
            }
        }
    }


}
