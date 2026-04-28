package com.vandoliak.coupleapp.presentation.screens

import android.app.Application
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.data.local.AppCurrency
import com.vandoliak.coupleapp.data.local.AppSettingsManager
import com.vandoliak.coupleapp.data.remote.WishlistItemDto
import com.vandoliak.coupleapp.presentation.components.AppCard
import com.vandoliak.coupleapp.presentation.components.EmptyState
import com.vandoliak.coupleapp.presentation.components.PrimaryActionButton
import com.vandoliak.coupleapp.presentation.components.SectionTitle
import com.vandoliak.coupleapp.presentation.components.SelectionChip
import com.vandoliak.coupleapp.presentation.util.formatCurrency
import com.vandoliak.coupleapp.presentation.util.priorityLabel
import com.vandoliak.coupleapp.presentation.util.scopeLabel
import com.vandoliak.coupleapp.presentation.util.transactionCategoryLabel
import com.vandoliak.coupleapp.presentation.viewmodel.WishlistViewModel

@Composable
fun WishlistScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: WishlistViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(
            context.applicationContext as Application
        )
    )
    val settingsManager = remember(context) { AppSettingsManager(context.applicationContext) }
    val currency by settingsManager.currencyFlow.collectAsState(initial = AppCurrency.UAH)
    var showCreateDialog by remember { mutableStateOf(false) }
    var purchaseDialogItem by remember { mutableStateOf<WishlistItemDto?>(null) }
    var categoryDialogItem by remember { mutableStateOf<WishlistItemDto?>(null) }
    var editingItem by remember { mutableStateOf<WishlistItemDto?>(null) }
    var deletingItem by remember { mutableStateOf<WishlistItemDto?>(null) }
    var selectedExpenseCategory by rememberSaveable { mutableStateOf("SHOPPING") }

    LaunchedEffect(Unit) {
        viewModel.loadWishlist()
    }

    purchaseDialogItem?.let { item ->
        AlertDialog(
            onDismissRequest = { purchaseDialogItem = null },
            title = { Text(stringResource(R.string.create_expense_from_item)) },
            text = { Text(stringResource(R.string.wishlist_purchase_finance_hint)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        purchaseDialogItem = null
                        selectedExpenseCategory = "SHOPPING"
                        categoryDialogItem = item
                    }
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        purchaseDialogItem = null
                        viewModel.purchaseItem(item.id, createTransaction = false)
                    }
                ) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }

    categoryDialogItem?.let { item ->
        AlertDialog(
            onDismissRequest = { categoryDialogItem = null },
            title = { Text(stringResource(R.string.choose_expense_category)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.expense_category_required))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.expenseTransactionCategories.forEach { option ->
                            SelectionChip(
                                label = context.transactionCategoryLabel(option),
                                selected = selectedExpenseCategory == option,
                                onClick = { selectedExpenseCategory = option }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.purchaseItem(
                            itemId = item.id,
                            createTransaction = true,
                            transactionCategory = selectedExpenseCategory
                        )
                        categoryDialogItem = null
                    }
                ) {
                    Text(stringResource(R.string.confirm_purchase))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { categoryDialogItem = null }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    editingItem?.let { item ->
        AlertDialog(
            onDismissRequest = {
                editingItem = null
                viewModel.resetForm()
            },
            title = { Text(stringResource(R.string.edit_wishlist_item)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    WishlistForm(viewModel = viewModel)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateItem(item.id) {
                            editingItem = null
                        }
                    },
                    enabled = !viewModel.isSubmitting.value
                ) {
                    Text(if (viewModel.isSubmitting.value) stringResource(R.string.saving) else stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editingItem = null
                        viewModel.resetForm()
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text(stringResource(R.string.delete_wishlist_item)) },
            text = { Text(stringResource(R.string.delete_wishlist_message, item.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(item.id) {
                            deletingItem = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                viewModel.resetForm()
            },
            title = { Text(stringResource(R.string.add_wishlist_item)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    WishlistForm(viewModel = viewModel)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createItem {
                            showCreateDialog = false
                        }
                    },
                    enabled = !viewModel.isSubmitting.value
                ) {
                    Text(
                        if (viewModel.isSubmitting.value) {
                            stringResource(R.string.saving)
                        } else {
                            stringResource(R.string.add_item)
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        viewModel.resetForm()
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.resetForm()
                    showCreateDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.add_wishlist_item)
                )
            }
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
                title = stringResource(R.string.wishlist_title),
                subtitle = stringResource(R.string.wishlist_subtitle)
            )

            viewModel.error.value?.takeIf {
                it.isNotBlank() &&
                    !showCreateDialog &&
                    purchaseDialogItem == null &&
                    categoryDialogItem == null &&
                    editingItem == null &&
                    deletingItem == null
            }?.let {
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

            SectionTitle(title = stringResource(R.string.planned_purchases))

            if (viewModel.isLoading.value && viewModel.items.value.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.loading_wishlist),
                    subtitle = stringResource(R.string.loading_wishlist_subtitle)
                )
            } else if (viewModel.items.value.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.wishlist_empty),
                    subtitle = stringResource(R.string.wishlist_empty_subtitle)
                )
            } else {
                viewModel.items.value.forEach { item ->
                    WishlistItemCard(
                        item = item,
                        currency = currency,
                        canEdit = item.createdBy.id == viewModel.currentUserId.value,
                        isSubmitting = viewModel.isSubmitting.value,
                        onOpenLinkClick = {
                            val normalizedUrl = normalizeExternalUrl(item.url)
                            if (normalizedUrl != null) {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl))
                                )
                            }
                        },
                        onPurchaseClick = {
                            if (item.price != null) {
                                purchaseDialogItem = item
                            } else {
                                viewModel.purchaseItem(item.id, createTransaction = false)
                            }
                        },
                        onEditClick = {
                            viewModel.populateEditor(item)
                            editingItem = item
                        },
                        onDeleteClick = {
                            deletingItem = item
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun WishlistForm(viewModel: WishlistViewModel) {
    viewModel.error.value?.takeIf { it.isNotBlank() }?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }

    OutlinedTextField(
        value = viewModel.title.value,
        onValueChange = viewModel::onTitleChange,
        label = { Text(stringResource(R.string.title)) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isSubmitting.value
    )

    OutlinedTextField(
        value = viewModel.price.value,
        onValueChange = viewModel::onPriceChange,
        label = { Text(stringResource(R.string.price_optional)) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isSubmitting.value
    )

    OutlinedTextField(
        value = viewModel.url.value,
        onValueChange = viewModel::onUrlChange,
        label = { Text(stringResource(R.string.link_optional)) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isSubmitting.value
    )

    Text(
        text = stringResource(R.string.priority),
        style = MaterialTheme.typography.titleMedium
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("HIGH", "MEDIUM", "LOW").forEach { option ->
            SelectionChip(
                label = LocalContext.current.priorityLabel(option),
                selected = viewModel.priority.value == option,
                onClick = { viewModel.onPriorityChange(option) },
                enabled = !viewModel.isSubmitting.value
            )
        }
    }

    Text(
        text = stringResource(R.string.category),
        style = MaterialTheme.typography.titleMedium
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("SELF", "PARTNER", "SHARED").forEach { option ->
            SelectionChip(
                label = LocalContext.current.scopeLabel(option),
                selected = viewModel.category.value == option,
                onClick = { viewModel.onCategoryChange(option) },
                enabled = !viewModel.isSubmitting.value
            )
        }
    }
}

@Composable
private fun WishlistItemCard(
    item: WishlistItemDto,
    currency: AppCurrency,
    canEdit: Boolean,
    isSubmitting: Boolean,
    onOpenLinkClick: () -> Unit,
    onPurchaseClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val priorityColor = priorityColor(item.priority)
    val hasLink = !item.url.isNullOrBlank()

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${stringResource(R.string.priority)}: ${LocalContext.current.priorityLabel(item.priority)}",
                color = priorityColor
            )
            Text(text = "${stringResource(R.string.category)}: ${LocalContext.current.scopeLabel(item.category)}")
            Text(text = stringResource(R.string.created_by, item.createdBy.email))

            item.price?.let {
                Text(text = stringResource(R.string.price_value, formatCurrency(it, currency)))
            }

            if (item.isPurchased) {
                Text(
                    text = stringResource(R.string.purchased),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                PrimaryActionButton(
                    text = stringResource(R.string.mark_as_purchased),
                    onClick = onPurchaseClick,
                    enabled = !isSubmitting
                )
            }

            if (hasLink || canEdit) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasLink) {
                        OutlinedButton(
                            onClick = onOpenLinkClick,
                            enabled = !isSubmitting
                        ) {
                            Text(stringResource(R.string.open_link))
                        }
                    }

                    if (canEdit && !item.isPurchased) {
                        OutlinedButton(
                            onClick = onEditClick,
                            enabled = !isSubmitting
                        ) {
                            Text(stringResource(R.string.edit))
                        }
                    }

                    if (canEdit) {
                        OutlinedButton(
                            onClick = onDeleteClick,
                            enabled = !isSubmitting
                        ) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }
}

private fun priorityColor(priority: String): Color {
    return when (priority.uppercase()) {
        "HIGH" -> Color(0xFFD32F2F)
        "MEDIUM" -> Color(0xFFF57C00)
        else -> Color(0xFF757575)
    }
}

private fun normalizeExternalUrl(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) {
        return null
    }

    return if (value.startsWith("http://") || value.startsWith("https://")) {
        value
    } else {
        "https://$value"
    }
}
