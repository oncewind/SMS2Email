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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oncewind.sms2Email.data.ForwardLog
import com.oncewind.sms2Email.data.ForwardStatus
import com.oncewind.sms2Email.data.LogCategory
import com.oncewind.sms2Email.data.LogLevel
import com.oncewind.sms2Email.data.RuntimeLog
import com.oncewind.sms2Email.viewmodel.LogViewModel
import com.oncewind.sms2Email.viewmodel.StatusViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 转发日志列表界面
 */
@Composable
fun LogScreen(
    statusViewModel: StatusViewModel,
    logViewModel: LogViewModel
) {
    val allForwardLogs by statusViewModel.allLogs.collectAsState()
    val runtimeLogs by logViewModel.runtimeLogs.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("转发日志", "运行日志")

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = tabs[selectedTab],
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = {
                    if (selectedTab == 0) {
                        statusViewModel.clearLogs()
                    } else {
                        logViewModel.clearLogs()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "清空",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab 选择
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = "$title (${
                                    if (index == 0) allForwardLogs.size else runtimeLogs.size
                                })"
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 内容区域
            when (selectedTab) {
                0 -> ForwardLogsList(logs = allForwardLogs)
                1 -> RuntimeLogsList(logs = runtimeLogs)
            }
        }
    }
}

/**
 * 转发日志列表
 */
@Composable
private fun ForwardLogsList(logs: List<ForwardLog>) {
    if (logs.isEmpty()) {
        EmptyState(
            title = "暂无转发记录",
            subtitle = "启动监控服务后，转发记录将在此显示"
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs, key = { it.id }) { log ->
                ForwardLogCard(log)
            }
        }
    }
}

/**
 * 运行日志列表
 */
@Composable
private fun RuntimeLogsList(logs: List<RuntimeLog>) {
    if (logs.isEmpty()) {
        EmptyState(
            title = "暂无运行日志",
            subtitle = "应用运行时日志将在此显示"
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs, key = { it.id }) { log ->
                RuntimeLogCard(log)
            }
        }
    }
}

/**
 * 空状态提 */
@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 转发日志卡片
 */
@Composable
private fun ForwardLogCard(log: ForwardLog) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val successColor = Color(0xFF2E7D32)
    val failColor = Color(0xFFC62828)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (log.forwardStatus == ForwardStatus.SUCCESS) {
                Color(0xFFE8F5E9)
            } else {
                Color(0xFFFFEBEE)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (log.forwardStatus == ForwardStatus.SUCCESS) "转发成功" else "转发失败",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (log.forwardStatus == ForwardStatus.SUCCESS) successColor else failColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateFormat.format(Date(log.receiveTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "来自: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = log.senderNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "卡槽: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (log.simSlot) {
                        1 -> "SIM1"
                        2 -> "SIM2"
                        else -> "未知"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.messageContent,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )
            if (log.errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "错误: ${log.errorMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = failColor
                )
            }
        }
    }
}

/**
 * 运行日志卡片
 */
@Composable
private fun RuntimeLogCard(log: RuntimeLog) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val backgroundColor = when (log.level) {
        LogLevel.DEBUG -> Color(0xFFF5F5F5)
        LogLevel.INFO -> Color(0xFFE3F2FD)
        LogLevel.WARNING -> Color(0xFFFFF3E0)
        LogLevel.ERROR -> Color(0xFFFFEBEE)
    }
    val levelColor = when (log.level) {
        LogLevel.DEBUG -> Color(0xFF9E9E9E)
        LogLevel.INFO -> Color(0xFF2196F3)
        LogLevel.WARNING -> Color(0xFFFF9800)
        LogLevel.ERROR -> Color(0xFFF44336)
    }
    val categoryColor = when (log.category) {
        LogCategory.SMS -> Color(0xFF4CAF50)
        LogCategory.EMAIL -> Color(0xFF9C27B0)
        LogCategory.SERVICE -> Color(0xFF2196F3)
        LogCategory.PERMISSION -> Color(0xFFFF9800)
        LogCategory.SYSTEM -> Color(0xFF607D8B)
        LogCategory.NETWORK -> Color(0xFF00BCD4)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // 级别标签
                    Card(
                        colors = CardDefaults.cardColors(containerColor = levelColor),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = log.level.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    // 分类标签
                    Card(
                        colors = CardDefaults.cardColors(containerColor = categoryColor),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = log.category.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    // 标题
                    Text(
                        text = log.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = dateFormat.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 消息
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 异常类型（如果有
            if (log.exceptionType != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "异常类型: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = log.exceptionType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 详情（如果有
            if (log.details != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = log.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 堆栈跟踪（如果有
            if (log.stackTrace != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A)
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "堆栈跟踪:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFBB86FC),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = log.stackTrace,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE0E0E0),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}