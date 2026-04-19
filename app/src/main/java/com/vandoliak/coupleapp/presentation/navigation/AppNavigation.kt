package com.vandoliak.coupleapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.vandoliak.coupleapp.presentation.screens.LoginScreen
import com.vandoliak.coupleapp.presentation.screens.HomeScreen
import com.vandoliak.coupleapp.presentation.screens.SplashScreen
import com.vandoliak.coupleapp.presentation.screens.RegisterScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen { isLoggedIn ->
                if (isLoggedIn) {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("login")
                }
            )
        }
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        composable("home") {
            HomeScreen()
        }
    }
}