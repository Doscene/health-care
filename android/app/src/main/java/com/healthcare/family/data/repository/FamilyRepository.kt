package com.healthcare.family.data.repository

import com.healthcare.family.data.remote.api.CreateFamilyRequest
import com.healthcare.family.data.remote.api.FamilyDto
import com.healthcare.family.data.remote.api.HealthCareApi
import com.healthcare.family.data.remote.api.InvitePreviewDto
import com.healthcare.family.data.remote.api.JoinFamilyRequest
import com.healthcare.family.data.remote.api.MemberDto
import com.healthcare.family.data.remote.api.UpdateMemberNicknameRequest
import com.healthcare.family.data.remote.api.UpdateMemberRoleRequest
import com.healthcare.family.data.remote.api.VerifyInviteRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FamilyRepository @Inject constructor(
    private val api: HealthCareApi,
) {

    suspend fun createFamily(name: String): Result<FamilyDto> {
        return try {
            val resp = api.createFamily(CreateFamilyRequest(name))
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyFamilies(): Result<List<FamilyDto>> {
        return try {
            val resp = api.getMyFamilies()
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyInviteCode(code: String): Result<InvitePreviewDto> {
        return try {
            val resp = api.verifyInviteCode(VerifyInviteRequest(code))
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinFamily(code: String, role: String): Result<String> {
        return try {
            val resp = api.joinFamily(JoinFamilyRequest(code, role))
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data.familyId)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMembers(familyId: String): Result<List<MemberDto>> {
        return try {
            val resp = api.getFamilyMembers(familyId)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMemberRole(familyId: String, memberId: String, role: String): Result<Unit> {
        return try {
            val resp = api.updateMemberRole(familyId, memberId, UpdateMemberRoleRequest(role))
            if (resp.code == 0 || resp.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMemberNickname(familyId: String, memberId: String, nickname: String): Result<Unit> {
        return try {
            val resp = api.updateMemberNickname(familyId, memberId, UpdateMemberNicknameRequest(nickname))
            if (resp.code == 0 || resp.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeMember(familyId: String, memberId: String): Result<Unit> {
        return try {
            val resp = api.removeMember(familyId, memberId)
            if (resp.code == 0 || resp.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
