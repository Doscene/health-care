package com.healthcare.family.data.repository

import com.healthcare.family.data.remote.api.AddContactRequest
import com.healthcare.family.data.remote.api.AlertDto
import com.healthcare.family.data.remote.api.ContactDto
import com.healthcare.family.data.remote.api.FirstAidKitDto
import com.healthcare.family.data.remote.api.FirstAidKitRequest
import com.healthcare.family.data.remote.api.FirstAidGuideDto
import com.healthcare.family.data.remote.api.HealthCareApi
import com.healthcare.family.data.remote.api.InquiryResponseDto
import com.healthcare.family.data.remote.api.MutualContactRequest
import com.healthcare.family.data.remote.api.UpdateAlertStatusRequest
import com.healthcare.family.data.remote.api.UpdateContactRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(
    private val api: HealthCareApi,
) {

    suspend fun getAlerts(status: String? = null): Result<List<AlertDto>> {
        return try {
            val resp = api.getAlerts(status)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAlertStatus(alertId: String, status: String): Result<Unit> {
        return try {
            val resp = api.updateAlertStatus(alertId, UpdateAlertStatusRequest(status))
            if (resp.code == 0 || resp.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitInquiry(alertId: String, answer: String): Result<InquiryResponseDto> {
        return try {
            val resp = api.submitInquiry(alertId, InquiryResponseDto(answer))
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addEmergencyContact(name: String, phone: String, relation: String): Result<ContactDto> {
        return try {
            val resp = api.addEmergencyContact(AddContactRequest(name, phone, relation))
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEmergencyContacts(): Result<List<ContactDto>> {
        return try {
            val resp = api.getEmergencyContacts()
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEmergencyContact(contactId: String, request: UpdateContactRequest): Result<ContactDto> {
        return try {
            val resp = api.updateEmergencyContact(contactId, request)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEmergencyContact(contactId: String): Result<Unit> {
        return try {
            val resp = api.deleteEmergencyContact(contactId)
            if (resp.code == 0 || resp.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setMutualContact(targetUserId: String): Result<Unit> {
        return try {
            val resp = api.setMutualContact(MutualContactRequest(targetUserId))
            if (resp.code == 0 || resp.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 急救包 ====================

    suspend fun getFirstAidKit(): Result<List<FirstAidKitDto>> {
        return try {
            val resp = api.getFirstAidKit()
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addFirstAidItem(data: FirstAidKitRequest): Result<FirstAidKitDto> {
        return try {
            val resp = api.addFirstAidItem(data)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFirstAidItem(itemId: String): Result<Unit> {
        return try {
            val resp = api.deleteFirstAidItem(itemId)
            if (resp.code == 0 || resp.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 急救指南 ====================

    suspend fun getEmergencyGuides(type: String? = null): Result<List<FirstAidGuideDto>> {
        return try {
            val resp = api.getEmergencyGuides(type)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
