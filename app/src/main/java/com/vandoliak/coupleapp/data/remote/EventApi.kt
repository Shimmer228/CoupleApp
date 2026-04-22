package com.vandoliak.coupleapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

data class EventCreateRequest(
    val title: String,
    val description: String?,
    val date: String
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

interface EventApi {

    @POST("api/events/create")
    suspend fun createEvent(
        @Header("Authorization") authorization: String,
        @Body request: EventCreateRequest
    ): Response<EventResponse>

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
