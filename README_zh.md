# Open GPS Tracker (GPSPY)

一個離線優先、高精準度的 Android 3D 軌跡追蹤應用程式。

[English](README.md) | **繁體中文**

![Platform: Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android)
![Language: Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin)
![UI: Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=flat-square&logo=android)
![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue?style=flat-square)

<br />

**Open GPS Tracker** 是一款開源 Android 應用程式，專為長時間背景記錄 3D 移動軌跡（包括飛行路徑、徒步健行、開車路線）而設計。它完全支援離線運作，並允許你輕鬆管理與匯出包含完整高度及元數據的標準 GPX 檔案。

---

## ✨ 主要功能

* **高精度 3D 追蹤：** 捕捉精確的緯度、經度、高度 (GPX `<ele>` 標籤)、速度與方向。
* **離線優先架構：** 無需網路連線，非常適合深山探險與飛行追蹤。
* **智慧背景服務：** 透過 Android 前景服務與 WakeLocks，確保即使在系統 Doze 模式下也能持續記錄。
* **GNSS 衛星詳細監控：** 即時查看 GPS 資料，並透過綠色/紅色燈號清楚掌握衛星使用狀態（GPS、GLONASS、Galileo、BEIDOU 等）。
* **智慧監控：** 自動偵測系統定位功能是否開啟，並主動提示使用者，防止遺失數據。
* **動態頻率控制：** 可即時調整記錄間隔（例如 1秒 至 60秒），無需中斷記錄。
* **完整的軌跡管理：** 透過清單介面查看所有歷史記錄及其持續時間。
* **輕鬆匯出：** 直接將記錄匯出為標準 `GPX 1.1` XML 檔案，支援刪除與備份。

## 🛠 技術棧

* **語言：** [Kotlin](https://kotlinlang.org/)
* **UI：** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3)
* **架構：** Clean Architecture + MVVM
* **相依性注入：** [Dagger-Hilt](https://dagger.dev/hilt/)
* **本地儲存：** [Room Database](https://developer.android.com/training/data-storage/room)
* **目標 SDK：** 35

## 🚀 如何開始

### 安裝與構建

1. **複製儲存庫：**
   ```bash
   git clone https://github.com/kuoyaoming/GPSPY.git
   cd GPSPY
   ```

2. **Android Studio：** 導入專案，Gradle 會自動下載所有依賴。

3. **命令列構建：**
   ```bash
   # 編譯 Debug APK
   ./gradlew assembleDebug
   ```

## ⚙️ 自動化 CI/CD
本專案使用 **GitHub Actions** 自動化構建與發布。當你推送到任何分支時，它都會觸發構建流程，並將產出的 APK/AAB 檔案放在 GitHub Releases 中。

## 🤝 參與貢獻
非常歡迎任何形式的貢獻！請查看 `CONTRIBUTING.md` 了解詳情。

## 📄 授權條款
採用 Apache License 2.0 授權。詳情請見 [`LICENSE`](LICENSE) 檔案。
