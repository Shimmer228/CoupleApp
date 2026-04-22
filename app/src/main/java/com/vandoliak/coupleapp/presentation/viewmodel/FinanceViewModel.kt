package com.vandoliak.coupleapp.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.data.local.TokenManager
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.data.remote.TransactionCreateRequest
import com.vandoliak.coupleapp.data.remote.TransactionDto
import com.vandoliak.coupleapp.data.remote.extractErrorMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FinanceViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    var title = mutableStateOf("")
        private set

    var amount = mutableStateOf("")
        private set

    var type = mutableStateOf("EXPENSE")
        private set

    var category = mutableStateOf("SELF")
        private set

    var transactions = mutableStateOf<List<TransactionDto>>(emptyList())
        private set

    var balance = mutableStateOf(0.0)
        private set

    var direction = mutableStateOf("PARTNER_OWES")
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

    fun onAmountChange(value: String) {
        amount.value = sanitizeAmount(value)
    }

    fun onTypeChange(value: String) {
        type.value = value
    }

    fun onCategoryChange(value: String) {
        category.value = value
    }

    fun loadFinance() {
        viewModelScope.launch {
            loadData(showLoader = true)
        }
    }

    fun createTransaction() {
        val parsedAmount = amount.value.toDoubleOrNull()

        if (title.value.isBlank()) {
            error.value = "Transaction title is required"
            return
        }

        if (parsedAmount == null || parsedAmount <= 0) {
            error.value = "Amount must be greater than 0"
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

                val response = RetrofitInstance.financeApi.createTransaction(
                    authorization = "Bearer $token",
                    request = TransactionCreateRequest(
                        title = title.value.trim(),
                        amount = parsedAmount,
                        type = type.value,
                        category = category.value
                    )
                )

                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage("Failed to create transaction")
                    return@launch
                }

                successMessage.value = "Transaction created successfully"
                title.value = ""
                amount.value = ""
                type.value = "EXPENSE"
                category.value = "SELF"
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
            val authorization = "Bearer $token"

            val transactionsResponse = RetrofitInstance.financeApi.getTransactions(authorization)
            if (!transactionsResponse.isSuccessful) {
                error.value = transactionsResponse.extractErrorMessage("Failed to load transactions")
                return
            }

            val balanceResponse = RetrofitInstance.financeApi.getBalance(authorization)
            if (!balanceResponse.isSuccessful) {
                error.value = balanceResponse.extractErrorMessage("Failed to load balance")
                return
            }

            transactions.value = transactionsResponse.body()?.transactions.orEmpty()
            balance.value = balanceResponse.body()?.balance ?: 0.0
            direction.value = balanceResponse.body()?.direction ?: "PARTNER_OWES"
        } catch (e: Exception) {
            error.value = e.message ?: "Unknown error"
        } finally {
            if (showLoader) {
                isLoading.value = false
            }
        }
    }

    private fun sanitizeAmount(value: String): String {
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
}
