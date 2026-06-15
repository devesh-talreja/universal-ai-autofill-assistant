# Team Contributions

This document outlines the roles, responsibilities, and specific component contributions of each of the 4 team members.

---

## 👥 Division of Responsibilities

### Member 1: Pushpraj Singhal (Core)

- **Role:** Core Engineer & System Integration Specialist.
- **Core Responsibilities:**
  - Maintained `MainActivity.kt` — primary navigation host and MVVM architecture via `ProfileViewModel`.
  - Setup of Room Database (`AppDatabase.kt`), profile data models (`UserProfile.kt`, `UserProfileDao.kt`), and JSON type converters.
  - Build configuration — `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`.

---

### Member 2: Vinay (Frontend & UX)

- **Role:** UI/UX Engineer.
- **Core Responsibilities:**
  - Design of the multi-screen Onboarding Flow (`OnboardingActivity.kt`) and Splash screen (`SplashActivity.kt`) with animated branding.
  - Development of theme files and color palette definitions (`Theme.kt`, `Color.kt`, `Type.kt`).
  - Design of floating bubble layout, profile selector popup, and profile list item XML layouts.

---

### Member 3: Samyak (Camera, Profile & ML Integration)

- **Role:** Frontend Engineer & Machine Learning Integration Specialist.
- **Core Responsibilities:**
  - Development of `CameraActivity.kt` — document scanning and Google ML Kit OCR for auto-filling from ID cards.
  - Creation of the User Info screen (`UserInfoActivity.kt`) — profile creation, custom fields, and section management.
  - Implementation of the Privacy Policy screen (`PrivacyPolicyActivity.kt`) and autofill item layout.

---

### Member 4: Devesh (Backend Services, Security & QA)

- **Role:** Backend Services, Security and Quality Assurance Engineer.
- **Core Responsibilities:**
  - Core development of `SmartAccessibilityService` — text-expansion engine, shortcut matching, and `SmartAutofillService` for credential auto-population.
  - Development of `PinManager` authentication engine, `AiFillTileService` Quick Settings tile, and `QuickCopyService` clipboard service.
  - Integration of security measures (`FLAG_SECURE`, root detection, `CopyReceiver`) and QA testing on Android 14/15/16.
