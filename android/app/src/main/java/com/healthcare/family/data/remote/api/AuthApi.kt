package com.healthcare.family.data.remote.api

import com.healthcare.family.data.remote.dto.ApiResponse
import com.healthcare.family.data.remote.dto.LoginRequest
import com.healthcare.family.data.remote.dto.RefreshTokenRequest
import com.healthcare.family.data.remote.dto.SendCodeRequest
import com.healthcare.family.data.remote.dto.SendCodeResponse
import com.healthcare.family.data.remote.dto.TokenResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/send-code")
    suspend fun sendCode(@Body request: SendCodeRequest): ApiResponse<SendCodeResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<TokenResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): ApiResponse<TokenResponse>
}
