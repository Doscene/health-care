package com.healthcare.family.data.repository

import com.healthcare.family.data.local.TokenManager
import com.healthcare.family.data.remote.api.AuthApi
import com.healthcare.family.data.remote.dto.LoginRequest
import com.healthcare.family.data.remote.dto.RefreshTokenRequest
import com.healthcare.family.data.remote.dto.SendCodeRequest
import com.healthcare.family.data.remote.dto.TokenResponse
import com.healthcare.family.data.remote.interceptor.AuthInterceptor
import com.healthcare.family.data.remote.interceptor.TokenRefreshProvider
import com.healthcare.family.util.toUserFriendlyMessage
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证仓库：封装登录、验证码、Token 刷新逻辑。
 * 同时实现 TokenRefreshProvider 供 OkHttp Authenticator 调用。
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val authInterceptor: AuthInterceptor,
) : TokenRefreshProvider {

    /** 发送短信验证码 */
    suspend fun sendCode(phone: String): Result<String> {
        return try {
            val response = authApi.sendCode(SendCodeRequest(phone))
            if (response.code == 0 || response.code == 200) {
                Result.success(response.data?.message ?: "验证码已发送")
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    /** 手机号 + 验证码登录 */
    suspend fun login(phone: String, code: String): Result<TokenResponse> {
        return try {
            val response = authApi.login(LoginRequest(phone, code))
            if ((response.code == 0 || response.code == 200) && response.data != null) {
                tokenManager.saveTokens(response.data.accessToken, response.data.refreshToken)
                response.data.userId?.let { tokenManager.saveUserId(it) }
                authInterceptor.accessToken = response.data.accessToken
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    /** 刷新 Token（实现 TokenRefreshProvider） */
    override suspend fun refreshToken(): Boolean {
        return try {
            val currentRefresh = tokenManager.getRefreshTokenSync() ?: return false
            val response = authApi.refreshToken(RefreshTokenRequest(currentRefresh))
            if ((response.code == 0 || response.code == 200) && response.data != null) {
                tokenManager.saveTokens(response.data.accessToken, response.data.refreshToken)
                authInterceptor.accessToken = response.data.accessToken
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /** 退出登录 */
    suspend fun logout() {
        tokenManager.clearAll()
        authInterceptor.accessToken = null
    }

    /** 是否已登录 */
    suspend fun isLoggedIn(): Boolean {
        return tokenManager.accessToken.firstOrNull() != null
    }

    /** 恢复 Token 到内存（App 启动时调用） */
    suspend fun restoreToken() {
        val token = tokenManager.getAccessTokenSync()
        if (!token.isNullOrEmpty()) {
            authInterceptor.accessToken = token
        }
    }
}
