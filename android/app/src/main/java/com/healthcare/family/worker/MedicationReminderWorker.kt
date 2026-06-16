package com.healthcare.family.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.healthcare.family.MainActivity
import com.healthcare.family.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * 用药提醒Worker
 * 使用WorkManager实现本地用药提醒
 */
@HiltWorker
class MedicationReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "MedicationReminder"
        private const val CHANNEL_ID = "medication_reminder"
        private const val CHANNEL_NAME = "用药提醒"
        private const val WORK_NAME_PREFIX = "medication_reminder_"

        // 输入数据键
        const val KEY_MEDICATION_ID = "medication_id"
        const val KEY_MEDICATION_NAME = "medication_name"
        const val KEY_DOSAGE = "dosage"
        const val KEY_TIME_SLOT = "time_slot"

        /**
         * 调度用药提醒
         * @param context 上下文
         * @param medicationId 药品ID
         * @param medicationName 药品名称
         * @param dosage 每次用量
         * @param timeSlot 时间段 (morning/noon/evening/before_sleep)
         */
        fun scheduleReminder(
            context: Context,
            medicationId: String,
            medicationName: String,
            dosage: Int,
            timeSlot: String,
        ) {
            val workName = "$WORK_NAME_PREFIX$medicationId-$timeSlot"

            // 计算提醒时间
            val reminderTime = getReminderTime(timeSlot)
            val now = ZonedDateTime.now()
            var targetTime = now.with(reminderTime)

            // 如果目标时间已过，设置为明天
            if (targetTime.isBefore(now)) {
                targetTime = targetTime.plusDays(1)
            }

            val initialDelay = Duration.between(now, targetTime).toMillis()

            // 创建输入数据
            val inputData = Data.Builder()
                .putString(KEY_MEDICATION_ID, medicationId)
                .putString(KEY_MEDICATION_NAME, medicationName)
                .putInt(KEY_DOSAGE, dosage)
                .putString(KEY_TIME_SLOT, timeSlot)
                .build()

            // 创建周期性工作请求（每天执行）
            val workRequest = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
                1, TimeUnit.DAYS,
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("medication_reminder")
                .addTag(medicationId)
                .build()

            // 调度工作
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                workName,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest,
            )

            Log.d(TAG, "调度用药提醒: $medicationName ($timeSlot), 延迟: ${initialDelay}ms")
        }

        /**
         * 取消用药提醒
         */
        fun cancelReminder(context: Context, medicationId: String, timeSlot: String) {
            val workName = "$WORK_NAME_PREFIX$medicationId-$timeSlot"
            WorkManager.getInstance(context).cancelUniqueWork(workName)
            Log.d(TAG, "取消用药提醒: $medicationId ($timeSlot)")
        }

        /**
         * 取消药品的所有提醒
         */
        fun cancelAllReminders(context: Context, medicationId: String) {
            WorkManager.getInstance(context).cancelAllWorkByTag(medicationId)
            Log.d(TAG, "取消药品所有提醒: $medicationId")
        }

        /**
         * 获取提醒时间
         */
        private fun getReminderTime(timeSlot: String): LocalTime {
            return when (timeSlot) {
                "morning" -> LocalTime.of(8, 0)  // 早上8点
                "noon" -> LocalTime.of(12, 0)    // 中午12点
                "evening" -> LocalTime.of(18, 0)  // 晚上6点
                "before_sleep" -> LocalTime.of(21, 30) // 睡前9:30
                else -> LocalTime.of(8, 0)
            }
        }
    }

    override suspend fun doWork(): Result {
        val medicationId = inputData.getString(KEY_MEDICATION_ID) ?: return Result.failure()
        val medicationName = inputData.getString(KEY_MEDICATION_NAME) ?: "未知药品"
        val dosage = inputData.getInt(KEY_DOSAGE, 1)
        val timeSlot = inputData.getString(KEY_TIME_SLOT) ?: "morning"

        Log.d(TAG, "执行用药提醒: $medicationName, 剂量: $dosage, 时间段: $timeSlot")

        // 创建通知渠道
        createNotificationChannel()

        // 显示通知
        showNotification(medicationId, medicationName, dosage, timeSlot)

        return Result.success()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "用药提醒通知"
                enableVibration(true)
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 显示通知
     */
    private fun showNotification(
        medicationId: String,
        medicationName: String,
        dosage: Int,
        timeSlot: String,
    ) {
        val timeSlotName = when (timeSlot) {
            "morning" -> "早晨"
            "noon" -> "中午"
            "evening" -> "晚上"
            "before_sleep" -> "睡前"
            else -> "未知"
        }

        // 点击通知打开应用
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "medication")
            putExtra("medication_id", medicationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            medicationId.hashCode() + timeSlot.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // 构建通知
        val title = "${timeSlotName}用药提醒"
        val content = "该服用 ${medicationName} 了，每次 ${dosage} 片/粒"
        val bigText = "该服用 ${medicationName} 了，每次 ${dosage} 片/粒。\n坚持按时服药，保持健康！"

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        // 显示通知
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            medicationId.hashCode() + timeSlot.hashCode(),
            notification,
        )

        Log.d(TAG, "显示用药通知: $medicationName ($timeSlotName)")
    }
}
