package com.healthcare.family.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自动在请求头添加 JWT Authorization。
 * 使用内存中的 volatile token，由 TokenManager 在登录/登出时同步更新。
 */
@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    @Volatile
    var accessToken: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = accessToken
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
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val TOKEN_PREFIX = "Bearer"
    }
}
