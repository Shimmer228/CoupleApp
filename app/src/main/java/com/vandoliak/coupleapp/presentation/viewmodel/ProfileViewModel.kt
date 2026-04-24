package com.vandoliak.coupleapp.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.data.local.TokenManager
import com.vandoliak.coupleapp.data.remote.MyProfileDto
import com.vandoliak.coupleapp.data.remote.PartnerProfileDto
import com.vandoliak.coupleapp.data.remote.ProfileUpdateRequest
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.data.remote.RewardPurchaseDto
import com.vandoliak.coupleapp.data.remote.extractErrorMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    val avatarOptions = listOf("cat", "fox", "bear", "star", "moon", "heart")

    var myProfile = mutableStateOf<MyProfileDto?>(null)
        private set

    var partnerProfile = mutableStateOf<PartnerProfileDto?>(null)
        private set

    var purchases = mutableStateOf<List<RewardPurchaseDto>>(emptyList())
        private set

    var nickname = mutableStateOf("")
        private set

    var avatarKey = mutableStateOf("heart")
        private set

    var isLoading = mutableStateOf(false)
        private set

    var isSaving = mutableStateOf(false)
        private set

    var error = mutableStateOf<String?>(null)
        private set

    var successMessage = mutableStateOf<String?>(null)
        private set

    fun onNicknameChange(value: String) {
        nickname.value = value
    }

    fun onAvatarKeyChange(value: String) {
        avatarKey.value = value
    }

    fun loadProfile() {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = "Session expired. Please log in again"
                return@launch
            }

            try {
                isLoading.value = true
                error.value = null
                val authorization = "Bearer $token"

                val myProfileResponse = RetrofitInstance.profileApi.getMyProfile(authorization)
                if (!myProfileResponse.isSuccessful) {
                    error.value = myProfileResponse.extractErrorMessage("Failed to load profile")
                    return@launch
                }

                val myProfileBody = myProfileResponse.body()?.profile
                myProfile.value = myProfileBody
                nickname.value = myProfileBody?.nickname.orEmpty()
                avatarKey.value = myProfileBody?.avatarKey?.lowercase() ?: avatarOptions.first()
                purchases.value = myProfileBody?.rewardPurchases.orEmpty()

                val partnerResponse = RetrofitInstance.profileApi.getPartnerProfile(authorization)
                if (partnerResponse.isSuccessful) {
                    partnerProfile.value = partnerResponse.body()?.profile
                } else {
                    partnerProfile.value = null
                }

                val purchasesResponse = RetrofitInstance.rewardApi.getPurchases(authorization)
                if (purchasesResponse.isSuccessful) {
                    purchases.value = purchasesResponse.body()?.purchases.orEmpty()
                }
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun saveProfile() {
        if (nickname.value.length > 40) {
            error.value = "Nickname must be 40 characters or fewer"
            return
        }

        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = "Session expired. Please log in again"
                return@launch
            }

            try {
                isSaving.value = true
                error.value = null
                successMessage.value = null

                val response = RetrofitInstance.profileApi.updateMyProfile(
                    authorization = "Bearer $token",
                    request = ProfileUpdateRequest(
                        nickname = nickname.value.trim().ifBlank { null },
                        avatarKey = avatarKey.value
                    )
                )

                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage("Failed to update profile")
                    return@launch
                }

                myProfile.value = response.body()?.profile
                nickname.value = myProfile.value?.nickname.orEmpty()
                avatarKey.value = myProfile.value?.avatarKey?.lowercase() ?: avatarOptions.first()
                successMessage.value = "Profile updated"
                loadProfile()
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isSaving.value = false
            }
        }
    }
}
