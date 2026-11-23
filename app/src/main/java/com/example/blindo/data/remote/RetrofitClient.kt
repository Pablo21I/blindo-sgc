package com.example.blindo.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    @Volatile
    private var apiService: BlindoApiService? = null

    fun init(context: Context) {
        if (apiService != null) return
        synchronized(this) {
            if (apiService != null) return
            val appContext = context.applicationContext

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(appContext))
                .addInterceptor(logging)
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(ApiConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(BlindoApiService::class.java)
        }
    }

    val api: BlindoApiService
        get() = apiService ?: throw IllegalStateException("RetrofitClient no ha sido inicializado con Context")
}
