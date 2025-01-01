import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NfcViewModel : ViewModel() {
    private val _nfcId = mutableStateOf<String?>(null)
    val nfcId: State<String?> get() = _nfcId

    private val _message = mutableStateOf("Tap Your Card or Scan QR Code")
    val message: State<String?> get() = _message

    private val _tripId = mutableStateOf<String?>(null)
    val tripId: State<String?> get() = _tripId

    private val _qrcodeData = mutableStateOf<String?>(null)
    val qrcodeData: State<String?> get() = _qrcodeData


    private val messageDelayMillis = 2000L

    fun setNfcId(id: String) {
        if (id.isNotBlank()) _nfcId.value = id
    }

    fun clearNfcId() {
        _nfcId.value = null
    }

    fun setMessage(message: String) {
        _message.value = message
    }

    fun setQrCodeData(data: String) {
        _qrcodeData.value = data
    }

    fun clearQrCodeData() {
        _qrcodeData.value = null
    }

    fun setTripId(id: String) {
        if (id.isNotBlank()) _tripId.value = id
    }

    fun clearTripId() {
        _tripId.value = null
    }

    fun resetMessage() {
        viewModelScope.launch {
            delay(messageDelayMillis)
            _message.value = "Tap Your Card or Scan QR Code"
        }
    }

}
