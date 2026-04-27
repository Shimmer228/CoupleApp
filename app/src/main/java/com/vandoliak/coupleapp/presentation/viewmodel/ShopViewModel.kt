package com.vandoliak.coupleapp.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.data.local.TokenManager
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.data.remote.RewardDto
import com.vandoliak.coupleapp.data.remote.extractErrorMessage
import com.vandoliak.coupleapp.presentation.util.appString
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ShopViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    var rewards = mutableStateOf<List<RewardDto>>(emptyList())
        private set

    var currentUserPoints = mutableStateOf(0)
        private set

    var currentUserWinStreak = mutableStateOf(0)
        private set

    var isLoading = mutableStateOf(false)
        private set

    var isSubmitting = mutableStateOf(false)
        private set

    var error = mutableStateOf<String?>(null)
        private set

    var successMessage = mutableStateOf<String?>(null)
        private set

    fun loadShop() {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = appString(R.string.session_expired_login)
                return@launch
            }

            try {
                isLoading.value = true
                error.value = null

                val response = RetrofitInstance.rewardApi.getRewards("Bearer $token")
                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage(appString(R.string.failed_to_load_reward_shop))
                    return@launch
                }

                currentUserPoints.value = response.body()?.currentUserPoints ?: 0
                currentUserWinStreak.value = response.body()?.currentUserWinStreak ?: 0
                rewards.value = response.body()?.rewards.orEmpty()
            } catch (e: Exception) {
                error.value = e.message ?: appString(R.string.unknown_error)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun buyReward(rewardId: String) {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = appString(R.string.session_expired_login)
                return@launch
            }

            try {
                isSubmitting.value = true
                error.value = null
                successMessage.value = null

                val response = RetrofitInstance.rewardApi.buyReward(
                    authorization = "Bearer $token",
                    rewardId = rewardId
                )

                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage(appString(R.string.failed_to_buy_reward))
                    return@launch
                }

                currentUserPoints.value = response.body()?.currentUserPoints ?: currentUserPoints.value
                successMessage.value = appString(R.string.reward_purchased)
                loadShop()
            } catch (e: Exception) {
                error.value = e.message ?: appString(R.string.unknown_error)
            } finally {
                isSubmitting.value = false
            }
        }
    }
}
