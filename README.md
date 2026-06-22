# Open GPS Tracker

An offline-first, high-accuracy 3D trajectory tracking app for Android.

[English](README.md) | [繁體中文](README_zh.md)

![Platform: Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android)
![Language: Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin)
![UI: Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=flat-square&logo=android)
![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue?style=flat-square)

<br />

**Open GPS Tracker** is an open-source Android application designed for long-term background recording of your 3D movement trajectory (including flight paths, hiking trails, and driving routes). It saves data locally, functions entirely offline, and allows you to easily manage and export standard GPX files containing full altitude and metadata.

---

## 🚀 Downloads
You can download the latest version of the app directly from our **[GitHub Releases](https://github.com/kuoyaoming/GPSPY/releases)** page.

---

## ✨ Key Features

* **High-Accuracy 3D Tracking:** Captures precise latitude, longitude, altitude (`<ele>` tag in GPX), speed, and bearing.
* **Offline-First Architecture:** No internet required. Perfect for deep wilderness hikes and flight tracking.
* **Smart Background Service:** Utilizes Android Foreground Services and `WakeLocks` to ensure trajectory recording survives Doze Mode and system limits.
* **GNSS Constellation Details:** View real-time GPS data and see exactly which GNSS satellites (GPS, GLONASS, Galileo, BEIDOU, SBAS, etc.) are currently in use with live green/red visual indicators.
* **Intelligent GPS Monitoring:** Automatically detects when the system's location provider is disabled and prompts the user, preventing silent data loss.
* **Dynamic Frequency Control:** Adjust your recording intervals dynamically (e.g., from 1 second to 60 seconds) without needing to interrupt the active session.
* **Comprehensive Track Management:** A clean, tabbed interface to view all historical recorded sessions, their start times, and durations.
* **Easy Export & Deletion:** Export any specific tracking session directly to a standard `GPX 1.1` XML file or delete it permanently from the local Room database to save space.
* **Google Play Policy Compliant:** Meets all strict background location policies (including prominent disclosures) for seamless publishing.

## 🛠 Tech Stack

The app is built utilizing modern Android development practices and the latest Jetpack libraries:

* **Language:** [Kotlin](https://kotlinlang.org/)
* **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3)
* **Architecture:** Clean Architecture + MVVM (Model-View-ViewModel)
* **Dependency Injection:** [Dagger-Hilt](https://dagger.dev/hilt/)
* **Local Storage:** [Room Database](https://developer.android.com/training/data-storage/room)
* **Preferences:** Jetpack DataStore
* **Asynchronous Programming:** Kotlin Coroutines & `Flow`
* **Location API:** Google Play Services `FusedLocationProviderClient` & `GnssStatusCompat`
* **Target SDK:** 35

## 🚀 Getting Started

### Prerequisites
* **Android Studio:** Ladybug or newer.
* **JDK:** Version 17+

### Installation & Build

1. **Clone the repository:**
   ```bash
   git clone https://github.com/kuoyaoming/GPSPY.git
   cd GPSPY
   ```

2. **Open in Android Studio:**
   Import the project into Android Studio. Gradle will automatically sync and download required dependencies.

3. **Build via Command Line (Optional):**
   ```bash
   # Compile and build the debug APK
   ./gradlew assembleDebug

   # Run local unit tests
   ./gradlew testDebugUnitTest
   ```

4. **Install on your device:** Run the app directly via Android Studio, or locate the APK in `app/build/outputs/apk/debug/app-debug.apk` and install it manually via `adb`.

## ⚙️ Automated CI/CD

This project uses **GitHub Actions** for Continuous Integration and Deployment. It will automatically build the APK and AAB files upon every push to the `main` branch or when a Pull Request is submitted.

To enable automated signed releases, configure the following **GitHub Secrets** in your repository settings:
* `KEYSTORE_BASE64`: Your release keystore, Base64 encoded.
* `KEY_ALIAS`: The alias of your release key.
* `KEY_PASSWORD`: The password for your key.
* `STORE_PASSWORD`: The password for your keystore.

## 🤝 Contributing

Contributions are always welcome! Whether it's reporting a bug, discussing improvements, or submitting a Pull Request, your input is highly valued.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

Please read our [CONTRIBUTING.md](CONTRIBUTING.md) for details on the process for submitting pull requests to us.

## 📄 License

Distributed under the Apache License 2.0. See [`LICENSE`](LICENSE) for more information.
