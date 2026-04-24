package com.vandoliak.coupleapp.presentation.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vandoliak.coupleapp.presentation.components.AppTopBar

private enum class MainTab(
    val title: String,
    val label: String,
    val iconText: String
) {
    CALENDAR("Calendar", "Calendar", "C"),
    ARENA("Arena", "Arena", "A"),
    FINANCE("Finance", "Finance", "$"),
    WISHLIST("Wishlist", "Wishlist", "W"),
    PROFILE("Profile", "Profile", "P")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToShop: () -> Unit
) {
    var selectedTab by rememberSaveable { androidx.compose.runtime.mutableStateOf(MainTab.CALENDAR.name) }
    val currentTab = MainTab.valueOf(selectedTab)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = currentTab.title,
                actionLabel = if (currentTab == MainTab.PROFILE) "Shop" else null,
                onActionClick = if (currentTab == MainTab.PROFILE) onNavigateToShop else null
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { selectedTab = tab.name },
                        icon = { Text(tab.iconText) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (currentTab) {
            MainTab.CALENDAR -> CalendarScreen(modifier = contentModifier)
            MainTab.ARENA -> TaskScreen(modifier = contentModifier)
            MainTab.FINANCE -> FinanceScreen(modifier = contentModifier)
            MainTab.WISHLIST -> WishlistScreen(modifier = contentModifier)
            MainTab.PROFILE -> ProfileScreen(
                modifier = contentModifier,
                onNavigateToShop = onNavigateToShop
            )
        }
    }
}
