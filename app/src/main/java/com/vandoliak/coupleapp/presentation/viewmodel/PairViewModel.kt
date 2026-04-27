package com.vandoliak.coupleapp.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.data.local.TokenManager
import com.vandoliak.coupleapp.data.remote.JoinPairRequest
import com.vandoliak.coupleapp.data.remote.PairResponse
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.data.remote.extractErrorMessage
import com.vandoliak.coupleapp.presentation.util.appString
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Response

class PairViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    var joinCode = mutableStateOf("")
        private set

    var currentPairId = mutableStateOf<String?>(null)
        private set

    var createdJoinCode = mutableStateOf<String?>(null)
        private set

    var isLoading = mutableStateOf(false)
        private set

    var error = mutableStateOf<String?>(null)
        private set

    var successMessage = mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            currentPairId.value = tokenManager.pairIdFlow.first()
        }
    }

    fun onJoinCodeChange(value: String) {
        joinCode.value = value.trim().uppercase()
    }

    fun clearMessage() {
        successMessage.value = null
    }

    fun createPair() {
        viewModelScope.launch {
            executePairRequest(
                emptyCodeMessage = null,
                successText = appString(R.string.pair_created_success)
            ) { authorization ->
                RetrofitInstance.pairApi.createPair(authorization)
            }
        }
    }

    fun joinPair(onSuccess: () -> Unit) {
        viewModelScope.launch {
            executePairRequest(
                emptyCodeMessage = appString(R.string.enter_join_code),
                successText = appString(R.string.pair_joined_success)
            ) { authorization ->
                RetrofitInstance.pairApi.joinPair(
                    authorization = authorization,
                    request = JoinPairRequest(joinCode.value)
                )
            }?.let {
                onSuccess()
            }
        }
    }

    private suspend fun executePairRequest(
        emptyCodeMessage: String?,
        successText: String,
        request: suspend (String) -> Response<PairResponse>
    ): String? {
        if (currentPairId.value != null) {
            successMessage.value = appString(R.string.pair_already_connected)
            return currentPairId.value
        }

        if (emptyCodeMessage != null && joinCode.value.isBlank()) {
            error.value = emptyCodeMessage
            return null
        }

        return try {
            isLoading.value = true
            error.value = null
            successMessage.value = null
            if (emptyCodeMessage == null) {
                createdJoinCode.value = null
            }

            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = appString(R.string.session_expired_login)
                return null
            }

            val response = request("Bearer $token")
            if (!response.isSuccessful) {
                error.value = response.extractErrorMessage(appString(R.string.pair_request_failed))
                return null
            }

            val body = response.body()
            val pairId = body?.pairId
            if (pairId.isNullOrBlank()) {
                error.value = appString(R.string.server_empty_pair_code)
                return null
            }

            tokenManager.savePairId(pairId)
            currentPairId.value = pairId
            createdJoinCode.value = body?.joinCode
            successMessage.value = successText
            pairId
        } catch (e: Exception) {
            error.value = e.message ?: appString(R.string.unknown_error)
            null
        } finally {
            isLoading.value = false
        }
    }
}
