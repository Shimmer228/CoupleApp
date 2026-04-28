package com.vandoliak.coupleapp.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class NotificationDto(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean,
    val createdAt: String
)

data class NotificationListResponse(
    val notifications: List<NotificationDto>
)

data class NotificationUnreadCountResponse(
    val unreadCount: Int
)

data class NotificationResponse(
    val notification: NotificationDto
)

interface NotificationApi {

    @GET("api/notifications")
    suspend fun getNotifications(
        @Header("Authorization") authorization: String
    ): Response<NotificationListResponse>

    @GET("api/notifications/unread-count")
    suspend fun getUnreadCount(
        @Header("Authorization") authorization: String
    ): Response<NotificationUnreadCountResponse>

    @POST("api/notifications/{id}/read")
    suspend fun markAsRead(
        @Header("Authorization") authorization: String,
        @Path("id") notificationId: String
    ): Response<NotificationResponse>
}
