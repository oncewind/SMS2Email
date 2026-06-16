package com.oncewind.sms2Email.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray

/**
 * 应用运行时日志仓库
 */
class RuntimeLogRepository(private val context: Context) {

    private val Context.logDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "runtime_logs")

    private val LOGS_KEY = stringPreferencesKey("runtime_logs_json")
    private val MAX_LOGS = 500

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getAllLogs(): Flow<List<RuntimeLog>> {
        return context.logDataStore.data.map { prefs ->
            val json = prefs[LOGS_KEY] ?: "[]"
            parseLogsFromJson(json).sortedByDescending { it.timestamp }
        }
    }

    fun getRecentLogs(limit: Int = 50): Flow<List<RuntimeLog>> {
        return context.logDataStore.data.map { prefs ->
            val json = prefs[LOGS_KEY] ?: "[]"
            parseLogsFromJson(json)
                .sortedByDescending { it.timestamp }
                .take(limit)
        }
    }

    fun getLogsByLevel(level: LogLevel): Flow<List<RuntimeLog>> {
        return context.logDataStore.data.map { prefs ->
            val json = prefs[LOGS_KEY] ?: "[]"
            parseLogsFromJson(json)
                .filter { it.level == level }
                .sortedByDescending { it.timestamp }
        }
    }

    fun getLogsByCategory(category: LogCategory): Flow<List<RuntimeLog>> {
        return context.logDataStore.data.map { prefs ->
            val json = prefs[LOGS_KEY] ?: "[]"
            parseLogsFromJson(json)
                .filter { it.category == category }
                .sortedByDescending { it.timestamp }
        }
    }

    fun insertLog(log: RuntimeLog) {
        scope.launch {
            context.logDataStore.edit { prefs ->
                val existingJson = prefs[LOGS_KEY] ?: "[]"
                val existingLogs = parseLogsFromJson(existingJson)
                val newId = (existingLogs.maxOfOrNull { it.id } ?: 0) + 1
                val newLog = log.copy(id = newId)
                val updatedLogs = existingLogs + newLog
                // 限制日志数量，防止数据过多
                val trimmedLogs = if (updatedLogs.size > MAX_LOGS) {
                    updatedLogs.sortedByDescending { it.timestamp }.take(MAX_LOGS)
                } else {
                    updatedLogs
                }
                prefs[LOGS_KEY] = serializeLogsToJson(trimmedLogs)
            }
        }
    }

    /**
     * 阻塞式写入日志，在 BroadcastReceiver 等短生命周期场景中使用
     * 确保日志在进程被杀前完成持久化
     */
    suspend fun insertLogBlocking(log: RuntimeLog) {
        context.logDataStore.edit { prefs ->
            val existingJson = prefs[LOGS_KEY] ?: "[]"
            val existingLogs = parseLogsFromJson(existingJson)
            val newId = (existingLogs.maxOfOrNull { it.id } ?: 0) + 1
            val newLog = log.copy(id = newId)
            val updatedLogs = existingLogs + newLog
            val trimmedLogs = if (updatedLogs.size > MAX_LOGS) {
                updatedLogs.sortedByDescending { it.timestamp }.take(MAX_LOGS)
            } else {
                updatedLogs
            }
            prefs[LOGS_KEY] = serializeLogsToJson(trimmedLogs)
        }
    }

    fun logInfo(category: LogCategory, title: String, message: String, details: String? = null) {
        insertLog(RuntimeLog(
            level = LogLevel.INFO,
            category = category,
            title = title,
            message = message,
            details = details
        ))
    }

    fun logWarning(category: LogCategory, title: String, message: String, details: String? = null) {
        insertLog(RuntimeLog(
            level = LogLevel.WARNING,
            category = category,
            title = title,
            message = message,
            details = details
        ))
    }

    fun logError(category: LogCategory, title: String, message: String, details: String? = null) {
        insertLog(RuntimeLog(
            level = LogLevel.ERROR,
            category = category,
            title = title,
            message = message,
            details = details
        ))
    }

    /**
     * 记录异常错误日志
     */
    fun logErrorWithException(
        category: LogCategory,
        title: String,
        message: String,
        exception: Exception
    ) {
        insertLog(RuntimeLog(
            level = LogLevel.ERROR,
            category = category,
            title = title,
            message = message,
            details = exception.message,
            exceptionType = exception.javaClass.simpleName,
            stackTrace = exception.stackTraceToString().take(1000)
        ))
    }

    fun logDebug(category: LogCategory, title: String, message: String, details: String? = null) {
        insertLog(RuntimeLog(
            level = LogLevel.DEBUG,
            category = category,
            title = title,
            message = message,
            details = details
        ))
    }

    suspend fun deleteAllLogs() {
        context.logDataStore.edit { prefs ->
            prefs[LOGS_KEY] = "[]"
        }
    }

    suspend fun clearLogs() {
        deleteAllLogs()
    }

    private fun parseLogsFromJson(json: String): List<RuntimeLog> {
        try {
            val array = JSONArray(json)
            val logs = mutableListOf<RuntimeLog>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                logs.add(
                    RuntimeLog(
                        id = obj.getLong("id"),
                        timestamp = obj.getLong("timestamp"),
                        level = try {
                            LogLevel.valueOf(obj.getString("level"))
                        } catch (_: Exception) {
                            LogLevel.INFO
                        },
                        category = try {
                            LogCategory.valueOf(obj.getString("category"))
                        } catch (_: Exception) {
                            LogCategory.SYSTEM
                        },
                        title = obj.getString("title"),
                        message = obj.getString("message"),
                        details = obj.optString("details", ""),
                        exceptionType = obj.optString("exceptionType", ""),
                        stackTrace = obj.optString("stackTrace", "")
                    )
                )
            }
            return logs
        } catch (_: Exception) {
            return emptyList()
        }
    }

    private fun serializeLogsToJson(logs: List<RuntimeLog>): String {
        val array = JSONArray()
        for (log in logs) {
            val obj = org.json.JSONObject()
            obj.put("id", log.id)
            obj.put("timestamp", log.timestamp)
            obj.put("level", log.level.name)
            obj.put("category", log.category.name)
            obj.put("title", log.title)
            obj.put("message", log.message)
            if (log.details != null) {
                obj.put("details", log.details)
            }
            if (log.exceptionType != null) {
                obj.put("exceptionType", log.exceptionType)
            }
            if (log.stackTrace != null) {
                obj.put("stackTrace", log.stackTrace)
            }
            array.put(obj)
        }
        return array.toString()
    }
}