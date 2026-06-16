package com.oncewind.sms2Email.data

/**
 * APP 配置数据模型
 */
data class AppSettings(
    val senderEmail: String = "",
    val senderPassword: String = "",
    val smtpServer: String = "smtp.qq.com",
    val smtpPort: Int = 465,
    val smtpEncryption: SmtpEncryption = SmtpEncryption.SSL,
    val recipientEmail: String = "",
    val monitoredSimSlot: SimSlotFilter = SimSlotFilter.ALL,
    val serviceEnabled: Boolean = false
)

/**
 * SMTP 加密方式
 */
enum class SmtpEncryption {
    SSL,    // 端口 465
    TLS,    // 端口 587 (STARTTLS)
    NONE    // 无加密
}
/**
 * SIM 卡槽过滤选项
 */
enum class SimSlotFilter {
    ALL,    // 监控所有卡槽
    SIM_1,  // 仅监控卡1
    SIM_2   // 仅监控卡2
}