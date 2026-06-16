package com.oncewind.sms2Email.viewmodel

import android.app.Application
import android.app.job.JobScheduler
import android.content.Intent
import android.content.ServiceConnection
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oncewind.sms2Email.AppContainer
import com.oncewind.sms2Email.data.AppSettings
import com.oncewind.sms2Email.data.ForwardLog
import com.oncewind.sms2Email.data.LogCategory
//import com.oncewind.sms2Email.receiver.SmsForwardJobService
import com.oncewind.sms2Email.service.SmsMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 状态总览ViewModel
 */
class StatusViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = AppContainer.getSettingsRepository(application)
    private val logRepo = AppContainer.getForwardLogRepository(application)
    private val runtimeLogRepo = AppContainer.getRuntimeLogRepository(application)

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val recentLogs: StateFlow<List<ForwardLog>> = logRepo.getRecentLogs(5)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allLogs: StateFlow<List<ForwardLog>> = logRepo.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

    private var serviceConnection: ServiceConnection? = null

    init {
        checkServiceRunning()
    }

    /**
     * 检查服务是否正在运行，以实际运行状态为准更新开关显示
     * 只更新UI状态，不修改配置（保留用户原有的设置）
     */
    fun checkServiceRunning() {
        val context = getApplication<Application>()
        
        // 检查前台服务是否运行
        val isForegroundServiceRunning = try {
            val manager = context.getSystemService(android.app.ActivityManager::class.java)
            manager?.getRunningServices(Int.MAX_VALUE)?.any {
                it.service.className == SmsMonitorService::class.java.name
            } ?: false
        } catch (e: Exception) {
            false
        }

        // 检查JobService是否有待处理的任务
//        val jobScheduler = context.getSystemService(JobScheduler::class.java)
//        val hasPendingJobs = jobScheduler?.allPendingJobs?.any {
//            it.service.className == SmsForwardJobService::class.java.name
//        } ?: false
//
//        val isActuallyRunning = isForegroundServiceRunning || hasPendingJobs
        
        // 以实际服务运行状态为准更新开关状态（只更新UI，不修改配置）
        _isServiceRunning.update { isForegroundServiceRunning }
    }

    /**
     * 启动服务（内部方法）
     */
    private fun startService() {
        val context = getApplication<Application>()
        val intent = Intent(context, SmsMonitorService::class.java).apply {
            action = SmsMonitorService.ACTION_START
        }
        try {
            ContextCompat.startForegroundService(context, intent)
            _isServiceRunning.update { true }
            
            runtimeLogRepo.logInfo(
                category = LogCategory.SERVICE,
                title = "服务启动成功",
                message = "短信监控服务已启动",
                details = null
            )
        } catch (e: Exception) {
            runtimeLogRepo.logErrorWithException(
                category = LogCategory.SERVICE,
                title = "服务启动失败",
                message = "无法启动短信监控服务",
                exception = e
            )
            _isServiceRunning.update { false }
        }
    }

    /**
     * 停止服务（内部方法）
     */
    private fun stopService() {
        val context = getApplication<Application>()
        
        // 取消所有待处理的短信转发任务
        val jobScheduler = context.getSystemService(JobScheduler::class.java)
        jobScheduler?.cancelAll()
        runtimeLogRepo.logInfo(
            category = LogCategory.SERVICE,
            title = "📋 任务已取消",
            message = "所有待处理的短信转发任务已取消",
            details = null
        )
        
        val intent = Intent(context, SmsMonitorService::class.java).apply {
            action = SmsMonitorService.ACTION_STOP
        }
        context.startService(intent)
        _isServiceRunning.update { false }
        
        runtimeLogRepo.logInfo(
            category = LogCategory.SERVICE,
            title = "服务已停止",
            message = "短信监控服务已停止",
            details = null
        )
    }

    /**
     * 启动/停止监控服务
     */
    fun toggleService(enabled: Boolean) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            
            if (enabled) {
                // 启动服务
                runtimeLogRepo.logInfo(
                    category = LogCategory.SERVICE,
                    title = "🚀 启动服务",
                    message = "用户请求启动短信监控服务",
                    details = null
                )
                
                val currentSettings = settings.value.copy(serviceEnabled = true)
                settingsRepo.updateSettings(currentSettings)
                
                startService()
            } else {
                // 停止服务
                runtimeLogRepo.logInfo(
                    category = LogCategory.SERVICE,
                    title = "🛑 停止服务",
                    message = "用户请求停止短信监控服务",
                    details = null
                )
                
                val currentSettings = settings.value.copy(serviceEnabled = false)
                settingsRepo.updateSettings(currentSettings)
                
                stopService()
            }
        }
    }

    /**
     * 清空日志
     */
    fun clearLogs() {
        viewModelScope.launch {
            logRepo.deleteAllLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        serviceConnection?.let {
            getApplication<Application>().unbindService(it)
        }
    }
}