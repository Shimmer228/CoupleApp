package com.vandoliak.coupleapp.presentation.screens

import android.app.Application
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ContextWrapper
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val activity = remember(context) { context.findActivity() }
    val viewModel: SettingsViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(
            context.applicationContext as Application
        )
    )
    var pendingLanguage by remember { mutableStateOf<AppLanguage?>(null) }

    pendingLanguage?.let { language ->
        AlertDialog(
            onDismissRequest = { pendingLanguage = null },
            title = { Text(stringResource(R.string.settings_language)) },
            text = { Text(stringResource(R.string.restart_to_apply_language)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setLanguage(language)
                        pendingLanguage = null
                        activity?.restartApplication()
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLanguage = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                AppTopBar(
                    title = stringResource(R.string.settings),
                    navigationLabel = stringResource(R.string.cancel),
                    onNavigationClick = onNavigateBack
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                                    onClick = {
                                        if (viewModel.language.value != option) {
                                            pendingLanguage = option
                                        }
                                    }
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
    }
}

private fun Activity.restartApplication() {
    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ?: return

    startActivity(launchIntent)
    finishAffinity()
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
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
