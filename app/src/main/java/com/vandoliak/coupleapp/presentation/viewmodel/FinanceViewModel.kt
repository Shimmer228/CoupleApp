package com.vandoliak.coupleapp.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.data.local.TokenManager
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.data.remote.TransactionCategorySummaryDto
import com.vandoliak.coupleapp.data.remote.TransactionCreateRequest
import com.vandoliak.coupleapp.data.remote.TransactionDto
import com.vandoliak.coupleapp.data.remote.TransactionUpdateRequest
import com.vandoliak.coupleapp.data.remote.extractErrorMessage
import com.vandoliak.coupleapp.presentation.util.appString
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FinanceViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    val scopeOptions = listOf("SELF", "PARTNER", "SHARED")
    val typeOptions = listOf("EXPENSE", "INCOME")
    private val expenseCategories = listOf(
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
    private val incomeCategories = listOf(
        "SALARY",
        "BONUS",
        "GIFT",
        "REFUND",
        "SIDE_JOB",
        "OTHER"
    )

    var currentUserId = mutableStateOf("")
        private set

    var title = mutableStateOf("")
        private set

    var amount = mutableStateOf("")
        private set

    var type = mutableStateOf("EXPENSE")
        private set

    var scope = mutableStateOf("SELF")
        private set

    var transactionCategory = mutableStateOf("OTHER")
        private set

    var transactions = mutableStateOf<List<TransactionDto>>(emptyList())
        private set

    var pendingConfirmations = mutableStateOf<List<TransactionDto>>(emptyList())
        private set

    var totalBudget = mutableStateOf(0.0)
        private set

    var balanceAmount = mutableStateOf(0.0)
        private set

    var balanceDirection = mutableStateOf("SETTLED")
        private set

    var expenseByCategory = mutableStateOf<List<TransactionCategorySummaryDto>>(emptyList())
        private set

    var incomeByCategory = mutableStateOf<List<TransactionCategorySummaryDto>>(emptyList())
        private set

    var isLoading = mutableStateOf(false)
        private set

    var isSubmitting = mutableStateOf(false)
        private set

    var error = mutableStateOf<String?>(null)
        private set

    var successMessage = mutableStateOf<String?>(null)
        private set

    val availableTransactionCategories: List<String>
        get() = if (type.value == "INCOME") incomeCategories else expenseCategories

    fun onTitleChange(value: String) {
        title.value = value
    }

    fun onAmountChange(value: String) {
        amount.value = sanitizeAmount(value)
    }

    fun onTypeChange(value: String) {
        type.value = value
        if (!availableTransactionCategories.contains(transactionCategory.value)) {
            transactionCategory.value = "OTHER"
        }
    }

    fun onScopeChange(value: String) {
        scope.value = value
    }

    fun onTransactionCategoryChange(value: String) {
        transactionCategory.value = value
    }

    fun populateEditor(transaction: TransactionDto) {
        title.value = transaction.title
        amount.value = transaction.amount.toString()
        type.value = transaction.type
        scope.value = transaction.category
        transactionCategory.value = transaction.transactionCategory
        if (!availableTransactionCategories.contains(transactionCategory.value)) {
            transactionCategory.value = "OTHER"
        }
    }

    fun resetForm() {
        title.value = ""
        amount.value = ""
        type.value = "EXPENSE"
        scope.value = "SELF"
        transactionCategory.value = "OTHER"
        error.value = null
    }

    fun loadFinance() {
        viewModelScope.launch {
            loadData(showLoader = true)
        }
    }

    fun createTransaction(onSuccess: (() -> Unit)? = null) {
        val parsedAmount = amount.value.toDoubleOrNull()

        if (title.value.isBlank()) {
            error.value = appString(R.string.transaction_title_required)
            return
        }

        if (parsedAmount == null || parsedAmount <= 0) {
            error.value = appString(R.string.amount_greater_than_zero)
            return
        }

        if (!availableTransactionCategories.contains(transactionCategory.value)) {
            error.value = appString(R.string.valid_category_required)
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

                val response = RetrofitInstance.financeApi.createTransaction(
                    authorization = "Bearer $token",
                    request = TransactionCreateRequest(
                        title = title.value.trim(),
                        amount = parsedAmount,
                        type = type.value,
                        scope = scope.value,
                        transactionCategory = transactionCategory.value
                    )
                )

                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage(appString(R.string.failed_to_create_transaction))
                    return@launch
                }

                successMessage.value = appString(R.string.transaction_created_successfully)
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

    fun updateTransaction(transactionId: String, onSuccess: (() -> Unit)? = null) {
        val parsedAmount = amount.value.toDoubleOrNull()

        if (title.value.isBlank()) {
            error.value = appString(R.string.transaction_title_required)
            return
        }

        if (parsedAmount == null || parsedAmount <= 0) {
            error.value = appString(R.string.amount_greater_than_zero)
            return
        }

        if (!availableTransactionCategories.contains(transactionCategory.value)) {
            error.value = appString(R.string.valid_category_required)
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

                val response = RetrofitInstance.financeApi.updateTransaction(
                    authorization = "Bearer $token",
                    id = transactionId,
                    request = TransactionUpdateRequest(
                        title = title.value.trim(),
                        amount = parsedAmount,
                        type = type.value,
                        scope = scope.value,
                        transactionCategory = transactionCategory.value
                    )
                )

                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage(appString(R.string.failed_to_update_transaction))
                    return@launch
                }

                successMessage.value = appString(R.string.transaction_updated)
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

    fun deleteTransaction(transactionId: String, onSuccess: (() -> Unit)? = null) {
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

                val response = RetrofitInstance.financeApi.deleteTransaction(
                    authorization = "Bearer $token",
                    id = transactionId
                )

                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage(appString(R.string.failed_to_delete_transaction))
                    return@launch
                }

                successMessage.value = appString(R.string.transaction_deleted)
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
            val authorization = "Bearer $token"

            val transactionsResponse = RetrofitInstance.financeApi.getTransactions(authorization)
            if (!transactionsResponse.isSuccessful) {
                error.value = transactionsResponse.extractErrorMessage(appString(R.string.failed_to_load_transactions))
                return
            }

            val summaryResponse = RetrofitInstance.financeApi.getSummary(authorization)
            if (!summaryResponse.isSuccessful) {
                error.value = summaryResponse.extractErrorMessage(appString(R.string.failed_to_load_finance_summary))
                return
            }

            currentUserId.value = transactionsResponse.body()?.currentUserId.orEmpty()
            transactions.value = transactionsResponse.body()?.transactions.orEmpty()
            pendingConfirmations.value = transactions.value.filter { transaction ->
                transaction.status == "PENDING_CONFIRMATION" && transaction.createdBy.id != currentUserId.value
            }
            val summary = summaryResponse.body()
            totalBudget.value = summary?.totalBudget ?: 0.0
            balanceAmount.value = summary?.balance?.amount ?: 0.0
            balanceDirection.value = summary?.balance?.direction ?: "SETTLED"
            expenseByCategory.value = summary?.expenseByCategory.orEmpty()
            incomeByCategory.value = summary?.incomeByCategory.orEmpty()
        } catch (e: Exception) {
            error.value = e.message ?: appString(R.string.unknown_error)
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

    fun confirmTransaction(transactionId: String, onSuccess: (() -> Unit)? = null) {
        runTransactionAction(
            successText = appString(R.string.transaction_confirmed),
            onSuccess = onSuccess
        ) { authorization ->
            RetrofitInstance.financeApi.confirmTransaction(authorization, transactionId)
        }
    }

    fun rejectTransaction(transactionId: String, onSuccess: (() -> Unit)? = null) {
        runTransactionAction(
            successText = appString(R.string.transaction_rejected),
            onSuccess = onSuccess
        ) { authorization ->
            RetrofitInstance.financeApi.rejectTransaction(authorization, transactionId)
        }
    }

    private fun runTransactionAction(
        successText: String,
        onSuccess: (() -> Unit)? = null,
        request: suspend (String) -> retrofit2.Response<*>
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

                val response = request("Bearer $token")
                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage(appString(R.string.finance_action_failed))
                    return@launch
                }

                successMessage.value = successText
                loadData(showLoader = false)
                onSuccess?.invoke()
            } catch (e: Exception) {
                error.value = e.message ?: appString(R.string.unknown_error)
            } finally {
                isSubmitting.value = false
            }
        }
    }
}
