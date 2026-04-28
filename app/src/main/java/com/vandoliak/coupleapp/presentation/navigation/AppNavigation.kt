package com.vandoliak.coupleapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vandoliak.coupleapp.data.local.SessionDestination
import com.vandoliak.coupleapp.data.local.SessionEvents
import com.vandoliak.coupleapp.presentation.screens.HomeScreen
import com.vandoliak.coupleapp.presentation.screens.LoginScreen
import com.vandoliak.coupleapp.presentation.screens.NotificationsScreen
import com.vandoliak.coupleapp.presentation.screens.PairScreen
import com.vandoliak.coupleapp.presentation.screens.RegisterScreen
import com.vandoliak.coupleapp.presentation.screens.SettingsScreen
import com.vandoliak.coupleapp.presentation.screens.ShopScreen
import com.vandoliak.coupleapp.presentation.screens.SplashScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        SessionEvents.events.collect { destination ->
            when (destination) {
                SessionDestination.LOGIN -> {
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }

                SessionDestination.PAIR -> {
                    navController.navigate("pair") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }
    }

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen { destination ->
                navController.navigate(destination) {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { hasPair ->
                    navController.navigate(if (hasPair) "home" else "pair") {
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
                onLoginSuccess = { hasPair ->
                    navController.navigate(if (hasPair) "home" else "pair") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        composable("pair") {
            PairScreen(
                onPairComplete = {
                    navController.navigate("home") {
                        popUpTo("pair") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onNavigateToShop = {
                    navController.navigate("shop")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToNotifications = {
                    navController.navigate("notifications")
                }
            )
        }

        composable("shop") {
            ShopScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                modifier = Modifier,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("notifications") {
            NotificationsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
