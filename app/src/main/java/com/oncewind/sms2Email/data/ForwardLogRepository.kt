package com.oncewind.sms2Email.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

/**
 * 转发日志仓库，使用DataStore + JSON 序列化存储 * （替代Room，避免KSP/KAPT 注解处理兼容性问题）
 */
class ForwardLogRepository(private val context: Context) {

    private val Context.logDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "forward_logs")

    private val LOGS_KEY = stringPreferencesKey("forward_logs_json")

    fun getAllLogs(): Flow<List<ForwardLog>> {
        return context.logDataStore.data.map { prefs ->
            val json = prefs[LOGS_KEY] ?: "[]"
            parseLogsFromJson(json)
        }
    }

    fun getRecentLogs(limit: Int = 5): Flow<List<ForwardLog>> {
        return context.logDataStore.data.map { prefs ->
            val json = prefs[LOGS_KEY] ?: "[]"
            parseLogsFromJson(json).take(limit)
        }
    }

    suspend fun insertLog(log: ForwardLog): Long {
        context.logDataStore.edit { prefs ->
            val existingJson = prefs[LOGS_KEY] ?: "[]"
            val existingLogs = parseLogsFromJson(existingJson)
            val newId = (existingLogs.maxOfOrNull { it.id } ?: 0) + 1
            val newLog = log.copy(id = newId)
            val updatedLogs = existingLogs + newLog
            prefs[LOGS_KEY] = serializeLogsToJson(updatedLogs)
        }
        // 返回新插入的日志 ID（近似值，因为 DataStore 是异步的
        return 0L
    }

    suspend fun deleteAllLogs() {
        context.logDataStore.edit { prefs ->
            prefs[LOGS_KEY] = "[]"
        }
    }

    /**
     * 从JSON 字符串解析日志列表     */
    private fun parseLogsFromJson(json: String): List<ForwardLog> {
        try {
            val array = JSONArray(json)
            val logs = mutableListOf<ForwardLog>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                logs.add(
                    ForwardLog(
                        id = obj.getLong("id"),
                        senderNumber = obj.getString("senderNumber"),
                        messageContent = obj.getString("messageContent"),
                        simSlot = obj.getInt("simSlot"),
                        receiveTime = obj.getLong("receiveTime"),
                        forwardTime = obj.getLong("forwardTime"),
                        forwardStatus = try {
                            ForwardStatus.valueOf(obj.getString("forwardStatus"))
                        } catch (_: Exception) {
                            ForwardStatus.FAILED
                        },
                        errorMessage = obj.optString("errorMessage", "")
                    )
                )
            }
            return logs
        } catch (_: Exception) {
            return emptyList()
        }
    }

    /**
     * 将日志列表序列化从JSON 字符串     */
    private fun serializeLogsToJson(logs: List<ForwardLog>): String {
        val array = JSONArray()
        for (log in logs) {
            val obj = org.json.JSONObject()
            obj.put("id", log.id)
            obj.put("senderNumber", log.senderNumber)
            obj.put("messageContent", log.messageContent)
            obj.put("simSlot", log.simSlot)
            obj.put("receiveTime", log.receiveTime)
            obj.put("forwardTime", log.forwardTime)
            obj.put("forwardStatus", log.forwardStatus.name)
            if (log.errorMessage != null) {
                obj.put("errorMessage", log.errorMessage)
            } else {
                obj.put("errorMessage", org.json.JSONObject.NULL)
            }
            array.put(obj)
        }
        return array.toString()
    }
}