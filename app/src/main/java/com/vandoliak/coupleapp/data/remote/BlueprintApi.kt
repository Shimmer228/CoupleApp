package com.vandoliak.coupleapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class BlueprintCreateRequest(
    val title: String,
    val description: String?,
    val type: String,
    val defaultPoints: Int?,
    val defaultTime: String?
)

data class BlueprintUseRequest(
    val date: String
)

data class BlueprintUserDto(
    val id: String,
    val email: String
)

data class BlueprintDto(
    val id: String,
    val title: String,
    val description: String?,
    val type: String,
    val defaultPoints: Int?,
    val defaultDueTime: String?,
    val defaultTime: String?,
    val createdBy: BlueprintUserDto,
    val createdAt: String
)

data class BlueprintListResponse(
    val blueprints: List<BlueprintDto>
)

data class BlueprintCreateResponse(
    val blueprint: BlueprintDto
)

data class BlueprintDeleteResponse(
    val deletedId: String
)

data class BlueprintUseResponse(
    val type: String,
    val task: TaskDto?,
    val event: EventDto?,
    val currentUserPoints: Int?
)

interface BlueprintApi {

    @GET("api/blueprints")
    suspend fun getBlueprints(
        @Header("Authorization") authorization: String
    ): Response<BlueprintListResponse>

    @POST("api/blueprints/create")
    suspend fun createBlueprint(
        @Header("Authorization") authorization: String,
        @Body request: BlueprintCreateRequest
    ): Response<BlueprintCreateResponse>

    @DELETE("api/blueprints/{id}")
    suspend fun deleteBlueprint(
        @Header("Authorization") authorization: String,
        @Path("id") blueprintId: String
    ): Response<BlueprintDeleteResponse>

    @POST("api/blueprints/{id}/use")
    suspend fun useBlueprint(
        @Header("Authorization") authorization: String,
        @Path("id") blueprintId: String,
        @Body request: BlueprintUseRequest
    ): Response<BlueprintUseResponse>
}
