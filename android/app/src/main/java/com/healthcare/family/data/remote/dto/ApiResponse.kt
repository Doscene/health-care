package com.healthcare.family.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("data") val data: T?,
    @SerializedName("message") val message: String,
)
