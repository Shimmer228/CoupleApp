package com.vandoliak.coupleapp.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.data.local.TokenManager
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.data.remote.WishlistCreateRequest
import com.vandoliak.coupleapp.data.remote.WishlistItemDto
import com.vandoliak.coupleapp.data.remote.WishlistPurchaseRequest
import com.vandoliak.coupleapp.data.remote.extractErrorMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WishlistViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

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

    fun loadWishlist() {
        viewModelScope.launch {
            loadData(showLoader = true)
        }
    }

    fun createItem() {
        val parsedPrice = if (price.value.isBlank()) null else price.value.toDoubleOrNull()

        if (title.value.isBlank()) {
            error.value = "Wishlist title is required"
            return
        }

        if (parsedPrice != null && parsedPrice < 0) {
            error.value = "Price must be 0 or greater"
            return
        }

        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = "Session expired. Please log in again"
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
                    error.value = response.extractErrorMessage("Failed to create wishlist item")
                    return@launch
                }

                successMessage.value = "Wishlist item created"
                title.value = ""
                price.value = ""
                url.value = ""
                priority.value = "MEDIUM"
                category.value = "SELF"
                loadData(showLoader = false)
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isSubmitting.value = false
            }
        }
    }

    fun purchaseItem(itemId: String, createTransaction: Boolean) {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = "Session expired. Please log in again"
                return@launch
            }

            try {
                isSubmitting.value = true
                error.value = null
                successMessage.value = null

                val response = RetrofitInstance.wishlistApi.purchaseItem(
                    authorization = "Bearer $token",
                    id = itemId,
                    request = WishlistPurchaseRequest(createTransaction = createTransaction)
                )

                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage("Failed to purchase wishlist item")
                    return@launch
                }

                successMessage.value = if (createTransaction) {
                    "Item marked as purchased and added to finance"
                } else {
                    "Item marked as purchased"
                }

                loadData(showLoader = false)
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isSubmitting.value = false
            }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = "Session expired. Please log in again"
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
                    error.value = response.extractErrorMessage("Failed to delete wishlist item")
                    return@launch
                }

                successMessage.value = "Wishlist item deleted"
                loadData(showLoader = false)
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isSubmitting.value = false
            }
        }
    }

    private suspend fun loadData(showLoader: Boolean) {
        val token = tokenManager.tokenFlow.first()
        if (token.isNullOrBlank()) {
            error.value = "Session expired. Please log in again"
            return
        }

        try {
            if (showLoader) {
                isLoading.value = true
            }

            error.value = null

            val response = RetrofitInstance.wishlistApi.getItems("Bearer $token")
            if (!response.isSuccessful) {
                error.value = response.extractErrorMessage("Failed to load wishlist")
                return
            }

            items.value = response.body()
                ?.items
                .orEmpty()
                .sortedWith(
                    compareBy<WishlistItemDto> { priorityRank(it.priority) }
                        .thenByDescending { it.createdAt }
                )
        } catch (e: Exception) {
            error.value = e.message ?: "Unknown error"
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
