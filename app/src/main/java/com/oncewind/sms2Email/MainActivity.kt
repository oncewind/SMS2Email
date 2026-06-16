package com.oncewind.sms2Email

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.oncewind.sms2Email.data.LogCategory
import com.oncewind.sms2Email.ui.navigation.AppNavigation
import com.oncewind.sms2Email.ui.theme.SMSForwarderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 记录应用启动日志
        val runtimeLogRepo = AppContainer.getRuntimeLogRepository(this)
        runtimeLogRepo.logInfo(
            category = LogCategory.SYSTEM,
            title = "📱 应用已启动",
            message = "SMS Forwarder 应用已启动",
            details = "Android 版本: ${Build.VERSION.SDK_INT}\n设备: ${Build.MODEL}"
        )

        setContent {
            SMSForwarderTheme {
                RequestPermissions()
                AppNavigation()
            }
        }
    }
}

/**
 * 请求必要的运行时权限
 */
@Composable
fun RequestPermissions() {
    val context = LocalContext.current
    val requiredPermissions = remember {
        mutableListOf<String>().apply {
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= 33) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toList()
    }

    val allGranted = remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(
                    context,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        allGranted.value = permissionsMap.all { it.value }

        // 记录权限请求结果
        val runtimeLogRepo = AppContainer.getRuntimeLogRepository(context)
        val grantedCount = permissionsMap.count { it.value }
        val totalCount = permissionsMap.size

        if (permissionsMap.all { it.value }) {
            runtimeLogRepo.logInfo(
                category = LogCategory.PERMISSION,
                title = "✅ 所有权限已授予",
                message = "所有 $totalCount 项权限均已授予",
                details = "权限列表: ${requiredPermissions.joinToString(", ")}"
            )
        } else {
            val deniedPermissions = permissionsMap.filter { !it.value }.keys.joinToString(", ")
            val deniedCount = totalCount - grantedCount
            runtimeLogRepo.logWarning(
                category = LogCategory.PERMISSION,
                title = "⚠️ 部分权限被拒绝",
                message = "$deniedCount/$totalCount 项权限被拒绝",
                details = "被拒绝的权限: $deniedPermissions\n已授予的权限: ${permissionsMap.filter { it.value }.keys.joinToString(", ")}"
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!allGranted.value) {
            val runtimeLogRepo = AppContainer.getRuntimeLogRepository(context)
            runtimeLogRepo.logInfo(
                category = LogCategory.PERMISSION,
                title = "🔐 正在请求权限",
                message = "正在请求 ${requiredPermissions.size} 项必要权限",
                details = "需要以下权限\n${requiredPermissions.joinToString("\n") { "- $it" }}"
            )
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        } else {
            val runtimeLogRepo = AppContainer.getRuntimeLogRepository(context)
            runtimeLogRepo.logDebug(
                category = LogCategory.PERMISSION,
                title = "🔓 权限已就绪",
                message = "所有权限已在之前授予",
                details = "权限数量: ${requiredPermissions.size}"
            )
        }
    }
}