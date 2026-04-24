package com.vandoliak.coupleapp.presentation.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.vandoliak.coupleapp.data.remote.MyProfileDto
import com.vandoliak.coupleapp.data.remote.PartnerProfileDto
import com.vandoliak.coupleapp.data.remote.RewardPurchaseDto
import com.vandoliak.coupleapp.presentation.components.AppCard
import com.vandoliak.coupleapp.presentation.components.PrimaryActionButton
import com.vandoliak.coupleapp.presentation.components.ProfileAvatar
import com.vandoliak.coupleapp.presentation.components.SectionTitle
import com.vandoliak.coupleapp.presentation.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onNavigateToShop: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val viewModel: ProfileViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(
            context.applicationContext as Application
        )
    )

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(
            title = "Profiles",
            subtitle = "Your identity, streak, and reward progress in one place."
        )

        if (viewModel.isLoading.value && viewModel.myProfile.value == null) {
            AppCard {
                Text("Loading profile...")
            }
        } else {
            viewModel.myProfile.value?.let { profile ->
                MyProfileCard(
                    profile = profile,
                    nickname = viewModel.nickname.value,
                    avatarKey = viewModel.avatarKey.value,
                    avatarOptions = viewModel.avatarOptions,
                    isSaving = viewModel.isSaving.value,
                    onNicknameChange = viewModel::onNicknameChange,
                    onAvatarChange = viewModel::onAvatarKeyChange,
                    onSave = viewModel::saveProfile,
                    onNavigateToShop = onNavigateToShop
                )
            }

            PartnerProfileCard(profile = viewModel.partnerProfile.value)
            PurchaseHistoryCard(purchases = viewModel.purchases.value)
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

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MyProfileCard(
    profile: MyProfileDto,
    nickname: String,
    avatarKey: String,
    avatarOptions: List<String>,
    isSaving: Boolean,
    onNicknameChange: (String) -> Unit,
    onAvatarChange: (String) -> Unit,
    onSave: () -> Unit,
    onNavigateToShop: (() -> Unit)?
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileAvatar(
                    avatarKey = avatarKey,
                    size = 88.dp
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = nickname.ifBlank { "Your profile" },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(text = profile.email)
                    Text(text = "Points: ${profile.points}")
                    Text(text = "Streak: ${profile.winStreak}")
                    if (profile.isWeeklyWinner) {
                        Text(
                            text = "Weekly Winner",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                label = { Text("Nickname") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )

            SectionTitle(title = "Choose avatar")

            avatarOptions.chunked(3).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { option ->
                        OutlinedButton(
                            onClick = { onAvatarChange(option) },
                            enabled = !isSaving,
                            modifier = Modifier.size(92.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ProfileAvatar(
                                    avatarKey = option,
                                    size = 42.dp
                                )
                                Text(
                                    text = option.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            PrimaryActionButton(
                text = if (isSaving) "Saving..." else "Save Profile",
                onClick = onSave,
                enabled = !isSaving
            )

            onNavigateToShop?.let {
                OutlinedButton(
                    onClick = it,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Point Shop")
                }
            }
        }
    }
}

@Composable
private fun PartnerProfileCard(profile: PartnerProfileDto?) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(title = "Partner Profile")

            if (profile == null) {
                Text("Partner profile is not available yet.")
                return@Column
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileAvatar(
                    avatarKey = profile.avatarKey,
                    size = 58.dp
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = profile.nickname ?: "No nickname yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(text = profile.email)
                    Text(text = "Points: ${profile.points}")
                    Text(text = "Streak: ${profile.winStreak}")
                    if (profile.isWeeklyWinner) {
                        Text(
                            text = "Weekly Winner",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseHistoryCard(purchases: List<RewardPurchaseDto>) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(title = "My Purchases")

            if (purchases.isEmpty()) {
                Text("No reward purchases yet.")
                return@Column
            }

            purchases.take(6).forEach { purchase ->
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = purchase.reward.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(text = "Cost: ${purchase.reward.cost} points")
                        Text(text = "Required streak: ${purchase.reward.minStreak}")
                    }
                }
            }
        }
    }
}
