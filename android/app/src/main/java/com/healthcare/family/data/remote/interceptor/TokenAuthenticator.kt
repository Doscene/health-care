package com.healthcare.family.data.remote.interceptor

import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Authenticator：当收到 401 时自动刷新 Token 并重试请求。
 * 使用 Lazy<TokenRefreshProvider> 打破循环依赖。
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val authInterceptor: AuthInterceptor,
    private val tokenRefreshProvider: Lazy<TokenRefreshProvider>,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // 避免无限重试
        if (response.request.header("X-Retry") != null) {
            return null
        }

        // 防止并发刷新
        synchronized(lock) {
            // 检查 token 是否已经被其他线程刷新过
            val currentToken = authInterceptor.accessToken
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (currentToken != null && currentToken != requestToken) {
                // token 已被刷新，直接用新 token 重试
                return response.request.newBuilder()
                    .removeHeader("Authorization")
                    .addHeader("Authorization", "Bearer $currentToken")
                    .addHeader("X-Retry", "1")
                    .build()
            }

            // 执行刷新
            val success = runBlocking {
                tokenRefreshProvider.get().refreshToken()
            }
            if (!success) return null
        }

        val newToken = authInterceptor.accessToken ?: return null
        return response.request.newBuilder()
            .removeHeader("Authorization")
            .addHeader("Authorization", "Bearer $newToken")
            .addHeader("X-Retry", "1")
            .build()
    }
}
