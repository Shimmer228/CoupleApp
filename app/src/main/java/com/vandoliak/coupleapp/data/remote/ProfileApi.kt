package com.vandoliak.coupleapp.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PUT

data class ProfileUpdateRequest(
    val nickname: String?,
    val avatarKey: String?
)

data class MyProfileDto(
    val id: String,
    val email: String,
    val nickname: String?,
    val avatarKey: String?,
    val avatarUrl: String?,
    val points: Int,
    val winStreak: Int,
    val isWeeklyWinner: Boolean,
    val rewardPurchases: List<RewardPurchaseDto>
)

data class PartnerProfileDto(
    val id: String,
    val email: String,
    val nickname: String?,
    val avatarKey: String?,
    val avatarUrl: String?,
    val points: Int,
    val winStreak: Int,
    val isWeeklyWinner: Boolean
)

data class MyProfileResponse(
    val profile: MyProfileDto
)

data class PartnerProfileResponse(
    val profile: PartnerProfileDto
)

interface ProfileApi {

    @GET("api/profile/me")
    suspend fun getMyProfile(
        @Header("Authorization") authorization: String
    ): Response<MyProfileResponse>

    @PUT("api/profile/me")
    suspend fun updateMyProfile(
        @Header("Authorization") authorization: String,
        @Body request: ProfileUpdateRequest
    ): Response<MyProfileResponse>

    @Multipart
    @POST("api/profile/avatar")
    suspend fun uploadAvatar(
        @Header("Authorization") authorization: String,
        @Part avatar: MultipartBody.Part
    ): Response<MyProfileResponse>

    @GET("api/profile/partner")
    suspend fun getPartnerProfile(
        @Header("Authorization") authorization: String
    ): Response<PartnerProfileResponse>
}
