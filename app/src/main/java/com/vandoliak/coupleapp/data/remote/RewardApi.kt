package com.vandoliak.coupleapp.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class RewardDto(
    val id: String,
    val title: String,
    val description: String?,
    val cost: Int,
    val minStreak: Int,
    val isActive: Boolean,
    val createdAt: String,
    val isUnlocked: Boolean
)

data class RewardPurchaseRewardDto(
    val id: String,
    val title: String,
    val description: String?,
    val cost: Int,
    val minStreak: Int
)

data class RewardPurchaseDto(
    val id: String,
    val createdAt: String,
    val reward: RewardPurchaseRewardDto
)

data class RewardListResponse(
    val currentUserPoints: Int,
    val currentUserWinStreak: Int,
    val rewards: List<RewardDto>
)

data class RewardBuyResponse(
    val purchase: RewardPurchaseDto,
    val currentUserPoints: Int
)

data class RewardPurchaseListResponse(
    val currentUserPoints: Int,
    val purchases: List<RewardPurchaseDto>
)

interface RewardApi {

    @GET("api/rewards")
    suspend fun getRewards(
        @Header("Authorization") authorization: String
    ): Response<RewardListResponse>

    @POST("api/rewards/buy/{id}")
    suspend fun buyReward(
        @Header("Authorization") authorization: String,
        @Path("id") rewardId: String
    ): Response<RewardBuyResponse>

    @GET("api/rewards/purchases")
    suspend fun getPurchases(
        @Header("Authorization") authorization: String
    ): Response<RewardPurchaseListResponse>
}
