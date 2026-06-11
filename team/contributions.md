# Team Contributions

This document outlines the roles, responsibilities, and specific component contributions of each of the 4 team members.

---

## 👥 Division of Responsibilities

### Member 1: [Member 1 Name] (Lead & Core Services)
* **Role:** Lead Software Engineer & Integration Specialist.
* **Core Responsibilities:**
  * Core development of `SmartAccessibilityService` layout tree scraping and text-matching integrations.
  * Implementation of the floating bubble overlay window, layout configuration files, and focus listeners.
  * Creation of the `AiFillTileService` Quick Settings tile interface.
  * Orchestration of lifecycle scopes, thread dispatching, and memory profiling.

### Member 2: [Member 2 Name] (Frontend & UX)
* **Role:** UI/UX Engineer.
* **Core Responsibilities:**
  * Development of the main Compose interfaces, theme files, and color palette definitions (`Theme.kt`).
  * Design of the 4-screen Onboarding Flow (`OnboardingActivity`) with feature previews and onboarding SVGs.
  * Creation of the User Info collection screen (`UserInfoActivity`) and configuration spinners.
  * Design of dashboard dialog overlays, bottom sheets, navigation menus, and profile detail screens.

### Member 3: [Member 3 Name] (Backend, Storage & ML Integration)
* **Role:** Backend and Machine Learning Engineer.
* **Core Responsibilities:**
  * Setting up the local Room Database architecture, custom Type Converters (`CustomFieldsConverter`), and profile entities (`UserProfile`).
  * Integration of Google ML Kit libraries for OCR Document Recognition, Language Identification, and Offline Translators.
  * Development of regex parsers for document credentials (e.g. Passports, ID cards) and marksheet transcript tables.
  * Construction of profile JSON serialization utilities for import/export capabilities.

### Member 4: [Member 4 Name] (Security, QA & Release Specialist)
* **Role:** Security and Quality Assurance Engineer.
* **Core Responsibilities:**
  * Development of the `PinManager` authentication engine, biometric prompts, and keypad validation.
  * Implementation of lockout logic and timeout configurations.
  * Integration of security measures: root detection checks, system screenshot blocks (`FLAG_SECURE`), and clipboard auto-clearing hooks (`CopyReceiver`).
  * Compilation of the QA test suite, manual testing on Android 16, release APK signing, and repository documentation.
