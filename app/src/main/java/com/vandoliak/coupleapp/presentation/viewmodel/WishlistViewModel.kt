package com.vandoliak.coupleapp.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.data.local.TokenManager
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.data.remote.WishlistCreateRequest
import com.vandoliak.coupleapp.data.remote.WishlistItemDto
import com.vandoliak.coupleapp.data.remote.WishlistPurchaseRequest
import com.vandoliak.coupleapp.data.remote.WishlistUpdateRequest
import com.vandoliak.coupleapp.data.remote.extractErrorMessage
import com.vandoliak.coupleapp.presentation.util.appString
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WishlistViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)
    val expenseTransactionCategories = listOf(
        "FOOD",
        "UTILITIES",
        "TRANSPORT",
        "HOME",
        "ENTERTAINMENT",
        "HEALTH",
        "SHOPPING",
        "SUBSCRIPTIONS",
        "OTHER"
    )

    var currentUserId = mutableStateOf("")
        private set

    var title = mutableStateOf("")
        private set

    var price = mutableStateOf("")
        private set

    var url = mutableStateOf("")
        private set

    var priority = mutableStateOf("MEDIUM")
        private set

    var category = mutableStateOf("SELF")
        private set

    var items = mutableStateOf<List<WishlistItemDto>>(emptyList())
        private set

    var isLoading = mutableStateOf(false)
        private set

    var isSubmitting = mutableStateOf(false)
        private set

    var error = mutableStateOf<String?>(null)
        private set

    var successMessage = mutableStateOf<String?>(null)
        private set

    fun onTitleChange(value: String) {
        title.value = value
    }

    fun onPriceChange(value: String) {
        price.value = sanitizePrice(value)
    }

    fun onUrlChange(value: String) {
        url.value = value
    }

    fun onPriorityChange(value: String) {
        priority.value = value
    }

    fun onCategoryChange(value: String) {
        category.value = value
    }

    fun populateEditor(item: WishlistItemDto) {
        title.value = item.title
        price.value = item.price?.toString().orEmpty()
        url.value = item.url.orEmpty()
        priority.value = item.priority
        category.value = item.category
    }

    fun resetForm() {
        title.value = ""
        price.value = ""
        url.value = ""
        priority.value = "MEDIUM"
        category.value = "SELF"
        error.value = null
    }

    fun loadWishlist() {
        viewModelScope.launch {
            loadData(showLoader = true)
        }
    }

    fun createItem(onSuccess: (() -> Unit)? = null) {
        val parsedPrice = if (price.value.isBlank()) null else price.value.toDoubleOrNull()

        if (title.value.isBlank()) {
            error.value = appString(R.string.wishlist_title_required)
            return
        }

        if (parsedPrice != null && parsedPrice < 0) {
            error.value = appString(R.string.price_non_negative)
            return
        }

        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = appString(R.string.session_expired_login)
                return@launch
            }

            try {
                isSubmitting.value = true
                error.value = null
                successMessage.value = null

                val response = RetrofitInstance.wishlistApi.createItem(
                    authorization = "Bearer $token",
                    request = WishlistCreateRequest(
                        title = title.value.trim(),
                        url = url.value.trim().ifBlank { null },
                        price = parsedPrice,
                        priority = priority.value,
                        category = category.value
                    )
                )

                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage(appString(R.string.failed_to_create_wishlist_item))
                    return@launch
                }

                successMessage.value = appString(R.string.wishlist_item_created)
                resetForm()
                loadData(showLoader = false)
                onSuccess?.invoke()
            } catch (e: Exception) {
                error.value = e.message ?: appString(R.string.unknown_error)
            } finally {
                isSubmitting.value = false
            }
        }
    }

    fun updateItem(itemId: String, onSuccess: (() -> Unit)? = null) {
        val parsedPrice = if (price.value.isBlank()) null else price.value.toDoubleOrNull()

        if (title.value.isBlank()) {
            error.value = appString(R.string.wishlist_title_required)
            return
        }

        if (parsedPrice != null && parsedPrice < 0) {
            error.value = appString(R.string.price_non_negative)
            return
        }

        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = appString(R.string.session_expired_login)
                return@launch
            }

            try {
                isSubmitting.value = true
                error.value = null
                successMessage.value = null

                val response = RetrofitInstance.wishlistApi.updateItem(
                    authorization = "Bearer $token",
                    id = itemId,
                    request = WishlistUpdateRequest(
                        title = title.value.trim(),
                        url = url.value.trim().ifBlank { null },
                        price = parsedPrice,
                        priority = priority.value,
                        category = category.value
                    )
                )

                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage(appString(R.string.failed_to_update_wishlist_item))
                    return@launch
                }

                successMessage.value = appString(R.string.wishlist_item_updated)
                resetForm()
                loadData(showLoader = false)
                onSuccess?.invoke()
            } catch (e: Exception) {
                error.value = e.message ?: appString(R.string.unknown_error)
            } finally {
                isSubmitting.value = false
            }
        }
    }

    fun purchaseItem(
        itemId: String,
        createTransaction: Boolean,
        transactionCategory: String? = null
    ) {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = appString(R.string.session_expired_login)
                return@launch
            }

            try {
                isSubmitting.value = true
                error.value = null
                successMessage.value = null

                val response = RetrofitInstance.wishlistApi.purchaseItem(
                    authorization = "Bearer $token",
                    id = itemId,
                    request = WishlistPurchaseRequest(
                        createTransaction = createTransaction,
                        transactionCategory = transactionCategory
                    )
                )

                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage(appString(R.string.failed_to_purchase_wishlist_item))
                    return@launch
                }

                successMessage.value = if (createTransaction) {
                    appString(R.string.wishlist_item_purchased_finance)
                } else {
                    appString(R.string.wishlist_item_purchased)
                }

                loadData(showLoader = false)
            } catch (e: Exception) {
                error.value = e.message ?: appString(R.string.unknown_error)
            } finally {
                isSubmitting.value = false
            }
        }
    }

    fun deleteItem(itemId: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = appString(R.string.session_expired_login)
                return@launch
            }

            try {
                isSubmitting.value = true
                error.value = null
                successMessage.value = null

                val response = RetrofitInstance.wishlistApi.deleteItem(
                    authorization = "Bearer $token",
                    id = itemId
                )

                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage(appString(R.string.failed_to_delete_wishlist_item))
                    return@launch
                }

                successMessage.value = appString(R.string.wishlist_item_deleted)
                loadData(showLoader = false)
                onSuccess?.invoke()
            } catch (e: Exception) {
                error.value = e.message ?: appString(R.string.unknown_error)
            } finally {
                isSubmitting.value = false
            }
        }
    }

    private suspend fun loadData(showLoader: Boolean) {
        val token = tokenManager.tokenFlow.first()
        if (token.isNullOrBlank()) {
            error.value = appString(R.string.session_expired_login)
            return
        }

        try {
            if (showLoader) {
                isLoading.value = true
            }

            error.value = null

            val response = RetrofitInstance.wishlistApi.getItems("Bearer $token")
            if (!response.isSuccessful) {
                error.value = response.extractErrorMessage(appString(R.string.failed_to_load_wishlist))
                return
            }

            currentUserId.value = response.body()?.currentUserId.orEmpty()
            items.value = response.body()
                ?.items
                .orEmpty()
                .sortedWith(
                    compareBy<WishlistItemDto> { priorityRank(it.priority) }
                        .thenByDescending { it.createdAt }
                )
        } catch (e: Exception) {
            error.value = e.message ?: appString(R.string.unknown_error)
        } finally {
            if (showLoader) {
                isLoading.value = false
            }
        }
    }

    private fun sanitizePrice(value: String): String {
        val builder = StringBuilder()
        var hasDot = false

        value.forEach { character ->
            when {
                character.isDigit() -> builder.append(character)
                character == '.' && !hasDot -> {
                    builder.append(character)
                    hasDot = true
                }
            }
        }

        return builder.toString()
    }

    private fun priorityRank(value: String): Int {
        return when (value.uppercase()) {
            "HIGH" -> 0
            "MEDIUM" -> 1
            else -> 2
        }
    }
}
