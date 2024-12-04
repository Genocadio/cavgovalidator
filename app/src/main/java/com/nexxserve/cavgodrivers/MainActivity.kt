package com.nexxserve.cavgodrivers

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.common.apiutil.util.SDKUtil
import com.google.firebase.FirebaseApp
import com.nexxserve.cavgodrivers.ui.theme.CavgodriversTheme


class MainActivity : ComponentActivity() {


    private var tagData by mutableStateOf("")

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenRepository.init(this)
        CarIdStorage.init(this)
        FirebaseApp.initializeApp(this)
        SDKUtil.getInstance(this).initSDK()




        val serviceIntent = Intent(this, TrackingService::class.java)
        startForegroundService(serviceIntent)



        setContent {
            CavgodriversTheme {
                // Define the NavController to manage navigation between pages
                val navController = rememberNavController()

                // Manage login state
                var loggedIn by remember { mutableStateOf(false) }
                var username by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                val token = TokenRepository.getToken()
                if (token != null) {
                    loggedIn = true
                    username = "yves"
                }



                // Create NavHost to navigate between pages
                NavHost(
                    navController,
                    startDestination = if (!loggedIn) "login" else "extra"
                ) {
                    composable("login") {
                        LoginPage { user, pass ->
                            // Update state with the login details
                            username = user
                            password = pass
                            loggedIn = true // Mark the user as logged in
                            navController.navigate("helloWorld") {
                                popUpTo("login") { inclusive = true }  // Clears "login" from the back stack
                                launchSingleTop = true                 // Prevents duplicate "helloWorld" instances
                            } // Navigate to HelloWorld screen
                        }
                    }
                    composable("helloWorld") {
                        HelloWorldPage(
                            username = username,
                            onLogout = {
                                loggedIn = false
                                TokenRepository.removeToken()
                                navController.navigate("login") {
                                    // Ensure we clear the back stack and reset state
                                    popUpTo("extra") { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onGoToExtra = { navController.navigate("extra") }, // Navigate to Extra screen
                            registerPosMachine = { serialNumber, carPlate ->
                                // Define your registerPosMachine logic here
                                // For example:
                                registerPosMachine(serialNumber, carPlate)
                            },
                            updateCarPlate = { serialNumber, plateNumber ->
                                // Define your updateCarPlate logic here
                                // For example:
                                updatePosMachine(serialNumber, plateNumber)
                            }
                        )
                    }
                    composable("extra") {
                        ExtraPage(
                            onGoBack = { loggedIn = false
//                                TokenRepository.removeToken()
                                navController.navigate("nfcScan") } //  back to HelloWorld
                        )
                    }

                    composable("nfcScan") {
                        NFCScanPage()
                    }


                }
            }
        }
    }



}
