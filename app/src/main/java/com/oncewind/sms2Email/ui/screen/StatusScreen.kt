package com.oncewind.sms2Email.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oncewind.sms2Email.data.ForwardLog
import com.oncewind.sms2Email.data.ForwardStatus
import com.oncewind.sms2Email.viewmodel.StatusViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 状态总览界面 - 显示服务运行状态、快捷开关、最近转发记 */
@Composable
fun StatusScreen(viewModel: StatusViewModel) {
    val settings by viewModel.settings.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()

    // 每次进入页面时检查服务状态
    LaunchedEffect(Unit) {
        viewModel.checkServiceRunning()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== 服务状态卡=====
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceRunning) {
                        Color(0xFFE8F5E9) // 绿色背景
                    } else {
                        Color(0xFFFFEBEE) // 红色背景
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isServiceRunning) "服务运行" else "服务已停止",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isServiceRunning) {
                                    Color(0xFF2E7D32)
                                } else {
                                    Color(0xFFC62828)
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isServiceRunning) {
                                    "正在监控短信并转发到 ${settings.recipientEmail}"
                                } else {
                                    "点击开关启动短信监控服"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = isServiceRunning,
                            onCheckedChange = viewModel::toggleService
                        )
                    }
                }
            }

            // ===== 配置摘要 =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "配置摘要",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigSummaryRow("发送邮箱", settings.senderEmail.ifEmpty { "未设置" })
                    ConfigSummaryRow("SMTP服务器", "${settings.smtpServer}:${settings.smtpPort}")
                    ConfigSummaryRow("加密方式", settings.smtpEncryption.name)
                    ConfigSummaryRow("接收邮箱", settings.recipientEmail.ifEmpty { "未设置 "})
                    ConfigSummaryRow(
                        "监控卡槽",
                        when (settings.monitoredSimSlot) {
                            com.oncewind.sms2Email.data.SimSlotFilter.ALL -> "所有卡槽"
                            com.oncewind.sms2Email.data.SimSlotFilter.SIM_1 -> "仅卡1"
                            com.oncewind.sms2Email.data.SimSlotFilter.SIM_2 -> "仅卡2"
                        }
                    )
                }
            }

            // ===== 最近转发记=====
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "最近转发记录",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (recentLogs.isEmpty()) {
                        Text(
                            text = "暂无转发记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentLogs.forEach { log ->
                                RecentLogItem(log)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun RecentLogItem(log: ForwardLog) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val successColor = Color(0xFF2E7D32)
    val failColor = Color(0xFFC62828)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 状态指
        Text(
            text = if (log.forwardStatus == ForwardStatus.SUCCESS) "OK" else "FAIL",
            style = MaterialTheme.typography.labelLarge,
            color = if (log.forwardStatus == ForwardStatus.SUCCESS) successColor else failColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        // 发送人号码
        Text(
            text = log.senderNumber,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        // 时间
        Text(
            text = timeFormat.format(Date(log.receiveTime)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}