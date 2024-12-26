import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NfcViewModel : ViewModel() {
    private val _nfcId = mutableStateOf<String?>(null)
    val nfcId: State<String?> get() = _nfcId

    private val _message = mutableStateOf<String?>(null)
    val message: State<String?> get() = _message

    fun setNfcId(id: String) {
        _nfcId.value = id
    }

    fun clearNfcId() {
        _nfcId.value = ""
    }

    fun setMessage(message: String) {
        _message.value = message
    }
    fun clearMessage() {
        // Launch a coroutine to wait for 3 seconds before clearing the message
        viewModelScope.launch {
            delay(3000)  // Wait for 3 seconds
            _message.value = "Tap Your Card or Scan QR Code"  // Clear the message
        }
    }
}
