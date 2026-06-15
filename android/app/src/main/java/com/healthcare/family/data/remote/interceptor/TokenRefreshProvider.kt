package com.healthcare.family.data.remote.interceptor

/**
 * Token 刷新提供者接口，由 AuthRepository 实现。
 * 用于打破 NetworkModule ↔ AuthRepository 的循环依赖。
 */
interface TokenRefreshProvider {
    suspend fun refreshToken(): Boolean
}
