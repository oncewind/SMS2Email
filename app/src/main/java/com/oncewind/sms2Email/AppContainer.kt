package com.oncewind.sms2Email

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.oncewind.sms2Email.data.ForwardLogRepository
import com.oncewind.sms2Email.data.RuntimeLogRepository
import com.oncewind.sms2Email.data.SettingsRepository

/**
 * 简单的依赖注入容器，管理全局单例
 */
object AppContainer {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    private var _settingsRepository: SettingsRepository? = null
    private var _forwardLogRepository: ForwardLogRepository? = null
    private var _runtimeLogRepository: RuntimeLogRepository? = null

    fun getSettingsRepository(context: Context): SettingsRepository {
        return _settingsRepository ?: SettingsRepository(context.dataStore).also {
            _settingsRepository = it
        }
    }

    fun getForwardLogRepository(context: Context): ForwardLogRepository {
        return _forwardLogRepository ?: ForwardLogRepository(context).also {
            _forwardLogRepository = it
        }
    }

    fun getRuntimeLogRepository(context: Context): RuntimeLogRepository {
        return _runtimeLogRepository ?: RuntimeLogRepository(context).also {
            _runtimeLogRepository = it
        }
    }
}
