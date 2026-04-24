package com.vandoliak.coupleapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class WishlistCreateRequest(
    val title: String,
    val url: String?,
    val price: Double?,
    val priority: String,
    val category: String
)

data class WishlistPurchaseRequest(
    val createTransaction: Boolean
)

data class WishlistUserDto(
    val id: String,
    val email: String
)

data class WishlistItemDto(
    val id: String,
    val title: String,
    val url: String?,
    val price: Double?,
    val priority: String,
    val category: String,
    val isPurchased: Boolean,
    val createdBy: WishlistUserDto,
    val createdAt: String
)

data class WishlistCreateResponse(
    val item: WishlistItemDto
)

data class WishlistListResponse(
    val currentUserId: String,
    val items: List<WishlistItemDto>
)

data class WishlistPurchaseResponse(
    val item: WishlistItemDto
)

data class WishlistDeleteResponse(
    val deletedId: String
)

interface WishlistApi {

    @POST("api/wishlist/create")
    suspend fun createItem(
        @Header("Authorization") authorization: String,
        @Body request: WishlistCreateRequest
    ): Response<WishlistCreateResponse>

    @GET("api/wishlist")
    suspend fun getItems(
        @Header("Authorization") authorization: String
    ): Response<WishlistListResponse>

    @POST("api/wishlist/purchase/{id}")
    suspend fun purchaseItem(
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
        @Body request: WishlistPurchaseRequest
    ): Response<WishlistPurchaseResponse>

    @DELETE("api/wishlist/{id}")
    suspend fun deleteItem(
        @Header("Authorization") authorization: String,
        @Path("id") id: String
    ): Response<WishlistDeleteResponse>
}
