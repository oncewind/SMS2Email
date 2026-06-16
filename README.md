# SMS Forwarder

一个 Android 短信转发应用，能够监控特定 SIM 卡的短信，并自动转发到配置的邮箱。

## 功能特性

- **短信监控**：支持监听指定 SIM 卡的短信接收
- **邮件转发**：自动将短信内容转发到配置的邮箱
- **双卡支持**：支持选择监控 SIM1、SIM2 或全部卡槽
- **邮件预设**：内置 QQ 邮箱、163 邮箱、126 邮箱等主流邮件服务器配置
- **运行日志**：实时记录应用运行状态和错误信息
- **配置导入导出**：支持配置的加密导入和导出
- **服务控制**：通过开关控制监控服务的启停

## 技术栈

- Kotlin
- Jetpack Compose
- WorkManager / JobScheduler
- JavaMail API
- Material3 Design

## 权限需求

- RECEIVE_SMS：接收短信
- READ_SMS：读取短信内容
- READ_PHONE_STATE：获取 SIM 卡信息
- INTERNET：发送邮件
- FOREGROUND_SERVICE：前台服务
- WAKE_LOCK：保持设备唤醒

## 使用方法

1. 安装应用后，进入配置页面
2. 设置发送邮箱和接收邮箱
3. 选择要监控的 SIM 卡
4. 在状态页面开启监控服务
5. 收到新短信时会自动转发到配置的邮箱

## 构建

```bash
./gradlew build
```

