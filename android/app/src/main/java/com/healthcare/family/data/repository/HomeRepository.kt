package com.healthcare.family.data.repository

import com.healthcare.family.data.remote.api.FamilyHomeDto
import com.healthcare.family.data.remote.api.HealthCareApi
import com.healthcare.family.data.remote.api.PatientHomeDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val api: HealthCareApi,
) {

    suspend fun getPatientHome(): Result<PatientHomeDto> {
        return try {
            val resp = api.getPatientHome()
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFamilyHome(): Result<FamilyHomeDto> {
        return try {
            val resp = api.getFamilyHome()
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
