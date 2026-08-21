# AGENTS.md — AI Agent Guidance for GPSPY

> **Repository:** `Max97k/GPSPY`  
> **Default Branch:** `main`  
> **Primary Technology Stack:** Kotlin (JVM 17), Android SDK 36, Jetpack Compose, Dagger-Hilt 2.50, Room 2.6.1 (KSP), Wear OS Compose, Play Services Location  
> **Visibility:** Public  

---

## 1. Project Overview & Architecture

### 1.1 Purpose & Mission
**GPSPY** is an offline-first, high-precision 3D trajectory recording, routine recognition, and location telemetry analysis application for Android (Mobile) and Wear OS (Smartwatches). It provides continuous spatio-temporal tracking, intelligent daily routine analysis (Home, Work, Moving, Outdoor Stay), 3D altitude visualization, and standards-compliant GPX 1.1 export with zero dependency on third-party cloud analytics.

### 1.2 System Architecture & Component Diagram
```
+-----------------------------------------------------------------------------+
|                               GPSPY Architecture                            |
+-----------------------------------------------------------------------------+
|                                                                             |
|  +------------------------+                     +------------------------+  |
|  |     :app (Mobile)      |                     |      :wear (Watch)     |  |
|  |  - MainActivity        |  DataClient Sync    |  - MainActivity        |  |
|  |  - Jetpack Compose M3  |<===================>|  - Wear Compose UI     |  |
|  |  - Dagger-Hilt DI      |  (Play Services)    |  - WearLocationService |  |
|  |  - Routine Analyzer    |                     |  - ConnectionMonitor   |  |
|  +------------------------+                     +------------------------+  |
|               |                                              |              |
|               v                                              v              |
|  +-----------------------------------------------------------------------+  |
|  |                           Location Engine                             |  |
|  |  - Foreground TrackingService (WakeLocks, Foreground Service Type)    |  |
|  |  - FusedLocationProviderClient + GnssStatusCompat                     |  |
|  |  - Activity Recognition Transition API (STILL, WALKING, IN_VEHICLE)   |  |
|  +-----------------------------------------------------------------------+  |
|                                       |                                     |
|                                       v                                     |
|  +-----------------------------------------------------------------------+  |
|  |                         Data & Export Layer                           |  |
|  |  - Room Database (Locations, Trajectories, Routines) via KSP          |  |
|  |  - GpxGenerator (GPX 1.1 XML generation with 3D elevation & time)     |  |
|  |  - DataStore Preferences (Configuration & Sensor calibration)        |  |
|  +-----------------------------------------------------------------------+  |
|                                                                             |
+-----------------------------------------------------------------------------+
```

### 1.3 Key File & Directory Map
| Path | Purpose / Description |
|---|---|
| `app/src/main/java/com/gpsspy/gpstracker/` | Mobile application root package. |
| `app/.../MainActivity.kt` | Main entrypoint for phone UI with Compose navigation. |
| `app/.../service/TrackingService.kt` | Foreground tracking service handling high-accuracy location stream and wake locks. |
| `app/.../analysis/RoutineAnalysisManager.kt` | Routine classification engine (identifying Home, Work, Moving, Outdoor Stay). |
| `app/.../receiver/ActivityUpdateReceiver.kt` | Broadcast receiver handling Activity Recognition transitions. |
| `app/.../utils/GpxGenerator.kt` | GPX 1.1 XML file builder for track logging and elevation analysis. |
| `app/src/test/.../` | Unit tests: `GpxGeneratorTest.kt`, `BenchmarkTest.kt`, `GpxExportBenchmarkTest.kt`. |
| `wear/src/main/java/com/gpsspy/gpstracker/` | Wear OS standalone companion app. |
| `wear/.../service/WearLocationService.kt` | Wearable background location recorder and telemetry provider. |
| `upload_aab_to_play_store.py` | Google Play Console automated AAB deployment script. |
| `build.gradle.kts` / `settings.gradle.kts` | Root Gradle build and module configuration scripts. |

---

## 2. Development, Build & Verification Commands

### 2.1 Prerequisites & Environment Setup
- **JDK:** OpenJDK 17 or higher (`JAVA_HOME` configured).
- **Android SDK:** `compileSdk = 36`, `minSdk = 26` (Mobile) / `minSdk = 30` (Wear), `targetSdk = 36`.
- **Gradle:** Gradle 8.x (managed via Gradle Wrapper `./gradlew`).

### 2.2 Build & Compilation Commands
```bash
# Build Mobile Debug APK
./gradlew :app:assembleDebug

# Build Wear OS Debug APK
./gradlew :wear:assembleDebug

# Build Release Android App Bundles (AAB)
./gradlew :app:bundleRelease :wear:bundleRelease
```

### 2.3 Verification & Testing Suite
```bash
# Run all unit and benchmark tests
./gradlew testDebugUnitTest

# Run specific GPX generator unit tests
./gradlew :app:testDebugUnitTest --tests "com.gpsspy.gpstracker.utils.GpxGeneratorTest"

# Run Android Lint quality checks
./gradlew lint
```

### 2.4 Clean & Reset
```bash
./gradlew clean
```

---

## 3. Coding Standards & Conventions

### 3.1 Code Style & Idioms
- **Kotlin Standard:** Kotlin 2.0+ idioms, immutable data classes, coroutines with structured concurrency (`viewModelScope`, `lifecycleScope`).
- **Dependency Injection:** Use Dagger-Hilt (`@AndroidEntryPoint`, `@HiltViewModel`, `@Inject`).
- **Modern Jetpack Compose:** Material 3 declarative UI patterns without direct View hierarchy manipulation.

### 3.2 File & Module Organization
- Keep business logic in domain/use case classes, database DAOs in data layers, and UI Composables in feature-specific packages.
- Shared constants or XML serialization utilities belong in `utils/`.

### 3.3 State Management & Error Handling
- Use Kotlin `StateFlow` and `SharedFlow` for reactive UI state.
- Gracefully handle location provider disablement (GPS turned off, airplane mode) and display actionable dialogs.

---

## 4. Safety, Security & Resource Constraints

### 4.1 Secrets & Environment Management
- **Keystore Credentials:** Never commit keystores, signing passwords, or `play-console-key.json` into source control. Supply via Gradle project properties (`-Pandroid.signingConfigs.release.storeFile=...`) or environment variables.

### 4.2 Android Platform & Power Constraints
- **Android 14+ Foreground Service Policy:** Declare and maintain correct foreground service types (`FOREGROUND_SERVICE_LOCATION`, `FOREGROUND_SERVICE_HEALTH`).
- **Battery Optimization:** Dynamically throttle location update intervals when the device is detected as `STILL` by the Activity Recognition API. Avoid unnecessary partial wake locks.
- **Privacy Compliance:** Maintain prominent disclosure for background location access; all GPS records are strictly local to device Room database.

### 4.3 Git & Branch Workflow
- **Default Branch:** `main`.
- **Commit Standards:** Use conventional commit messages: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`.
