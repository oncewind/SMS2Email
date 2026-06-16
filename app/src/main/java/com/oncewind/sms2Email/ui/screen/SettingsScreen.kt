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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.oncewind.sms2Email.data.SmtpEncryption
import com.oncewind.sms2Email.data.SimSlotFilter
import com.oncewind.sms2Email.viewmodel.SettingsViewModel

/**
 * 配置设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val editSettings by viewModel.editSettings.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val testResult by viewModel.testResult.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // 导入对话框状态
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    // Snackbar 消息状态
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // 显示 Snackbar
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    // 首次加载时读取持久化配置
    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    // 保存成功提示
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            snackbarHostState.showSnackbar("配置已保存")
            viewModel.resetSaveSuccess()
            focusManager.clearFocus()
        }
    }

    // 测试结果提示
    LaunchedEffect(testResult) {
        testResult?.let { result ->
            snackbarHostState.showSnackbar(
                message = result.message,
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ===== 发送邮箱配置=====
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "发送邮箱配置",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editSettings.senderEmail,
                        onValueChange = viewModel::updateSenderEmail,
                        label = { Text("发送邮箱地址") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editSettings.senderPassword,
                        onValueChange = viewModel::updateSenderPassword,
                        label = { Text("密码/授权") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "提示：QQ/163/126邮箱需使用授权码，139/189邮箱使用登录密码",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = viewModel::testEmailConnection,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        enabled = !isTesting && !isSaving
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("测试..")
                        } else {
                            Text("测试连接")
                        }
                    }
                    // 测试结果详情显示
                    testResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (result.success) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (result.success) "测试成功" else "测试失败",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                result.details?.let { details ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = details,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ===== SMTP 服务器配=====
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SMTP 服务器",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // SMTP 加密方式下拉选择
                    var encryptionExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = encryptionExpanded,
                        onExpandedChange = { encryptionExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = when (editSettings.smtpEncryption) {
                                SmtpEncryption.SSL -> "SSL (端口465)"
                                SmtpEncryption.TLS -> "TLS/STARTTLS (端口587)"
                                SmtpEncryption.NONE -> "无加密"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("加密方式") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = encryptionExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = encryptionExpanded,
                            onDismissRequest = { encryptionExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("SSL (端口465)") },
                                onClick = {
                                    viewModel.updateSmtpEncryption(SmtpEncryption.SSL)
                                    viewModel.updateSmtpPort(465)
                                    encryptionExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("TLS/STARTTLS (端口587)") },
                                onClick = {
                                    viewModel.updateSmtpEncryption(SmtpEncryption.TLS)
                                    viewModel.updateSmtpPort(587)
                                    encryptionExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("无加密") },
                                onClick = {
                                    viewModel.updateSmtpEncryption(SmtpEncryption.NONE)
                                    encryptionExpanded = false
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editSettings.smtpServer,
                        onValueChange = viewModel::updateSmtpServer,
                        label = { Text("SMTP 服务器地址") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editSettings.smtpPort.toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { viewModel.updateSmtpPort(it) }
                        },
                        label = { Text("SMTP 端口") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 快捷 SMTP 预设按钮
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "快捷预设置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.updateSmtpServer("smtp.qq.com")
                                viewModel.updateSmtpPort(465)
                                viewModel.updateSmtpEncryption(SmtpEncryption.SSL)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("QQ邮箱") }
                        Button(
                            onClick = {
                                viewModel.updateSmtpServer("smtp.163.com")
                                viewModel.updateSmtpPort(465)
                                viewModel.updateSmtpEncryption(SmtpEncryption.SSL)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("163邮箱") }
                        Button(
                            onClick = {
                                viewModel.updateSmtpServer("smtp.126.com")
                                viewModel.updateSmtpPort(465)
                                viewModel.updateSmtpEncryption(SmtpEncryption.SSL)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("126邮箱") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.updateSmtpServer("smtp.139.com")
                                viewModel.updateSmtpPort(465)
                                viewModel.updateSmtpEncryption(SmtpEncryption.SSL)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("139邮箱") }
                        Button(
                            onClick = {
                                viewModel.updateSmtpServer("smtp.189.com")
                                viewModel.updateSmtpPort(465)
                                viewModel.updateSmtpEncryption(SmtpEncryption.SSL)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("189邮箱") }
                        Button(
                            onClick = {
                                viewModel.updateSmtpServer("smtp.aliyun.com")
                                viewModel.updateSmtpPort(465)
                                viewModel.updateSmtpEncryption(SmtpEncryption.SSL)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("阿里邮箱") }
                    }
                }
            }

            // ===== 接收邮箱配置 =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "接收邮箱",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editSettings.recipientEmail,
                        onValueChange = viewModel::updateRecipientEmail,
                        label = { Text("接收转发邮件的邮箱地址") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // ===== SIM 卡槽监控配置 =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SIM 卡槽监控",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    var simExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = simExpanded,
                        onExpandedChange = { simExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = when (editSettings.monitoredSimSlot) {
                                SimSlotFilter.ALL -> "所有卡槽"
                                SimSlotFilter.SIM_1 -> "仅卡1 (SIM1)"
                                SimSlotFilter.SIM_2 -> "仅卡2 (SIM2)"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("监控卡槽") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = simExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = simExpanded,
                            onDismissRequest = { simExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("所有卡槽") },
                                onClick = {
                                    viewModel.updateMonitoredSimSlot(SimSlotFilter.ALL)
                                    simExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("仅卡1 (SIM1)") },
                                onClick = {
                                    viewModel.updateMonitoredSimSlot(SimSlotFilter.SIM_1)
                                    simExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("仅卡2 (SIM2)") },
                                onClick = {
                                    viewModel.updateMonitoredSimSlot(SimSlotFilter.SIM_2)
                                    simExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ===== 导入导出按钮 =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "配置导入导出",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = {
                            val success = viewModel.exportConfig()
                            snackbarMessage = if (success) "配置已复制到剪贴" else "导出失败"
                        },
                            modifier = Modifier.weight(1f),
                            enabled = !isTesting && !isSaving
                        ) {
                            Text("导出配置")
                        }
                        Button(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled = !isTesting && !isSaving
                        ) {
                            Text("导入配置")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ===== 保存按钮 =====
            Button(
                onClick = viewModel::saveSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isTesting && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(24.dp)
                            .height(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存储..")
                } else {
                    Text("保存配置")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 导入配置对话
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入配置") },
            text = {
                Column {
                    Text("请粘贴导出的配置内容")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("配置内容") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 6,
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.importConfig(importText)
                        snackbarMessage = if (success) "配置导入成功" else "导入失败，请检查配置格式"
                        showImportDialog = false
                        importText = ""
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showImportDialog = false
                        importText = ""
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}
