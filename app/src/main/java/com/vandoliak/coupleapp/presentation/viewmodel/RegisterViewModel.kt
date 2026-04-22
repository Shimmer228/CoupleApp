package com.vandoliak.coupleapp.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.data.local.TokenManager
import com.vandoliak.coupleapp.data.remote.AuthRequest
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.data.remote.extractErrorMessage
import kotlinx.coroutines.launch

class RegisterViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    var email = mutableStateOf("")
        private set

    var password = mutableStateOf("")
        private set

    var confirmPassword = mutableStateOf("")
        private set

    var error = mutableStateOf<String?>(null)
        private set

    var isLoading = mutableStateOf(false)
        private set

    fun onEmailChange(value: String) {
        email.value = value
    }

    fun onPasswordChange(value: String) {
        password.value = value
    }

    fun onConfirmPasswordChange(value: String) {
        confirmPassword.value = value
    }

    fun register(onSuccess: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                error.value = null

                if (email.value.isBlank() || password.value.isBlank() || confirmPassword.value.isBlank()) {
                    error.value = "All fields are required"
                    return@launch
                }

                if (password.value != confirmPassword.value) {
                    error.value = "Passwords do not match"
                    return@launch
                }

                isLoading.value = true

                val response = RetrofitInstance.authApi.register(
                    AuthRequest(email.value, password.value)
                )

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    val token = authResponse?.token

                    token?.let {
                        tokenManager.saveSession(it, authResponse.user.pairId)
                        onSuccess(!authResponse.user.pairId.isNullOrBlank())
                    } ?: run {
                        error.value = "Server returned an empty response"
                    }
                } else {
                    error.value = response.extractErrorMessage("Registration failed")
                }

            } catch (e: Exception) {
                error.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }
}
