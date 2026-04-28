package com.vandoliak.coupleapp.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.data.local.TokenManager
import com.vandoliak.coupleapp.data.remote.NotificationDto
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.data.remote.extractErrorMessage
import com.vandoliak.coupleapp.presentation.util.appString
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    var notifications = mutableStateOf<List<NotificationDto>>(emptyList())
        private set

    var unreadCount = mutableStateOf(0)
        private set

    var isLoading = mutableStateOf(false)
        private set

    var isSubmitting = mutableStateOf(false)
        private set

    var error = mutableStateOf<String?>(null)
        private set

    fun loadUnreadCount() {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                return@launch
            }

            try {
                val response = RetrofitInstance.notificationApi.getUnreadCount("Bearer $token")
                if (response.isSuccessful) {
                    unreadCount.value = response.body()?.unreadCount ?: 0
                }
            } catch (_: Exception) {
            }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = appString(R.string.session_expired_login)
                return@launch
            }

            try {
                isLoading.value = true
                error.value = null

                val response = RetrofitInstance.notificationApi.getNotifications("Bearer $token")
                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage(appString(R.string.failed_to_load_notifications))
                    return@launch
                }

                notifications.value = response.body()?.notifications.orEmpty()
                unreadCount.value = notifications.value.count { !it.isRead }
            } catch (e: Exception) {
                error.value = e.message ?: appString(R.string.unknown_error)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun markAsRead(notificationId: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = appString(R.string.session_expired_login)
                return@launch
            }

            try {
                isSubmitting.value = true
                error.value = null

                val response = RetrofitInstance.notificationApi.markAsRead("Bearer $token", notificationId)
                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage(appString(R.string.failed_to_mark_notification_read))
                    return@launch
                }

                val updated = response.body()?.notification
                if (updated != null) {
                    notifications.value = notifications.value.map { notification ->
                        if (notification.id == updated.id) updated else notification
                    }
                    unreadCount.value = notifications.value.count { !it.isRead }
                }
                onSuccess?.invoke()
            } catch (e: Exception) {
                error.value = e.message ?: appString(R.string.unknown_error)
            } finally {
                isSubmitting.value = false
            }
        }
    }
}
