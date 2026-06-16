package com.oncewind.sms2Email.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 权限说明帮助页面
 */
@Composable
fun HelpScreen() {
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 页面标题
            item {
                Column {
                    Text(
                        text = "权限设置指南",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "为确保短信转发功能正常工作，请按以下步骤配置权限",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // 通用配置
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "通用设置",
                                modifier = Modifier.padding(end = 8.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "通用设置",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        PermissionItem(
                            name = "短信权限 (RECEIVE_SMS, READ_SMS)",
                            description = "用于接收和读取短信内容"
                        )
                        PermissionItem(
                            name = "电话状态权(READ_PHONE_STATE)",
                            description = "用于获取SIM卡槽信息"
                        )
                        PermissionItem(
                            name = "通知权限 (POST_NOTIFICATIONS)",
                            description = "用于显示前台服务通知"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "💡 提示",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "如果短信转发不工作，请检查上述所有设置\n建议将应用加入系统白名单\n更新应用后可能需要重新配置权限",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // 小米手机配置
            item {
                PhoneBrandCard(
                    brand = "小米 / Redmi",
                    color = Color(0xFFFF6900),
                    steps = listOf(
                        "长按应用图标 进入应用信息 ",
                        "权限管理 读取短信开启 通知类短信开启",
                        "电池优化 选择 \"不优化\"",
                        "后台管理 选择 \"无限制\"",
                        "在最近任务中，长按应用图点击锁定"
                    )
                )
            }

            // 华为手机配置
            item {
                PhoneBrandCard(
                    brand = "华为 / Honor",
                    color = Color(0xFFCF0A2C),
                    steps = listOf(
                        "设置 应用 应用管理 搜索 \"SMS2Email\"",
                        "权限 开启短信权限 电话权限 通知权限",
                        "设置中搜索“验证码安全保护”，找到后关闭验证码安全保护",
                        "电池管理 启动管理 手动管理",
                        "开\"允许自启动\"、\"允许后台活动\"、\"允许关联启动\"",
                        "在最近任务中，长按应用图点击锁定"
                    )
                )
            }

            // OPPO手机配置
            item {
                PhoneBrandCard(
                    brand = "OPPO / Realme",
                    color = Color(0xFF1DB954),
                    steps = listOf(
                        "设置 应用管理 搜索 \"SMS2Email\"",
                        "权限 开启所有权限",
                        "耗电管理 选择 \"允许后台耗电\"",
                        "自启动管开\"短信转发\"",
                        "在最近任务中，下拉应用卡点击锁定"
                    )
                )
            }

            // VIVO手机配置
            item {
                PhoneBrandCard(
                    brand = "VIVO / iQOO",
                    color = Color(0xFF415FFF),
                    steps = listOf(
                        "设置 应用与权应用管理 搜索 \"SMS2Email\"",
                        "权限 开启所有权限",
                        "电池 后台耗电管理 选择 \"允许后台运行\"",
                        "自启开\"短信转发\"",
                        "在最近任务中，长按应用图点击锁定"
                    )
                )
            }

            // 三星手机配置
            item {
                PhoneBrandCard(
                    brand = "三星",
                    color = Color(0xFF1428A0),
                    steps = listOf(
                        "设置 应用程序 搜索 \"SMS2Email\"",
                        "权限 开启所有权限",
                        "电池 电池使用选择 \"优化\" \"不受限制\"",
                        "在最近任务中，点击应用图标右上角 点击锁定"
                    )
                )
            }

            
        }
    }
}

/**
 * 手机品牌配置卡片
 */
@Composable
private fun PhoneBrandCard(brand: String, color: Color, steps: List<String>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = color),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = brand,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "设置步骤",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            steps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 权限 */
@Composable
private fun PermissionItem(name: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "✔️",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}