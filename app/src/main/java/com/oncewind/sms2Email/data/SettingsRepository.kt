package com.oncewind.sms2Email.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 配置持久化仓库，使用 DataStore (Preferences)
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val SENDER_EMAIL = stringPreferencesKey("sender_email")
        val SENDER_PASSWORD = stringPreferencesKey("sender_password")
        val SMTP_SERVER = stringPreferencesKey("smtp_server")
        val SMTP_PORT = intPreferencesKey("smtp_port")
        val SMTP_ENCRYPTION = stringPreferencesKey("smtp_encryption")
        val RECIPIENT_EMAIL = stringPreferencesKey("recipient_email")
        val MONITORED_SIM_SLOT = stringPreferencesKey("monitored_sim_slot")
        val SERVICE_ENABLED = stringPreferencesKey("service_enabled")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            senderEmail = prefs[Keys.SENDER_EMAIL] ?: "",
            senderPassword = prefs[Keys.SENDER_PASSWORD] ?: "",
            smtpServer = prefs[Keys.SMTP_SERVER] ?: "smtp.gmail.com",
            smtpPort = prefs[Keys.SMTP_PORT] ?: 465,
            smtpEncryption = try {
                SmtpEncryption.valueOf(prefs[Keys.SMTP_ENCRYPTION] ?: "SSL")
            } catch (_: IllegalArgumentException) {
                SmtpEncryption.SSL
            },
            recipientEmail = prefs[Keys.RECIPIENT_EMAIL] ?: "",
            monitoredSimSlot = try {
                SimSlotFilter.valueOf(prefs[Keys.MONITORED_SIM_SLOT] ?: "ALL")
            } catch (_: IllegalArgumentException) {
                SimSlotFilter.ALL
            },
            serviceEnabled = prefs[Keys.SERVICE_ENABLED]?.toBooleanStrictOrNull() ?: false
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.SENDER_EMAIL] = settings.senderEmail
            prefs[Keys.SENDER_PASSWORD] = settings.senderPassword
            prefs[Keys.SMTP_SERVER] = settings.smtpServer
            prefs[Keys.SMTP_PORT] = settings.smtpPort
            prefs[Keys.SMTP_ENCRYPTION] = settings.smtpEncryption.name
            prefs[Keys.RECIPIENT_EMAIL] = settings.recipientEmail
            prefs[Keys.MONITORED_SIM_SLOT] = settings.monitoredSimSlot.name
            prefs[Keys.SERVICE_ENABLED] = settings.serviceEnabled.toString()
        }
    }
}