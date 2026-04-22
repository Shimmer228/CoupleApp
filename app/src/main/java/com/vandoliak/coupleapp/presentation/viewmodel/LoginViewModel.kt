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

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    var email = mutableStateOf("")
        private set

    var password = mutableStateOf("")
        private set

    var isLoading = mutableStateOf(false)
        private set

    var error = mutableStateOf<String?>(null)
        private set

    fun onEmailChange(value: String) {
        email.value = value
    }

    fun onPasswordChange(value: String) {
        password.value = value
    }

    fun login(onSuccess: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                if (email.value.isBlank() || password.value.isBlank()) {
                    error.value = "Email and password are required"
                    return@launch
                }

                isLoading.value = true
                error.value = null

                val response = RetrofitInstance.authApi.login(
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
                    error.value = response.extractErrorMessage("Login failed")
                }

            } catch (e: Exception) {
                error.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }
}
