package com.vandoliak.coupleapp.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    //private const val BASE_URL = "http://10.0.2.2:5000/"
    private const val BASE_URL = "http://localhost:5000/"
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
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
}
