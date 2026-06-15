# Installation & Setup Guide

This guide details the simplest way to install and run the application on your Android device as an evaluator, followed by instructions for developers who want to compile the project from source.

---

## 📲 Quick Mobile Installation (For Evaluators)

You can install and run the application directly on your phone in under 2 minutes:

### Step 1: Download & Install the APK
1. Copy or download the compiled release APK file onto your Android device: **[app-release.apk](file:///c:/Users/talre/OneDrive/Documents/new/universal-ai-autofill-assistant/app/release/app-release.apk)**.
2. Tap the APK file to initiate installation.
3. If Android displays a **"Blocked by Play Protect"** or **"Unknown Source"** warning (common for sideloaded files not yet published to the Play Store), click **"Install Anyway"** to proceed.

### Step 2: Set Up PIN & Biometrics
1. Launch the app on your phone.
2. Follow the onboarding screens to enter your initial basic profile details.
3. Define a **4-digit PIN** to protect your offline profile storage.
4. *(Optional)* Go to the settings menu and enable **Biometric Lock** to unlock the app with your fingerprint.

### Step 3: Enable System Permissions
For the universal form-filling engine to detect forms and display the floating bubble, enable these settings:
1. **Display Over Other Apps (Overlay):** Turn this toggle ON for "AI Autofill" when prompted by the app. This allows the overlay bubble to float on top of other forms.
2. **Accessibility Service:** Go to **Settings ➔ Accessibility ➔ Installed Apps / Services ➔ AI Autofill** and toggle the service to **ON**.
   * *Troubleshooting Android 13+ Restrictions:* If the toggle is greyed out, navigate to your phone's general settings ➔ **Apps** ➔ **AI Autofill**, tap the three dots in the top-right corner, select **"Allow Restricted Settings"**, and then return to the Accessibility menu to turn the toggle ON.

---

## 🛠️ Workstation Setup (For Developers)

If you wish to edit, debug, or build the application from the source files:

### 💻 Workstation Prerequisites
* **Operating System:** Windows 10/11, macOS, or Linux.
* **Java Development Kit (JDK):** JDK 11 or higher (bundled natively with Android Studio).
* **Android Studio:** Hedgehog (2023.1.1) or newer.
* **Android SDK Platforms:** compileSdk 36 (Android 16 / latest revision) installed via the Android SDK Manager.
* **Internet Connection:** Required for the initial build to download Gradle plugins and dependencies.

### 🛠️ Step 1: Open the Project in Android Studio
1. Open Android Studio.
2. Select **File ➔ Open...** and select the folder `universal-ai-autofill-assistant`.
3. Wait for the Gradle wrapper to synchronize dependencies. Confirm that the status bar indicates a successful sync:
   ```
   Gradle sync finished in Xs XXXms
   ```

### ⚙️ Step 2: Environment Configuration
The application operates entirely offline. However, environment flags can be adjusted for debugging:
1. Duplicate `.env.example` in the root folder and rename the copy to `.env`.
2. Edit flags (e.g. `DEBUG_LOGS_ENABLED=true` for verbose console statements).

### 🚀 Step 3: Compiling and Deploying the App
1. Connect your physical test device via USB.
2. Enable **USB Debugging** inside your phone's Developer Options.
3. In Android Studio, ensure your physical device is selected in the run configuration target list.
4. Click the green **Run** icon (or press **Shift + F10**) to build and deploy.
5. If running Android 16 (where direct sideloads might be restricted), deploy using ADB from your terminal:
   ```powershell
   # Open your terminal in the directory of your compiled APK and run:
   .\adb install -r -d app-debug.apk
   ```
