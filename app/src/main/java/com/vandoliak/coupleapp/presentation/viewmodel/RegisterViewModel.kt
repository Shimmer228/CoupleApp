package com.vandoliak.coupleapp.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.data.local.TokenManager
import com.vandoliak.coupleapp.data.remote.AuthRequest
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
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

    fun register(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                error.value = null

                if (password.value != confirmPassword.value) {
                    error.value = "Passwords do not match"
                    return@launch
                }

                isLoading.value = true

                val response = RetrofitInstance.api.register(
                    AuthRequest(email.value, password.value)
                )

                if (response.isSuccessful) {
                    val token = response.body()?.token

                    token?.let {
                        tokenManager.saveToken(it)
                        onSuccess()
                    }
                } else {
                    error.value = "Registration failed"
                }

            } catch (e: Exception) {
                error.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }
}