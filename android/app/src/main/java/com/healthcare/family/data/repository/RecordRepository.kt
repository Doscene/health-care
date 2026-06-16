package com.healthcare.family.data.repository

import com.healthcare.family.data.remote.api.AddBpRecordRequest
import com.healthcare.family.data.remote.api.AddBgRecordRequest
import com.healthcare.family.data.remote.api.BgRecordDto
import com.healthcare.family.data.remote.api.BpRecordDto
import com.healthcare.family.data.remote.api.HealthCareApi
import com.healthcare.family.data.remote.api.VoiceRecognizeRequest
import com.healthcare.family.data.remote.api.VoiceRecognizeResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordRepository @Inject constructor(
    private val api: HealthCareApi,
) {

    suspend fun addBpRecord(
        systolic: Int,
        diastolic: Int,
        heartRate: Int? = null,
        inputMethod: String = "manual",
    ): Result<BpRecordDto> {
        return try {
            val resp = api.addBpRecord(AddBpRecordRequest(systolic, diastolic, heartRate, inputMethod))
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBpRecords(limit: Int = 30): Result<List<BpRecordDto>> {
        return try {
            val resp = api.getBpRecords(limit)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBpRecord(recordId: String): Result<Unit> {
        return try {
            val resp = api.deleteBpRecord(recordId)
            if (resp.code == 0 || resp.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addBgRecord(
        type: String,
        value: Double,
        inputMethod: String = "manual",
    ): Result<BgRecordDto> {
        return try {
            val resp = api.addBgRecord(AddBgRecordRequest(type, value, inputMethod))
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBgRecords(limit: Int = 30): Result<List<BgRecordDto>> {
        return try {
            val resp = api.getBgRecords(limit)
            if ((resp.code == 0 || resp.code == 200) && resp.data != null) {
                Result.success(resp.data)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBgRecord(recordId: String): Result<Unit> {
        return try {
            val resp = api.deleteBgRecord(recordId)
            if (resp.code == 0 || resp.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recognizeVoice(audioBase64: String, format: String = "pcm"): Result<VoiceRecognizeResponse> {
        return try {
            val resp = api.recognizeVoice(VoiceRecognizeRequest(audioBase64, format))
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
