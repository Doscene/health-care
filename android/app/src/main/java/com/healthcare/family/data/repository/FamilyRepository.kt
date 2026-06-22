package com.healthcare.family.data.repository

import com.healthcare.family.data.remote.api.CreateChallengeRequest
import com.healthcare.family.data.remote.api.CreateFamilyRequest
import com.healthcare.family.data.remote.api.CreateGoalRequest
import com.healthcare.family.data.remote.api.FamilyDto
import com.healthcare.family.data.remote.api.HealthCareApi
import com.healthcare.family.data.remote.api.InvitePreviewDto
import com.healthcare.family.data.remote.api.JoinFamilyRequest
import com.healthcare.family.data.remote.api.MemberDto
import com.healthcare.family.data.remote.api.SendReminderRequest
import com.healthcare.family.data.remote.api.UpdateGoalRequest
import com.healthcare.family.data.remote.api.UpdateMemberNicknameRequest
import com.healthcare.family.data.remote.api.UpdateMemberRoleRequest
import com.healthcare.family.data.remote.api.VerifyInviteRequest
import com.healthcare.family.util.toUserFriendlyMessage
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
            Result.failure(Exception(e.toUserFriendlyMessage()))
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
            Result.failure(Exception(e.toUserFriendlyMessage()))
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
            Result.failure(Exception(e.toUserFriendlyMessage()))
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
            Result.failure(Exception(e.toUserFriendlyMessage()))
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
            Result.failure(Exception(e.toUserFriendlyMessage()))
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
            Result.failure(Exception(e.toUserFriendlyMessage()))
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
            Result.failure(Exception(e.toUserFriendlyMessage()))
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
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    // ==================== Phase 3: 家庭协作 ====================

    suspend fun getFamilySummary(familyId: String): Result<List<com.healthcare.family.data.remote.api.MemberHealthSummaryDto>> {
        return try {
            val resp = api.getFamilySummary(familyId)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun getMemberDetail(familyId: String, memberId: String, metric: String? = null): Result<com.healthcare.family.data.remote.api.MemberDetailDto> {
        return try {
            val resp = api.getMemberDetail(familyId, memberId, metric)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun getChemistry(familyId: String): Result<com.healthcare.family.data.remote.api.ChemistryDto> {
        return try {
            val resp = api.getChemistry(familyId)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun getReminders(familyId: String, box: String? = null): Result<List<com.healthcare.family.data.remote.api.FamilyReminderDto>> {
        return try {
            val resp = api.getReminders(familyId, box)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun sendReminder(familyId: String, targetUserId: String, type: String, message: String? = null): Result<com.healthcare.family.data.remote.api.FamilyReminderDto> {
        return try {
            val resp = api.sendReminder(familyId, SendReminderRequest(targetUserId, type, message))
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun completeReminder(familyId: String, reminderId: String): Result<Unit> {
        return try {
            val resp = api.completeReminder(familyId, reminderId)
            if (resp.code == 0 || resp.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun getGoals(familyId: String): Result<List<com.healthcare.family.data.remote.api.FamilyGoalDto>> {
        return try {
            val resp = api.getGoals(familyId)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun createGoal(familyId: String, request: CreateGoalRequest): Result<com.healthcare.family.data.remote.api.FamilyGoalDto> {
        return try {
            val resp = api.createGoal(familyId, request)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun updateGoal(familyId: String, goalId: String, request: UpdateGoalRequest): Result<com.healthcare.family.data.remote.api.FamilyGoalDto> {
        return try {
            val resp = api.updateGoal(familyId, goalId, request)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun deleteGoal(familyId: String, goalId: String): Result<Unit> {
        return try {
            val resp = api.deleteGoal(familyId, goalId)
            if (resp.code == 0 || resp.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun getChallenges(familyId: String): Result<List<com.healthcare.family.data.remote.api.FamilyChallengeDto>> {
        return try {
            val resp = api.getChallenges(familyId)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun createChallenge(familyId: String, request: CreateChallengeRequest): Result<com.healthcare.family.data.remote.api.FamilyChallengeDto> {
        return try {
            val resp = api.createChallenge(familyId, request)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun joinChallenge(familyId: String, challengeId: String): Result<Unit> {
        return try {
            val resp = api.joinChallenge(familyId, challengeId)
            if (resp.code == 0 || resp.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }
}
