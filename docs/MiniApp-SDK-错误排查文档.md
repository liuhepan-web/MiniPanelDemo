# 涂鸦 MiniApp SDK（安卓）错误排查文档

> **对象**：安卓客户端开发 / 技术支持 / 对接同学  
> **依据**：  
> - [集成 SDK 官方文档](https://developer.tuya.com/cn/docs/app-development/mini-app-sdk-integration?id=Kcwzmgsmy3zg4)  
> - 本仓库 `MiniPanelDemo` 实战代码  
> - 内部 Wiki：面板小程序报错排查 / APP 跳转小程序问题 / 小程序启动错误码排查  
> **目标**：按标准流程排查小程序打不开、跳转失败、kit 版本不匹配、启动错误码、面板白屏等问题

集成与操作步骤见：[集成文档](./MiniPanelDemo_SDK_集成文档.md) · [操作文档](./MiniPanelDemo_操作文档.md)

---

## 一、简介

MiniApp SDK 让 **智能生活小程序** 运行在自有 Smart App 上：App 负责容器、登录态、家庭/设备上下文与能力扩展。

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

**排查前请确认**：

1. 已集成智能生活 App SDK，并完成 MiniApp / 业务包依赖。  
2. 用户已登录；多数能力依赖 **当前家庭（homeId）**。  
3. 初始化顺序：`ThingHomeSdk` → `SoLoader` → `BizBundleInitializer` → `ThingMiniAppClient.initialize`。

打开链路（排障时对照）：

```
打开小程序 / 打开面板
  → 容器请求 smartlife.m.miniprogram.info.get（携带 App kit 版本）
  → 校验白名单 / 投放 / 版本匹配
  → 下载或命中缓存加载包
  → JS 调 basekit / bizkit / homekit / devicekit ...
```

---

## 二、面板小程序报错排查（标准流程）



### 2.1 收集信息

| 项 | 获取方式 |
|----|----------|
| uid | Android：`ThingHomeSdk.getUserInstance().getUser().getUid()`；iOS：`[ThingSmartUser sharedInstance].uid` |
| 账号 / 邮箱 | 客户提供 |
| 集成依赖 | 客户 `build.gradle`（BOM、panel、miniapp、各 kit） |

### 2.2 获取 App 当前 kit 版本

Debug 包：小程序页面 **长按右上角关闭按钮（X）**，弹窗中可看到全部 Kit 版本。

### 2.3 对比小程序声明的 kit

平台：小程序开发平台
输入小程序 ID，查看小程序依赖的 kit 版本，与 App 逐项对比。

**规则（核心）**：

- **App kit 版本 ≥ 小程序声明的 kit 版本** → 才能正常打开  
- App **小于** 小程序 → 打开失败 → 让客户升级业务包，或降低小程序 kit 版本  
- App **缺少** 某个 kit → 补依赖；无法补则转移工单 / 降级小程序依赖  

示例：小程序 Basekit `3.0.0`，App Basekit `2.9.0` → App < 小程序 → 失败。

### 2.4 BizBundlesBom 7.8.14 参考 kit 一览（App 侧示例）

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

> 实际以长按 X 弹窗或 `thing_pbt_group_config.json`（业务包内置）为准；上表为 7.8.14 对照参考。

---

## 三、APP 跳转小程序：客户端常见报错


### 3.1「app 版本无法支持此服务」

**原因**：小程序需要的 kit **高于** 当前 App kit。

**处理**：

1. 长按 X，对比 App kit 与小程序（relevance 平台）。若最新业务包仍低于小程序，**降低小程序 kit 版本**。  
2. 若项目自行放了过期 `assets`（如 `thing_pbt_group_config.json`），**删除自建 assets**，使用业务包内置；并确保已调用 `BizBundleInitializer.init`。

### 3.2「无法打开小程序，请稍后再试（10010）」

**原因**：App **缺少** 小程序对应 kit。

**处理**：

1. 按依赖清单补齐 kit，与小程序声明保持一致后重试。  
2. 若是 **体验版**：确认账号已加白名单；登录账号的国家码对应数据中心，与白名单配置的数据中心一致。

### 3.3 进入小程序无报错，但 UI 不渲染

**原因**：缺少关键扩展 kit（如 basekit 等），导致界面无法渲染。

**处理**：补齐 `basekit` / `bizkit` 等拓展能力依赖。

### 3.4 初始化相关

- 不做 `BizBundleInitializer.init` → kit 与面板不匹配，打不开。  
- 不做 `SoLoader.init` → 打开崩溃。

---

## 四、小程序启动错误码排查（云端 / Loki）


### 4.1 排查步骤

1. 获取用户 **uid**（账号反查或客户端 `getUser().getUid()`）。  
2. 打开 Loki / Trace，查接口：  
   **`smartlife.m.miniprogram.info.get`**  
3. 按 uid + 时间定位失败请求，看返回错误码。

> 小程序相关接口请求会带上 kit 版本信息，来源于 App `assets` 中的 `thing_pbt_group_config.json`（由业务包生成；相关逻辑如 `GZLAtopRequest#getMiniAppInfoApiParams`）。

### 4.2 错误码对照

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

---

## 五、排障检查表（汇总）

按顺序过一遍即可覆盖大部分工单：

1. **账号 / 环境**：uid、国家码、数据中心、体验白名单、App 是否已投放关联。  
2. **登录 / 家庭**：已登录；`shiftCurrentFamily(homeId, name)` 已设置。  
3. **依赖**：panel + miniapp + basekit + bizkit + devicekit + homekit；场景 kit（map/media/p2p/ipc/ai）是否齐全。  
4. **初始化**：`ThingHomeSdk` → `SoLoader` → `BizBundleInitializer` → `ThingMiniAppClient.initialize`。  
5. **kit 版本**：长按 X vs relevance；App ≥ 小程序；勿用过期自建 `thing_pbt_group_config.json`。  
6. **云端错误码**：Loki 查 `smartlife.m.miniprogram.info.get`。  
7. **本地**：清缓存、开 vConsole、检查 so `pickFirst`、网络。

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

## 参考资料

1. [MiniApp SDK 集成（安卓）](https://developer.tuya.com/cn/docs/app-development/mini-app-sdk-integration?id=Kcwzmgsmy3zg4)  
2. [面板小程序报错排查](https://wiki.tuya-inc.com:7799/page/2036733014322450447)  
---

*文档版本：基于 MiniPanelDemo（BizBundlesBom 7.8.14）。定位为错误排查手册。*
