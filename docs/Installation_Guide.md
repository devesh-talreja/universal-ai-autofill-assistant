# Installation & Environment Setup Guide

This document walks developers and evaluators through setting up the local development environment, compiling the source code, and deploying the application to a physical testing device.

---

## 💻 Workstation Prerequisites

Ensure your development computer meets the following criteria:
* **Operating System:** Windows 10/11, macOS, or Linux.
* **Java Development Kit (JDK):** JDK 11 or higher (bundled with Android Studio).
* **Android Studio:** Hedgehog (2023.1.1) or newer.
* **Android SDK Platforms:** compileSdk 36 (Android 16 preview / latest revision) installed via the Android SDK Manager.
* **Internet Connection:** Required for the initial build to download Gradle plugins and dependencies.

---

## 🛠️ Step 1: Open the Project in Android Studio

1. Open Android Studio.
2. Select **File ➔ Open...** (or choose **Open** on the Welcome screen).
3. Select the folder `universal-ai-autofill-assistant`.
4. Wait for Android Studio to initialize. The IDE will download the Gradle wrapper and run a sync task. This might take several minutes on the first launch as it downloads dependencies.
5. Confirm that the status bar indicates a successful Gradle sync:
   ```
   Gradle sync finished in Xs XXXms
   ```

---

## ⚙️ Step 2: Environment Configuration

The application is offline-first. However, environment flags can be configured for development/debugging purposes.

1. Navigate to the root directory of the project.
2. Duplicate `.env.example` and rename the copy to `.env`.
3. Modify flags in the `.env` file as needed (e.g. `DEBUG_LOGS_ENABLED=true` to enable verbose accessibility log dumps).

---

## 📱 Step 3: Preparing a Physical Android Test Device

> [!IMPORTANT]
> The app relies heavily on Android overlays (`WindowManager` views), System Accessibility Services (`AccessibilityService`), and Camera APIs. Android Emulators do not support these services reliably. A physical testing device running Android 8.0 (API 26) or above is highly recommended.

1. On your test device, open **Settings ➔ About Phone**.
2. Tap the **Build Number** 7 times to enable Developer Options.
3. Return to the main Settings menu and navigate to **System ➔ Developer Options** (or search for it).
4. Turn on the following toggles:
   * **USB Debugging** (Allows Android Studio to deploy packages to the device).
   * **Install via USB** (Allows app installation via ADB).
   * *(If running Xiaomi/Oppo/Realme devices)* **USB Debugging (Security Settings)** (Required to simulate inputs/clicks).

---

## 🚀 Step 4: Compiling and Deploying the App

### Standard Run (Debug Build)
1. Connect your physical device to your computer via USB.
2. If prompted on the device screen, grant **Allow USB Debugging** permission.
3. In Android Studio, ensure the target drop-down menu in the top toolbar shows your physical device name.
4. Click the green **Run** icon (or press **Shift + F10**).
5. The IDE will compile the application, assemble the APK, and install it on your device.

### Handling Android 16 Sideload Limitations (e.g. OnePlus Nord / Android 16+)
Newer Android versions restrict the installation of debug/release APKs via simple file explorers. Follow these steps to deploy using the Android Debug Bridge (ADB):

1. Locate the ADB executable in your SDK installation. By default:
   * Windows: `C:\Users\[YourUsername]\AppData\Local\Android\Sdk\platform-tools\adb.exe`
2. Open terminal in the directory where your compiled APK is located (typically `app/build/outputs/apk/debug/` or `app/release/`).
3. Run the following command:
   ```powershell
   # On Windows PowerShell:
   .\adb install -r -d app-debug.apk
   ```
   The `-r` flag replaces the existing application, and `-d` allows version code downgrades if needed.

---

## 🔑 Permissions Configuration (Required on First Launch)

Once installed, launch the application. You must grant the following access parameters for full functionality:
1. **Camera Permission:** Prompted during the onboarding flow or when clicking "Scan Card". Required for document scanning OCR features.
2. **Display Over Other Apps (Overlay):** Go to **Settings ➔ Apps ➔ Special App Access ➔ Display Over Other Apps**, find "AI Autofill", and enable the toggle. This is required for the floating bubble.
3. **Accessibility Service Activation:** Go to **Settings ➔ Accessibility ➔ Installed Services ➔ AI Autofill**, and turn on the service.
   * *Note:* If the toggle is greyed out (restricted setting in Android 13+), go to the app's info page in Android Settings, tap the three dots in the top-right corner, select **Allow Restricted Settings**, and then retry enabling accessibility.
