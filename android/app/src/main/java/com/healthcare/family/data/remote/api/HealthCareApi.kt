package com.healthcare.family.data.remote.api

import com.healthcare.family.data.remote.dto.ApiResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

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

    /** 更新成员角色 */
    @PUT("family/{familyId}/members/{memberId}/role")
    suspend fun updateMemberRole(
        @Path("familyId") familyId: String,
        @Path("memberId") memberId: String,
        @Body request: UpdateMemberRoleRequest,
    ): ApiResponse<Unit>

    /** 更新成员备注昵称 */
    @PUT("family/{familyId}/members/{memberId}/nickname")
    suspend fun updateMemberNickname(
        @Path("familyId") familyId: String,
        @Path("memberId") memberId: String,
        @Body request: UpdateMemberNicknameRequest,
    ): ApiResponse<Unit>

    /** 移除家庭成员 */
    @DELETE("family/{familyId}/members/{memberId}")
    suspend fun removeMember(
        @Path("familyId") familyId: String,
        @Path("memberId") memberId: String,
    ): ApiResponse<Unit>

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

    // ==================== 首页 ====================

    /** 获取患者首页数据 */
    @GET("home/patient")
    suspend fun getPatientHome(): ApiResponse<PatientHomeDto>

    /** 获取家庭健康概览 */
    @GET("home/family")
    suspend fun getFamilyHome(): ApiResponse<FamilyHomeDto>

    // ==================== 健康记录 ====================

    /** 添加血压记录 */
    @POST("record/bp")
    suspend fun addBpRecord(@Body request: AddBpRecordRequest): ApiResponse<BpRecordDto>

    /** 获取血压记录列表 */
    @GET("record/bp")
    suspend fun getBpRecords(@Query("limit") limit: Int = 30): ApiResponse<List<BpRecordDto>>

    /** 删除血压记录 */
    @DELETE("record/bp/{recordId}")
    suspend fun deleteBpRecord(@Path("recordId") recordId: String): ApiResponse<Unit>

    /** 添加血糖记录 */
    @POST("record/bg")
    suspend fun addBgRecord(@Body request: AddBgRecordRequest): ApiResponse<BgRecordDto>

    /** 获取血糖记录列表 */
    @GET("record/bg")
    suspend fun getBgRecords(@Query("limit") limit: Int = 30): ApiResponse<List<BgRecordDto>>

    /** 删除血糖记录 */
    @DELETE("record/bg/{recordId}")
    suspend fun deleteBgRecord(@Path("recordId") recordId: String): ApiResponse<Unit>

    // ==================== 用药管理 ====================

    /** 获取用药列表 */
    @GET("medication")
    suspend fun getMedications(@Query("status") status: String? = null): ApiResponse<List<MedicationDto>>

    /** 添加用药计划 */
    @POST("medication")
    suspend fun addMedication(@Body request: AddMedicationRequest): ApiResponse<MedicationDto>

    /** 更新用药计划 */
    @PUT("medication/{medicationId}")
    suspend fun updateMedication(
        @Path("medicationId") medicationId: String,
        @Body request: UpdateMedicationRequest,
    ): ApiResponse<MedicationDto>

    /** 删除用药计划 */
    @DELETE("medication/{medicationId}")
    suspend fun deleteMedication(@Path("medicationId") medicationId: String): ApiResponse<Unit>

    // ==================== 风险预警 ====================

    /** 获取预警列表 */
    @GET("alert")
    suspend fun getAlerts(@Query("status") status: String? = null): ApiResponse<List<AlertDto>>

    /** 更新预警状态 */
    @PUT("alert/{alertId}/status")
    suspend fun updateAlertStatus(
        @Path("alertId") alertId: String,
        @Body request: UpdateAlertStatusRequest,
    ): ApiResponse<Unit>

    /** 提交问询回答 */
    @POST("alert/{alertId}/inquiry")
    suspend fun submitInquiry(
        @Path("alertId") alertId: String,
        @Body request: InquiryResponseDto,
    ): ApiResponse<InquiryResponseDto>

    /** 添加紧急联系人 */
    @POST("alert/contact")
    suspend fun addEmergencyContact(@Body request: AddContactRequest): ApiResponse<ContactDto>

    /** 获取紧急联系人列表 */
    @GET("alert/contact")
    suspend fun getEmergencyContacts(): ApiResponse<List<ContactDto>>

    /** 更新紧急联系人 */
    @PUT("alert/contact/{contactId}")
    suspend fun updateEmergencyContact(
        @Path("contactId") contactId: String,
        @Body request: UpdateContactRequest,
    ): ApiResponse<ContactDto>

    /** 删除紧急联系人 */
    @DELETE("alert/contact/{contactId}")
    suspend fun deleteEmergencyContact(@Path("contactId") contactId: String): ApiResponse<Unit>

    /** 设为互为紧急联系人 */
    @POST("alert/contact/mutual")
    suspend fun setMutualContact(@Body request: MutualContactRequest): ApiResponse<Unit>

    // ==================== 急救包 ====================

    /** 获取急救包物品列表 */
    @GET("alert/kit")
    suspend fun getFirstAidKit(): ApiResponse<List<FirstAidKitDto>>

    /** 添加急救包物品 */
    @POST("alert/kit")
    suspend fun addFirstAidItem(@Body request: FirstAidKitRequest): ApiResponse<FirstAidKitDto>

    /** 删除急救包物品 */
    @DELETE("alert/kit/{itemId}")
    suspend fun deleteFirstAidItem(@Path("itemId") itemId: String): ApiResponse<Unit>

    // ==================== 急救指南 ====================

    /** 获取急救指南 */
    @GET("alert/guide")
    suspend fun getEmergencyGuides(@Query("type") type: String? = null): ApiResponse<List<FirstAidGuideDto>>

    // ==================== 饮食管理 ====================

    /** 获取食谱列表 */
    @GET("diet/recipe")
    suspend fun getRecipes(@Query("suitableFor") suitableFor: String? = null): ApiResponse<List<RecipeDto>>

    /** 获取食谱详情 */
    @GET("diet/recipe/{recipeId}")
    suspend fun getRecipe(@Path("recipeId") recipeId: String): ApiResponse<RecipeDto>

    /** 获取今日菜单 */
    @GET("diet/menu")
    suspend fun getDailyMenu(): ApiResponse<DailyMenuDto>

    /** 查询食材替换 */
    @GET("diet/substitution")
    suspend fun getSubstitutions(@Query("ingredient") ingredient: String): ApiResponse<SubstitutionDto>
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

data class UpdateMemberRoleRequest(
    val role: String,
)

data class UpdateMemberNicknameRequest(
    val nickname: String,
)

// ==================== 首页 DTOs ====================

data class PatientHomeDto(
    val user: PatientUserInfo,
    val latestBp: BpSummaryDto? = null,
    val latestBg: BgSummaryDto? = null,
    val todayMedications: List<TodayMedDto> = emptyList(),
    val activeAlerts: List<AlertSummaryDto> = emptyList(),
    val todayAppointments: List<AppointmentSummaryDto> = emptyList(),
)

data class PatientUserInfo(
    val name: String,
    val selfRole: String,
    val diseases: List<String>,
)

data class BpSummaryDto(
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int? = null,
    val recordedAt: String? = null,
)

data class BgSummaryDto(
    val type: String,
    val value: Double,
    val recordedAt: String? = null,
)

data class TodayMedDto(
    val id: String,
    val name: String,
    val dosagePerTime: Int,
    val frequencyPerDay: Int,
    val timeSlots: Any? = null,
)

data class AlertSummaryDto(
    val id: String,
    val level: String,
    val triggerType: String,
    val triggerValue: String,
    val createdAt: String? = null,
)

data class AppointmentSummaryDto(
    val id: String,
    val hospital: String,
    val department: String,
    val date: String,
)

data class FamilyHomeDto(
    val families: List<FamilyOverviewDto> = emptyList(),
)

data class FamilyOverviewDto(
    val familyId: String,
    val familyName: String,
    val myRole: String,
    val members: List<MemberHealthDto> = emptyList(),
)

data class MemberHealthDto(
    val userId: String,
    val name: String,
    val selfRole: String,
    val diseases: List<String>,
    val latestBp: BpSummaryDto? = null,
    val latestBg: BgSummaryDto? = null,
    val activeAlertCount: Int = 0,
    val hasHighAlert: Boolean = false,
)

// ==================== 健康记录 DTOs ====================

data class AddBpRecordRequest(
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int? = null,
    val inputMethod: String = "manual",
)

data class BpRecordDto(
    val id: String,
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int? = null,
    val inputMethod: String,
    val recordedAt: String,
)

data class AddBgRecordRequest(
    val type: String,
    val value: Double,
    val inputMethod: String = "manual",
)

data class BgRecordDto(
    val id: String,
    val type: String,
    val value: Double,
    val inputMethod: String,
    val recordedAt: String,
)

// ==================== 用药管理 DTOs ====================

data class MedicationDto(
    val id: String,
    val name: String,
    val specification: String,
    val dosagePerTime: Int,
    val frequencyPerDay: Int,
    val timeSlots: Any? = null,
    val remindTimes: Any? = null,
    val startDate: String,
    val endDate: String? = null,
    val status: String = "active",
    val notes: String? = null,
)

data class AddMedicationRequest(
    val name: String,
    val specification: String,
    val dosagePerTime: Int,
    val frequencyPerDay: Int,
    val timeSlots: List<String>,
    val remindTimes: List<String>,
    val startDate: String,
    val endDate: String? = null,
    val notes: String? = null,
)

data class UpdateMedicationRequest(
    val name: String? = null,
    val specification: String? = null,
    val dosagePerTime: Int? = null,
    val frequencyPerDay: Int? = null,
    val timeSlots: List<String>? = null,
    val remindTimes: List<String>? = null,
    val endDate: String? = null,
    val notes: String? = null,
    val status: String? = null,
)

// ==================== 风险预警 DTOs ====================

data class AlertDto(
    val id: String,
    val level: String,
    val triggerType: String,
    val triggerValue: String,
    val status: String,
    val notifiedContacts: Any? = null,
    val createdAt: String,
)

data class UpdateAlertStatusRequest(
    val status: String,
)

data class AddContactRequest(
    val name: String,
    val phone: String,
    val relation: String,
    val priority: Int? = null,
)

data class ContactDto(
    val id: String,
    val name: String,
    val phone: String,
    val relation: String,
    val priority: Int,
    val isMutual: Boolean = false,
)

data class UpdateContactRequest(
    val name: String? = null,
    val phone: String? = null,
    val relation: String? = null,
    val priority: Int? = null,
)

// ==================== 饮食 DTOs ====================

data class RecipeDto(
    val id: String,
    val name: String,
    val image: String? = null,
    val sodium: Double,
    val glycemicIndex: String,
    val calories: Int,
    val suitableFor: List<String> = emptyList(),
    val servings: Int,
    val ingredients: Any? = null,
    val steps: Any? = null,
    val substitutionTips: Any? = null,
)

data class DailyMenuDto(
    val breakfast: List<RecipeDto> = emptyList(),
    val lunch: List<RecipeDto> = emptyList(),
    val dinner: List<RecipeDto> = emptyList(),
    val tips: List<String> = emptyList(),
)

data class SubstitutionDto(
    val ingredient: String,
    val substitutions: List<SubstitutionItem> = emptyList(),
    val message: String? = null,
)

data class SubstitutionItem(
    val original: String,
    val substitute: String,
    val reason: String,
)

// ==================== 新增 DTOs (Phase 2) ====================

data class InquiryResponseDto(
    val answer: String,
)

data class MutualContactRequest(
    val targetUserId: String,
)

data class FirstAidKitDto(
    val id: String,
    val name: String,
    val type: String,
    val quantity: Int,
    val expireDate: String? = null,
    val notes: String? = null,
    val createdAt: String,
)

data class FirstAidKitRequest(
    val name: String,
    val type: String,
    val quantity: Int = 1,
    val expireDate: String? = null,
    val notes: String? = null,
)

data class FirstAidGuideDto(
    val id: String,
    val type: String,
    val title: String,
    val content: String,
    val steps: Any? = null,
    val order: Int = 0,
)
