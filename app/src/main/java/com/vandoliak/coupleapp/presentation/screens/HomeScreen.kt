package com.vandoliak.coupleapp.presentation.screens

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.presentation.components.AppTopBar
import com.vandoliak.coupleapp.presentation.components.ProfileAvatar
import com.vandoliak.coupleapp.presentation.viewmodel.ProfileViewModel

private enum class MainTab {
    CALENDAR,
    CHALLENGES,
    FINANCE,
    WISHLIST,
    PROFILE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToShop: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(
            context.applicationContext as Application
        )
    )
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.CALENDAR.name) }
    val currentTab = MainTab.valueOf(selectedTab)

    LaunchedEffect(Unit) {
        profileViewModel.loadProfile()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = currentTab.title(),
                actionLabel = if (currentTab == MainTab.PROFILE) stringResource(R.string.shop) else null,
                onActionClick = if (currentTab == MainTab.PROFILE) onNavigateToShop else null
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { selectedTab = tab.name },
                        icon = {
                            when (tab) {
                                MainTab.CALENDAR -> Icon(Icons.Outlined.CalendarMonth, contentDescription = tab.label())
                                MainTab.CHALLENGES -> Icon(Icons.Outlined.Checklist, contentDescription = tab.label())
                                MainTab.FINANCE -> Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = tab.label())
                                MainTab.WISHLIST -> Icon(Icons.Outlined.FavoriteBorder, contentDescription = tab.label())
                                MainTab.PROFILE -> ProfileAvatar(
                                    avatarKey = profileViewModel.myProfile.value?.avatarKey ?: profileViewModel.avatarKey.value,
                                    avatarUrl = profileViewModel.myProfile.value?.avatarUrl,
                                    size = 24.dp
                                )
                            }
                        },
                        label = { Text(tab.label()) }
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
            MainTab.CHALLENGES -> TaskScreen(modifier = contentModifier)
            MainTab.FINANCE -> FinanceScreen(modifier = contentModifier)
            MainTab.WISHLIST -> WishlistScreen(modifier = contentModifier)
            MainTab.PROFILE -> ProfileScreen(
                modifier = contentModifier,
                onNavigateToShop = onNavigateToShop,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    }
}

@Composable
private fun MainTab.title(): String {
    return when (this) {
        MainTab.CALENDAR -> stringResource(R.string.nav_calendar)
        MainTab.CHALLENGES -> stringResource(R.string.nav_challenges)
        MainTab.FINANCE -> stringResource(R.string.nav_finance)
        MainTab.WISHLIST -> stringResource(R.string.nav_wishlist)
        MainTab.PROFILE -> stringResource(R.string.nav_profile)
    }
}

@Composable
private fun MainTab.label(): String = title()
