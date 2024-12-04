package com.nexxserve.cavgodrivers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import com.apollographql.apollo.api.Optional

@Composable
fun HelloWorldPage(
    username: String,
    onLogout: () -> Unit,
    onGoToExtra: () -> Unit,
    registerPosMachine: suspend (serialNumber: String, carPlate: String) -> String, // Return message instead of throwing errors
    updateCarPlate: suspend (serialNumber: String, plateNumber: String) -> String // Return message instead of throwing errors
) {
    var serialNumber by remember { mutableStateOf<String?>(CarIdStorage.getSerial()) }
    var carPlate by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showRegistration by remember { mutableStateOf(serialNumber == null) }
    var isSerialEditable by remember { mutableStateOf(serialNumber == null) }
    var message by remember { mutableStateOf("") }



    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Hello, $username",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (showRegistration) {
            TextField(
                value = serialNumber.orEmpty(),
                onValueChange = {
                    if (isSerialEditable) {
                        serialNumber = it
                    }
                },
                label = { Text("Serial Number") },
                enabled = isSerialEditable,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
            TextField(
                value = carPlate,
                onValueChange = { carPlate = it },
                label = { Text("Car Plate") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
            Button(
                onClick = {
                    if (!serialNumber.isNullOrEmpty() && carPlate.isNotEmpty()) {
                        isLoading = true
                        CoroutineScope(Dispatchers.IO).launch {
                            message = registerPosMachine(serialNumber!!, carPlate)
                            isLoading = false
                            if (message.contains("Successfully")) {
                                showRegistration = false
                                isSerialEditable = false
                            }
                            else {
                                serialNumber = null
                            }
                        }
                    } else {
                        message = "Please fill in all fields"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Register POS Machine")
            }
        } else {
            TextField(
                value = carPlate,
                onValueChange = { carPlate = it },
                label = { Text("New Car Plate") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
            Button(
                onClick = {
                    if (carPlate.isNotEmpty()) {
                        isLoading = true
                        CoroutineScope(Dispatchers.IO).launch {
                            message = updateCarPlate(serialNumber!!, carPlate)
                            isLoading = false
                        }
                    } else {
                        message = "Please enter a new car plate"
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text("Update Car Plate")
            }
        }

        Button(
            onClick = { onGoToExtra() },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("Go to Extra Page")
        }

        Button(
            onClick = { onLogout() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }

        if (message.isNotEmpty()) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

// Register POS machine - now returns message instead of throwing errors
suspend fun registerPosMachine(serialNumber: String, carPlate: String): String {
    Log.w("RegisterPosMachine", "Registering POS machine with serial number: $serialNumber and car plate: $carPlate")
    val response = apolloClient.mutation(RegisterPosMachineMutation(serialNumber = serialNumber, carPlate = carPlate)).execute()

    return when {
        response.exception != null -> {
            "Failed to register POS machine: ${response.exception?.message}"
        }
        response.hasErrors() -> {
            "Failed to register POS machine: ${response.errors?.get(0)?.message}"
        }
        response.data?.registerPosMachine?.success == true -> {
            val posId = response.data?.registerPosMachine?.data?.id
            CarIdStorage.saveSerial(posId ?: "")
            val linkedCarId = response.data?.registerPosMachine?.data?.linkedCar?.id
            if (linkedCarId != null) {
                CarIdStorage.saveLinkedCarId(linkedCarId)
            }
            "POS Machine Registered Successfully!"
        }
        response.data?.registerPosMachine?.message != null -> {
            response.data?.registerPosMachine?.message!!
        }
        else -> {
            "POS Machine registration failed"
        }
    }
}

// Update POS machine - now returns message instead of throwing errors
suspend fun updatePosMachine(serialNumber: String?, plateNumber: String?): String {
    if (serialNumber == null && plateNumber == null) {
        return "Both serialNumber and plateNumber cannot be null."
    }

    try {
        val response = apolloClient.mutation(
            UpdatePosMachineMutation(
                serialNumber = Optional.presentIfNotNull(serialNumber),
                plateNumber = Optional.presentIfNotNull(plateNumber)
            )
        ).execute()

        return when {
            response.exception != null -> {
                "Failed to update POS machine: ${response.exception?.message}"
            }
            response.hasErrors() -> {
                "GraphQL errors: ${response.errors?.get(0)?.message}"
            }
            response.data?.updatePosMachine?.success == true -> {
                val linkedCarId = response.data?.updatePosMachine?.data?.linkedCar?.id
                if (linkedCarId != null) {
                    CarIdStorage.saveLinkedCarId(linkedCarId)
                }
                "POS Machine Updated Successfully!"
            }
            else -> {
                "Update failed: ${response.data?.updatePosMachine?.message}"
            }
        }
    } catch (e: Exception) {
        return "Exception occurred: ${e.message}"
    }
}

