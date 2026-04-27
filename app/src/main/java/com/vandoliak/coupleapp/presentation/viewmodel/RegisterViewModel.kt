package com.vandoliak.coupleapp.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.data.local.TokenManager
import com.vandoliak.coupleapp.data.remote.AuthRequest
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.data.remote.extractErrorMessage
import com.vandoliak.coupleapp.presentation.util.appString
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
                    error.value = appString(R.string.all_fields_required)
                    return@launch
                }

                if (password.value != confirmPassword.value) {
                    error.value = appString(R.string.passwords_do_not_match)
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
                        RetrofitInstance.markSessionActive()
                        onSuccess(!authResponse.user.pairId.isNullOrBlank())
                    } ?: run {
                        error.value = appString(R.string.server_empty_response)
                    }
                } else {
                    error.value = response.extractErrorMessage(appString(R.string.registration_failed))
                }

            } catch (e: Exception) {
                error.value = e.message ?: appString(R.string.unknown_error)
            } finally {
                isLoading.value = false
            }
        }
    }
}
