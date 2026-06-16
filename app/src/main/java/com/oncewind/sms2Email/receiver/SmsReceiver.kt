package com.oncewind.sms2Email.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Telephony
import android.telephony.SmsMessage
import android.telephony.SubscriptionManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import com.oncewind.sms2Email.AppContainer
import com.oncewind.sms2Email.data.AppSettings
import com.oncewind.sms2Email.data.ForwardLog
import com.oncewind.sms2Email.data.ForwardStatus
import com.oncewind.sms2Email.data.LogCategory
import com.oncewind.sms2Email.data.LogLevel
import com.oncewind.sms2Email.data.RuntimeLog
import com.oncewind.sms2Email.service.EmailSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * 短信接收广播处理器 *
 * 监听 SMS_RECEIVED_ACTION 广播，解析短信内容并转发
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val BASE_JOB_ID = 1001
        internal const val KEY_SENDER = "sender"
        internal const val KEY_BODY = "body"
        internal const val KEY_TIMESTAMP = "timestamp"
        internal const val KEY_SIM_SLOT = "sim_slot"
        internal const val KEY_SIM_PHONE_NUMBER = "sim_phone_number"
        private const val TAG = "SmsReceiver"
    }
    
    // 处理短信接收广播
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    override fun onReceive(context: Context, intent: Intent) {

        Log.i(TAG, "========================================")
        Log.i(TAG, "onReceive: 收到广播 - action=${intent.action}")
        Log.i(TAG, "onReceive: intent extras = ${intent.extras?.keySet()?.joinToString()}")
        Log.i(TAG, "========================================")

        // 验证广播 action
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.w(TAG, "onReceive: 广播动作不匹配，忽略 - actual=${intent.action}")
            return
        }

        // 检查服务是否启用，如果未启用则忽略短信
        if (!isServiceEnabled(context)) {
            Log.i(TAG, "onReceive: 服务未启用，忽略短信")
            return
        }
        Toast.makeText(context,"新短信", Toast.LENGTH_LONG).show()
        // 从intent 中提取短信
        val messages = extractSmsFromIntent(context, intent)
        if (messages.isNullOrEmpty()) {
            Log.w(TAG, "onReceive: 没有提取到短信，忽略")
            return
        }

        Log.i(TAG, "onReceive: 收到 ${messages.size} 条短信片段")

        // 合并长短信
        val fullMessageBody = StringBuilder()
        for (smsMessage in messages) {
            fullMessageBody.append(smsMessage.messageBody ?: "")
        }

        val senderNumber = messages.firstOrNull()?.originatingAddress ?: "未知号码"
        val timestamp = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
        val subscriptionId = intent.getIntExtra("subscription", -1)
        val simInfo = getSimInfo(context, subscriptionId)

        Log.i(TAG, "onReceive: 发送者$senderNumber, 卡槽=${simInfo.slot}, 卡号=${simInfo.phoneNumber}, 内容长度=${fullMessageBody.length}")

        // 使用 goAsync() 延长 BroadcastReceiver 存活时间（约10秒）
        // 所有关键操作在 goAsync 保护范围内同步完成，确保进程不被提前杀
        val pendingResult = goAsync()

        // 获取唤醒锁，确保在处理短信过程中设备不会进入深度睡眠
        val wakeLock = acquireWakeLock(context)

        try {
            // 同步阻塞式写入日志，确保在进程被杀前完成DataStore 持久化
            Log.d(TAG, "onReceive: 开始写入日志..")
            runBlocking(Dispatchers.IO) {
                val runtimeLogRepo = AppContainer.getRuntimeLogRepository(context)
                runtimeLogRepo.insertLogBlocking(
                    RuntimeLog(
                        level = LogLevel.INFO,
                        category = LogCategory.SMS,
                        title = "📩 收到新短信",
                        message = "发送者 $senderNumber | 卡槽: ${getSimSlotLabel(simInfo.slot)}",
                        details = "内容: ${
                            fullMessageBody.toString().take(100)
                        }${if (fullMessageBody.length > 100) "..." else ""}" +
                                (if (!simInfo.phoneNumber.isNullOrBlank()) "\n接收号码: ${simInfo.phoneNumber}" else "")
                    )
                )
            }
            Log.i(TAG, "onReceive: 运行时日志已记录成功")

            // 直接执行邮件转发，不通过 JobScheduler（避免 Doze 模式延迟）
            processSmsForwarding(context, senderNumber, fullMessageBody.toString(), timestamp, simInfo.slot, simInfo.phoneNumber)
        } catch (e: Exception) {
            Log.e(TAG, "onReceive: 处理失败: ${e.message}", e)
            try {
                AppContainer.getRuntimeLogRepository(context).logErrorWithException(
                    category = LogCategory.SMS,
                    title = "onReceive处理异常",
                    message = "广播处理发生异常",
                    exception = e
                )
            } catch (_: Exception) {
                // 彻底无法写入，只能依logcat
            }
        } finally {
            // 释放唤醒锁
            releaseWakeLock(wakeLock)
            // 必须调用 finish()，否则系统会认为广播处理超时
            Log.d(TAG, "onReceive: 调用 pendingResult.finish()")
            pendingResult.finish()
        }
    }

    /**
     * 获取 SIM 卡槽信息（包括卡号）
     */
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun getSimInfo(context: Context, subscriptionId: Int): SimInfo {
        Log.d(TAG, "getSimInfo: subscriptionId=$subscriptionId")
        if (subscriptionId == -1) {
            Log.w(TAG, "getSimInfo: subscriptionId -1，返回默认")
            return SimInfo(slot = 0, phoneNumber = null)
        }
        return try {
            val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
                ?: throw IllegalStateException("无法获取 SubscriptionManager")
            val subscriptionInfo = subscriptionManager.getActiveSubscriptionInfo(subscriptionId)
            val slot = (subscriptionInfo?.simSlotIndex ?: -1) + 1
            // 使用标准API获取手机号码（SubscriptionInfo.getNumber() 从 API 22 开始可用）
            val phoneNumber = subscriptionInfo?.number?.replace(" ", "")?.replace("-", "")
            Log.d(TAG, "getSimInfo: 获取到的卡槽=$slot, 卡号=$phoneNumber")
            SimInfo(slot = slot, phoneNumber = phoneNumber)
        } catch (e: Exception) {
            Log.e(TAG, "getSimInfo: 获取卡槽信息失败: ${e.message}", e)
            try {
                AppContainer.getRuntimeLogRepository(context).logErrorWithException(
                    category = LogCategory.SMS,
                    title = "获取SIM信息失败",
                    message = "无法获取SIM卡槽信息",
                    exception = e
                )
            } catch (_: Exception) {
            }
            SimInfo(slot = 0, phoneNumber = null)
        }
    }

    /**
     * SIM 信息数据     */
    private data class SimInfo(
        val slot: Int,
        val phoneNumber: String?
    )

    /**
     * Intent 中提取短信     * 使用标准PDU 解析方法，兼容更多设置     */
    private fun extractSmsFromIntent(context: Context, intent: Intent): Array<SmsMessage>? {
        return try {
            val pdus = intent.extras?.get("pdus") as? Array<*> ?: return null
            Log.d(TAG, "extractSmsFromIntent: 找到 ${pdus.size} PDU")

            val format = intent.extras?.getString("format")
            Log.d(TAG, "extractSmsFromIntent: 格式=$format")

            val messages = mutableListOf<SmsMessage>()
            for (pdu in pdus) {
                try {
                    val smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(pdu as? ByteArray, format)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsMessage.createFromPdu(pdu as? ByteArray)
                    }
                    messages.add(smsMessage)
                    Log.d(TAG, "extractSmsFromIntent: 成功解析一PDU")
                } catch (e: Exception) {
                    Log.e(TAG, "extractSmsFromIntent: 解析 PDU 失败: ${e.message}", e)
                }
            }
            messages.toTypedArray()
        } catch (e: Exception) {
            Log.e(TAG, "extractSmsFromIntent: 提取短信异常: ${e.message}", e)
            try {
                AppContainer.getRuntimeLogRepository(context).logErrorWithException(
                    category = LogCategory.SMS,
                    title = "提取短信失败",
                    message = "无法从Intent提取短信内容",
                    exception = e
                )
            } catch (_: Exception) {
            }
            null
        }
    }

    private fun getSimSlotLabel(simSlot: Int): String {
        return when (simSlot) {
            1 -> "SIM1"
            2 -> "SIM2"
            else -> "未知"
        }
    }

    /**
     * 检查服务是否启     */
    private fun isServiceEnabled(context: Context): Boolean {
        return runBlocking(Dispatchers.IO) {
            try {
                val settingsRepo = AppContainer.getSettingsRepository(context)
                val settings = settingsRepo.settings.first()
                settings.serviceEnabled
            } catch (e: Exception) {
                Log.e(TAG, "isServiceEnabled: 读取配置失败，默认返false", e)
                false
            }
        }
    }

    /**
     * 直接处理短信转发（不在 JobScheduler 中延迟执行）
     */
    private fun processSmsForwarding(
        context: Context,
        sender: String,
        body: String,
        timestamp: Long,
        simSlot: Int,
        simPhoneNumber: String?
    ) {
        Log.d(TAG, "processSmsForwarding: 开始处理短信转发")

        runBlocking(Dispatchers.IO) {
            try {
                val settingsRepo = AppContainer.getSettingsRepository(context)
                val settings = settingsRepo.settings.first()
                val runtimeLogRepo = AppContainer.getRuntimeLogRepository(context)
                val forwardLogRepo = AppContainer.getForwardLogRepository(context)

                Log.d(TAG, "processSmsForwarding: 读取配置 - serviceEnabled=${settings.serviceEnabled}")

                var success = false
                var errorMsg: String? = null

                if (!settings.serviceEnabled) {
                    Log.w(TAG, "processSmsForwarding: 服务未启用，跳过转发")
                    runtimeLogRepo.insertLog(
                        RuntimeLog(
                            level = LogLevel.WARNING,
                            category = LogCategory.SERVICE,
                            title = "⚠️ 服务未启动",
                            message = "短信转发服务已关闭，跳过转发",
                            details = "发送者 $sender"
                        )
                    )
                } else if (!shouldProcessSimSlot(settings, simSlot)) {
                    Log.w(TAG, "processSmsForwarding: 卡槽不匹配，跳过 - 当前=${simSlot}, 配置=${settings.monitoredSimSlot}")
                    runtimeLogRepo.insertLog(
                        RuntimeLog(
                            level = LogLevel.INFO,
                            category = LogCategory.SERVICE,
                            title = "⏭️ 卡槽不匹配",
                            message = "短信来自卡槽 ${getSimSlotLabel(simSlot)}，已配置仅监控特定卡",
                            details = "当前监控卡槽: ${settings.monitoredSimSlot}"
                        )
                    )
                } else {
                    Log.i(TAG, "processSmsForwarding: 准备发送邮件到 ${settings.recipientEmail}")
                    runtimeLogRepo.insertLog(
                        RuntimeLog(
                            level = LogLevel.INFO,
                            category = LogCategory.EMAIL,
                            title = "📧 正在发送邮件",
                            message = "准备发送转发邮件到 ${settings.recipientEmail}",
                            details = "SMTP: ${settings.smtpServer}:${settings.smtpPort} | 加密: ${settings.smtpEncryption}"
                        )
                    )

                    try {
                        Log.d(TAG, "processSmsForwarding: 调用 EmailSender.sendForwardEmail")
                        success = EmailSender.sendForwardEmail(
                            settings = settings,
                            senderNumber = sender,
                            messageContent = body,
                            simSlot = simSlot,
                            simPhoneNumber = simPhoneNumber,
                            receiveTime = timestamp
                        )

                        if (success) {
                            Log.i(TAG, "processSmsForwarding: 邮件发送成功")
                            runtimeLogRepo.insertLog(
                                RuntimeLog(
                                    level = LogLevel.INFO,
                                    category = LogCategory.EMAIL,
                                    title = "邮件发送成功",
                                    message = "短信已成功转发到 ${settings.recipientEmail}",
                                    details = "发送者 $sender | 内容预览: ${body.take(50)}..."
                                )
                            )
                        } else {
                            errorMsg = "邮件发送失败"
                            Log.e(TAG, "processSmsForwarding: 邮件发送返回失败")
                            runtimeLogRepo.insertLog(
                                RuntimeLog(
                                    level = LogLevel.ERROR,
                                    category = LogCategory.EMAIL,
                                    title = "邮件发送返回失败",
                                    message = "邮件发送返回失败",
                                    details = "发送者 $sender | SMTP: ${settings.smtpServer}"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        errorMsg = e.message ?: "未知错误"
                        Log.e(TAG, "processSmsForwarding: 发送邮件时发生异常: ${e.message}", e)
                        runtimeLogRepo.logErrorWithException(
                            category = LogCategory.EMAIL,
                            title = "邮件发送异常",
                            message = "发送邮件时发生异常",
                            exception = e
                        )
                    }
                }

                Log.d(TAG, "processSmsForwarding: 保存转发日志")
                forwardLogRepo.insertLog(
                    ForwardLog(
                        senderNumber = sender,
                        messageContent = body,
                        simSlot = simSlot,
                        receiveTime = timestamp,
                        forwardTime = System.currentTimeMillis(),
                        forwardStatus = if (success) ForwardStatus.SUCCESS else ForwardStatus.FAILED,
                        errorMessage = errorMsg
                    )
                )

                Log.i(TAG, "processSmsForwarding: 短信处理完成 - 成功=${success}")
                runtimeLogRepo.insertLog(
                    RuntimeLog(
                        level = LogLevel.DEBUG,
                        category = LogCategory.SMS,
                        title = "🏁 短信处理完成",
                        message = "短信转发任务已全部完成",
                        details = "转发状态 ${if (success) "成功" else "失败"}"
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "processSmsForwarding: 处理过程中发生未捕获的异常${e.message}", e)
                try {
                    AppContainer.getRuntimeLogRepository(context).logErrorWithException(
                        category = LogCategory.SYSTEM,
                        title = "💥 处理异常",
                        message = "短信处理过程中发生未捕获的异常",
                        exception = e
                    )
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun shouldProcessSimSlot(settings: AppSettings, simSlot: Int): Boolean {
        return when (settings.monitoredSimSlot) {
            com.oncewind.sms2Email.data.SimSlotFilter.ALL -> true
            com.oncewind.sms2Email.data.SimSlotFilter.SIM_1 -> simSlot == 1
            com.oncewind.sms2Email.data.SimSlotFilter.SIM_2 -> simSlot == 2
        }
    }

    /**
     * 获取唤醒锁
     */
    private fun acquireWakeLock(context: Context): PowerManager.WakeLock? {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SmsReceiver::WakeLock"
            ).apply {
                acquire(30 * 1000L) // 保持30秒，足够完成邮件发送
            }
        } catch (e: Exception) {
            Log.e(TAG, "acquireWakeLock: 获取唤醒锁失败: ${e.message}", e)
            null
        }
    }

    /**
     * 释放唤醒锁
     */
    private fun releaseWakeLock(wakeLock: PowerManager.WakeLock?) {
        wakeLock?.apply {
            if (isHeld) {
                try {
                    release()
                } catch (e: Exception) {
                    Log.e(TAG, "releaseWakeLock: 释放唤醒锁失败: ${e.message}", e)
                }
            }
        }
    }
}