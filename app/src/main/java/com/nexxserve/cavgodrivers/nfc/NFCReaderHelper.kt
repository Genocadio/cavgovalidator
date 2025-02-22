package com.nexxserve.cavgodrivers.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle

class NFCReaderHelper(
    private val context: Context,
    private val onTagRead: (Tag) -> Unit,
    private val onError: (String) -> Unit
) {
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    fun isNfcSupported(): Boolean = nfcAdapter != null

    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled ?: false

    fun enableNfcReader(activity: Activity) {
        if (nfcAdapter != null && isNfcEnabled()) {
            nfcAdapter.enableReaderMode(
                activity,
                NfcAdapter.ReaderCallback { tag ->
                    tag?.let { onTagRead(it) } ?: onError("Tag not found.")
                },
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                Bundle()
            )
        } else {
            onError("NFC is not enabled or supported.")
        }
    }

    fun disableNfcReader(activity: Activity) {
        nfcAdapter?.disableReaderMode(activity)
    }
}
