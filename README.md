# Rename APPs

[English](README_en.md) | 简体中文

---

## 目录

- [项目简介](#项目简介)
- [功能概览](#功能概览)
- [架构与模块](#架构与模块)
- [环境要求](#环境要求)
- [构建与运行](#构建与运行)
- [模块生效机制](#模块生效机制)
- [技术栈](#技术栈)
- [鸣谢](#鸣谢)
- [许可声明](#许可声明)

---

## 项目简介

Rename APPs 是一款基于 LSPosed / Xposed 框架的 Android 辅助模块（应用包名：`Akari.LSPosed.namechanger`）。项目旨在为用户提供**在无需对第三方 APK 进行二次打包或签名修改的前提下**，动态自定义、批量修改系统桌面与系统框架中展示的各类应用名称的解决方案。

主要设计目标：
- **无痕修改**：通过 Hook 系统底层接口替换应用名称，不破坏原应用的签名与完整性。
- **现代化 UI**：采用 Material Design 3 设计语言，提供沉浸式的直观管理体验。
- **即时生效**：打破传统的“重启生效”限制，通过内存与配置的热重载机制实现桌面名称的无缝刷新。

---

## 功能概览

### 核心功能与体验

| 功能 | 说明 |
|------|------|
| 动态重命名 | Hook `PackageManager` 等系统 API，全局替换指定应用的显示名称 |
| 桌面无缝刷新 | 自动识别当前系统桌面，修改名称后即时同步配置，无需重启手机即可生效 |
| 应用筛选排序 | 支持按包名、应用名、安装时间进行正序/倒序排列，并提供实时搜索功能 |
| 智能分区管理 | 自动将已修改名称的应用置顶显示，与未配置的应用直观分隔 |
| 模块状态检测 | 内置智能检测，能够准确识别 LSPosed 框架的激活状态及 Root 权限情况 |

### 界面与交互

- **Material Design 3**：全面适配 Android 最新的 M3 规范，卡片式布局清晰现代。
- **动态取色与深色模式**：支持 Material You 动态取色（Dynamic Color），并可根据系统自动切换深/浅色模式，或手动在设置中覆盖。
- **下拉刷新与交互提示**：集成 `SwipeRefreshLayout`，修改完成后通过 Toast 和卡片刷新提供直观的操作反馈。
- **多语言无缝切换**：原生支持简体中文、繁体中文与英文，并在应用内设置中提供免重启的语言切换体验。

---

## 架构与模块

项目采用单模块结构，按职责划分为以下子包：

```text
app/src/main/java/Akari/LSPosed/namechanger/
├── hook/         # Xposed 核心注入逻辑（MainHook，处理 PackageManager 拦截）
├── ui/           # UI 界面层（MainActivity, SettingsActivity, AppListAdapter）
└── utils/        # 通用工具与状态检测（ModuleStatusChecker, PrefUtils）
```

---

## 环境要求

| 项目 | 版本要求 |
|------|----------|
| Android SDK | 34 (Android 14) |
| Android minSdk | 29 (Android 10) |
| Android compileSdk | 34 |
| JDK | 17 |
| 依赖框架 | LSPosed 或兼容 Xposed API (de.robv.android.xposed:api:82) 的框架 |

此外，编译本项目需要 Android Studio Ladybug (2024.2) 或更高版本。

---

## 构建与运行

1. 克隆仓库：
   ```bash
   git clone https://github.com/YourUsername/RenameAPPs.git
   cd RenameAPPs
   ```

2. 使用 Android Studio 打开项目，等待 Gradle 同步完成。

3. 选择目标设备并点击 **Run** 进行安装，或使用命令行构建：
   ```bash
   ./gradlew assembleDebug
   ```

4. **模块激活**：安装后，在 LSPosed 管理器中启用 **Rename APPs**，并勾选作用域（必须包含**系统框架**及您当前的**系统桌面**），首次勾选后建议重启系统或强制停止作用域应用以完成初次注入。

---

## 模块生效机制

本模块的核心修改依赖于跨进程的 SharedPreferences (`XSharedPreferences`)。

### 状态匹配与热重载

1. **配置读取**：系统框架与桌面（被 Hook 进程）在需要获取应用名称时，会拦截请求并向全局世界可读 (World-Readable) 的配置文件读取自定义名称。
2. **即时热重载**：为了解决缓存导致的“必须重启手机”问题，本模块在写入新名称后会通过特定的刷新机制（如文件权限重置与间隔性重载）确保底层系统进程能够立刻感知配置变更。
3. **强缓存桌面的特殊处理**：由于某些深度定制系统（如 MIUI / HyperOS）的系统桌面存在强级别的内存与图标缓存，若修改未即时显示，用户可能需要进行“清除桌面缓存”、“切换系统主题”或“重启桌面”等操作。

---

## 技术栈

### UI 与界面框架
- Material Components for Android (M3)
- ViewBinding
- RecyclerView + DiffUtil

### 核心注入引擎
- Xposed API (`de.robv.android.xposed`)

### 数据与权限
- SharedPreferences (跨进程全局可读)
- Kotlin Coroutines (协程异步加载与 IO 处理)

---

## 鸣谢

- 感谢 [LSPosed](https://github.com/LSPosed/LSPosed) 团队提供的出色框架与 API 支持。

---

## 许可声明

本项目仅供技术学习与交流使用。未经授权，请勿用于任何商业用途。
