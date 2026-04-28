package com.vandoliak.coupleapp.presentation.screens

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.data.remote.MyProfileDto
import com.vandoliak.coupleapp.data.remote.PartnerProfileDto
import com.vandoliak.coupleapp.data.remote.RewardPurchaseDto
import com.vandoliak.coupleapp.presentation.components.AppCard
import com.vandoliak.coupleapp.presentation.components.CouplePointsHeader
import com.vandoliak.coupleapp.presentation.components.PrimaryActionButton
import com.vandoliak.coupleapp.presentation.components.PointBadge
import com.vandoliak.coupleapp.presentation.components.ProfileAvatar
import com.vandoliak.coupleapp.presentation.components.SectionTitle
import com.vandoliak.coupleapp.presentation.util.avatarOptionLabel
import com.vandoliak.coupleapp.presentation.viewmodel.NotificationViewModel
import com.vandoliak.coupleapp.presentation.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onNavigateToShop: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToNotifications: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val viewModel: ProfileViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(
            context.applicationContext as Application
        )
    )
    val notificationViewModel: NotificationViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(
            context.applicationContext as Application
        )
    )
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadAvatar(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
        notificationViewModel.loadUnreadCount()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(
            title = stringResource(R.string.profiles_title),
            subtitle = stringResource(R.string.profiles_subtitle)
        )

        if (viewModel.isLoading.value && viewModel.myProfile.value == null) {
            AppCard {
                Text(stringResource(R.string.loading_profile))
            }
        } else {
            viewModel.myProfile.value?.let { profile ->
                CouplePointsHeader(
                    myLabel = stringResource(R.string.my_points_label),
                    myPoints = profile.points,
                    partnerLabel = viewModel.partnerProfile.value?.let { partner ->
                        partner.nickname ?: stringResource(R.string.partner_points_label)
                    },
                    partnerPoints = viewModel.partnerProfile.value?.points,
                    subtitle = stringResource(R.string.points_header_subtitle)
                )
            }

            viewModel.myProfile.value?.let { profile ->
                MyProfileCard(
                    profile = profile,
                    nickname = viewModel.nickname.value,
                    avatarKey = viewModel.avatarKey.value,
                    avatarPreviewBytes = viewModel.localAvatarPreviewBytes.value,
                    avatarOptions = viewModel.avatarOptions,
                    isSaving = viewModel.isSaving.value,
                    isUploadingAvatar = viewModel.isUploadingAvatar.value,
                    onNicknameChange = viewModel::onNicknameChange,
                    onAvatarChange = viewModel::onAvatarKeyChange,
                    onSave = viewModel::saveProfile,
                    onPickAvatar = { imagePicker.launch("image/*") },
                    onNavigateToShop = onNavigateToShop,
                    onNavigateToSettings = onNavigateToSettings,
                    unreadNotifications = notificationViewModel.unreadCount.value,
                    onNavigateToNotifications = onNavigateToNotifications
                )
            }

            PartnerProfileCard(profile = viewModel.partnerProfile.value)
            PurchaseHistoryCard(purchases = viewModel.purchases.value)
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

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MyProfileCard(
    profile: MyProfileDto,
    nickname: String,
    avatarKey: String,
    avatarPreviewBytes: ByteArray?,
    avatarOptions: List<String>,
    isSaving: Boolean,
    isUploadingAvatar: Boolean,
    onNicknameChange: (String) -> Unit,
    onAvatarChange: (String) -> Unit,
    onSave: () -> Unit,
    onPickAvatar: () -> Unit,
    onNavigateToShop: (() -> Unit)?,
    onNavigateToSettings: (() -> Unit)?,
    unreadNotifications: Int,
    onNavigateToNotifications: (() -> Unit)?
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfileAvatar(
                        avatarKey = avatarKey,
                        avatarUrl = profile.avatarUrl,
                        localAvatarBytes = avatarPreviewBytes,
                        size = 88.dp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = nickname.ifBlank { stringResource(R.string.your_profile) },
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(text = profile.email)
                        Text(text = "\uD83D\uDD25 ${stringResource(R.string.streak)}: ${profile.winStreak}")
                        if (profile.isWeeklyWinner) {
                            Text(
                                text = "\uD83D\uDC51 ${stringResource(R.string.weekly_winner)}",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    onNavigateToNotifications?.let {
                        Box {
                            IconButton(onClick = it) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = stringResource(R.string.notifications)
                                )
                            }

                            if (unreadNotifications > 0) {
                                Surface(
                                    modifier = Modifier.align(Alignment.TopEnd),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.error
                                ) {
                                    Text(
                                        text = unreadNotifications.coerceAtMost(99).toString(),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                        color = MaterialTheme.colorScheme.onError,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    onNavigateToSettings?.let {
                        IconButton(onClick = it) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.settings)
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onPickAvatar,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUploadingAvatar && !isSaving
            ) {
                Text(
                    if (isUploadingAvatar) {
                        stringResource(R.string.uploading_avatar)
                    } else {
                        stringResource(R.string.upload_avatar)
                    }
                )
            }

            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                label = { Text(stringResource(R.string.nickname)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )

            SectionTitle(title = stringResource(R.string.choose_preset_avatar))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                avatarOptions.forEach { option ->
                    Surface(
                        modifier = Modifier
                            .size(96.dp)
                            .clickable(enabled = !isSaving) { onAvatarChange(option) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        tonalElevation = if (avatarKey == option) 3.dp else 0.dp,
                        color = if (avatarKey == option) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = if (avatarKey == option) 2.dp else 1.dp,
                            color = if (avatarKey == option) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ProfileAvatar(
                                avatarKey = option,
                                size = 42.dp
                            )
                            Text(
                                text = LocalContext.current.avatarOptionLabel(option),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            PrimaryActionButton(
                text = if (isSaving) stringResource(R.string.saving) else stringResource(R.string.save_profile),
                onClick = onSave,
                enabled = !isSaving
            )

            onNavigateToShop?.let {
                OutlinedButton(
                    onClick = it,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.open_point_shop))
                }
            }
        }
    }
}

@Composable
private fun PartnerProfileCard(profile: PartnerProfileDto?) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(title = stringResource(R.string.partner_profile))

            if (profile == null) {
                Text(stringResource(R.string.partner_profile_unavailable))
                return@Column
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileAvatar(
                    avatarKey = profile.avatarKey,
                    avatarUrl = profile.avatarUrl,
                    size = 58.dp
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = profile.nickname ?: stringResource(R.string.no_nickname_yet),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(text = profile.email)
                    Text(text = "\uD83D\uDD25 ${stringResource(R.string.streak)}: ${profile.winStreak}")
                    if (profile.isWeeklyWinner) {
                        Text(
                            text = "\uD83D\uDC51 ${stringResource(R.string.weekly_winner)}",
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
            SectionTitle(title = stringResource(R.string.my_purchases))

            if (purchases.isEmpty()) {
                Text(stringResource(R.string.no_reward_purchases_yet))
                return@Column
            }

            purchases.take(6).forEach { purchase ->
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = purchase.reward.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        PointBadge(points = purchase.reward.cost)
                        Text(text = stringResource(R.string.required_streak, purchase.reward.minStreak))
                    }
                }
            }
        }
    }
}
