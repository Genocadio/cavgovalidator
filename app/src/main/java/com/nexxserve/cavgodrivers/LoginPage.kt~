package com.nexxserve.cavgodrivers

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun LoginPage(onLoginSuccess: (String, String) -> Unit) {
    // State to manage TextField values
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // FocusRequester for email and password fields
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    // Scope for launching coroutines
    val scope = rememberCoroutineScope()

    // Reset state whenever this composable is recomposed (when navigating to it)
    LaunchedEffect(key1 = true) {
        email = ""
        password = ""
        errorMessage = null
        emailFocusRequester.requestFocus() // Automatically focus on the email field
    }

    // Column with padding
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Login",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Email field
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .focusRequester(emailFocusRequester) // Attach FocusRequester
        )

        // Password field
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .focusRequester(passwordFocusRequester) // Attach FocusRequester
        )

        // Error message
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Login Button
        Button(
            onClick = {
                isLoading = true
                errorMessage = null
                scope.launch {
                    val ok = login(email, password)
                    if (ok) {
                        isLoading = false
                        onLoginSuccess(email, password)
                    } else {
                        isLoading = false
                        errorMessage = "Login failed. Check your email or password."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading // Disable button while loading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Login")
            }
        }
    }
}


suspend fun login(email: String, password: String): Boolean {
    return try {
        val response =
            apolloClient.mutation(LoginUserMutation(email = email, password = password)).execute()
        when {
            response.exception != null -> {
                Log.w("Login", "Failed to login", response.exception)
                false
            }
            response.hasErrors() -> {
                Log.w("Login", "Failed to login: ${response.errors?.get(0)?.message}")
                false
            }
            response.data?.loginUser?.success == false -> {
                Log.w("Login", "Failed to login: no token returned by the backend")
                false
            }
            else -> {
                TokenRepository.setToken(response.data!!.loginUser!!.data!!.token!!)
                Log.i("Login", "Login successful" + TokenRepository.getToken())
                true
            }
        }
    } catch (e: Exception) {
        Log.e("Login", "Exception during login", e)
        false
    }
}
