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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import com.vandoliak.coupleapp.presentation.viewmodel.FinanceViewModel
import java.util.Locale

@Composable
fun FinanceScreen(
    onNavigateHome: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToCalendar: () -> Unit
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Finance",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNavigateHome,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Home")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onNavigateToTasks,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tasks")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onNavigateToCalendar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calendar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Current Balance",
                    style = MaterialTheme.typography.titleLarge
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
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Transaction",
                    style = MaterialTheme.typography.titleLarge
                )

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

                Button(
                    onClick = viewModel::createTransaction,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                ) {
                    Text(if (viewModel.isSubmitting.value) "Loading..." else "Add Transaction")
                }
            }
        }

        viewModel.error.value?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        viewModel.successMessage.value?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.isLoading.value && viewModel.transactions.value.isEmpty()) {
            CircularProgressIndicator()
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (viewModel.transactions.value.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No transactions yet.",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                viewModel.transactions.value.forEach { transaction ->
                    FinanceItem(transaction)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FinanceItem(transaction: TransactionDto) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.titleLarge
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
