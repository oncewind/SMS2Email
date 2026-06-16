package com.oncewind.sms2Email.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oncewind.sms2Email.AppContainer
import com.oncewind.sms2Email.data.LogCategory
import com.oncewind.sms2Email.data.RuntimeLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 日志页面 ViewModel
 */
class LogViewModel(application: Application) : AndroidViewModel(application) {

    private val runtimeLogRepo = AppContainer.getRuntimeLogRepository(application)

    val runtimeLogs: StateFlow<List<RuntimeLog>> = runtimeLogRepo.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentLogs: StateFlow<List<RuntimeLog>> = runtimeLogRepo.getRecentLogs(50)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun logInfo(category: LogCategory, title: String, message: String, details: String? = null) {
        runtimeLogRepo.logInfo(category, title, message, details)
    }

    fun logWarning(category: LogCategory, title: String, message: String, details: String? = null) {
        runtimeLogRepo.logWarning(category, title, message, details)
    }

    fun logError(category: LogCategory, title: String, message: String, details: String? = null) {
        runtimeLogRepo.logError(category, title, message, details)
    }

    fun logDebug(category: LogCategory, title: String, message: String, details: String? = null) {
        runtimeLogRepo.logDebug(category, title, message, details)
    }

    fun clearLogs() {
        viewModelScope.launch {
            runtimeLogRepo.clearLogs()
        }
    }
}
