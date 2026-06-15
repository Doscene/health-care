package com.healthcare.family.data.repository

import com.healthcare.family.data.remote.api.AddContactRequest
import com.healthcare.family.data.remote.api.AlertDto
import com.healthcare.family.data.remote.api.ContactDto
import com.healthcare.family.data.remote.api.HealthCareApi
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
}
