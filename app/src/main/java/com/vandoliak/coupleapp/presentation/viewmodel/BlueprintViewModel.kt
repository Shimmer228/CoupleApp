package com.vandoliak.coupleapp.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.data.local.TokenManager
import com.vandoliak.coupleapp.data.remote.BlueprintCreateRequest
import com.vandoliak.coupleapp.data.remote.BlueprintDto
import com.vandoliak.coupleapp.data.remote.BlueprintUseRequest
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.data.remote.extractErrorMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BlueprintViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    var blueprints = mutableStateOf<List<BlueprintDto>>(emptyList())
        private set

    var isLoading = mutableStateOf(false)
        private set

    var isSubmitting = mutableStateOf(false)
        private set

    var error = mutableStateOf<String?>(null)
        private set

    var successMessage = mutableStateOf<String?>(null)
        private set

    fun loadBlueprints() {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = "Session expired. Please log in again"
                return@launch
            }

            try {
                isLoading.value = true
                error.value = null

                val response = RetrofitInstance.blueprintApi.getBlueprints("Bearer $token")
                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage("Failed to load blueprints")
                    return@launch
                }

                blueprints.value = response.body()?.blueprints.orEmpty()
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun createBlueprint(
        title: String,
        description: String?,
        type: String,
        defaultPoints: Int?,
        defaultTime: String?,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = "Session expired. Please log in again"
                return@launch
            }

            try {
                isSubmitting.value = true
                error.value = null
                successMessage.value = null

                val response = RetrofitInstance.blueprintApi.createBlueprint(
                    authorization = "Bearer $token",
                    request = BlueprintCreateRequest(
                        title = title,
                        description = description,
                        type = type,
                        defaultPoints = defaultPoints,
                        defaultTime = defaultTime
                    )
                )

                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage("Failed to create blueprint")
                    return@launch
                }

                successMessage.value = "Blueprint saved"
                loadBlueprints()
                onSuccess?.invoke()
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isSubmitting.value = false
            }
        }
    }

    fun deleteBlueprint(blueprintId: String) {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = "Session expired. Please log in again"
                return@launch
            }

            try {
                isSubmitting.value = true
                error.value = null
                successMessage.value = null

                val response = RetrofitInstance.blueprintApi.deleteBlueprint(
                    authorization = "Bearer $token",
                    blueprintId = blueprintId
                )

                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage("Failed to delete blueprint")
                    return@launch
                }

                successMessage.value = "Blueprint deleted"
                loadBlueprints()
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isSubmitting.value = false
            }
        }
    }

    fun useBlueprint(
        blueprintId: String,
        date: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = "Session expired. Please log in again"
                return@launch
            }

            try {
                isSubmitting.value = true
                error.value = null
                successMessage.value = null

                val response = RetrofitInstance.blueprintApi.useBlueprint(
                    authorization = "Bearer $token",
                    blueprintId = blueprintId,
                    request = BlueprintUseRequest(date = date)
                )

                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage("Failed to use blueprint")
                    return@launch
                }

                successMessage.value = "Blueprint applied"
                onSuccess?.invoke()
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isSubmitting.value = false
            }
        }
    }
}
