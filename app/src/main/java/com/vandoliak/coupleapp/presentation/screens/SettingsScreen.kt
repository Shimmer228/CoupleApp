package com.vandoliak.coupleapp.presentation.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.data.local.AppCurrency
import com.vandoliak.coupleapp.data.local.AppLanguage
import com.vandoliak.coupleapp.data.local.ThemeMode
import com.vandoliak.coupleapp.presentation.components.AppCard
import com.vandoliak.coupleapp.presentation.components.AppTopBar
import com.vandoliak.coupleapp.presentation.components.SectionTitle
import com.vandoliak.coupleapp.presentation.components.SelectionChip
import com.vandoliak.coupleapp.presentation.viewmodel.SettingsNotice
import com.vandoliak.coupleapp.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(
            context.applicationContext as Application
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppTopBar(
            title = stringResource(R.string.settings),
            navigationLabel = stringResource(R.string.cancel),
            onNavigationClick = onNavigateBack
        )

        SectionTitle(
            title = stringResource(R.string.settings),
            subtitle = stringResource(R.string.settings_subtitle)
        )

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(title = stringResource(R.string.settings_appearance))
                Text(
                    text = stringResource(R.string.theme),
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.themeOptions.forEach { option ->
                        SelectionChip(
                            label = option.label(),
                            selected = viewModel.themeMode.value == option,
                            onClick = { viewModel.setThemeMode(option) }
                        )
                    }
                }
            }
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(title = stringResource(R.string.settings_language))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.languageOptions.forEach { option ->
                        SelectionChip(
                            label = option.label(),
                            selected = viewModel.language.value == option,
                            onClick = { viewModel.setLanguage(option) }
                        )
                    }
                }
            }
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(title = stringResource(R.string.currency))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.currencyOptions.forEach { option ->
                        SelectionChip(
                            label = option.label(),
                            selected = viewModel.currency.value == option,
                            onClick = { viewModel.setCurrency(option) }
                        )
                    }
                }
            }
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(title = stringResource(R.string.account))

                OutlinedButton(
                    onClick = viewModel::logout,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                ) {
                    Text(stringResource(R.string.log_out))
                }

                OutlinedButton(
                    onClick = viewModel::leavePair,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                ) {
                    Text(stringResource(R.string.leave_pair))
                }
            }
        }

        viewModel.notice.value?.let { notice ->
            Text(
                text = notice.label(),
                color = MaterialTheme.colorScheme.primary
            )
        }

        viewModel.error.value?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ThemeMode.label(): String {
    return when (this) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
    }
}

@Composable
private fun AppLanguage.label(): String {
    return when (this) {
        AppLanguage.ENGLISH -> stringResource(R.string.language_english)
        AppLanguage.UKRAINIAN -> stringResource(R.string.language_ukrainian)
    }
}

@Composable
private fun AppCurrency.label(): String {
    return when (this) {
        AppCurrency.UAH -> stringResource(R.string.currency_uah)
        AppCurrency.USD -> stringResource(R.string.currency_usd)
        AppCurrency.EUR -> stringResource(R.string.currency_eur)
        AppCurrency.SEK -> stringResource(R.string.currency_sek)
    }
}

@Composable
private fun SettingsNotice.label(): String {
    return when (this) {
        SettingsNotice.RESTART_LANGUAGE -> stringResource(R.string.restart_to_apply_language)
        SettingsNotice.PAIR_DISCONNECTED -> stringResource(R.string.pair_disconnected)
    }
}
