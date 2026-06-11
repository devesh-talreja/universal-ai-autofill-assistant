# Universal AI Autofill Assistant

An offline-first, intelligent Android application designed to automate form-filling across any app, browser, or WebView on Android devices using a secure floating overlay and on-device machine learning.

---

## 📖 Introduction
Form filling is a ubiquitous task. Whether registering for academic courses, applying for jobs, registering on government portals, or purchasing items on e-commerce sites, users are constantly forced to input identical personal, academic, and professional details. 

**Universal AI Autofill Assistant** eliminates this repetition. By storing personal data locally in structured profiles with custom sections (e.g., identity cards, academic marksheets), the app traverses the active screen's layout hierarchy, matches labels using string metrics and translation heuristics, and populates the forms instantly via a single-tap floating overlay.

---

## ⚠️ Problem Statement
1. **Redundant Data Entry:** Repeated entry of names, emails, registration numbers, addresses, and subject-wise grades across numerous platforms.
2. **Context Fragmentation:** Standard autofill APIs (like Android Autofill or Google Autofill) only work in fields that explicitly declare their content types and are unsupported in many third-party apps, custom web browsers, and hybrid WebViews.
3. **Data Privacy Concerns:** Uploading highly sensitive information (like identification numbers, bank accounts, or grades) to cloud-based autofill extensions exposes users to privacy breaches.
4. **Language Barriers:** Forms in multi-lingual countries are often presented in regional languages, rendering standard English-focused autofill tools ineffective.

---

## 🎯 Objectives
* Develop a **universal form-filling mechanism** that works across *all* applications and WebViews, regardless of third-party platform configuration.
* Keep all sensitive data **100% offline and localized** on the device, ensuring absolute user privacy.
* Leverage **device-side OCR** to scan identity documents and academic marksheets to auto-populate user profiles.
* Implement **offline translation** and language identification to support multi-lingual form matching.
* Maintain a highly **secure sandbox** using encrypted stores, biometric prompts, and lockout mechanisms.

---

## 🌟 Key Features
* **Floating Bubble Interface:** A non-intrusive system overlay allowing users to trigger form-filling or switch active profiles directly from any open form.
* **Smart Hierarchy Parsing:** Accessibility Service-based node traversal that scans field labels, hints, and content descriptions dynamically.
* **Document Scanner (OCR):** CameraX interface integrated with Google ML Kit Text Recognition to scan ID cards (e.g., PAN, Aadhaar, Driver License, Passport) and academic marksheets to automatically construct profile sections.
* **Multi-Language Support:** Local Language ID and Translation models that translate regional field labels to English in real-time, allowing English profiles to fill regional language forms.
* **Text Expansion Shortcuts:** Custom abbreviations (e.g., typing `name-` or `email-` in a field followed by a bubble tap) to perform instant inline text expansions.
* **Intelligent Auto-Clicks:** Automated radio button and checkbox selection for fields like country, state, gender, and date-of-birth.
* **Robust Security:** AES-256 protected credentials via `EncryptedSharedPreferences`, biometrics validation (`BiometricPrompt`), root-detection checks, and automatic 30-second clipboard clearing.

---

## 🛠️ Technology Stack
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Declarative UI) and standard Android XML Layouts (for overlay Windows)
* **Architecture:** MVVM (Model-View-ViewModel) + StateFlow
* **Database:** SQLite managed via Room Persistence Library
* **ML Engines:** Google ML Kit (Text Recognition, Language ID, Translation)
* **API Targets:** Compile/Target SDK 36, Min SDK 26 (Android 8.0+)
* **Device APIs:** CameraX, Android Accessibility Service Framework, Android Autofill Framework, Biometric API

---

## 🏗️ Architecture Overview

The system operates strictly on-device, split into clean layers:

```
┌────────────────────────────────────────────────────────────────────────┐
│                              USER INTERFACE                            │
│   ┌──────────────────────┐  ┌────────────────────┐  ┌──────────────┐   │
│   │ MainActivity (Compose)│  │ Floating Bubble View│  │ Camera (Scan)│   │
│   └──────────┬───────────┘  └─────────┬──────────┘  └──────┬───────┘   │
└──────────────┼────────────────────────┼────────────────────┼───────────┘
               ▼                        ▼                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        BUSINESS LOGIC / SERVICES                       │
│  ┌───────────────────────┐  ┌───────────────────────────────────────┐  │
│  │   ProfileViewModel    │  │       SmartAccessibilityService       │  │
│  └───────────┬───────────┘  └─────────┬───────────────────┬─────────┘  │
└──────────────┼────────────────────────┼───────────────────┼───────────┘
               ▼                        ▼                   ▼
┌───────────────────────────┐ ┌───────────────────┐ ┌────────────────────┐
│      LOCAL DATABASE       │ │  SECURITY ENGINE  │ │   OFFLINE ML KIT   │
│ ┌───────────────────────┐ │ │ ┌───────────────┐ │ │ ┌────────────────┐ │
│ │  Room DB (SQLite)     │ │ │ │ PinManager    │ │ │ │ OCR & Translate│ │
│ │  Profiles & Custom    │ │ │ │ Biometrics    │ │ │ │ Language ID   │ │
│ │  Sections (JSON)      │ │ │ └───────────────┘ │ │ └────────────────┘ │
│ └───────────────────────┘ │ └───────────────────┘ └────────────────────┘
└───────────────────────────┘
```

---

## 📁 Repository Structure
```
universal-ai-autofill-assistant/
├── .github/                  # PR templates and issue configurations
├── app/                      # Main Android source module
│   └── src/
│       ├── main/             # App source code, assets, and AndroidManifest
│       ├── test/             # Local JVM unit tests
│       └── androidTest/      # Device-instrumentation tests
├── database/                 # SQL schemas and sample import profiles
├── docs/                     # User, technical, and architectural docs
├── screenshots/              # UI screens & demonstrations
├── team/                     # Contributions, commit plans, and workflows
└── tests/                    # Detailed QA test case matrix
```
*For a detailed walkthrough of directory contents, see [System Architecture](file:///docs/System_Architecture.md).*

---

## 🚀 Installation & Setup

### Prerequisites
1. **Android Studio** (Hedgehog 2023.1.1 or newer recommended)
2. **JDK 11** or higher
3. **Android SDK Platform** (compileSdk 36)
4. A physical Android device (API 26+) with USB Debugging enabled. *(Note: Accessibility overlays and Camera features do not run reliably on virtual Emulators).*

### Local Development Setup
1. Clone the repository to your local drive.
2. Open Android Studio and choose **File ➔ Open**, pointing to the cloned root directory.
3. Gradle will synchronize automatically.
4. Set up environment properties by creating a `.env` file from the [Template](file:///.env.example).
5. Build the application: **Build ➔ Make Project**.

### Deployment to Device
1. Connect your physical Android device via USB.
2. In Android Studio, select your device from the run configuration target list.
3. Press **Shift + F10** (or click the green Run button).
4. On first launch, follow the onboarding wizard to grant **Camera**, **Display Overlay**, and **Accessibility Service** permissions.

---

## 🗄️ Database Design
The application uses local storage to ensure user data remains private.

* **Room Database (`AppDatabase`, Version 5):**
  * Maintains a single table `UserProfile`.
  * Complex structural mappings (such as `customFields` Maps and lists of structured `ProfileSections`) are serialized into JSON strings via `CustomFieldsConverter` and saved directly in SQLite text columns.
  
*For schema details, SQL declarations, and mock JSON profile imports, view the [Database Documentation](file:///database/README.md).*

---

## 📸 Screenshots & UI Flow
Below is a outline of the user interface flow:

| Onboarding Flow | Profile Management | Smart Form Filling |
| :---: | :---: | :---: |
| ![Onboarding UI](screenshots/onboarding.png) <br> *1. Multi-step features tour and basic user details setup* | ![Profiles Screen](screenshots/profiles.png) <br> *2. Profile dashboards, custom fields, and sections* | ![Floating Bubble Fill](screenshots/bubble_fill.png) <br> *3. Overlay bubble triggered on forms to auto-populate* |

*(Note: If screenshots are not rendering, view the [Screenshots Directory Guide](file:///screenshots/README.md) to locate raw assets).*

---

## 🧪 Testing Summary
The QA process comprises:
* **Unit Verification:** Validates matching algorithms, translation buffers, configuration parsing, and PIN lockout sequences (located in [Test Cases](file:///tests/Test_Cases.md)).
* **UI Testing:** Jetpack Compose UI layout tests verifying dialog actions, menu toggles, and edit fields.
* **On-Device Diagnostics:** Running Accessibility tree captures to evaluate performance and memory footprints during continuous page traversal.

*Review complete validation scenarios in [Test Cases Matrix](file:///tests/Test_Cases.md) and metrics in [Test Results](file:///tests/Test_Results.md).*

---

## 👥 Team & Responsibilities
This is a 4-member academic team project:

* **[Member 1 Name] (Lead & Integration):** Designed the core Accessibility parsing engine, overlay window interactions, and Quick Settings tile integrations.
* **[Member 2 Name] (Frontend Developer):** Constructed the Jetpack Compose pages, onboarding flow widgets, theme configurations, and animations.
* **[Member 3 Name] (Backend & Storage):** Implemented the local Room SQLite bindings, security modules (Biometrics, PIN verified storage), and local OCR/Language wrappers.
* **[Member 4 Name] (QA & Deployment Specialist):** Created the test scenarios, checked layouts across device API targets, handled release building, and consolidated documentation.

*To review development guidelines and Git configurations, read the [Team Workflows](file:///team/workflow.md) and [Commit Plan](file:///team/commit_plan.md).*

---

## 🔮 Future Enhancements
* **Dropdown Selection Support:** Integrate automated options-clicking inside native Android spinner components and HTML select menus.
* **WebView JS Injection:** Standardize autofill in complex banking and secure sandboxed hybrid pages via experimental DOM-level injection.
* **OTP Detection:** Read verification codes via SMS retriever hooks and insert directly into input boxes.
* **Database Encrypted Migration:** Re-evaluate page-size compliant SQLite database wrappers once SQLCipher compatibility is restored for Android 16.
* **Local Backups:** Encrypted backups to Google Drive or local storage utilizing the Android backup engine.

---

## 📚 References & Resources
1. Android Developers Guide: [Accessibility Service API](https://developer.android.com/guide/topics/ui/accessibility/service)
2. Jetpack Compose UI documentation: [Compose UI Layouts](https://developer.android.com/compose)
3. Google Developers: [ML Kit Text Recognition Guide](https://developers.google.com/ml-kit/vision/text-recognition)
4. Android Security: [Cryptography and EncryptedSharedPreferences](https://developer.android.com/topic/security/data)
