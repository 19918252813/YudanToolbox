# 鱼蛋工具箱 · Android App

> 作者：鱼蛋 · 酷安 <https://www.coolapk.com/u/42391964>
> 从 5300 行 SH 脚本（1010 个菜单项 / 28 个模块）迁移而来的安卓原生工具箱

## 核心特点

### 1. 双模式：免 Root + Root

| 模式 | 后端 | 能做什么 |
|---|---|---|
| **免 Root 模式** | Shizuku（adb 级，uid 2000） | `settings` / `pm` / `wm` / `appops` / `dumpsys` / `getprop` 等，约 304 项 |
| **Root 模式** | su（uid 0） | 分区读写、系统文件、Magisk 模块、resetprop、刷机相关，约 370 项 |
| 只读模式 | 不申请提权 | 仅查看设备信息、诊断、导出报告 |

**诚实说明**：Shizuku 是 ADB 桥，**不是"无 Root 的 Root"**。它做不到 dd 写块设备、改 `/system`、
装 Magisk 模块、`resetprop`、重启到 recovery。这些功能标为「需Root」，在免 Root 模式下点开会明确
告诉你为什么做不到、以及怎么解决（Root 或电脑 adb/fastboot），**绝不灰显了事，也绝不静默调用 Root**。

### 2. 写后回读校验 —— 不假报成功

这是从 SH 脚本继承的核心设计。每条修改类命令执行后立刻回读比对：

```
✓ 已生效   global.adb_enabled = 1
✗ 未生效   global.adb_enabled 未写入（读取为空：只读属性 或 权限不足）
           └─ 权限不足：需要 Shizuku 授权或 Root
```

校验类型覆盖：`settings put/delete`、`wm size/density`、`pm disable/enable`、`appops set`。

### 3. 能力分级

每个功能项标注 `L0 免Root` / `L1 Shizuku` / `L2 需Root`，模块卡片上有可用比例条，
一眼看出这个模块在你的设备上能跑多少。

---

## 编译出 APK（两种方式，都不需要你装 Android Studio）

### 方式一：GitHub Actions 自动出包（推荐，最省事）

1. 把整个 `YudanToolbox` 目录推到 GitHub（新建仓库 → 上传）
2. 推完 Actions 会自动编译，**约 5-10 分钟**
3. 进仓库 → `Actions` → 点最新的那次运行 → 底部 `Artifacts` 下载 `鱼蛋工具箱-APK`
4. 解压得到 APK，传到手机安装

想发布正式版：打个 tag 推上去（`git tag v1.0.0 && git push --tags`），
会自动生成 Release 并附上 APK。

**正式签名（可选）**：仓库未配置 Secrets 时，Actions 会自动生成一个自带签名，
出包即可安装使用（自发自用没问题）。若要正式签名：

```
Settings → Secrets → New repository secret，添加 4 个：
  KEYSTORE_BASE64      = base64 -w0 your.jks
  KEYSTORE_PASSWORD    = 密钥库密码
  KEY_ALIAS            = 别名
  KEY_PASSWORD         = 别名密码
```

### 方式二：本机 Android Studio 编译

1. 装 [Android Studio](https://developer.android.com/studio)（Ladybug 或更新）
2. `File → Open` 选中 `YudanToolbox` 目录
3. 等 Gradle Sync 完成（首次会下载依赖，需联网）
4. `Build → Generate Signed Bundle / APK` → 选 APK → 创建或选密钥 → Finish
5. APK 在 `app/release/app-release.apk`

> 若 Gradle 报 JDK 版本错：`Settings → Build Tools → Gradle → Gradle JDK` 选 17。

---

## 使用步骤

1. 装 APK
2. **免 Root 用户**：装 [Shizuku](https://shizuku.rikka.app/) → 打开启动服务 →
   回到本应用，状态条会提示授权 → 回 Shizuku 点「授权」
3. **已 Root 用户**：设置里切「Root 模式」→ 首次执行会弹 su 授权，点允许
4. 首页 28 个模块宫格 → 点进去选功能 → 执行 → 看 ✓/✗

### 免 Root 启动 Shizuku 的两种办法

- **已 Root**：Shizuku 里选「通过 root 启动」，重启后自动运行
- **未 Root**：开发者选项 → 无线调试 → 打开 → 用 Shizuku 的配对码配对（Android 11+）

---

## 项目结构

```
YudanToolbox/
├── app/src/main/
│   ├── assets/features.json          ← 806 项功能清单（从 SH 脚本自动提取）
│   └── java/com/yudan/toolbox/
│       ├── core/
│       │   ├── Capability.kt         能力分级 L0/L1/L2 + 后端模式
│       │   ├── Model.kt              Feature / ExecResult 数据模型
│       │   ├── Executor.kt           统一执行抽象层 + 命令解析
│       │   ├── ShizukuExecutor.kt    免 Root 后端
│       │   ├── RootExecutor.kt       Root 后端（libsu）
│       │   ├── VerifyEngine.kt       ★ 写后回读校验引擎
│       │   ├── FeatureRepo.kt        功能清单加载 / 搜索
│       │   └── ToolboxCore.kt        ★ 五段流水线调度中枢
│       ├── data/                     Room：收藏 + 操作历史
│       └── ui/                       Compose：宫格 / 列表 / 详情 / 设置 / 作者页
├── .github/workflows/build-apk.yml   自动编译签名出包
└── extract_features.py               SH 脚本 → features.json 提取器（放在上层目录）
```

### 五段执行流水线

```
命令渲染（替换 $参数）
  → 权限裁决（L2 要 Root、L1 要 Shizuku、只读模式拦截所有修改）
  → 执行（Shizuku 或 su）
  → 写后回读（settings/wm/pm/appops 各自比对）
  → 审计（写入 Room 历史，可导出分享）
```

---

## 技术栈

| 项 | 选择 |
|---|---|
| 语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3（BOM 2024.12.01） |
| 架构 | 单 Activity + Navigation Compose + 状态提升 |
| 数据库 | Room 2.6.1（收藏、历史） |
| 免 Root | Shizuku API 13.1.5 |
| Root | libsu 6.0.0 |
| minSdk / targetSdk | 26 (Android 8.0) / 35 |

> 依赖版本号建议以各项目官方文档为准，升级时同步 `app/build.gradle.kts`。

---

## 已知边界

- **`reboot recovery / bootloader`**：第三方 App 拿不到 `REBOOT` 权限，App 内不直接执行重启到
  恢复模式，改为展示手动 fastboot 指引
- **刷机 / 救砖模块**：这些在手机端大多需要 fastboot 二进制 + OTG，App 里目前以指引和
  命令展示为主，真正刷机建议用电脑
- **写 IMEI**：**不提供、也不会实现**。修改 IMEI 在多数国家和地区违法
- 806 项里有一部分在原脚本是「交互式/说明型」功能（没有单一可执行命令），
  在 App 里标记为「说明型」，后续版本会逐步改造成可一键操作

---

## 免责

本工具涉及系统修改、Root 与刷机操作，可能导致数据丢失、设备无法开机或失去保修。
所有后果由操作者自行承担。请勿对他人设备执行本工具的任何修改类操作。
