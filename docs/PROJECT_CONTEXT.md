# PROJECT_CONTEXT.md — Universal AI Autofill (Android App)

> **Purpose of this document:** Complete handoff context for any AI coding agent (Claude, ChatGPT, Cursor, Gemini) or developer continuing this project. Read this before touching any code.

---

## 1. WHAT THIS APP IS

**Universal AI Autofill** is an Android app that automatically fills forms on any app or website using a floating bubble overlay. The user stores their personal data (name, email, phone, credentials, marksheet data, bank details, etc.) in structured "profiles" with custom "sections." When a form is open on any app, the user taps the floating bubble → the app reads all visible fields using Android Accessibility Service → matches each field label against stored profile data → fills them automatically.

**Why it exists:** Government forms, college admission forms, job application forms, exam forms — all require the same data repeatedly. This app fills them all with one tap.

---

## 2. CURRENT STATUS (as of June 2026)

### ✅ FULLY WORKING

- Profile management (create, edit, delete, export, import)
- Floating bubble — tap to fill forms, long press for profile selector
- Accessibility Service-based form detection and fill
- Custom sections per profile (e.g., "Marksheet", "Identity Card")
- Camera scan → detect document type → auto-create section with data
- PIN lock + optional biometric (fingerprint)
- Dark/Light mode (system + manual toggle)
- Quick Settings Tile (notification panel AI Fill button)
- Text expansion shortcuts (`name-`, `email-`, `mob-`, custom field shortcuts)
- Multi-language detection + ML Kit offline translation (8 regional languages)
- Auto-click for radio buttons/checkboxes (gender, country, state, DOB options)
- Onboarding flow (4 screens with animations)
- Onboarding "Features Tour" WebView animations polish
- UserInfo onboarding (collects name, country, state, gender, DOB on first launch)
- "My Info" special profile (created from UserInfo onboarding, shown separately)
- Export/Import profiles as JSON
- Root detection warning
- Quick Copy foreground notification panel service

### 🔶 PARTIALLY WORKING / KNOWN ISSUES

- **Auto-click for radio buttons**: Code is written but needs real-world testing across more apps
- **UserInfoActivity cartoon SVGs**: Rendered via WebView inside Compose — may have rendering delays on some devices
- **Field matching accuracy**: Improved but still can mismatch in very complex forms
- **Release APK install**: Cannot install release APK directly on Android 16 device without ADB — Play Store publish needed
- **Quick Settings Tile**: Works for bubble toggle; when Accessibility is OFF, opens general Accessibility settings (not the specific app screen — Android restriction)

### ❌ NOT YET DONE

- Play Store publish (keystore created at `D:\Keystore\keystore.jks`)
- OTP/phone number login
- ProGuard rules may need updates for release build
- FLAG_SECURE removed for debugging — must be re-added before Play Store

---

## 3. TECH STACK

| Layer              | Choice                                  | Why                                                                                                          |
| ------------------ | --------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| Language           | Kotlin                                  | Android standard                                                                                             |
| UI                 | Jetpack Compose                         | Modern declarative UI, no XML layouts for main screens                                                       |
| Architecture       | MVVM                                    | `ProfileViewModel` + `StateFlow` + Room                                                                      |
| Database           | Room (SQLite)                           | Local, no internet needed. **SQLCipher was removed** due to Android 16 / 16KB page size incompatibility       |
| Security           | EncryptedSharedPreferences (AES256-GCM) | PIN storage, biometric flag                                                                                  |
| Camera             | CameraX                                 | Modern camera API, lifecycle-aware                                                                           |
| OCR/Scan           | Google ML Kit Text Recognition (Latin)  | Free, offline, accurate for ID cards                                                                         |
| Language Detection | ML Kit Language ID                      | Detect regional language in forms                                                                            |
| Translation        | ML Kit Translate                        | Offline translation for regional languages                                                                   |
| Form Fill Method   | Android Accessibility Service           | Only reliable way to fill ANY app's fields                                                                   |
| Autofill Framework | Android AutofillService                 | Secondary method (keyboard-style, app must support it)                                                       |
| Serialization      | kotlinx.serialization                   | JSON for Room TypeConverters and export/import                                                               |
| DI                 | None (manual)                           | App is small enough; ViewModel factory pattern                                                               |
| Min SDK            | 26 (Android 8.0)                        | EncryptedSharedPreferences requires 23+, Camera2 26+                                                         |
| Target/Compile SDK | 36                                      | Latest; tested on Android 16                                                                                 |

---

## 4. PROJECT STRUCTURE

```
universal-ai-autofill-assistant/
├── backend/                  # Services, security engines, and background tasks
│   ├── AiFillTileService.kt         # Quick Settings Tile
│   ├── AppDatabase.kt               # Room DB singleton, version=5
│   ├── Converters.kt                # Room TypeConverters for Map serialization
│   ├── CopyReceiver.kt              # Clipboard auto-clear BroadcastReceiver (30s)
│   ├── PinManager.kt                # PIN set/verify, lockout, timeout, biometric toggle
│   ├── QuickCopyService.kt          # Foreground notification Quick Copy service
│   ├── SmartAccessibilityService.kt # CORE — bubble, form fill, auto-click, text expansion
│   ├── SmartAutofillService.kt      # Android Autofill Framework service (secondary)
│   ├── UserProfile.kt               # Room entity + ProfileSection + SectionField models
│   └── UserProfileDao.kt            # DAO — getAllProfiles() Flow, insert, update, delete
├── core/                     # Application lifecycle, main screens, database setup, and configurations
│   ├── AppDatabase.kt
│   ├── Converters.kt
│   ├── MainActivity.kt              # Main screen, all UI composables
│   ├── ProfileViewModel.kt          # AndroidViewModel, StateFlow<List<UserProfile>>, export/import
│   ├── UserProfile.kt
│   ├── UserProfileDao.kt
│   ├── build.gradle.kts             # Gradle configurations
│   ├── gradle.properties
│   └── settings.gradle.kts
└── frontend/                 # UI layouts, colors, theme typography, and Compose Activities
    ├── CameraActivity.kt            # ID card scan with ML Kit
    ├── Color.kt                     # Color palette
    ├── FeaturesActivity.kt          # Features demo tour (from menu)
    ├── OnboardingActivity.kt        # App feature tour (first launch)
    ├── PrivacyPolicyActivity.kt     # Privacy policy screen
    ├── SplashActivity.kt            # Launcher, routes to onboarding or main
    ├── Theme.kt                     # Theme structure
    ├── Type.kt                      # Typography
    ├── UserInfoActivity.kt          # Personal info collection (name, country, gender, DOB)
    ├── autofill_item.xml            # Custom layouts for UI and overlays
    ├── layout_floating_bubble.xml
    ├── layout_profile_item.xml
    └── layout_profile_selector.xml
```

---

## 5. DATA MODELS

### UserProfile (Room Entity)

```kotlin
UserProfile(
    id: Int,                              // Auto-generated primary key
    profileName: String,                  // "My Info", "Work", "College", etc.
    fullName: String,
    email: String,
    phoneNumber: String,
    address: String,
    customFields: Map<String, String>,    // Stored as JSON — arbitrary key-value pairs
    sections: List<ProfileSection>        // Stored as JSON — structured sections
)
```

### ProfileSection

```kotlin
ProfileSection(
    id: String,           // UUID
    name: String,         // "Marksheet", "Identity Card", etc.
    icon: String,         // Emoji icon: "🎓", "🪪", "🆔", etc.
    fields: List<SectionField>
)
```

### SectionField

```kotlin
SectionField(
    id: String,           // UUID
    label: String,        // "Roll Number", "Mathematics", etc.
    value: String         // The actual data
)
```

---

## 6. APP FLOW (Navigation)

```
SplashActivity (LAUNCHER)
    │
    ├── onboarding_done = false → OnboardingActivity (4-page feature tour)
    │                                       ↓
    │                              user_info_done = false → UserInfoActivity
    │
    ├── onboarding_done = true, user_info_done = false → UserInfoActivity
    │       ├── Step 1: Country + State (state picker)
    │       ├── Step 2: Full Name
    │       ├── Step 3: Gender (Male/Female/Other with SVG cartoons)
    │       └── Step 4: DOB (Day/Month/Year spinners)
    │               ↓ saves "My Info" profile → MainActivity
    │
    └── both done → MainActivity
            ├── Shows "My Info" card (special, always on top, gradient border)
            ├── Shows other profiles
            ├── Hamburger menu:
            │   ├── Dark/Light Mode
            │   ├── Accessibility Settings
            │   ├── Overlay Permission
            │   ├── Change PIN
            │   ├── Fingerprint Lock toggle
            │   ├── Lock Timeout
            │   ├── Export Profiles
            │   ├── Import Profiles
            │   ├── Features Tour → FeaturesActivity
            │   └── Privacy Policy → PrivacyPolicyActivity
            └── Add Profile → AddProfileDialog (bottom sheet)
                    └── Camera button → CameraActivity
```
