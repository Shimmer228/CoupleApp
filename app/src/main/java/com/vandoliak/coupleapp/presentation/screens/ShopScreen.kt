package com.vandoliak.coupleapp.presentation.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vandoliak.coupleapp.data.remote.RewardDto
import com.vandoliak.coupleapp.presentation.components.AppCard
import com.vandoliak.coupleapp.presentation.components.AppTopBar
import com.vandoliak.coupleapp.presentation.components.EmptyState
import com.vandoliak.coupleapp.presentation.components.PrimaryActionButton
import com.vandoliak.coupleapp.presentation.components.SectionTitle
import com.vandoliak.coupleapp.presentation.viewmodel.ShopViewModel

@Composable
fun ShopScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ShopViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(
            context.applicationContext as Application
        )
    )

    LaunchedEffect(Unit) {
        viewModel.loadShop()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Point Shop",
                navigationLabel = "<",
                onNavigationClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle(
                title = "Reward Catalog",
                subtitle = "Spend points on playful perks and unlocked rewards."
            )

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Points: ${viewModel.currentUserPoints.value}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(text = "Streak: ${viewModel.currentUserWinStreak.value}")
                }
            }

            viewModel.error.value?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }

            viewModel.successMessage.value?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (viewModel.isLoading.value && viewModel.rewards.value.isEmpty()) {
                EmptyState(
                    title = "Loading rewards",
                    subtitle = "Fetching the reward catalog."
                )
            } else if (viewModel.rewards.value.isEmpty()) {
                EmptyState(
                    title = "No rewards available",
                    subtitle = "Rewards will appear here when the catalog is ready."
                )
            } else {
                viewModel.rewards.value.forEach { reward ->
                    RewardCard(
                        reward = reward,
                        currentUserPoints = viewModel.currentUserPoints.value,
                        isSubmitting = viewModel.isSubmitting.value,
                        onBuy = { viewModel.buyReward(reward.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RewardCard(
    reward: RewardDto,
    currentUserPoints: Int,
    isSubmitting: Boolean,
    onBuy: () -> Unit
) {
    val canAfford = currentUserPoints >= reward.cost
    val canBuy = reward.isUnlocked && canAfford && reward.isActive && !isSubmitting

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = reward.title,
                style = MaterialTheme.typography.titleLarge
            )

            reward.description?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(text = "Cost: ${reward.cost} points")
            Text(text = "Required streak: ${reward.minStreak}")
            Text(text = if (reward.isUnlocked) "Unlocked" else "Locked")

            if (!canAfford) {
                Text(
                    text = "Not enough points",
                    color = MaterialTheme.colorScheme.error
                )
            }

            PrimaryActionButton(
                text = if (isSubmitting) "Buying..." else "Buy",
                onClick = onBuy,
                enabled = canBuy
            )
        }
    }
}
