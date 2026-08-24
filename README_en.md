# Rename APPs

[简体中文](README.md) | English

---

## Table of Contents

- [Project Introduction](#project-introduction)
- [Feature Overview](#feature-overview)
- [Architecture & Modules](#architecture--modules)
- [Environment Requirements](#environment-requirements)
- [Build & Run](#build--run)
- [Module Execution Mechanism](#module-execution-mechanism)
- [Tech Stack](#tech-stack)
- [Acknowledgments](#acknowledgments)
- [License](#license)

---

## Project Introduction

Rename APPs is an Android helper module based on the LSPosed / Xposed framework (Package Name: `Akari.LSPosed.namechanger`). This project aims to provide a solution to dynamically customize and bulk rename applications displayed in the system launcher and system framework, **without the need to repackage or modify the signatures of third-party APKs**.

Key design goals:
- **Traceless Modification**: Hooks system-level APIs to replace app names dynamically, preserving the signature and integrity of the original applications.
- **Modern UI**: Adopts the Material Design 3 language to offer an immersive and intuitive management experience.
- **Immediate Effect**: Breaks the traditional "reboot-to-apply" restriction, seamlessly refreshing launcher names through a memory and configuration hot-reload mechanism.

---

## Feature Overview

### Core Features & Experience

| Feature | Description |
|---------|-------------|
| Dynamic Renaming | Hooks `PackageManager` and other system APIs to globally replace the display names of target applications |
| Seamless Launcher Refresh | Automatically detects the current system launcher and immediately syncs configurations without requiring a system reboot |
| Sorting & Filtering | Sort apps by package name, app name, or install time (ascending/descending), with real-time search support |
| Smart Partitioning | Automatically pins modified applications to the top of the list, visually separating them from unconfigured apps |
| Module Status Detection | Built-in smart checkers accurately identify the LSPosed framework's activation status and Root permissions |

### Interface & Interaction

- **Material Design 3**: Fully adapted to Android's latest M3 guidelines, featuring a clean card-based layout.
- **Dynamic Color & Dark Mode**: Supports Material You dynamic colors and automatically switches between dark/light modes based on the system, with manual overrides available in Settings.
- **Swipe-to-Refresh & Feedback**: Integrates `SwipeRefreshLayout` and provides intuitive operation feedback via Toasts and card refreshes upon saving modifications.
- **Multi-language Support**: Natively supports Simplified Chinese, Traditional Chinese, and English, offering a seamless, reboot-free language switching experience in the app.

---

## Architecture & Modules

The project uses a single-module structure, divided into the following sub-packages by responsibility:

```text
app/src/main/java/Akari/LSPosed/namechanger/
├── hook/         # Core Xposed injection logic (MainHook, handles PackageManager interceptions)
├── ui/           # UI layer (MainActivity, SettingsActivity, AppListAdapter)
└── utils/        # Common utilities & status checking (ModuleStatusChecker, PrefUtils)
```

---

## Environment Requirements

| Item | Version Requirement |
|------|---------------------|
| Android SDK | 34 (Android 14) |
| Android minSdk | 29 (Android 10) |
| Android compileSdk | 34 |
| JDK | 17 |
| Dependency | LSPosed or a compatible Xposed API (`de.robv.android.xposed:api:82`) framework |

Additionally, Android Studio Ladybug (2024.2) or higher is required to build this project.

---

## Build & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/YourUsername/RenameAPPs.git
   cd RenameAPPs
   ```

2. Open the project using Android Studio and wait for Gradle synchronization to complete.

3. Select the target device and click **Run** to install, or use the command line:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Module Activation**: After installation, enable **Rename APPs** in your LSPosed Manager and check the target scopes (you MUST include **System Framework** and your current **System Launcher**). A system reboot or force-stopping the scoped apps is recommended for the initial injection.

---

## Module Execution Mechanism

The core modification mechanism relies on cross-process SharedPreferences (`XSharedPreferences`).

### State Matching & Hot-Reload

1. **Configuration Read**: When the system framework or launcher (the hooked processes) requests an app name, the module intercepts the request and reads the custom name from the global world-readable configuration file.
2. **Instant Hot-Reload**: To eliminate the "must reboot" issue caused by caching, the module uses specific refresh mechanisms (e.g., file permission resets and periodic reloads) upon writing a new name, ensuring underlying system processes detect the changes immediately.
3. **Aggressive Cache Handling**: For heavily customized systems (like MIUI / HyperOS) with aggressive memory and icon caching in their launchers, users might still need to manually "Clear Launcher Cache," "Switch System Theme," or "Restart Launcher" if the changes do not reflect instantly.

---

## Tech Stack

### UI & Framework
- Material Components for Android (M3)
- ViewBinding
- RecyclerView + DiffUtil

### Core Injection Engine
- Xposed API (`de.robv.android.xposed`)

### Data & Permissions
- SharedPreferences (Cross-process World-Readable)
- Kotlin Coroutines (Asynchronous loading and IO handling)

---

## Acknowledgments

- Thanks to the [LSPosed](https://github.com/LSPosed/LSPosed) team for providing an excellent framework and API support.

---

## License

This project is for educational and technical communication purposes only. Unauthorized commercial use is strictly prohibited.
