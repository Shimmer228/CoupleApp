package com.vandoliak.coupleapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class TaskCreateRequest(
    val title: String,
    val points: Int,
    val dueDate: String?
)

data class TaskUpdateRequest(
    val title: String? = null,
    val dueDate: String? = null
)

data class TaskUserDto(
    val id: String,
    val email: String
)

data class TaskDto(
    val id: String,
    val title: String,
    val bank: Int,
    val status: String,
    val assignedTo: TaskUserDto,
    val createdBy: TaskUserDto,
    val completionRequestedBy: TaskUserDto?,
    val dueDate: String?,
    val createdAt: String
)

data class TaskListResponse(
    val currentUserId: String,
    val currentUserPoints: Int,
    val tasks: List<TaskDto>
)

data class TaskActionResponse(
    val task: TaskDto,
    val currentUserPoints: Int
)

data class TaskDeleteResponse(
    val deletedId: String,
    val currentUserPoints: Int
)

interface TaskApi {

    @GET("api/tasks")
    suspend fun getTasks(
        @Header("Authorization") authorization: String
    ): Response<TaskListResponse>

    @POST("api/tasks/create")
    suspend fun createTask(
        @Header("Authorization") authorization: String,
        @Body request: TaskCreateRequest
    ): Response<TaskActionResponse>

    @PUT("api/tasks/{id}")
    suspend fun updateTask(
        @Header("Authorization") authorization: String,
        @Path("id") taskId: String,
        @Body request: TaskUpdateRequest
    ): Response<TaskActionResponse>

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(
        @Header("Authorization") authorization: String,
        @Path("id") taskId: String
    ): Response<TaskDeleteResponse>

    @POST("api/tasks/request-completion/{id}")
    suspend fun requestCompletion(
        @Header("Authorization") authorization: String,
        @Path("id") taskId: String
    ): Response<TaskActionResponse>

    @POST("api/tasks/confirm-completion/{id}")
    suspend fun confirmCompletion(
        @Header("Authorization") authorization: String,
        @Path("id") taskId: String
    ): Response<TaskActionResponse>

    @POST("api/tasks/reject-completion/{id}")
    suspend fun rejectCompletion(
        @Header("Authorization") authorization: String,
        @Path("id") taskId: String
    ): Response<TaskActionResponse>

    @POST("api/tasks/return/{id}")
    suspend fun returnTask(
        @Header("Authorization") authorization: String,
        @Path("id") taskId: String
    ): Response<TaskActionResponse>

    @POST("api/tasks/fail/{id}")
    suspend fun failTask(
        @Header("Authorization") authorization: String,
        @Path("id") taskId: String
    ): Response<TaskActionResponse>
}
