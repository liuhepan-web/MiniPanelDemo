# 涂鸦 MiniApp SDK（安卓）培训文档

> **对象**：安卓客户端开发 / 技术支持 / 对接同学  
> **依据**：  
> - [集成 SDK 官方文档](https://developer.tuya.com/cn/docs/app-development/mini-app-sdk-integration?id=Kcwzmgsmy3zg4)  
> - 本仓库 `MiniPanelDemo2` 实战代码  
> - 内部 Wiki：面板小程序报错排查 / APP 跳转小程序问题 / 小程序启动错误码排查 / 配网日志 TAG 总结  
> **目标**：完成 MiniApp SDK 集成与打开闭环，并能按标准流程排查跳转失败、版本不匹配、启动错误码等问题

---

## 一、背景：MiniApp SDK 解决什么问题

### 1.1 一句话定位

MiniApp SDK 让 **智能生活小程序** 能运行在你自己的 Smart App 上：业务以小程序形态交付，App 侧负责容器、登录态、家庭/设备上下文与能力扩展。

### 1.2 和智能生活 App SDK / UI 业务包的关系

```
智能生活 App SDK（ThingHomeSdk）          ← 账号、家庭、设备等基础能力
        │
        ├── UI BizBundle（面板/配网/家庭设置等）← Native 页面路由
        │
        └── MiniApp SDK（ThingMiniAppClient） ← 小程序容器 + 能力包
                    │
                    ├── basekit / bizkit / homekit / devicekit ...
                    └── 扫码、地图、P2P、IPC 等按需扩展
```

**关键前提**（官方要求）：

1. 已在涂鸦开发者平台创建智能生活 App，拿到 AppKey / AppSecret。  
2. **已完成智能生活 App SDK 集成**，再集成 MiniApp SDK。  
3. 用户需登录；多数业务能力依赖 **当前家庭（homeId）**。

参考：[集成 SDK](https://developer.tuya.com/cn/docs/app-development/mini-app-sdk-integration?id=Kcwzmgsmy3zg4)

---

## 二、集成清单（对照本 Demo）

### 2.1 Maven 仓库

父工程 / `settings.gradle` 需声明涂鸦仓库（Demo 已配置）：

```gradle
maven { url 'https://maven-other.tuya.com/repository/maven-releases/' }
maven { url "https://maven-other.tuya.com/repository/maven-commercial-releases/" }
```

### 2.2 packagingOptions（必做）

小程序运行时依赖 V8 相关 so，需避免多 AAR 冲突：

```gradle
android {
    packagingOptions {
        jniLibs {
            pickFirsts += [
                'lib/*/libv8wrapper.so',
                'lib/*/libv8android.so',
                // 以及项目中其它 pickFirst（log / yuv / openh264 等）
            ]
        }
    }
}
```

### 2.3 依赖：面板跳转 + 小程序（必选与按需）

跳转小程序面板时，需同时具备 **设备控制 UI 业务包** 与 **小程序能力包**（来源：APP-跳转小程序问题 Wiki）。

#### 设备控制 UI 业务包

```gradle
implementation 'com.thingclips.smart:thingsmart-bizbundle-panel'
// 基础扩展能力-必选
implementation "com.thingclips.smart:thingsmart-bizbundle-basekit"
// 业务扩展能力-必选
implementation "com.thingclips.smart:thingsmart-bizbundle-bizkit"
// 设备控制相关能力-必选
implementation "com.thingclips.smart:thingsmart-bizbundle-devicekit"
```

#### 小程序 SDK / 能力包

```gradle
api enforcedPlatform("com.thingclips.smart:thingsmart-BizBundlesBom:7.8.14")
implementation "com.thingclips.smart:thingsmart-bizbundle-miniapp"
// 家庭 必选
implementation 'com.thingclips.smart:thingsmart-bizbundle-homekit'
// 地图 非必选（小程序带地图选择，例如扫地机）
implementation 'com.thingclips.smart:thingsmart-bizbundle-mapkit'
// 摄像头、扫地机、门锁等带可视 必选
implementation 'com.thingclips.smart:thingsmart-bizbundle-mediakit'
implementation 'com.thingclips.smart:thingsmart-bizbundle-p2pkit'
implementation 'com.thingclips.smart:thingsmart-bizbundle-ipckit'
// AI 面板、智能体对话 必选
implementation "com.thingclips.smart:thingsmart-bizbundle-aistreamkit"
```

| 依赖 | 作用 | Demo |
|------|------|------|
| `thingsmart-BizBundlesBom`（enforcedPlatform） | 业务包版本统一 | ✅ `7.8.14` |
| `thingsmart-bizbundle-miniapp` | MiniApp 容器（必选） | ✅ |
| `thingsmart-bizbundle-panel` | 设备控制面板 UI | ✅ |
| `basekit` / `bizkit` / `devicekit` | 基础 / 业务 / 设备能力（必选） | ✅ |
| `homekit` | 家庭相关 API（必选） | ✅ |
| `mapkit` / `mediakit` / `p2pkit` / `ipckit` | 地图 / 媒体 / P2P / IPC | ✅ 按场景 |
| `aistreamkit` | AI 面板 / 智能体 | ✅ |
| `qrcode_mlkit` | 扫码（海外常用 mlkit） | ✅ |

> **要点**：BOM 用 `enforcedPlatform`，子模块一般不要再写版本号。缺失 kit 会导致打不开、UI 不渲染或提示「app 版本无法支持此服务」。

### 2.4 扫码能力配置

若接入扫码，需在 `assets/module_app.json` 配置 `moduleMap` / `serviceMap`：

- **中国大陆**：ScanKit  
- **其他地区**：MLKit  

两者**不能同时配置**。本 Demo 使用 `thingsmart-bizbundle-qrcode_mlkit`。

### 2.5 权限（按需申请）

| 权限 | 典型 API |
|------|----------|
| 相册读/写 | `chooseImage` / `saveToAlbum` |
| 相机 | `scanCode` / 选图 |
| 麦克风 | `chooseMedia` |
| 蓝牙 | 配网 / 设备控制相关 |
| 位置 | 地图能力包 |

原则：Manifest 声明 + 运行时按需申请。

---

## 三、初始化顺序（Demo 标准流程）

文件：`AppLation.java`

```
1. ThingHomeSdk.init(this)          // 智能生活 SDK
2. SoLoader.init(this, false)       // 加载 native so（小程序依赖）
3. BizBundleInitializer.init(...)   // UI 业务包框架（会生成/对齐 assets 内 kit 配置）
4. registerService(家庭服务)         // AbsBizBundleFamilyService
5. ThingMiniAppClient.initialClient().initialize()
6. （可选）vConsole 等调试开关恢复
```

```java
ThingHomeSdk.init(this);
SoLoader.init(this, false);
BizBundleInitializer.init(this, routeListener, serviceListener);
BizBundleInitializer.registerService(
        AbsBizBundleFamilyService.class, new BizBundleFamilyServiceImpl());
ThingMiniAppClient.initialClient().initialize();
```

### 初始化踩坑（Wiki 强调）

| 未做项 | 后果 |
|--------|------|
| 未调用 `BizBundleInitializer.init` | 无法打开对应小程序面板；App kit 与小程序声明不匹配 |
| 未调用 `SoLoader.init` | 打开小程序面板 **崩溃** |
| 项目里手写过期的 `assets`（如 `thing_pbt_group_config.json`） | kit 版本不是最新；请删除自建 assets，以业务包内置为准 |

### 家庭服务

面板、设备控制类小程序依赖「当前家庭」。Demo 通过 `BizBundleFamilyServiceImpl` + `DemoCurrentHomeStore` 维护 `homeId`，并在 Hub 页 `syncFamilyFromStore()` 调用 `shiftCurrentFamily`。

**口诀**：先登录 → 再选家庭 → 再打开小程序 / 面板。

---

## 四、能力包选型

| 能力包 | 依赖坐标 | 建议 |
|--------|----------|------|
| 基础能力包 | `thingsmart-bizbundle-basekit` | 默认必选 |
| 业务能力包 | `thingsmart-bizbundle-bizkit` | 默认必选 |
| 家庭管理 | `thingsmart-bizbundle-homekit` | 有家庭概念必选 |
| 设备控制 | `devicekit` + `panel` | 控设备 / 打开面板必选 |
| 地图 | `mapkit` | 扫地机等带地图选择时 |
| 媒体 / P2P / IPC | `mediakit` / `p2pkit` / `ipckit` | 可视类设备必选 |
| AI | `aistreamkit` | AI 面板 / 智能体必选 |
| 配网 UI | `device_activator` | Demo 已集成，配网场景用 |

选型原则：缺什么补什么；多开会增体积与传递依赖（如 `devicekit` → `TUNIDeviceKit` → `tunidevicedetailmanager`）。

```bash
./gradlew :app:dependencyInsight --dependency tunidevicedetailmanager --configuration debugRuntimeClasspath
```

---

## 五、打开小程序

### 5.1 三种入口（官方）

```kotlin
openMiniAppByAppId(context, appId, appVersion?, params?)
openMiniAppByUrl(context, url, params?)      // 如 godzilla://xxx
openMiniAppByQrcode(context, url, params?)
```

### 5.2 Demo 入口（`DemoHubActivity`）

| 按钮 | 行为 |
|------|------|
| MiniApp by App ID | `ThingMiniAppClient.coreClient().openMiniAppByAppId(...)` |
| Clear mini app cache | `ThingMiniAppClient.coreClient().clearCache()` |
| vConsole debug | `ThingMiniAppClient.debugClient().vConsoleDebugEnable(bool)` |

```java
ThingMiniAppClient
        .coreClient()
        .openMiniAppByAppId(this, "tyfarinynzhfisqswp", null, null);

// openMiniAppByUrl(this,
//     "godzilla://tyfarinynzhfisqswp?aiPtChannel=aipt_eflkb68j85c0", null);
```

### 5.3 调试与缓存

```java
ThingMiniAppClient.debugClient().vConsoleDebugEnable(true); // 仅开发期
ThingMiniAppClient.coreClient().clearCache();               // 白屏 / 包异常时重拉
```

正式包务必关闭 Debug / vConsole。

### 5.4 调用链（排障用）

```
打开小程序 / 打开面板
  → 容器请求 smartlife.m.miniprogram.info.get（携带 App kit 版本）
  → 校验白名单 / 投放 / 版本匹配
  → 下载或命中缓存加载包
  → JS 调 basekit / bizkit / homekit / devicekit ...
  → ThingHomeSdk / 家庭服务 / 权限
```

---

## 六、面板小程序报错排查（标准流程）

来源：[面板小程序报错排查](https://wiki.tuya-inc.com:7799/page/2036733014322450447)

### 6.1 收集信息

| 项 | 获取方式 |
|----|----------|
| uid | Android：`ThingHomeSdk.getUserInstance().getUser().getUid()`；iOS：`[ThingSmartUser sharedInstance].uid` |
| 账号 / 邮箱 | 客户提供 |
| 集成依赖 | 客户 `build.gradle`（BOM、panel、miniapp、各 kit） |

### 6.2 获取 App 当前 kit 版本

Debug 包：小程序页面 **长按右上角关闭按钮（X）**，弹窗中可看到全部 Kit 版本。

### 6.3 对比小程序声明的 kit

平台：[https://mini.tuya-inc.com:7799/relevance](https://mini.tuya-inc.com:7799/relevance)  
输入小程序 ID，查看小程序依赖的 kit 版本，与 App 逐项对比。

**规则（核心）**：

- **App kit 版本 ≥ 小程序声明的 kit 版本** → 才能正常打开  
- App **小于** 小程序 → 打开失败 → 让客户升级业务包，或降低小程序 kit 版本  
- App **缺少** 某个 kit → 补依赖；无法补则转移工单 / 降级小程序依赖  

示例：小程序 Basekit `3.0.0`，App Basekit `2.9.0` → App < 小程序 → 失败。

### 6.4 BizBundlesBom 7.8.14 参考 kit 一览（App 侧示例）

| npmName | version | PBTGroupName |
|---------|---------|--------------|
| WearKit | 3.0.4 | TUNIWearKit |
| HomeKit | 3.17.3 | TUNIHomeKit |
| SweeperKit | 2.0.0 | TUNISweeperKit |
| BizKit | 4.29.12 | TUNIBizKit |
| IPCKit | 7.7.12 | TUNIIPCKit |
| P2PKit | 7.7.6 | TUNIP2PKit |
| MapKit | 7.8.2 | TUNIMapKit |
| AIKit | 2.1.2 | TUNIAIKit |
| PlayNetKit | 7.3.0 | TUNIPlayNetKit |
| DeviceKit | 7.8.1 | TUNIDeviceKit |
| BaseKit | 3.36.3 | TUNIBaseKit |
| MediaKit | 3.6.3 | TUNIMediaKit |
| HealthKit | 7.2.1 | TUNIHealthKit |
| LightKit | 1.0.18 | TUNILightKit |
| CategoryCommonBizKit | 6.4.1 | TUNICategoryCommonBizKit |
| AIStreamKit | 2.2.2 | TUNIAIStreamKit |
| ThirdPartyDeviceKit | 1.0.0-rc.4 | TUNIThirdPartyDeviceKit |
| ThirdAuthKit | 1.0.14 | TUNIThirdAuthKit |
| AVideoKit | 1.0.9 | TUNIAVideoKit |
| MediaPlayerKit | 1.0.34 | TUNIMediaPlayerKit |
| MiniKit | 3.33.1 | TUNIMiniKit |

> 实际以长按 X 弹窗或 `thing_pbt_group_config.json`（业务包内置）为准；上表为 7.8.14 培训对照（最新参考）。

---

## 七、APP 跳转小程序：客户端常见报错

来源：[APP-跳转小程序问题](https://wiki.tuya-inc.com:7799/page/2038527549503438922)

### 7.1「app 版本无法支持此服务」

**原因**：小程序需要的 kit **高于** 当前 App kit。

**处理**：

1. 长按 X，对比 App kit 与小程序（relevance 平台）。若最新业务包仍低于小程序，**降低小程序 kit 版本**。  
2. 若项目自行放了过期 `assets`（如 `thing_pbt_group_config.json`），**删除自建 assets**，使用业务包内置；并确保已调用 `BizBundleInitializer.init`。

### 7.2「无法打开小程序，请稍后再试（10010）」

**原因**：App **缺少** 小程序对应 kit。

**处理**：

1. 按依赖清单补齐 kit，与小程序声明保持一致后重试。  
2. 若是 **体验版**：确认账号已加白名单；登录账号的国家码对应数据中心，与白名单配置的数据中心一致。

### 7.3 进入小程序无报错，但 UI 不渲染

**原因**：缺少关键扩展 kit（如 basekit 等），导致界面无法渲染。

**处理**：补齐 `basekit` / `bizkit` 等拓展能力依赖。

### 7.4 初始化相关

- 不做 `BizBundleInitializer.init` → kit 与面板不匹配，打不开。  
- 不做 `SoLoader.init` → 打开崩溃。

---

## 八、小程序启动错误码排查（云端 / Loki）

来源：[小程序：启动错误码排查](https://wiki.tuya-inc.com:7799/page/1930436822635122749)

### 8.1 排查步骤

1. 获取用户 **uid**（账号反查或客户端 `getUser().getUid()`）。  
2. 打开 Loki / Trace，查接口：  
   **`smartlife.m.miniprogram.info.get`**  
3. 按 uid + 时间定位失败请求，看返回错误码。

> 小程序相关接口请求会带上 kit 版本信息，来源于 App `assets` 中的 `thing_pbt_group_config.json`（由业务包生成；相关逻辑如 `GZLAtopRequest#getMiniAppInfoApiParams`）。

### 8.2 错误码对照

| 错误码 | 含义 | 处理方向 |
|--------|------|----------|
| `BUSI_PROGRAM_VERSION_NO_ACCESS` | Do not have limited access to this version：用户无权限 / 无白名单 | 给账号加体验白名单；核对数据中心 |
| `BUSI_PROGRAM_VERSION_NOT_SUPPORT` | 没有找到可用版本 | 见下方细分 |
| `BUSI_PROGRAM_VERSION_NOT_AVAILABLE` | 小程序没有发布线上版本 | 走正式发布 |
| `BUSI_PROGRAM_APP_NOT_MATCH` | 小程序尚未关联 App | 平台「投放」关联对应 App，并选择投放区域 |
| `BUSI_PROGRAM_EXPERIENCE_VERSION_NOT_AVAILABLE` | 该小程序没有体验版本 | 创建/发布体验版 |

#### `BUSI_PROGRAM_VERSION_NOT_SUPPORT` 细分

常见原因：

1. 小程序没有正式版本。  
2. 请求里携带的 **kit 版本** 与小程序设置不匹配：  
   - 小程序在 `thing_pbt_group_config` 侧声明的 kit **必须 ≤** 容器（App）kit。  
   - 查看 App kit：Debug 长按关闭按钮；或在 Loki 该接口请求参数中看 kit。  
3. **特殊规则**：小程序声明的 kit 带 `rc`，而 App 对应 kit **不带 rc** → 视为不匹配。  
4. 用 [relevance](https://mini.tuya-inc.com:7799/relevance) 核对小程序依赖。

---

## 九、排障检查表（汇总）

按顺序过一遍即可覆盖大部分工单：

1. **账号 / 环境**：uid、国家码、数据中心、体验白名单、App 是否已投放关联。  
2. **登录 / 家庭**：已登录；`shiftCurrentFamily(homeId, name)` 已设置。  
3. **依赖**：panel + miniapp + basekit + bizkit + devicekit + homekit；场景 kit（map/media/p2p/ipc/ai）是否齐全。  
4. **初始化**：`ThingHomeSdk` → `SoLoader` → `BizBundleInitializer` → `ThingMiniAppClient.initialize`。  
5. **kit 版本**：长按 X vs relevance；App ≥ 小程序；勿用过期自建 `thing_pbt_group_config.json`。  
6. **云端错误码**：Loki 查 `smartlife.m.miniprogram.info.get`。  
7. **本地**：清缓存、开 vConsole、检查 so `pickFirst`、网络。

---

## 十、配网日志 TAG 与错误码（关联专题）

> Demo 含配网业务包；打开面板前常先配网。来源：[配网日志 TAG 总结](https://wiki.tuya-inc.com:7799/page/1939929681113382958)

### 10.1 Android 设备发现主要 TAG

| TAG | 功能 |
|-----|------|
| `thingActivator_ThingDeviceDiscoverManager` | 设备发现管理：扫描、缓存、`stopScan` / `clearCache` / `discoverDeviceIdList` |
| `thingActivator_scanDeviceInnerPresenter` | 扫描内部逻辑；蓝牙发现 `onFoundBlueTooth`（名称、地址、信号等） |
| `thingActivator_activator-search-result` | 发现结果；`deviceFound`、`cancelActiveDevice`、`cancelAllActive` |
| `thingActivator_AutoScanV2ViewModel` | 自动扫描 VM：菜单、跳转激活器、`stopScan` |
| `thingActivator_AutoScanFragment` | 自动扫描 UI；`auto scan status changed to` |
| `thingActivator_` | 通用激活器日志前缀 |

### 10.2 其他相关 TAG

| TAG | 功能 |
|-----|------|
| `thingActivator_InputWifiHomeFragment` | Wi-Fi 输入界面 |
| `thingActivator_matter_hongkong` | Matter 相关 |
| `thingActivator_MultModeActivateUseCase` | 多模式激活 |
| `thingActivator_actll` | 激活步骤日志 |

**特殊**：日志出现 `isAppExcludedDevice` → 该 **pid 做了绑定限制**（App 不可配该产品）。

iOS 失败可关注：`ThingSmartActivatorDiscoveryError`（另有专门文档）。

### 10.3 常见配网错误码

| 错误码 | 含义 |
|--------|------|
| `1006` | 整体配网超时 |
| `207022` | 设备去云端 Wi-Fi 激活时获取 url 失败 |
| `205105` | 蓝牙连接超时（物理连接失败） |
| `205205` | `0x0000` 获取设备信息超时 |
| `205101` | 蓝牙连接失败（连接后无 GATT 数据） |
| `205217` | 设备正在连接中，勿重复调用配网 |
| `207034` | 设备连接 AP 热点超时 |
| `207211` | Wi-Fi 密码错误 |
| `207210` | 设备找不到路由器 |

---

## 十一、与本 Demo 的映射速查

| 知识点 | 代码位置 |
|--------|----------|
| Maven 仓库 | `settings.gradle` |
| BOM / miniapp / panel / 各 kit | `app/build.gradle` |
| SoLoader + BizBundle + MiniApp 初始化 | `AppLation.java` |
| 家庭服务 | `BizBundleFamilyServiceImpl.java` |
| 当前家庭 | `DemoCurrentHomeStore.java` |
| 打开小程序 / 清缓存 / vConsole | `DemoHubActivity.java` |
| 配网入口 | `DemoHubActivity.openActivator()` |

---

## 十二、参考资料

1. [MiniApp SDK 集成（安卓）](https://developer.tuya.com/cn/docs/app-development/mini-app-sdk-integration?id=Kcwzmgsmy3zg4)  
2. [面板小程序报错排查](https://wiki.tuya-inc.com:7799/page/2036733014322450447)  
3. [APP-跳转小程序问题](https://wiki.tuya-inc.com:7799/page/2038527549503438922)  
4. [小程序：启动错误码排查](https://wiki.tuya-inc.com:7799/page/1930436822635122749)  
5. [配网日志 TAG 总结](https://wiki.tuya-inc.com:7799/page/1939929681113382958)  
6. 小程序 kit 依赖查询：[relevance](https://mini.tuya-inc.com:7799/relevance)  
7. 本仓库：`MiniPanelDemo2`

---

## 附录：常见 Q&A

**Q：只集成 miniapp，不集成 basekit 可以吗？**  
A：容器可能起来，但 UI 常无法渲染、大量 API 不可用；必选 basekit / bizkit。

**Q：提示 app 版本无法支持此服务？**  
A：App kit < 小程序声明 kit。长按 X 对比 relevance；升级业务包或降低小程序 kit；删过期自建 `thing_pbt_group_config.json`。

**Q：10010 无法打开？**  
A：缺 kit，或体验版白名单 / 数据中心不一致。

**Q：Loki 看到 BUSI_PROGRAM_VERSION_NOT_SUPPORT？**  
A：无正式版、kit 不匹配（含 rc 特殊规则）、或容器 kit 偏低。

**Q：devicekit 为什么带出 tunidevicedetailmanager？**  
A：经 `TUNIDeviceKit` 传递引入，属预期，勿随意 exclude。

**Q：正式包要注意什么？**  
A：关 `setDebugMode`、关 vConsole；勿日志输出 Token / uid 敏感细节。

---

*文档版本：基于 MiniPanelDemo2（BizBundlesBom 7.8.14）、官方集成文档与上述内部 Wiki 整理。*
