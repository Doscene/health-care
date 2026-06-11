package com.healthcare.family.data.remote.interceptor

import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val prefs: SharedPreferences,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = prefs.getString(KEY_JWT_TOKEN, null)
        val request = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder()
                .addHeader(HEADER_AUTHORIZATION, "$TOKEN_PREFIX $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }

    companion object {
        const val KEY_JWT_TOKEN = "jwt_access_token"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val TOKEN_PREFIX = "Bearer"
    }
}
