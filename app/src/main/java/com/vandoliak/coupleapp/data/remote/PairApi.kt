package com.vandoliak.coupleapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class JoinPairRequest(
    val joinCode: String
)

data class PairResponse(
    val pairId: String,
    val joinCode: String?
)

data class LeavePairResponse(
    val leftPair: Boolean
)

interface PairApi {

    @POST("api/pair/create")
    suspend fun createPair(
        @Header("Authorization") authorization: String
    ): Response<PairResponse>

    @POST("api/pair/join")
    suspend fun joinPair(
        @Header("Authorization") authorization: String,
        @Body request: JoinPairRequest
    ): Response<PairResponse>

    @POST("api/pair/leave")
    suspend fun leavePair(
        @Header("Authorization") authorization: String
    ): Response<LeavePairResponse>
}
