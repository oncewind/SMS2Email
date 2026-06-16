package com.oncewind.sms2Email.data

/**
 * 转发日志数据模型
 * （不再使用Room Entity，改为DataStore + JSON 存储 */
data class ForwardLog(
    val id: Long = 0,
    val senderNumber: String,
    val messageContent: String,
    val simSlot: Int,
    val receiveTime: Long,
    val forwardTime: Long,
    val forwardStatus: ForwardStatus,
    val errorMessage: String? = null
)

/**
 * 转发状态 */
enum class ForwardStatus {
    SUCCESS,
    FAILED,
    PENDING
}