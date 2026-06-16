package com.oncewind.sms2Email.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.oncewind.sms2Email.AppContainer
import com.oncewind.sms2Email.MainActivity
import com.oncewind.sms2Email.data.LogCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 短信监控前台服务
 *
 * 确保在后台时仍能可靠接收短信广播
 * Android 8+ 要求前台服务必须显示持续通知
 */
class SmsMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "sms_monitor_channel"
        const val CHANNEL_NAME = "短信监控服务"
        const val NOTIFICATION_ID = 1

        const val ACTION_START = "com.oncewind.smsforwarder.ACTION_START"
        const val ACTION_STOP = "com.oncewind.smsforwarder.ACTION_STOP"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // 获取唤醒锁，防止设备休眠
        acquireWakeLock()

        val runtimeLogRepo = AppContainer.getRuntimeLogRepository(applicationContext)
        runtimeLogRepo.logInfo(
            category = LogCategory.SERVICE,
            title = "🔧 服务已创建",
            message = "短信监控服务组件已初始化",
            details = "Service: SmsMonitorService"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                val runtimeLogRepo = AppContainer.getRuntimeLogRepository(applicationContext)
                runtimeLogRepo.logInfo(
                    category = LogCategory.SERVICE,
                    title = "🛑 收到停止命令",
                    message = "正在停止短信监控服务",
                    details = "从通知栏停止"
                )

                // 用户主动停止时，才将 serviceEnabled 设为 false
                serviceScope.launch {
                    try {
                        val settingsRepo = AppContainer.getSettingsRepository(applicationContext)
                        val currentSettings = settingsRepo.settings.first()
                        settingsRepo.updateSettings(currentSettings.copy(serviceEnabled = false))
                        runtimeLogRepo.logInfo(
                            category = LogCategory.SERVICE,
                            title = "💾 配置已更新",
                            message = "服务启用状态已设置为关闭",
                            details = "serviceEnabled = false（用户主动停止）"
                        )
                    } catch (e: Exception) {
                        runtimeLogRepo.logErrorWithException(
                            category = LogCategory.SERVICE,
                            title = "配置更新失败",
                            message = "无法更新服务配置",
                            exception = e
                        )
                    }
                }

                releaseWakeLock()
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val runtimeLogRepo = AppContainer.getRuntimeLogRepository(applicationContext)

        serviceScope.launch {
            try {
                val settingsRepo = AppContainer.getSettingsRepository(applicationContext)
                val settings = settingsRepo.settings.first()

                runtimeLogRepo.logInfo(
                    category = LogCategory.SERVICE,
                    title = "🚀 服务启动中",
                    message = "正在启动短信监控前台服务",
                    details = "监控卡槽: ${settings.monitoredSimSlot}\n转发邮箱: ${settings.recipientEmail}"
                )
            } catch (e: Exception) {
                runtimeLogRepo.logErrorWithException(
                    category = LogCategory.SERVICE,
                    title = "读取配置失败",
                    message = "无法读取服务配置",
                    exception = e
                )
            }
        }

        val notification = createNotification()
        val foregroundServiceType = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, foregroundServiceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        runtimeLogRepo.logInfo(
            category = LogCategory.SERVICE,
            title = "服务已启动",
            message = "短信监控服务正在后台运行",
            details = "通知已显示 | 服务ID: $NOTIFICATION_ID"
        )

        return START_STICKY
    }

    override fun onDestroy() {
        val runtimeLogRepo = AppContainer.getRuntimeLogRepository(applicationContext)
        runtimeLogRepo.logInfo(
            category = LogCategory.SERVICE,
            title = "🔌 服务已停止",
            message = "短信监控服务已销毁",
            details = "前台通知已移除"
        )

        releaseWakeLock()

        // 尝试重启服务（如果服务应该运行）
        // 这在系统杀死服务后尝试自动恢复
        serviceScope.launch {
            try {
                val settingsRepo = AppContainer.getSettingsRepository(applicationContext)
                val settings = settingsRepo.settings.first()
                
                if (settings.serviceEnabled) {
                    runtimeLogRepo.logInfo(
                        category = LogCategory.SERVICE,
                        title = "🔄 尝试重启服务",
                        message = "服务被意外终止，正在尝试重启",
                        details = "serviceEnabled = true"
                    )
                    
                    // 延迟一段时间后重启，避免快速重启导致的问题
                    delay(3000)
                    
                    val intent = Intent(applicationContext, SmsMonitorService::class.java).apply {
                        action = ACTION_START
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        applicationContext.startForegroundService(intent)
                    } else {
                        applicationContext.startService(intent)
                    }
                }
            } catch (e: Exception) {
                runtimeLogRepo.logErrorWithException(
                    category = LogCategory.SERVICE,
                    title = "重启服务失败",
                    message = "无法自动重启短信监控服务",
                    exception = e
                )
            }
        }

        super.onDestroy()
    }

    /**
     * 获取唤醒锁
     */
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SmsMonitorService::WakeLock"
            ).apply {
                acquire(10 * 60 * 1000L) // 保持10分钟
            }
        } catch (e: Exception) {
            // 获取唤醒锁失败，继续运行
        }
    }

    /**
     * 释放唤醒锁
     */
    private fun releaseWakeLock() {
        wakeLock?.apply {
            if (isHeld) {
                release()
            }
        }
        wakeLock = null
    }

    /**
     * 创建通知渠道（Android 8+ 要求）
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "短信转发监控服务正在运行"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SmsMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("短信转发服务")
            .setContentText("正在监控短信并转发到邮箱")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "停止", stopPendingIntent)
            .setOngoing(true)
            .build()
    }
}