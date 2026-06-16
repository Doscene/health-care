package com.healthcare.family.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.healthcare.family.MainActivity
import com.healthcare.family.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知帮助类
 * 处理各种推送通知的创建和显示
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "NotificationHelper"

        // 通知渠道ID
        const val CHANNEL_MEDICATION = "medication_reminder"
        const val CHANNEL_ALERT = "health_alert"
        const val CHANNEL_GENERAL = "general"

        // 通知渠道名称
        private const val CHANNEL_NAME_MEDICATION = "用药提醒"
        private const val CHANNEL_NAME_ALERT = "健康预警"
        private const val CHANNEL_NAME_GENERAL = "通用通知"

        // 通知ID范围
        const val NOTIFICATION_ID_MEDICATION_BASE = 1000
        const val NOTIFICATION_ID_ALERT_BASE = 2000
        const val NOTIFICATION_ID_GENERAL_BASE = 3000
    }

    init {
        createNotificationChannels()
    }

    /**
     * 创建所有通知渠道
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_MEDICATION,
                    CHANNEL_NAME_MEDICATION,
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "用药提醒通知"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_ALERT,
                    CHANNEL_NAME_ALERT,
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "健康预警通知"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_GENERAL,
                    CHANNEL_NAME_GENERAL,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "通用通知"
                },
            )

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { notificationManager.createNotificationChannel(it) }

            Log.d(TAG, "通知渠道创建完成")
        }
    }

    /**
     * 显示用药提醒通知
     */
    fun showMedicationReminder(
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

        val intent = createMainActivityIntent("medication", medicationId)
        val pendingIntent = createPendingIntent(medicationId.hashCode() + timeSlot.hashCode(), intent)

        val title = "${timeSlotName}用药提醒"
        val content = "该服用 ${medicationName} 了，每次 ${dosage} 片/粒"
        val bigText = "该服用 ${medicationName} 了，每次 ${dosage} 片/粒。\n坚持按时服药，保持健康！"

        val notification = NotificationCompat.Builder(context, CHANNEL_MEDICATION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .addAction(createConfirmAction(medicationId, timeSlot))
            .addAction(createDelayAction(medicationId, timeSlot))
            .build()

        showNotification(
            NOTIFICATION_ID_MEDICATION_BASE + medicationId.hashCode() + timeSlot.hashCode(),
            notification,
        )
    }

    /**
     * 显示健康预警通知
     */
    fun showHealthAlert(
        alertId: String,
        level: String,
        title: String,
        message: String,
    ) {
        val intent = createMainActivityIntent("alert", alertId)
        val pendingIntent = createPendingIntent(alertId.hashCode(), intent)

        val priority = when (level) {
            "red" -> NotificationCompat.PRIORITY_MAX
            "orange" -> NotificationCompat.PRIORITY_HIGH
            "yellow" -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .build()

        showNotification(
            NOTIFICATION_ID_ALERT_BASE + alertId.hashCode(),
            notification,
        )
    }

    /**
     * 显示通用通知
     */
    fun showGeneralNotification(
        title: String,
        message: String,
        navigateTo: String? = null,
    ) {
        val intent = createMainActivityIntent(navigateTo)
        val pendingIntent = createPendingIntent(title.hashCode(), intent)

        val notification = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        showNotification(
            NOTIFICATION_ID_GENERAL_BASE + title.hashCode(),
            notification,
        )
    }

    /**
     * 创建确认服药操作
     */
    private fun createConfirmAction(medicationId: String, timeSlot: String): NotificationCompat.Action {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "CONFIRM_MEDICATION"
            putExtra("medication_id", medicationId)
            putExtra("time_slot", timeSlot)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            "confirm_$medicationId$timeSlot".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(
            0,
            "确认服药",
            pendingIntent,
        ).build()
    }

    /**
     * 创建延迟提醒操作
     */
    private fun createDelayAction(medicationId: String, timeSlot: String): NotificationCompat.Action {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "DELAY_MEDICATION"
            putExtra("medication_id", medicationId)
            putExtra("time_slot", timeSlot)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            "delay_$medicationId$timeSlot".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(
            0,
            "稍后提醒",
            pendingIntent,
        ).build()
    }

    /**
     * 创建MainActivity Intent
     */
    private fun createMainActivityIntent(navigateTo: String? = null, id: String? = null): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            navigateTo?.let { putExtra("navigate_to", it) }
            id?.let { putExtra("id", it) }
        }
    }

    /**
     * 创建PendingIntent
     */
    private fun createPendingIntent(requestCode: Int, intent: Intent): PendingIntent {
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * 显示通知
     */
    private fun showNotification(notificationId: Int, notification: android.app.Notification) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "显示通知: $notificationId")
    }

    /**
     * 取消通知
     */
    fun cancelNotification(notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    /**
     * 取消所有通知
     */
    fun cancelAllNotifications() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }
}
