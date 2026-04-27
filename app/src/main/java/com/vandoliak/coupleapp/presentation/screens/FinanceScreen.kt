package com.vandoliak.coupleapp.presentation.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.data.local.AppCurrency
import com.vandoliak.coupleapp.data.local.AppSettingsManager
import com.vandoliak.coupleapp.data.remote.TransactionCategorySummaryDto
import com.vandoliak.coupleapp.data.remote.TransactionDto
import com.vandoliak.coupleapp.presentation.components.AppCard
import com.vandoliak.coupleapp.presentation.components.EmptyState
import com.vandoliak.coupleapp.presentation.components.PrimaryActionButton
import com.vandoliak.coupleapp.presentation.components.SectionTitle
import com.vandoliak.coupleapp.presentation.components.SelectionChip
import com.vandoliak.coupleapp.presentation.util.formatCurrency
import com.vandoliak.coupleapp.presentation.util.scopeLabel
import com.vandoliak.coupleapp.presentation.util.transactionCategoryLabel
import com.vandoliak.coupleapp.presentation.util.transactionTypeLabel
import com.vandoliak.coupleapp.presentation.viewmodel.FinanceViewModel
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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
    val settingsManager = remember(context) { AppSettingsManager(context.applicationContext) }
    val currency by settingsManager.currencyFlow.collectAsState(initial = AppCurrency.UAH)
    var editingTransaction by remember { mutableStateOf<TransactionDto?>(null) }
    var deletingTransaction by remember { mutableStateOf<TransactionDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadFinance()
    }

    editingTransaction?.let { transaction ->
        TransactionEditorDialog(
            transaction = transaction,
            viewModel = viewModel,
            onDismiss = {
                editingTransaction = null
                viewModel.resetForm()
            },
            onSave = {
                viewModel.updateTransaction(transaction.id) {
                    editingTransaction = null
                }
            }
        )
    }

    deletingTransaction?.let { transaction ->
        AlertDialog(
            onDismissRequest = { deletingTransaction = null },
            title = { Text(stringResource(R.string.delete_transaction_title)) },
            text = { Text(stringResource(R.string.delete_transaction_message, transaction.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(transaction.id) {
                            deletingTransaction = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTransaction = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(
            title = stringResource(R.string.finance_title),
            subtitle = stringResource(R.string.finance_subtitle)
        )

        BudgetOverviewCard(
            totalBudget = viewModel.totalBudget.value,
            balanceAmount = viewModel.balanceAmount.value,
            balanceDirection = viewModel.balanceDirection.value,
            currency = currency
        )

        CategorySummarySection(
            title = stringResource(R.string.expense_categories_title),
            subtitle = stringResource(R.string.expense_categories_subtitle),
            summaries = viewModel.expenseByCategory.value,
            currency = currency
        )

        if (viewModel.incomeByCategory.value.isNotEmpty()) {
            CategorySummarySection(
                title = stringResource(R.string.income_categories_title),
                subtitle = stringResource(R.string.income_categories_subtitle),
                summaries = viewModel.incomeByCategory.value,
                currency = currency
            )
        }

        AddTransactionCard(
            viewModel = viewModel,
            onSubmit = { viewModel.createTransaction() }
        )

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

        SectionTitle(
            title = stringResource(R.string.recent_activity_title),
            subtitle = stringResource(R.string.recent_activity_subtitle)
        )

        if (viewModel.isLoading.value && viewModel.transactions.value.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.loading_finance_title),
                subtitle = stringResource(R.string.loading_finance_subtitle)
            )
        } else if (viewModel.transactions.value.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.no_transactions_title),
                subtitle = stringResource(R.string.no_transactions_subtitle)
            )
        } else {
            viewModel.transactions.value.forEach { transaction ->
                FinanceItem(
                    transaction = transaction,
                    currency = currency,
                    canEdit = transaction.createdBy.id == viewModel.currentUserId.value,
                    onEditClick = {
                        viewModel.populateEditor(transaction)
                        editingTransaction = transaction
                    },
                    onDeleteClick = {
                        deletingTransaction = transaction
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun BudgetOverviewCard(
    totalBudget: Double,
    balanceAmount: Double,
    balanceDirection: String,
    currency: AppCurrency
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.couple_budget),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatCurrency(totalBudget, currency),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when (balanceDirection) {
                    "YOU_OWE" -> stringResource(R.string.balance_you_owe_partner, formatCurrency(balanceAmount, currency))
                    "PARTNER_OWES" -> stringResource(R.string.balance_partner_owes_you, formatCurrency(balanceAmount, currency))
                    else -> stringResource(R.string.balance_settled)
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (balanceDirection == "SETTLED") {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun AddTransactionCard(
    viewModel: FinanceViewModel,
    onSubmit: () -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(
                title = stringResource(R.string.add_transaction_title),
                subtitle = stringResource(R.string.add_transaction_subtitle)
            )
            TransactionEditorForm(viewModel = viewModel)
            PrimaryActionButton(
                text = if (viewModel.isSubmitting.value) stringResource(R.string.saving) else stringResource(R.string.add_transaction_button),
                onClick = onSubmit,
                enabled = !viewModel.isSubmitting.value
            )
        }
    }
}

@Composable
private fun TransactionEditorDialog(
    transaction: TransactionDto,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_transaction_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.edit_transaction_message, transaction.title),
                    style = MaterialTheme.typography.bodyMedium
                )
                TransactionEditorForm(viewModel = viewModel)
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = !viewModel.isSubmitting.value
            ) {
                Text(if (viewModel.isSubmitting.value) stringResource(R.string.saving) else stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun TransactionEditorForm(viewModel: FinanceViewModel) {
    val context = LocalContext.current

    OutlinedTextField(
        value = viewModel.title.value,
        onValueChange = viewModel::onTitleChange,
        label = { Text(stringResource(R.string.title)) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isSubmitting.value
    )

    OutlinedTextField(
        value = viewModel.amount.value,
        onValueChange = viewModel::onAmountChange,
        label = { Text(stringResource(R.string.amount)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isSubmitting.value
    )

    SectionTitle(title = stringResource(R.string.type))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        viewModel.typeOptions.forEach { option ->
            SelectionChip(
                label = context.transactionTypeLabel(option),
                selected = viewModel.type.value == option,
                onClick = { viewModel.onTypeChange(option) },
                enabled = !viewModel.isSubmitting.value
            )
        }
    }

    SectionTitle(title = stringResource(R.string.scope))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        viewModel.scopeOptions.forEach { option ->
            SelectionChip(
                label = context.scopeLabel(option),
                selected = viewModel.scope.value == option,
                onClick = { viewModel.onScopeChange(option) },
                enabled = !viewModel.isSubmitting.value
            )
        }
    }

    SectionTitle(title = stringResource(R.string.transaction_category))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        viewModel.availableTransactionCategories.forEach { option ->
            SelectionChip(
                label = "${categoryEmoji(option)} ${context.transactionCategoryLabel(option)}",
                selected = viewModel.transactionCategory.value == option,
                onClick = { viewModel.onTransactionCategoryChange(option) },
                enabled = !viewModel.isSubmitting.value
            )
        }
    }
}

@Composable
private fun CategorySummarySection(
    title: String,
    subtitle: String,
    summaries: List<TransactionCategorySummaryDto>,
    currency: AppCurrency
) {
    val context = LocalContext.current

    SectionTitle(title = title, subtitle = subtitle)

    if (summaries.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.no_data_yet_title),
            subtitle = stringResource(R.string.no_data_yet_subtitle)
        )
    } else {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            summaries.forEach { summary ->
                Surface(
                    modifier = Modifier.widthIn(min = 148.dp, max = 220.dp),
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 1.dp,
                    color = categoryTint(summary.category)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${categoryEmoji(summary.category)} ${context.transactionCategoryLabel(summary.category)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatCurrency(summary.total, currency),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.of_section, formatPercent(summary.percentage)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceItem(
    transaction: TransactionDto,
    currency: AppCurrency,
    canEdit: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val isIncome = transaction.type == "INCOME"
    val amountColor = if (isIncome) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${categoryEmoji(transaction.transactionCategory)} ${transaction.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(
                            R.string.transaction_summary_line,
                            context.scopeLabel(transaction.category),
                            context.transactionCategoryLabel(transaction.transactionCategory),
                            context.transactionTypeLabel(transaction.type)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (isIncome) "+${formatCurrency(transaction.amount, currency)}" else "-${formatCurrency(transaction.amount, currency)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = stringResource(
                    R.string.created_by_with_date,
                    transaction.createdBy.email,
                    formatTransactionDate(transaction.createdAt)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (canEdit) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEditClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.edit))
                    }
                    OutlinedButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        }
    }
}

private fun formatPercent(amount: Double): String {
    return String.format(Locale.US, "%.0f%%", amount)
}

private fun categoryEmoji(category: String): String {
    return when (category.uppercase()) {
        "FOOD" -> "\uD83C\uDF7D"
        "UTILITIES" -> "\uD83D\uDCA1"
        "TRANSPORT" -> "\uD83D\uDE8C"
        "HOME" -> "\uD83C\uDFE0"
        "ENTERTAINMENT" -> "\uD83C\uDF89"
        "HEALTH" -> "\u2695\uFE0F"
        "SHOPPING" -> "\uD83D\uDED2"
        "SUBSCRIPTIONS" -> "\uD83D\uDCFA"
        "SALARY" -> "\uD83D\uDCBC"
        "BONUS" -> "\uD83C\uDFC6"
        "GIFT" -> "\uD83C\uDF81"
        "REFUND" -> "\uD83D\uDCB8"
        "SIDE_JOB" -> "\uD83D\uDEE0\uFE0F"
        else -> "\uD83D\uDCE6"
    }
}

@Composable
private fun categoryTint(category: String): Color {
    return when (category.uppercase()) {
        "FOOD" -> MaterialTheme.colorScheme.secondaryContainer
        "UTILITIES" -> MaterialTheme.colorScheme.tertiaryContainer
        "TRANSPORT" -> MaterialTheme.colorScheme.primaryContainer
        "HOME" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f)
        "ENTERTAINMENT" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.78f)
        "HEALTH" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        "SHOPPING" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        "SUBSCRIPTIONS" -> MaterialTheme.colorScheme.surfaceVariant
        "SALARY", "BONUS", "REFUND", "SIDE_JOB" -> MaterialTheme.colorScheme.primaryContainer
        "GIFT" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

private fun formatTransactionDate(raw: String): String {
    return try {
        Instant.parse(raw)
            .atZone(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault()))
    } catch (_: Exception) {
        raw
    }
}
