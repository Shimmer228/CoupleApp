package com.vandoliak.coupleapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class AuthRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: String,
    val email: String,
    val pairId: String?,
    val points: Int
)

interface AuthApi {

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>
}
