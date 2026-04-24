package com.vandoliak.coupleapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import retrofit2.http.Path

data class EventCreateRequest(
    val title: String,
    val description: String?,
    val date: String
)

data class EventUpdateRequest(
    val title: String? = null,
    val description: String? = null,
    val date: String? = null
)

data class EventUserDto(
    val id: String,
    val email: String
)

data class EventDto(
    val id: String,
    val title: String,
    val description: String?,
    val date: String,
    val createdBy: EventUserDto,
    val createdAt: String
)

data class EventResponse(
    val event: EventDto
)

data class EventListResponse(
    val events: List<EventDto>
)

data class EventDeleteResponse(
    val deletedId: String
)

interface EventApi {

    @POST("api/events/create")
    suspend fun createEvent(
        @Header("Authorization") authorization: String,
        @Body request: EventCreateRequest
    ): Response<EventResponse>

    @PUT("api/events/{id}")
    suspend fun updateEvent(
        @Header("Authorization") authorization: String,
        @Path("id") eventId: String,
        @Body request: EventUpdateRequest
    ): Response<EventResponse>

    @DELETE("api/events/{id}")
    suspend fun deleteEvent(
        @Header("Authorization") authorization: String,
        @Path("id") eventId: String
    ): Response<EventDeleteResponse>

    @GET("api/events")
    suspend fun getEventsForDate(
        @Header("Authorization") authorization: String,
        @Query("date") date: String
    ): Response<EventListResponse>

    @GET("api/events/all")
    suspend fun getAllEvents(
        @Header("Authorization") authorization: String
    ): Response<EventListResponse>
}
