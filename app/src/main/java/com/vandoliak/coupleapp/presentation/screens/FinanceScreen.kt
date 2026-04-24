package com.vandoliak.coupleapp.presentation.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vandoliak.coupleapp.data.remote.TransactionDto
import com.vandoliak.coupleapp.presentation.components.AppCard
import com.vandoliak.coupleapp.presentation.components.EmptyState
import com.vandoliak.coupleapp.presentation.components.PrimaryActionButton
import com.vandoliak.coupleapp.presentation.components.SectionTitle
import com.vandoliak.coupleapp.presentation.viewmodel.FinanceViewModel
import java.util.Locale

@Composable
fun FinanceScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: FinanceViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(
            context.applicationContext as Application
        )
    )

    LaunchedEffect(Unit) {
        viewModel.loadFinance()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(
            title = "Shared Finance",
            subtitle = "Track what is shared, owed, and already settled."
        )

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Current balance",
                    style = MaterialTheme.typography.titleMedium
                )

                val balanceText = if (viewModel.balance.value == 0.0) {
                    "Settled up"
                } else if (viewModel.direction.value == "YOU_OWE") {
                    "You owe ${formatAmount(viewModel.balance.value)}"
                } else {
                    "Partner owes ${formatAmount(viewModel.balance.value)}"
                }

                Text(
                    text = balanceText,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(title = "Add Transaction")

                OutlinedTextField(
                    value = viewModel.title.value,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                )

                OutlinedTextField(
                    value = viewModel.amount.value,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                )

                Text(
                    text = "Type",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedButton(
                    onClick = { viewModel.onTypeChange("EXPENSE") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (viewModel.type.value == "EXPENSE") "Expense Selected" else "Expense")
                }

                OutlinedButton(
                    onClick = { viewModel.onTypeChange("INCOME") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (viewModel.type.value == "INCOME") "Income Selected" else "Income")
                }

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedButton(
                    onClick = { viewModel.onCategoryChange("SELF") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (viewModel.category.value == "SELF") "Self Selected" else "Self")
                }

                OutlinedButton(
                    onClick = { viewModel.onCategoryChange("PARTNER") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (viewModel.category.value == "PARTNER") "Partner Selected" else "Partner")
                }

                OutlinedButton(
                    onClick = { viewModel.onCategoryChange("SHARED") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (viewModel.category.value == "SHARED") "Shared Selected" else "Shared")
                }

                PrimaryActionButton(
                    text = if (viewModel.isSubmitting.value) "Saving..." else "Add Transaction",
                    onClick = viewModel::createTransaction,
                    enabled = !viewModel.isSubmitting.value
                )
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

        SectionTitle(title = "Recent Activity")

        if (viewModel.isLoading.value && viewModel.transactions.value.isEmpty()) {
            EmptyState(
                title = "Loading finance",
                subtitle = "Pulling the latest transactions and balance."
            )
        } else if (viewModel.transactions.value.isEmpty()) {
            EmptyState(
                title = "No transactions yet",
                subtitle = "Add the first shared expense or income entry."
            )
        } else {
            viewModel.transactions.value.forEach { transaction ->
                FinanceItem(transaction)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FinanceItem(transaction: TransactionDto) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = "Amount: ${formatAmount(transaction.amount)}")
            Text(text = "Type: ${transaction.type.lowercase().replaceFirstChar { it.uppercase() }}")
            Text(text = "Category: ${transaction.category.lowercase().replaceFirstChar { it.uppercase() }}")
            Text(text = "Created by: ${transaction.createdBy.email}")
        }
    }
}

private fun formatAmount(amount: Double): String {
    return String.format(Locale.US, "%.2f", amount)
}
