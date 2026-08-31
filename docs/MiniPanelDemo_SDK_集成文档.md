# MiniPanel Demo · SDK 集成文档

> 与当前工程（`com.example.hallmond`）同步。操作与 UI 见：[MiniPanelDemo_操作文档.md](./MiniPanelDemo_操作文档.md)  
> 官方入口：  
> - [MiniApp SDK 集成](https://developer.tuya.com/cn/docs/app-development/mini-app-sdk-integration?id=Kcwzmgsmy3zg4)  
> - [应用内多语言切换 Demo 文档](https://github.com/tuya/tuya-ui-bizbundle-android-demo/blob/feature/setLanguage/docs/%E5%BA%94%E7%94%A8%E5%86%85%E5%A4%9A%E8%AF%AD%E8%A8%80%E5%88%87%E6%8D%A2.md)  
> - [IPC 增值服务 2.0](https://developer.tuya.com/cn/docs/app-development/ipc-value-added-service-2?id=Ke2iaqr2xoyz5)

---

## 1. 工程概况

| 项 | 说明 |
|----|------|
| 模块 | 单模块 `:app` |
| 包名 / applicationId | `com.example.hallmond` |
| namespace | `com.example.minipaneldemo` |
| 语言 | Java（少量 Compose / Kotlin 插件） |
| minSdk / targetSdk / compileSdk | 23 / 33 / 34 |
| ABI | `armeabi-v7a`、`arm64-v8a` |

### 1.1 源码结构（要点）

```text
app/src/main/java/com/example/minipaneldemo/
├── AppLation.java                 # Application：SDK / BizBundle / MiniApp 初始化
├── DemoLoginActivity.java
├── UserRegisterActivity.java
├── DemoHubActivity.java           # Hub 控制台
├── DemoHomePickerActivity.java
├── DemoDeviceListActivity.java
├── DemoIpcCloudVasActivity.java   # IPC 云存储 VAS → miniApp
├── DemoCurrentHomeStore.java      # 当前 homeId 持久化
├── BizBundleFamilyServiceImpl.java
└── language/
    └── LocaleHelper.java          # 应用内多语言
```

错误排查补充材料：`docs/MiniApp-SDK-错误排查文档.md`。

---

## 2. 平台侧准备

1. [涂鸦 IoT 平台](https://iot.tuya.com/) 创建 App，获取 AppKey / AppSecret。  
2. 包名绑定 `com.example.hallmond`，上传证书 SHA256。  
3. 安全算法包 AAR 放入 `app/libs/`（按平台指引）。

---

## 3. Maven 仓库

`settings.gradle` 的 `pluginManagement` 与 `dependencyResolutionManagement` 均需：

```gradle
maven { url 'https://maven-other.tuya.com/repository/maven-releases/' }
maven { url "https://maven-other.tuya.com/repository/maven-commercial-releases/" }
```

---

## 4. 依赖版本

见 `app/build.gradle`。

| 组件 | 版本 |
|------|------|
| thingsmart | **7.8.0** |
| thingsmart-ipcsdk | **7.8.1** |
| BizBundles BOM | **7.8.14** |
| thingsmart-theme-open | 2.0.6 |

核心依赖（节选）：

```gradle
implementation 'com.thingclips.smart:thingsmart:7.8.0'
api enforcedPlatform("com.thingclips.smart:thingsmart-BizBundlesBom:7.8.14")
//必选
implementation 'com.thingclips.smart:thingsmart-bizbundle-panel'
implementation "com.thingclips.smart:thingsmart-bizbundle-basekit"
implementation "com.thingclips.smart:thingsmart-bizbundle-bizkit"
implementation "com.thingclips.smart:thingsmart-bizbundle-devicekit"
api "com.thingclips.smart:thingsmart-bizbundle-miniapp"
api "com.thingclips.smart:thingsmart-bizbundle-homekit"
api "com.thingclips.smart:thingsmart-bizbundle-camera"
api "com.thingclips.smart:thingsmart-bizbundle-ipckit"
implementation 'com.thingclips.smart:thingsmart-ipcsdk:7.8.1'
//非必须，自行选择
implementation "com.thingclips.smart:thingsmart-bizbundle-family"
api "com.thingclips.smart:thingsmart-bizbundle-device_activator"

```

注意：

- 排除 `thingsmart-modularCampAnno`。  
- `packagingOptions.jniLibs.pickFirsts` 必须包含 `libv8wrapper.so` / `libv8android.so`（小程序）。  
- `manifestPlaceholders.PACKAGE_NAME = applicationId`（消息业务包 deep link 需要）。

---

## 5. AppKey / AppSecret

从 `local.properties` 注入 Manifest 占位符（**勿把真实密钥写进仓库**）：

```xml
<meta-data
    android:name="THING_SMART_APPKEY"
    android:value="${TUYA_SMART_APPKEY}" />
<meta-data
    android:name="THING_SMART_SECRET"
    android:value="${TUYA_SMART_SECRET}" />
```

`app/build.gradle` 读取示例：

```gradle
def localProps = new Properties()
def localFile = rootProject.file("local.properties")
if (localFile.exists()) {
    localFile.withInputStream { localProps.load(it) }
}
defaultConfig {
    manifestPlaceholders += [
            PACKAGE_NAME      : applicationId,
            TUYA_SMART_APPKEY : localProps.getProperty("appKey", ""),
            TUYA_SMART_SECRET : localProps.getProperty("appSecret", "")
    ]
}
```

---

## 6. Application 初始化

`AppLation` 推荐顺序：

```text
ThingHomeSdk.init
  → SoLoader.init
  → BizBundleInitializer.init + registerService(AbsBizBundleFamilyService)
  → ThingMiniAppClient.initialClient().initialize()
  → （可选）vConsole 开关恢复
  → setOnNeedLoginListener
```

缺 `ThingMiniAppClient.initialize()` 时，小程序 / 云存储 VAS 页极易白屏或打不开。

---

## 7. 权限（配网 / 蓝牙）

`compileSdk ≥ 31` 且在 Android 12+ 运行时，配网业务包会检查蓝牙三项：

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />

<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
```

**Manifest 声明 ≠ 运行时已授权。** 配网页出现「访问蓝牙权限」引导时，点「继续」完成系统授权。

---

## 8. 核心能力与代码映射

### 8.1 账号 / 家庭

| 能力 | Demo |
|------|------|
| 登录 / 注册 | `DemoLoginActivity` / `UserRegisterActivity` |
| 当前家庭 | `DemoCurrentHomeStore` + `DemoHomePickerActivity` |
| 业务包家庭服务 | `BizBundleFamilyServiceImpl` → `shiftCurrentFamily` |
| Hub | `DemoHubActivity` |

### 8.2 配网

```java
ThingDeviceActivatorManager.INSTANCE.startDeviceActiveAction(activity);
ThingDeviceActivatorManager.INSTANCE.addListener(...);
```

依赖：`thingsmart-bizbundle-device_activator`。

### 8.3 面板 / 小程序

| 能力 | Demo |
|------|------|
| 打开设备面板 | `AbsPanelCallerService.goPanelWithCheckAndTip` |
| 按 appId 打开小程序 | `ThingMiniAppClient.coreClient().openMiniAppByAppId` |
| UrlRouter 打开 miniApp | `new UrlBuilder(ctx, "miniApp").putExtras(bundle)` |
| vConsole | `ThingMiniAppClient.debugClient().vConsoleDebugEnable` |

### 8.4 IPC 云存储 VAS

`DemoIpcCloudVasActivity`：

```java
CameraVASParams params = new CameraVASParams();
params.devId = "...";
params.spaceId = String.valueOf(homeId);   // 必须有效家庭
params.languageCode = "zh";
params.categoryCode = ThingIPCConstant.CATEGORY_CODE_SECURITY_CLOUD_SERVICE;
params.hybridType = ThingIPCConstant.HYBRID_TYPE_MINI_APP;
ThingIPCSdk.getIPCVAS().fetchValueAddedServiceUrl(params, callback);
// onSuccess → UrlBuilder(..., "miniApp")
```

注意：`homeId` 禁止用 `-1`；跳转前确保 MiniApp 已初始化。

### 8.5 应用内多语言

方案：AndroidX `AppCompatDelegate.setApplicationLocales`（覆盖全部 Activity，含 BizBundle）。

| 文件 | 说明 |
|------|------|
| `language/LocaleHelper.java` | `switchLanguage` / `switchLanguageAndRestart` / `getLanguage` |
| `res/values/strings_language.xml` | 默认英文文案 |
| `res/values-zh/strings_language.xml` | 中文文案 |
| Hub `btn_language` | 弹窗选择后重启 |

```java
// 切换并冷重启（Demo 推荐，便于业务包/小程序重新拉语言）
LocaleHelper.switchLanguageAndRestart(activity, LocaleHelper.LANG_CHINESE);

// 仅切换（系统会 recreate 当前页；Demo 额外做了进程重启）
LocaleHelper.switchLanguage(context, LocaleHelper.LANG_ENGLISH);

// 跟随系统
LocaleHelper.switchLanguage(context, LocaleHelper.LANG_SYSTEM);
```

语言常量：

| 常量 | 值 |
|------|----|
| `LANG_ENGLISH` | `en` |
| `LANG_CHINESE` | `zh` |
| `LANG_SYSTEM` | `""`（空串） |

新增语言示例（日语）：

1. `LocaleHelper` 增加 `LANG_JAPANESE = "ja"`。  
2. 新建 `values-ja/strings_language.xml`。  
3. Hub 弹窗 `options` 追加一项。  
4. 按需补 `getLanguage` 映射。

依赖：BOM 会把 `appcompat` resolve 到 **1.6+**，无需单独改版本即可使用 `setApplicationLocales`。

---

## 9. 集成到自有 App 的最小清单

1. Maven + thingsmart + BizBundlesBom + 所需业务包 / MiniApp kits。  
2. `ThingHomeSdk.init` → `SoLoader` → `BizBundleInitializer` → `ThingMiniAppClient.initialize`。  
3. 注册 `AbsBizBundleFamilyService`，跳转前 `shiftCurrentFamily`。  
4. 配网：补齐蓝牙三权限 + 运行时授权。  
5. 多语言：拷贝 `LocaleHelper` 思路，切换后重启 App。  
6. 云存储：`ipcsdk` + `fetchValueAddedServiceUrl` + `UrlBuilder("miniApp")`。  
7. 密钥仅放 `local.properties`。

---

## 10. 常见问题（集成侧）

| 现象 | 原因 / 处理 |
|------|-------------|
| 配网一直要蓝牙权限 | 缺 `BLUETOOTH_ADVERTISE` 或未运行时授权 |
| 小程序打不开 | 缺 `homekit` / `basekit` / 未 `initialize()` |
| VAS URL 成功但页空白 | MiniApp 未初始化或 homeId 无效 |
| 自研多语言业务包不生效 | 未用 `setApplicationLocales`，或未重启拉取 |
| Manifest 合并缺 `PACKAGE_NAME` | `manifestPlaceholders` 补 `PACKAGE_NAME` |
