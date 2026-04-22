package com.vandoliak.coupleapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class TransactionCreateRequest(
    val title: String,
    val amount: Double,
    val type: String,
    val category: String
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
    val createdBy: TransactionUserDto,
    val createdAt: String
)

data class TransactionResponse(
    val transaction: TransactionDto
)

data class TransactionListResponse(
    val currentUserId: String,
    val transactions: List<TransactionDto>
)

data class TransactionBalanceResponse(
    val balance: Double,
    val direction: String
)

interface FinanceApi {

    @POST("api/transactions/create")
    suspend fun createTransaction(
        @Header("Authorization") authorization: String,
        @Body request: TransactionCreateRequest
    ): Response<TransactionResponse>

    @GET("api/transactions")
    suspend fun getTransactions(
        @Header("Authorization") authorization: String
    ): Response<TransactionListResponse>

    @GET("api/transactions/balance")
    suspend fun getBalance(
        @Header("Authorization") authorization: String
    ): Response<TransactionBalanceResponse>
}
