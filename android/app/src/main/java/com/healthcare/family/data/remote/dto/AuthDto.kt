package com.healthcare.family.data.remote.dto

import com.google.gson.annotations.SerializedName

/** 发送验证码请求 */
data class SendCodeRequest(
    @SerializedName("phone") val phone: String,
)

/** 登录请求 */
data class LoginRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("code") val code: String,
)

/** 刷新 Token 请求 */
data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String,
)

/** Token 响应 */
data class TokenResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
)

/** 发送验证码响应 */
data class SendCodeResponse(
    @SerializedName("message") val message: String,
)
