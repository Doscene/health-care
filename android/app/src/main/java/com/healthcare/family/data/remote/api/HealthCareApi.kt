package com.healthcare.family.data.remote.api

import com.healthcare.family.data.remote.dto.ApiResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface HealthCareApi {

    @GET("health")
    suspend fun healthCheck(): ApiResponse<Unit>

    // ==================== 用户 ====================

    /** 获取当前用户信息 */
    @GET("user/me")
    suspend fun getCurrentUser(): ApiResponse<UserDto>

    /** 更新用户角色 */
    @PUT("user/role")
    suspend fun updateRole(@Body request: UpdateRoleRequest): ApiResponse<UserDto>

    /** 更新用户资料 */
    @PUT("user/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiResponse<UserDto>

    // ==================== 家庭圈 ====================

    /** 创建家庭 */
    @POST("family")
    suspend fun createFamily(@Body request: CreateFamilyRequest): ApiResponse<FamilyDto>

    /** 获取我的家庭列表 */
    @GET("family/my")
    suspend fun getMyFamilies(): ApiResponse<List<FamilyDto>>

    /** 验证邀请码 */
    @POST("family/invite/verify")
    suspend fun verifyInviteCode(@Body request: VerifyInviteRequest): ApiResponse<InvitePreviewDto>

    /** 加入家庭 */
    @POST("family/join")
    suspend fun joinFamily(@Body request: JoinFamilyRequest): ApiResponse<JoinFamilyResult>

    /** 获取家庭成员列表 */
    @GET("family/{familyId}/members")
    suspend fun getFamilyMembers(@Path("familyId") familyId: String): ApiResponse<List<MemberDto>>

    /** 重新生成邀请码 */
    @POST("family/{familyId}/invite/regenerate")
    suspend fun regenerateInviteCode(@Path("familyId") familyId: String): ApiResponse<InviteCodeDto>

    // ==================== 复诊计划 ====================

    /** 获取复诊计划列表 */
    @GET("appointment")
    suspend fun getAppointments(): ApiResponse<List<PlanDto>>

    /** 添加复诊计划 */
    @POST("appointment")
    suspend fun addAppointment(@Body request: AddPlanRequest): ApiResponse<PlanDto>

    /** 删除复诊计划 */
    @DELETE("appointment/{planId}")
    suspend fun deleteAppointment(@Path("planId") planId: String): ApiResponse<Unit>
}

// ==================== DTOs ====================

data class UserDto(
    val id: String,
    val phone: String,
    val name: String,
    val selfRole: String,
    val diseases: List<String>,
    val avatar: String? = null,
    val age: Int? = null,
)

data class UpdateRoleRequest(
    val selfRole: String,
    val diseases: List<String> = emptyList(),
)

data class UpdateProfileRequest(
    val name: String? = null,
    val avatar: String? = null,
    val age: Int? = null,
)

data class CreateFamilyRequest(
    val name: String,
)

data class FamilyDto(
    val id: String,
    val name: String,
    val inviteCode: String?,
    val inviteCodeExpiresAt: String?,
    val memberCount: Int,
    val myRole: String? = null,
)

data class VerifyInviteRequest(
    val code: String,
)

data class InvitePreviewDto(
    val familyName: String,
    val memberCount: Int,
)

data class JoinFamilyRequest(
    val code: String,
    val role: String,
)

data class JoinFamilyResult(
    val message: String,
    val familyId: String,
)

data class InviteCodeDto(
    val inviteCode: String,
    val inviteCodeExpiresAt: String,
)

data class MemberDto(
    val userId: String,
    val name: String,
    val avatar: String? = null,
    val role: String,
    val selfRole: String,
    val diseases: List<String>,
    val joinedAt: String,
)

data class PlanDto(
    val id: String,
    val hospital: String,
    val department: String,
    val date: String,
    val reminderDays: Int,
    val status: String? = null,
    val notes: String? = null,
)

data class AddPlanRequest(
    val hospital: String,
    val department: String,
    val date: String,
    val reminderDays: Int = 3,
    val notes: String? = null,
)
