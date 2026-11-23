package com.example.blindo.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Crea y configura el cliente HTTP (Retrofit + OkHttp) una sola vez para toda la app.
 * Aquí se conecta el AuthInterceptor para mandar el token de Sanctum en cada petición.
 */
object RetrofitClient {

    @Volatile
    private var apiService: BlindoApiService? = null

    // Inicializa Retrofit con tiempos límite de espera y el interceptor de autenticación
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
