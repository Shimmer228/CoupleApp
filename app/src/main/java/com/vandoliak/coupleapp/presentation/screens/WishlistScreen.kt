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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vandoliak.coupleapp.data.remote.WishlistItemDto
import com.vandoliak.coupleapp.presentation.components.AppCard
import com.vandoliak.coupleapp.presentation.components.EmptyState
import com.vandoliak.coupleapp.presentation.components.PrimaryActionButton
import com.vandoliak.coupleapp.presentation.components.SectionTitle
import com.vandoliak.coupleapp.presentation.viewmodel.WishlistViewModel
import java.util.Locale

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
    var purchaseDialogItem by remember { mutableStateOf<WishlistItemDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadWishlist()
    }

    purchaseDialogItem?.let { item ->
        AlertDialog(
            onDismissRequest = { purchaseDialogItem = null },
            title = { Text("Create expense from this item?") },
            text = { Text("You can also add this purchase straight into Finance.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        purchaseDialogItem = null
                        viewModel.purchaseItem(item.id, createTransaction = true)
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        purchaseDialogItem = null
                        viewModel.purchaseItem(item.id, createTransaction = false)
                    }
                ) {
                    Text("No")
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
            title = "Wishlist",
            subtitle = "Plan what you want next and turn purchases into finance records."
        )

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(title = "Add Wishlist Item")

                OutlinedTextField(
                    value = viewModel.title.value,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                )

                OutlinedTextField(
                    value = viewModel.price.value,
                    onValueChange = viewModel::onPriceChange,
                    label = { Text("Price (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                )

                OutlinedTextField(
                    value = viewModel.url.value,
                    onValueChange = viewModel::onUrlChange,
                    label = { Text("Link (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                )

                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.titleMedium
                )

                listOf("HIGH", "MEDIUM", "LOW").forEach { option ->
                    OutlinedButton(
                        onClick = { viewModel.onPriorityChange(option) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (viewModel.priority.value == option) {
                                "${option.lowercase().replaceFirstChar { it.uppercase() }} Selected"
                            } else {
                                option.lowercase().replaceFirstChar { it.uppercase() }
                            }
                        )
                    }
                }

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleMedium
                )

                listOf("SELF", "PARTNER", "SHARED").forEach { option ->
                    OutlinedButton(
                        onClick = { viewModel.onCategoryChange(option) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (viewModel.category.value == option) {
                                "${option.lowercase().replaceFirstChar { it.uppercase() }} Selected"
                            } else {
                                option.lowercase().replaceFirstChar { it.uppercase() }
                            }
                        )
                    }
                }

                PrimaryActionButton(
                    text = if (viewModel.isSubmitting.value) "Saving..." else "Add Item",
                    onClick = viewModel::createItem,
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

        SectionTitle(title = "Planned Purchases")

        if (viewModel.isLoading.value && viewModel.items.value.isEmpty()) {
            EmptyState(
                title = "Loading wishlist",
                subtitle = "Pulling your planned purchases."
            )
        } else if (viewModel.items.value.isEmpty()) {
            EmptyState(
                title = "Wishlist is empty",
                subtitle = "Add something fun, useful, or shared for later."
            )
        } else {
            viewModel.items.value.forEach { item ->
                WishlistItemCard(
                    item = item,
                    isSubmitting = viewModel.isSubmitting.value,
                    onPurchaseClick = {
                        if (item.price != null) {
                            purchaseDialogItem = item
                        } else {
                            viewModel.purchaseItem(item.id, createTransaction = false)
                        }
                    },
                    onDeleteClick = {
                        viewModel.deleteItem(item.id)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun WishlistItemCard(
    item: WishlistItemDto,
    isSubmitting: Boolean,
    onPurchaseClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val priorityColor = priorityColor(item.priority)

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Priority: ${formatLabel(item.priority)}",
                color = priorityColor
            )
            Text(text = "Category: ${formatLabel(item.category)}")
            Text(text = "Created by: ${item.createdBy.email}")

            item.price?.let {
                Text(text = "Price: ${formatAmount(it)}")
            }

            item.url?.takeIf { it.isNotBlank() }?.let {
                Text(text = "Link: $it")
            }

            if (item.isPurchased) {
                Text(
                    text = "Purchased",
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                PrimaryActionButton(
                    text = "Mark as Purchased",
                    onClick = onPurchaseClick,
                    enabled = !isSubmitting
                )
            }

            OutlinedButton(
                onClick = onDeleteClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                Text("Delete")
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

private fun formatLabel(value: String): String {
    return value.lowercase().replaceFirstChar { character ->
        character.titlecase(Locale.getDefault())
    }
}

private fun formatAmount(amount: Double): String {
    return String.format(Locale.US, "%.2f", amount)
}
