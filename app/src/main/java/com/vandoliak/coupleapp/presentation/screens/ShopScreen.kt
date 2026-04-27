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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vandoliak.coupleapp.R
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
                title = stringResource(R.string.point_shop),
                navigationLabel = stringResource(R.string.cancel),
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
                title = stringResource(R.string.reward_catalog),
                subtitle = stringResource(R.string.reward_catalog_subtitle)
            )

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${stringResource(R.string.points)}: ${viewModel.currentUserPoints.value}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(text = "${stringResource(R.string.streak)}: ${viewModel.currentUserWinStreak.value}")
                }
            }

            viewModel.error.value?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }

            viewModel.successMessage.value?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (viewModel.isLoading.value && viewModel.rewards.value.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.loading),
                    subtitle = stringResource(R.string.reward_catalog_subtitle)
                )
            } else if (viewModel.rewards.value.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.reward_catalog),
                    subtitle = stringResource(R.string.reward_catalog_subtitle)
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

            Text(text = stringResource(R.string.cost_points_value, reward.cost))
            Text(text = stringResource(R.string.required_streak_value, reward.minStreak))
            Text(text = if (reward.isUnlocked) stringResource(R.string.unlocked) else stringResource(R.string.locked))

            if (!canAfford) {
                Text(
                    text = stringResource(R.string.not_enough_points),
                    color = MaterialTheme.colorScheme.error
                )
            }

            PrimaryActionButton(
                text = if (isSubmitting) stringResource(R.string.buying) else stringResource(R.string.buy),
                onClick = onBuy,
                enabled = canBuy
            )
        }
    }
}
