package com.vandoliak.coupleapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.vandoliak.coupleapp.presentation.screens.CalendarScreen
import com.vandoliak.coupleapp.presentation.screens.FinanceScreen
import com.vandoliak.coupleapp.presentation.screens.LoginScreen
import com.vandoliak.coupleapp.presentation.screens.HomeScreen
import com.vandoliak.coupleapp.presentation.screens.PairScreen
import com.vandoliak.coupleapp.presentation.screens.SplashScreen
import com.vandoliak.coupleapp.presentation.screens.RegisterScreen
import com.vandoliak.coupleapp.presentation.screens.TaskScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

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
                onNavigateToTasks = {
                    navController.navigate("tasks")
                },
                onNavigateToCalendar = {
                    navController.navigate("calendar")
                },
                onNavigateToFinance = {
                    navController.navigate("finance")
                }
            )
        }

        composable("tasks") {
            TaskScreen(
                onNavigateToCalendar = {
                    navController.navigate("calendar")
                },
                onNavigateHome = {
                    navController.navigate("home")
                },
                onNavigateToFinance = {
                    navController.navigate("finance")
                }
            )
        }

        composable("calendar") {
            CalendarScreen(
                onNavigateToTasks = {
                    navController.navigate("tasks")
                },
                onNavigateHome = {
                    navController.navigate("home")
                },
                onNavigateToFinance = {
                    navController.navigate("finance")
                }
            )
        }

        composable("finance") {
            FinanceScreen(
                onNavigateHome = {
                    navController.navigate("home")
                },
                onNavigateToTasks = {
                    navController.navigate("tasks")
                },
                onNavigateToCalendar = {
                    navController.navigate("calendar")
                }
            )
        }
    }
}
