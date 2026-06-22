package com.healthcare.family.data.repository

import com.healthcare.family.data.remote.api.HealthCareApi
import com.healthcare.family.data.remote.api.UpdateProfileRequest
import com.healthcare.family.data.remote.api.UpdateRoleRequest
import com.healthcare.family.data.remote.api.UserDto
import com.healthcare.family.util.toUserFriendlyMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api: HealthCareApi,
) {

    suspend fun getCurrentUser(): Result<UserDto> {
        return try {
            val resp = api.getCurrentUser()
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun updateRole(selfRole: String, diseases: List<String>): Result<UserDto> {
        return try {
            val resp = api.updateRole(UpdateRoleRequest(selfRole, diseases))
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun updateProfile(name: String? = null, avatar: String? = null, age: Int? = null): Result<UserDto> {
        return try {
            val resp = api.updateProfile(UpdateProfileRequest(name, avatar, age))
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }
}
