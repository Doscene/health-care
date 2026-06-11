package com.healthcare.family.data.remote.api

import com.healthcare.family.data.remote.dto.ApiResponse
import retrofit2.http.GET

interface HealthCareApi {

    @GET("health")
    suspend fun healthCheck(): ApiResponse<Unit>
}
