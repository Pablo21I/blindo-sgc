package com.example.blindo.data.remote

import android.content.Context
import com.example.blindo.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor de Retrofit / OkHttp que inyecta automáticamente:
 * - Accept: application/json
 * - Authorization: Bearer <token_sanctum> almacenado en SharedPreferences
 */
class AuthInterceptor(private val appContext: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = SessionManager.getAuthToken(appContext)

        val requestBuilder = originalRequest.newBuilder()
            .header("Accept", "application/json")

        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
