package com.healthcare.family.worker

import android.content.Context
import android.util.Log
import com.healthcare.family.data.remote.api.MedicationDto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用药提醒管理器
 * 管理所有用药提醒的调度和取消
 */
@Singleton
class MedicationReminderManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "MedicationReminderMgr"
    }

    /**
     * 为药品设置提醒
     * 根据药品的时间段设置多个提醒
     */
    fun scheduleReminders(medication: MedicationDto) {
        val timeSlots = parseTimeSlots(medication.timeSlots)
        val dosage = medication.dosagePerTime

        timeSlots.forEach { timeSlot ->
            MedicationReminderWorker.scheduleReminder(
                context = context,
                medicationId = medication.id,
                medicationName = medication.name,
                dosage = dosage,
                timeSlot = timeSlot,
            )
        }

        Log.d(TAG, "为药品 ${medication.name} 设置了 ${timeSlots.size} 个提醒")
    }

    /**
     * 取消药品的所有提醒
     */
    fun cancelReminders(medicationId: String) {
        MedicationReminderWorker.cancelAllReminders(context, medicationId)
        Log.d(TAG, "取消药品 $medicationId 的所有提醒")
    }

    /**
     * 暂停药品提醒
     */
    fun pauseReminders(medicationId: String) {
        MedicationReminderWorker.cancelAllReminders(context, medicationId)
        Log.d(TAG, "暂停药品 $medicationId 的提醒")
    }

    /**
     * 恢复药品提醒
     */
    fun resumeReminders(medication: MedicationDto) {
        scheduleReminders(medication)
        Log.d(TAG, "恢复药品 ${medication.name} 的提醒")
    }

    /**
     * 解析时间段
     */
    private fun parseTimeSlots(timeSlots: Any?): List<String> {
        return when (timeSlots) {
            is List<*> -> timeSlots.filterIsInstance<String>()
            is String -> {
                // 尝试解析JSON数组字符串
                try {
                    if (timeSlots.startsWith("[")) {
                        timeSlots.removeSurrounding("[", "]")
                            .split(",")
                            .map { it.trim().removeSurrounding("\"") }
                            .filter { it.isNotBlank() }
                    } else {
                        listOf(timeSlots)
                    }
                } catch (e: Exception) {
                    listOf("morning", "noon", "evening")
                }
            }
            else -> listOf("morning", "noon", "evening")
        }
    }
}
