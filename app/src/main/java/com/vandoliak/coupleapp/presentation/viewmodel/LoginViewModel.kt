package com.vandoliak.coupleapp.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.data.remote.AuthRequest
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

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

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                isLoading.value = true
                error.value = null

                val response = RetrofitInstance.api.login(
                    AuthRequest(email.value, password.value)
                )

                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    error.value = "Login failed"
                }

            } catch (e: Exception) {
                error.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }
}