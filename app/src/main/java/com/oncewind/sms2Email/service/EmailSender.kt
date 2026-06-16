package com.oncewind.sms2Email.service

import com.oncewind.sms2Email.data.AppSettings
import com.oncewind.sms2Email.data.SmtpEncryption
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * 邮件发送器，使用JavaMail API 通过 SMTP 发送邮 */
object EmailSender {

    /**
     * 发送短信转发邮     *
     * @param settings APP 配置
     * @param senderNumber 短信发送人号码
     * @param messageContent 短信内容
     * @param simSlot SIM 卡槽 (0=未知, 1=, 2=)
     * @param simPhoneNumber SIM卡号码（如果能获取到     * @param receiveTime 接收时间     * @return 发送成功返true，失败返false
     */
    fun sendForwardEmail(
        settings: AppSettings,
        senderNumber: String,
        messageContent: String,
        simSlot: Int,
        simPhoneNumber: String? = null,
        receiveTime: Long
    ): Boolean {
        return try {
            val session = createSession(settings)
            val message = MimeMessage(session)

            message.setFrom(InternetAddress(settings.senderEmail))
            message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(settings.recipientEmail)
            )

            val simSlotLabel = when (simSlot) {
                1 -> " (SIM1)"
                2 -> " (SIM2)"
                else -> "未知"
            }

            // 构建SIM卡槽信息（包含卡号）
            val simInfo = if (!simPhoneNumber.isNullOrBlank()) {
                "$simSlotLabel - $simPhoneNumber"
            } else {
                simSlotLabel
            }

            val timeStr = java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()
            ).format(java.util.Date(receiveTime))

            message.subject = "【短信转发】来$senderNumber"
            message.setContent(
                """
                <div style="font-family: sans-serif; padding: 16px; max-width: 600px;">
                    <h3 style="margin:0 0 16px 0; color:#1976D2; border-bottom:2px solid #1976D2; padding-bottom:8px;">短信转发通知</h3>
                    <table style="border-collapse:collapse; width:100%;">
                        <tr>
                            <td style="padding:10px 12px; border:1px solid #E0E0E0; background:#FAFAFA; font-weight:bold; width:120px; vertical-align:top;">发送人</td>
                            <td style="padding:10px 12px; border:1px solid #E0E0E0; font-family:monospace;">$senderNumber</td>
                        </tr>
                        <tr>
                            <td style="padding:10px 12px; border:1px solid #E0E0E0; background:#FAFAFA; font-weight:bold; width:120px; vertical-align:top;">接收号码</td>
                            <td style="padding:10px 12px; border:1px solid #E0E0E0; font-family:monospace;">$simInfo</td>
                        </tr>
                        <tr>
                            <td style="padding:10px 12px; border:1px solid #E0E0E0; background:#FAFAFA; font-weight:bold; width:120px; vertical-align:top;">接收时间</td>
                            <td style="padding:10px 12px; border:1px solid #E0E0E0;">$timeStr</td>
                        </tr>
                        <tr>
                            <td style="padding:10px 12px; border:1px solid #E0E0E0; background:#FAFAFA; font-weight:bold; width:120px; vertical-align:top;">短信内容</td>
                            <td style="padding:10px 12px; border:1px solid #E0E0E0; line-height:1.6;">$messageContent</td>
                        </tr>
                    </table>
                    <div style="margin-top:16px; padding-top:12px; border-top:1px dashed #E0E0E0; color:#757575; font-size:12px;">
                        <p style="margin:0;">本邮件由短信转发应用自动发</p>
                        <p style="margin:4px 0 0 0;">转发邮箱: ${settings.senderEmail}</p>
                    </div>
                </div>
                """.trimIndent(),
                "text/html; charset=utf-8"
            )

            Transport.send(message)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 创建 SMTP Session
     */
    private fun createSession(settings: AppSettings): Session {
        val props = java.util.Properties()

        props["mail.smtp.host"] = settings.smtpServer
        props["mail.smtp.port"] = settings.smtpPort.toString()
        props["mail.smtp.auth"] = "true"

        // 设置超时时间0秒）
        props["mail.smtp.timeout"] = "30000"
        props["mail.smtp.connectiontimeout"] = "30000"

        when (settings.smtpEncryption) {
            SmtpEncryption.SSL -> {
                props["mail.smtp.ssl.enable"] = "true"
                props["mail.smtp.ssl.checkserveridentity"] = "false"
                props["mail.smtp.ssl.trust"] = "*"
                props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
                props["mail.smtp.socketFactory.port"] = settings.smtpPort.toString()
            }
            SmtpEncryption.TLS -> {
                props["mail.smtp.starttls.enable"] = "true"
                props["mail.smtp.starttls.checkserveridentity"] = "false"
                props["mail.smtp.ssl.trust"] = "*"
            }
            SmtpEncryption.NONE -> {
                // 无加密，不设置SSL/TLS 属
            }
        }

        return Session.getInstance(props, object : javax.mail.Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(settings.senderEmail, settings.senderPassword)
            }
        })
    }
}