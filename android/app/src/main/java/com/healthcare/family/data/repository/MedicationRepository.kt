package com.healthcare.family.data.repository

import com.healthcare.family.data.remote.api.AddMedicationRequest
import com.healthcare.family.data.remote.api.HealthCareApi
import com.healthcare.family.data.remote.api.MedicationCalendarRecordDto
import com.healthcare.family.data.remote.api.MedicationDto
import com.healthcare.family.data.remote.api.UpdateMedicationRequest
import com.healthcare.family.ui.medication.MedicationCalendarRecord
import com.healthcare.family.util.toUserFriendlyMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationRepository @Inject constructor(
    private val api: HealthCareApi,
) {

    suspend fun getMedications(status: String? = null): Result<List<MedicationDto>> {
        return try {
            val resp = api.getMedications(status)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun addMedication(request: AddMedicationRequest): Result<MedicationDto> {
        return try {
            val resp = api.addMedication(request)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun updateMedication(medicationId: String, request: UpdateMedicationRequest): Result<MedicationDto> {
        return try {
            val resp = api.updateMedication(medicationId, request)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun deleteMedication(medicationId: String): Result<Unit> {
        return try {
            val resp = api.deleteMedication(medicationId)
            if (resp.code == 0 || resp.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun getCalendarData(year: Int, month: Int): Result<Map<String, List<MedicationCalendarRecord>>> {
        return try {
            val resp = api.getMedicationCalendar(year, month)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                val result = resp.data.mapValues { (_, records) ->
                    records.map { dto ->
                        MedicationCalendarRecord(
                            medicationName = dto.medicationName,
                            status = dto.status,
                            scheduledTime = dto.scheduledTime,
                        )
                    }
                }
                Result.success(result)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }
}
