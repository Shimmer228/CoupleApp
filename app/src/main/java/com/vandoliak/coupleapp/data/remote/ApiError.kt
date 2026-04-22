package com.vandoliak.coupleapp.data.remote

import com.google.gson.Gson
import retrofit2.Response

data class ApiErrorResponse(
    val message: String?
)

fun Response<*>.extractErrorMessage(defaultMessage: String): String {
    val rawBody = errorBody()?.string()

    if (rawBody.isNullOrBlank()) {
        return defaultMessage
    }

    return try {
        Gson()
            .fromJson(rawBody, ApiErrorResponse::class.java)
            ?.message
            ?.takeIf { it.isNotBlank() }
            ?: defaultMessage
    } catch (_: Exception) {
        defaultMessage
    }
}
