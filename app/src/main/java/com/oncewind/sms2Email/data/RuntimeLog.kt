package com.oncewind.sms2Email.data

/**
 * 应用运行时日志数据模型 */
data class RuntimeLog(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val category: LogCategory,
    val title: String,
    val message: String,
    val details: String? = null,
    val exceptionType: String? = null,
    val stackTrace: String? = null
)

/**
 * 日志级别
 */
enum class LogLevel {
    DEBUG,      // 调试信息
    INFO,       // 一般信息
    WARNING,    // 警告
    ERROR       // 错误
}

/**
 * 日志分类
 */
enum class LogCategory {
    SMS,        // 短信相关
    EMAIL,      // 邮件相关
    SERVICE,    // 服务相关
    PERMISSION, // 权限相关
    SYSTEM,     // 系统相关
    NETWORK     // 网络相关
}