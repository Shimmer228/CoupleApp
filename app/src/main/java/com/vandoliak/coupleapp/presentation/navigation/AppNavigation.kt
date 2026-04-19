package com.vandoliak.coupleapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.vandoliak.coupleapp.presentation.screens.LoginScreen
import com.vandoliak.coupleapp.presentation.screens.HomeScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen()
        }
    }
}