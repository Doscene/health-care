package com.healthcare.family.data.repository

import com.healthcare.family.data.remote.api.AddPlanRequest
import com.healthcare.family.data.remote.api.HealthCareApi
import com.healthcare.family.data.remote.api.PlanDto
import com.healthcare.family.util.toUserFriendlyMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentRepository @Inject constructor(
    private val api: HealthCareApi,
) {

    suspend fun getAppointments(): Result<List<PlanDto>> {
        return try {
            val resp = api.getAppointments()
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun addAppointment(
        hospital: String,
        department: String,
        date: String,
        reminderDays: Int,
        notes: String? = null,
    ): Result<PlanDto> {
        return try {
            val resp = api.addAppointment(
                AddPlanRequest(hospital, department, date, reminderDays, notes),
            )
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage()))
        }
    }

    suspend fun deleteAppointment(planId: String): Result<Unit> {
        return try {
            val resp = api.deleteAppointment(planId)
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
