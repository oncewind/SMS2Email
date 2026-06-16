package com.oncewind.sms2Email.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.ClipboardManager
import androidx.core.content.ContextCompat
import com.oncewind.sms2Email.AppContainer
import com.oncewind.sms2Email.data.AppSettings
import com.oncewind.sms2Email.data.SmtpEncryption
import com.oncewind.sms2Email.data.SimSlotFilter
import com.oncewind.sms2Email.service.EmailTester
import com.oncewind.sms2Email.service.TestResult
import com.oncewind.sms2Email.util.ConfigEncryptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 配置ViewModel
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = AppContainer.getSettingsRepository(application)

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    // 编辑中的临时配置（界面输入的值）
    val editSettings = MutableStateFlow(AppSettings())

    // 是否正在保存
    val isSaving = MutableStateFlow(false)

    // 保存成功提示
    val saveSuccess = MutableStateFlow(false)

    // 测试邮箱连接相关状态
    val isTesting = MutableStateFlow(false)
    val testResult = MutableStateFlow<TestResult?>(null)

    /**
     * 从持久化配置加载到编辑状态     */
    fun loadSettings() {
        viewModelScope.launch {
            val current = settings.value
            editSettings.value = current
        }
    }

    /**
     * 保存配置DataStore
     */
    fun saveSettings() {
        viewModelScope.launch {
            isSaving.value = true
            settingsRepo.updateSettings(editSettings.value)
            isSaving.value = false
            saveSuccess.value = true
        }
    }

    /**
     * 重置保存成功提示
     */
    fun resetSaveSuccess() {
        saveSuccess.value = false
    }

    /**
     * 测试邮箱连接
     */
    fun testEmailConnection() {
        viewModelScope.launch {
            isTesting.value = true
            testResult.value = null

            val result = withContext(Dispatchers.IO) {
                EmailTester.testConnection(editSettings.value)
            }

            testResult.value = result
            isTesting.value = false
        }
    }

    /**
     * 重置测试结果
     */
    fun resetTestResult() {
        testResult.value = null
    }

    /**
     * 更新编辑中的配置字段
     */
    fun updateSenderEmail(value: String) {
        editSettings.value = editSettings.value.copy(senderEmail = value)
    }

    fun updateSenderPassword(value: String) {
        editSettings.value = editSettings.value.copy(senderPassword = value)
    }

    fun updateSmtpServer(value: String) {
        editSettings.value = editSettings.value.copy(smtpServer = value)
    }

    fun updateSmtpPort(value: Int) {
        editSettings.value = editSettings.value.copy(smtpPort = value)
    }

    fun updateSmtpEncryption(value: SmtpEncryption) {
        editSettings.value = editSettings.value.copy(smtpEncryption = value)
    }

    fun updateRecipientEmail(value: String) {
        editSettings.value = editSettings.value.copy(recipientEmail = value)
    }

    fun updateMonitoredSimSlot(value: SimSlotFilter) {
        editSettings.value = editSettings.value.copy(monitoredSimSlot = value)
    }

    /**
     * 导出配置到剪贴板
     */
    fun exportConfig(): Boolean {
        val config = editSettings.value
        val exportData = buildString {
            appendLine("senderEmail=${encrypt(config.senderEmail)}")
            appendLine("senderPassword=${encrypt(config.senderPassword)}")
            appendLine("recipientEmail=${encrypt(config.recipientEmail)}")
            appendLine("smtpServer=${encrypt(config.smtpServer)}")
            appendLine("smtpPort=${config.smtpPort}")
            appendLine("smtpEncryption=${config.smtpEncryption.name}")
        }
        
        val context = getApplication<Application>()
        val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        if (clipboard != null) {
            val clip = android.content.ClipData.newPlainText("SMS Forwarder Config", exportData)
            clipboard.setPrimaryClip(clip)
            return true
        }
        return false
    }

    /**
     * 导入配置
     */
    fun importConfig(importData: String): Boolean {
        return try {
            val lines = importData.lines()
            val configMap = mutableMapOf<String, String>()
            
            for (line in lines) {
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    configMap[parts[0].trim()] = parts[1].trim()
                }
            }

            val newSettings = AppSettings(
                senderEmail = decrypt(configMap["senderEmail"] ?: ""),
                senderPassword = decrypt(configMap["senderPassword"] ?: ""),
                recipientEmail = decrypt(configMap["recipientEmail"] ?: ""),
                smtpServer = decrypt(configMap["smtpServer"] ?: ""),
                smtpPort = configMap["smtpPort"]?.toIntOrNull() ?: 465,
                smtpEncryption = try {
                    SmtpEncryption.valueOf(configMap["smtpEncryption"] ?: "SSL")
                } catch (e: Exception) {
                    SmtpEncryption.SSL
                },
                monitoredSimSlot = editSettings.value.monitoredSimSlot,
                serviceEnabled = editSettings.value.serviceEnabled
            )

            editSettings.value = newSettings
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun encrypt(text: String): String {
        return ConfigEncryptor.encrypt(text)
    }

    private fun decrypt(text: String): String {
        return ConfigEncryptor.decrypt(text)
    }
}