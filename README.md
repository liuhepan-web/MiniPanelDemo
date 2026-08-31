# MiniPanel Demo

涂鸦 **Smart Life App SDK + UI BizBundle + MiniApp SDK** 演示工程（包名 `com.example.hallmond`）。

面向开发者的统一入口：克隆本仓库 → 按下方步骤跑通 Demo → 对照文档集成到自有 App。

## 文档入口

| 文档 | 说明 |
|------|------|
| [操作文档](docs/MiniPanelDemo_操作文档.md) | Hub 流程、配网、小程序、IPC 云存储 VAS、**应用内多语言**、验收清单 |
| [集成文档](docs/MiniPanelDemo_SDK_集成文档.md) | 依赖 7.8.x、初始化、权限、LocaleHelper、VAS API 与代码映射 |
| [MiniApp 错误排查文档](docs/MiniApp-SDK-错误排查文档.md) | 打不开 / kit 不匹配 / 启动错误码 / 白屏等排查 |

## 环境要求

- Android Studio
- JDK 11+
- 真机（ARM：`armeabi-v7a` / `arm64-v8a`）
- [涂鸦 IoT 平台](https://iot.tuya.com/) 账号与已创建 App

## 快速开始

1. **克隆仓库**
   ```bash
   git clone https://github.com/liuhepan-web/MiniPanelDemo.git
   cd MiniPanelDemo
   ```
2. **配置密钥（勿提交）**
   ```bash
   cp local.properties.example local.properties
   ```
   编辑 `local.properties`：填写 `sdk.dir`、`appKey`、`appSecret`。
3. **安全算法包**  
   将平台下发的安全算法 AAR 放入 `app/libs/`（如平台要求）。
4. **包名与证书**  
   默认 `applicationId` 为 `com.example.hallmond`，须与 IoT 平台 App 包名、证书 SHA256 一致。
5. **运行**  
   Android Studio 打开工程 → 真机 Debug 运行 → 按 [操作文档](docs/MiniPanelDemo_操作文档.md) 体验。

## 功能一览

- 账号登录 / 注册、家庭选择与家庭设置业务包  
- 设备配网业务包（蓝牙权限适配 Android 12+）  
- 设备列表打开面板（Panel / MiniApp）  
- 打开小程序、清缓存、vConsole  
- **IPC 云存储增值服务**（`fetchValueAddedServiceUrl` → `miniApp`）  
- **应用内多语言切换**（`AppCompatDelegate.setApplicationLocales`，切换后冷重启）

## 版本

| 组件 | 版本 |
|------|------|
| thingsmart | 7.8.0 |
| thingsmart-ipcsdk | 7.8.1 |
| BizBundles BOM | 7.8.14 |

详见 `app/build.gradle`。

## 安全说明

- 不要把 AppKey、AppSecret、签名密码写入仓库或截图。  
- 仅使用 `local.properties`（已在 `.gitignore`）。  
- 公开分享前请确认 Manifest / Gradle 中无硬编码密钥。
