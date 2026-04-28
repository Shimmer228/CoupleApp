package com.vandoliak.coupleapp.presentation.viewmodel

import android.app.Application
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.data.local.TokenManager
import com.vandoliak.coupleapp.data.remote.MyProfileDto
import com.vandoliak.coupleapp.data.remote.PartnerProfileDto
import com.vandoliak.coupleapp.data.remote.ProfileUpdateRequest
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.data.remote.RewardPurchaseDto
import com.vandoliak.coupleapp.data.remote.extractErrorMessage
import com.vandoliak.coupleapp.presentation.util.appString
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    val avatarOptions = listOf("cat", "fox", "bear", "star", "moon", "heart")

    var myProfile = mutableStateOf<MyProfileDto?>(null)
        private set

    var partnerProfile = mutableStateOf<PartnerProfileDto?>(null)
        private set

    var purchases = mutableStateOf<List<RewardPurchaseDto>>(emptyList())
        private set

    var localAvatarPreviewBytes = mutableStateOf<ByteArray?>(null)
        private set

    var nickname = mutableStateOf("")
        private set

    var avatarKey = mutableStateOf("heart")
        private set

    var isLoading = mutableStateOf(false)
        private set

    var isSaving = mutableStateOf(false)
        private set

    var isUploadingAvatar = mutableStateOf(false)
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
        localAvatarPreviewBytes.value = null
    }

    fun loadProfile() {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = appString(R.string.session_expired_login)
                return@launch
            }

            try {
                isLoading.value = true
                error.value = null
                val authorization = "Bearer $token"

                val myProfileResponse = RetrofitInstance.profileApi.getMyProfile(authorization)
                if (!myProfileResponse.isSuccessful) {
                    error.value = myProfileResponse.extractErrorMessage(appString(R.string.failed_to_load_profile))
                    return@launch
                }

                val myProfileBody = myProfileResponse.body()?.profile
                myProfile.value = myProfileBody
                nickname.value = myProfileBody?.nickname.orEmpty()
                avatarKey.value = myProfileBody?.avatarKey?.lowercase() ?: avatarOptions.first()
                purchases.value = myProfileBody?.rewardPurchases.orEmpty()

                val partnerResponse = RetrofitInstance.profileApi.getPartnerProfile(authorization)
                partnerProfile.value = if (partnerResponse.isSuccessful) {
                    partnerResponse.body()?.profile
                } else {
                    null
                }

                val purchasesResponse = RetrofitInstance.rewardApi.getPurchases(authorization)
                if (purchasesResponse.isSuccessful) {
                    purchases.value = purchasesResponse.body()?.purchases.orEmpty()
                }
            } catch (e: Exception) {
                error.value = e.message ?: appString(R.string.unknown_error)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun saveProfile() {
        if (nickname.value.length > 40) {
            error.value = appString(R.string.nickname_too_long)
            return
        }

        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = appString(R.string.session_expired_login)
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
                    error.value = response.extractErrorMessage(appString(R.string.failed_to_update_profile))
                    return@launch
                }

                myProfile.value = response.body()?.profile
                nickname.value = myProfile.value?.nickname.orEmpty()
                avatarKey.value = myProfile.value?.avatarKey?.lowercase() ?: avatarOptions.first()
                successMessage.value = appString(R.string.profile_updated)
                loadProfile()
            } catch (e: Exception) {
                error.value = e.message ?: appString(R.string.unknown_error)
            } finally {
                isSaving.value = false
            }
        }
    }

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = appString(R.string.session_expired_login)
                return@launch
            }

            val context = getApplication<Application>()
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)?.takeIf { it.isNotBlank() } ?: "image/*"
            val bytes = try {
                contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (_: Exception) {
                null
            }

            if (bytes == null || bytes.isEmpty()) {
                error.value = appString(R.string.failed_to_read_selected_image)
                return@launch
            }

            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                ?.takeIf { it.isNotBlank() }
                ?: "jpg"

            try {
                isUploadingAvatar.value = true
                error.value = null
                successMessage.value = null
                localAvatarPreviewBytes.value = bytes

                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData(
                    "avatar",
                    "avatar.$extension",
                    requestBody
                )

                val response = RetrofitInstance.profileApi.uploadAvatar(
                    authorization = "Bearer $token",
                    avatar = filePart
                )

                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage(appString(R.string.failed_to_upload_avatar))
                    return@launch
                }

                myProfile.value = response.body()?.profile
                successMessage.value = appString(R.string.avatar_uploaded)
                loadProfile()
            } catch (e: Exception) {
                localAvatarPreviewBytes.value = null
                error.value = e.message ?: appString(R.string.unknown_error)
            } finally {
                isUploadingAvatar.value = false
            }
        }
    }
}
