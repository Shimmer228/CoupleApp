package com.vandoliak.coupleapp.data.remote

import android.content.Context
import com.vandoliak.coupleapp.data.local.SessionDestination
import com.vandoliak.coupleapp.data.local.SessionEvents
import com.vandoliak.coupleapp.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicBoolean

object RetrofitInstance {

    //private const val BASE_URL = "http://10.0.2.2:5000/"
    private const val BASE_URL = "http://localhost:5000/"

    private lateinit var appContext: Context
    private val didTriggerLogout = AtomicBoolean(false)

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        handleAuthFailure(response)
        response
    }

    fun initialize(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
        }
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(authInterceptor)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun handleAuthFailure(response: Response) {
        val bodyMessage = try {
            response.peekBody(Long.MAX_VALUE).string()
        } catch (_: Exception) {
            ""
        }

        val isExpired = response.code == 401 ||
            bodyMessage.contains("invalid or expired token", ignoreCase = true)

        if (!isExpired || !::appContext.isInitialized) {
            return
        }

        if (didTriggerLogout.compareAndSet(false, true)) {
            runBlocking {
                TokenManager(appContext).clearSession()
            }
            SessionEvents.emit(SessionDestination.LOGIN)
        }
    }

    fun markSessionActive() {
        didTriggerLogout.set(false)
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val pairApi: PairApi by lazy {
        retrofit.create(PairApi::class.java)
    }

    val taskApi: TaskApi by lazy {
        retrofit.create(TaskApi::class.java)
    }

    val eventApi: EventApi by lazy {
        retrofit.create(EventApi::class.java)
    }

    val financeApi: FinanceApi by lazy {
        retrofit.create(FinanceApi::class.java)
    }

    val wishlistApi: WishlistApi by lazy {
        retrofit.create(WishlistApi::class.java)
    }

    val profileApi: ProfileApi by lazy {
        retrofit.create(ProfileApi::class.java)
    }

    val rewardApi: RewardApi by lazy {
        retrofit.create(RewardApi::class.java)
    }

    val blueprintApi: BlueprintApi by lazy {
        retrofit.create(BlueprintApi::class.java)
    }

    val notificationApi: NotificationApi by lazy {
        retrofit.create(NotificationApi::class.java)
    }

    fun resolveUrl(rawUrl: String?): String? {
        val value = rawUrl?.trim().orEmpty()
        if (value.isBlank()) {
            return null
        }

        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value
        }

        val normalizedPath = if (value.startsWith("/")) value.drop(1) else value
        return BASE_URL + normalizedPath
    }
}
