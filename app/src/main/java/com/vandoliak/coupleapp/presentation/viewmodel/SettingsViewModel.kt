package com.vandoliak.coupleapp.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.data.local.AppCurrency
import com.vandoliak.coupleapp.data.local.AppLanguage
import com.vandoliak.coupleapp.data.local.AppSettingsManager
import com.vandoliak.coupleapp.data.local.SessionDestination
import com.vandoliak.coupleapp.data.local.SessionEvents
import com.vandoliak.coupleapp.data.local.ThemeMode
import com.vandoliak.coupleapp.data.local.TokenManager
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.data.remote.extractErrorMessage
import com.vandoliak.coupleapp.presentation.util.appString
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsManager = AppSettingsManager(app)
    private val tokenManager = TokenManager(app)

    val themeOptions = ThemeMode.entries
    val languageOptions = AppLanguage.entries
    val currencyOptions = AppCurrency.entries

    var themeMode = mutableStateOf(ThemeMode.SYSTEM)
        private set

    var language = mutableStateOf(AppLanguage.ENGLISH)
        private set

    var currency = mutableStateOf(AppCurrency.UAH)
        private set

    var isSubmitting = mutableStateOf(false)
        private set

    var notice = mutableStateOf<SettingsNotice?>(null)
        private set

    var error = mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            settingsManager.themeModeFlow.collect { themeMode.value = it }
        }
        viewModelScope.launch {
            settingsManager.languageFlow.collect { language.value = it }
        }
        viewModelScope.launch {
            settingsManager.currencyFlow.collect { currency.value = it }
        }
    }

    fun setThemeMode(value: ThemeMode) {
        viewModelScope.launch {
            settingsManager.setThemeMode(value)
        }
    }

    fun setLanguage(value: AppLanguage) {
        viewModelScope.launch {
            settingsManager.setLanguage(value)
            notice.value = SettingsNotice.RESTART_LANGUAGE
        }
    }

    fun setCurrency(value: AppCurrency) {
        viewModelScope.launch {
            settingsManager.setCurrency(value)
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearSession()
            RetrofitInstance.markSessionActive()
            SessionEvents.emit(SessionDestination.LOGIN)
        }
    }

    fun leavePair() {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                SessionEvents.emit(SessionDestination.LOGIN)
                return@launch
            }

            try {
                isSubmitting.value = true
                error.value = null
                notice.value = null

                val response = RetrofitInstance.pairApi.leavePair("Bearer $token")
                if (!response.isSuccessful) {
                    if (response.code() == 401) {
                        return@launch
                    }
                    error.value = response.extractErrorMessage(appString(R.string.failed_to_leave_pair))
                    return@launch
                }

                tokenManager.clearPairId()
                notice.value = SettingsNotice.PAIR_DISCONNECTED
                SessionEvents.emit(SessionDestination.PAIR)
            } catch (e: Exception) {
                error.value = e.message ?: appString(R.string.unknown_error)
            } finally {
                isSubmitting.value = false
            }
        }
    }
}

enum class SettingsNotice {
    RESTART_LANGUAGE,
    PAIR_DISCONNECTED
}
