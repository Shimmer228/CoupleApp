package com.vandoliak.coupleapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class TransactionCreateRequest(
    val title: String,
    val amount: Double,
    val type: String,
    val scope: String,
    val transactionCategory: String
)

data class TransactionUserDto(
    val id: String,
    val email: String
)

data class TransactionDto(
    val id: String,
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val transactionCategory: String,
    val createdBy: TransactionUserDto,
    val createdAt: String
)

data class TransactionResponse(
    val transaction: TransactionDto
)

data class TransactionUpdateRequest(
    val title: String,
    val amount: Double,
    val type: String,
    val scope: String,
    val transactionCategory: String
)

data class TransactionListResponse(
    val currentUserId: String,
    val transactions: List<TransactionDto>
)

data class TransactionBalanceResponse(
    val amount: Double,
    val direction: String
)

data class TransactionCategorySummaryDto(
    val category: String,
    val total: Double,
    val percentage: Double
)

data class TransactionSummaryResponse(
    val totalBudget: Double,
    val balance: TransactionBalanceResponse,
    val expenseByCategory: List<TransactionCategorySummaryDto>,
    val incomeByCategory: List<TransactionCategorySummaryDto>
)

data class TransactionDeleteResponse(
    val deletedId: String
)

interface FinanceApi {

    @POST("api/transactions/create")
    suspend fun createTransaction(
        @Header("Authorization") authorization: String,
        @Body request: TransactionCreateRequest
    ): Response<TransactionResponse>

    @PUT("api/transactions/{id}")
    suspend fun updateTransaction(
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
        @Body request: TransactionUpdateRequest
    ): Response<TransactionResponse>

    @DELETE("api/transactions/{id}")
    suspend fun deleteTransaction(
        @Header("Authorization") authorization: String,
        @Path("id") id: String
    ): Response<TransactionDeleteResponse>

    @GET("api/transactions")
    suspend fun getTransactions(
        @Header("Authorization") authorization: String
    ): Response<TransactionListResponse>

    @GET("api/transactions/balance")
    suspend fun getBalance(
        @Header("Authorization") authorization: String
    ): Response<TransactionBalanceResponse>

    @GET("api/transactions/summary")
    suspend fun getSummary(
        @Header("Authorization") authorization: String
    ): Response<TransactionSummaryResponse>
}
