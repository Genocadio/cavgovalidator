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
import androidx.compose.ui.text.input.PasswordVisualTransformation


@Composable
fun HelloWorldPage(
    username: String,
    onLogout: () -> Unit,
    onGoToExtra: () -> Unit,
    registerPosMachine: suspend (serialNumber: String, carPlate: String, password: String) -> String, // Now includes password
    updateCarPlate: suspend (serialNumber: String, plateNumber: String) -> String // Return message instead of throwing errors
) {
    var serialNumber by remember { mutableStateOf<String?>(CarIdStorage.getSerial()) }
    var carPlate by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") } // New state for password
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
            // Serial Number input field
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

            // Car Plate input field
            TextField(
                value = carPlate,
                onValueChange = { carPlate = it },
                label = { Text("Car Plate") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            // Password input field
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(), // Mask password input
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    if (!serialNumber.isNullOrEmpty() && carPlate.isNotEmpty() && password.isNotEmpty()) {
                        isLoading = true
                        CoroutineScope(Dispatchers.IO).launch {
                            message = registerPosMachine(serialNumber!!, carPlate, password) // Pass the password to the function
                            isLoading = false
                            if (message.contains("Successfully")) {
                                showRegistration = false
                                isSerialEditable = false
                            } else {
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
            // Update Car Plate logic remains the same
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
// Updated Register POS machine - now includes password and returns token
suspend fun registerPosMachine(serialNumber: String, carPlate: String, password: String): String {
    Log.w("RegisterPosMachine", "Registering POS machine with serial number: $serialNumber, car plate: $carPlate, and password: $password")

    // Update mutation to include the password
    val response = apolloClient.mutation(RegisterPosMachineMutation(
        serialNumber = serialNumber,
        carPlate = carPlate,
        password = password
    )).execute()

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

            // Save linked car ID if available
            val linkedCarId = response.data?.registerPosMachine?.data?.linkedCar?.id
            if (linkedCarId != null) {
                CarIdStorage.saveLinkedCarId(linkedCarId)
            }

            // Save token for future use
            val token = response.data?.registerPosMachine?.token
            val reftoken = response.data?.registerPosMachine?.refreshToken
            if (token != null) {
                TokenRepository.setToken(token)
                if(reftoken != null) {
                    TokenRepository.setRefresh(reftoken)
                }
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

