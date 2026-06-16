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
 * 邮箱连接测试
 */
object EmailTester {

    /**
     * 测试邮箱连接和认
     *
     * @param settings APP 配置
     * @return 测试结果，包含成失败状态和详细信息
     */
    fun testConnection(settings: AppSettings): TestResult {
        if (settings.senderEmail.isBlank()) {
            return TestResult(
                success = false,
                message = "发送邮箱地址不能为空",
                details = "请输入发送邮箱地址，例如：your_email@qq.com"
            )
        }

        if (settings.senderPassword.isBlank()) {
            return TestResult(
                success = false,
                message = "密码/授权码不能为空",
                details = "请输入邮箱的密码或授权码"
            )
        }

        if (settings.recipientEmail.isBlank()) {
            return TestResult(
                success = false,
                message = "接收邮箱地址不能为空",
                details = "请输入接收转发邮件的邮箱地址"
            )
        }

        return try {
            val session = createSession(settings)
            val message = MimeMessage(session)

            message.setFrom(InternetAddress(settings.senderEmail))
            message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(settings.recipientEmail)
            )

            message.subject = "【短信转发】测试邮件"
            message.setContent(
                """
                <div style="font-family: sans-serif; padding: 16px;">
                    <h3 style="margin:0 0 12px 0; color:#4CAF50;">邮箱连接测试成功/h3>
                    <p>这是一封测试邮件，用于验证短信转发应用的邮箱配置是否正确/p>
                    <hr style="border:none; border-top:1px solid #ddd; margin:16px 0;" />
                    <p style="color:#666; font-size:12px;">
                        测试时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}<br/>
                        SMTP 服务 ${settings.smtpServer}:${settings.smtpPort}<br/>
                        加密方式: ${settings.smtpEncryption.name}
                    </p>
                </div>
                """.trimIndent(),
                "text/html; charset=utf-8"
            )

            Transport.send(message)

            TestResult(
                success = true,
                message = "测试邮件发送成功！",
                details = "已成功连接到 ${settings.smtpServer} 并发送测试邮件到 ${settings.recipientEmail}。\n\n请检查接收邮箱是否收到测试邮件"
            )
        } catch (e: Exception) {
            val errorMessage = e.message ?: "未知错误"
            val errorType = e.javaClass.simpleName

            // 根据不同的错误类型提供具体的解决方案
            val solution = when {
                errorMessage.contains("535") || errorType.contains("Auth") -> {
                    generateAuthErrorSolution(settings)
                }
                errorMessage.contains("Connect") || errorMessage.contains("Network") -> {
                    "网络连接问题：请检查网络是否正常，尝试更换WiFi或移动数据"
                }
                errorMessage.contains("timeout", ignoreCase = true) -> {
                    "连接超时：请检查SMTP服务器地址是否正确，或稍后重试"
                }
                errorMessage.contains("SSL", ignoreCase = true) || errorMessage.contains("TLS", ignoreCase = true) -> {
                    generateEncryptionErrorSolution(settings)
                }
                else -> {
                    "未知错误，请检查配置是否正确"
                }
            }

            TestResult(
                success = false,
                message = "邮箱连接失败",
                details = """
                    |错误类型: $errorType
                    |错误信息: $errorMessage
                    |
                    |📋 可能原因及解决方案：
                    |
                    |$solution
                """.trimMargin()
            )
        }
    }

    /**
     * 根据授权密码错误生成解决方案
     */
    private fun generateAuthErrorSolution(settings: AppSettings): String {
        val emailType = detectEmailType(settings.smtpServer)

        return when (emailType) {
            "qq" -> """
                |1. **授权码错*：QQ邮箱必须使用"授权而非登录密码
                |   获取方法
                |   登录 QQ 邮箱网页设置 账户
                |   找到"POP3/IMAP/SMTP/Exchange/CardDAV/CalDAV服务"
                |   开SMTP服务" 点击"生成授权 发送短信验
                |   复制授权码（16位）填入密码
                |
                |2. **SMTP服务未开*
                |   确认已在QQ邮箱设置中开启了SMTP服务
                |   确认授权码还有效（可重新生成
                |
                |3. **账户异常**
                |   登录网页QQ 邮箱检查是否有异常提示
                |   确保账户已绑定手机号
                |   尝试重新生成授权
            """.trimMargin()

            "163", "126" -> """
                |1. **授权码错*63/126邮箱需要使用授权而非登录密码
                |   获取方法
                |   登录 163/126 邮箱网页设置 POP3/SMTP/IMAP
                |   开SMTP服务" 客户端授权密获取授权
                |   使用授权码填入密码栏
                |
                |2. **确保已开启SMTP服务**
                |
                |3. **账户安全限制**
                |   同一IP短时间内多次失败可能被限制，等待30分钟后重
            """.trimMargin()

            "139", "189" -> """
                |1. **密码错误**39/189邮箱通常使用登录密码
                |   确认方式
                |   登录网页版邮箱检查是否需要设置客户端密码
                |   部分运营商邮箱支持专用客户端密码
                |
                |2. **服务未开*
                |   确认已开通SMTP服务（部分邮箱可能需要单独开通）
                |
                |3. **账户问题**
                |   确认手机号仍在使用
                |   确认邮箱未欠费或被暂
            """.trimMargin()

            "aliyun" -> """
                |1. **密码/授权码错*：阿里邮箱需要使用专用客户端密码
                |   获取方法
                |   登录阿里邮箱网页设置 账户
                |   开POP3/SMTP 服务
                |   设置客户端专用密
                |
                |2. **企业邮箱注意**
                |   如果是企业邮箱，可能需要管理员开通权
                |   确认邮箱域名的MX记录配置正确
            """.trimMargin()

            else -> """
                |1. **密码/授权码错*
                |   检查密码是否正
                |   确认使用的是授权码而非登录密码
                |
                |2. **SMTP服务状态*
                |   确认邮箱已开启SMTP服务
                |   尝试重新生成授权
                |
                |3. **账户状态*
                |   确认账户未被限制
                |   等待一段时间后重试（可能触发频率限制）
            """.trimMargin()
        }
    }

    /**
     * 根据加密方式错误生成解决方案
     */
    private fun generateEncryptionErrorSolution(settings: AppSettings): String {
        val emailType = detectEmailType(settings.smtpServer)

        return when (emailType) {
            "qq" -> """
                |SSL加密配置
                |端口65
                |加密方式：SSL
                |服务器：smtp.qq.com
                |
                |如果使用TLS
                |端口87
                |加密方式：TLS/STARTTLS
            """.trimMargin()

            "163", "126" -> """
                |SSL加密配置
                |端口65
                |加密方式：SSL
                |服务器：smtp.163.com smtp.126.com
            """.trimMargin()

            else -> """
                |推荐配置
                |端口65（SSL）或 587（TLS
                |加密方式：SSL TLS
                |确保端口与加密方式匹
            """.trimMargin()
        }
    }

    /**
     * 检测邮箱类
     */
    private fun detectEmailType(smtpServer: String): String {
        return when {
            smtpServer.contains("qq") -> "qq"
            smtpServer.contains("163") -> "163"
            smtpServer.contains("126") -> "126"
            smtpServer.contains("139") -> "139"
            smtpServer.contains("189") -> "189"
            smtpServer.contains("aliyun") || smtpServer.contains("alibaba") -> "aliyun"
            else -> "unknown"
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

/**
 * 测试结果数据
 */
data class TestResult(
    val success: Boolean,
    val message: String,
    val details: String?
)
