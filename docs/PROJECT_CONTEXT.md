# PROJECT_CONTEXT.md — Universal AI Autofill (Android App)

> **Purpose of this document:** Complete handoff context for any AI coding agent (Claude, ChatGPT, Cursor, Gemini) or developer continuing this project. Read this before touching any code.

---

## 1. WHAT THIS APP IS

**Universal AI Autofill** is an Android app that automatically fills forms on any app or website using a floating bubble overlay. The user stores their personal data (name, email, phone, Aadhaar, PAN, marksheet data, bank details, etc.) in structured "profiles" with custom "sections." When a form is open on any app, the user taps the floating bubble → the app reads all visible fields using Android Accessibility Service → matches each field label against stored profile data → fills them automatically.

**Why it exists:** Indian government forms, college admission forms, job application forms, exam forms — all require the same data (name, DOB, Aadhaar, PAN, address, marks) repeatedly. This app fills them all with one tap.

**Target users:** Indian students/professionals who fill many forms (college admissions, NEET, JEE, government services, bank forms).

---

## 2. CURRENT STATUS (as of March 2026)

### ✅ FULLY WORKING

- Profile management (create, edit, delete, export, import)
- Floating bubble — tap to fill forms, long press for profile selector
- Accessibility Service-based form detection and fill
- Custom sections per profile (e.g., "10th Marksheet", "Aadhaar Card")
- Camera scan → detect document type → auto-create section with data
- PIN lock + optional biometric (fingerprint)
- Dark/Light mode (system + manual toggle)
- Quick Settings Tile (notification panel AI Fill button)
- Text expansion shortcuts (`name-`, `email-`, `mob-`, custom field shortcuts)
- Multi-language detection + ML Kit offline translation (8 Indian languages)
- Auto-click for radio buttons/checkboxes (gender, country, state, DOB options)
- Onboarding flow (4 screens with animations)
- UserInfo onboarding (collects name, country, state, gender, DOB on first launch)
- "My Info" special profile (created from UserInfo onboarding, shown separately)
- Export/Import profiles as JSON
- Root detection warning

### 🔶 PARTIALLY WORKING / KNOWN ISSUES

- **Auto-click for radio buttons**: Code is written but needs real-world testing across more apps
- **UserInfoActivity cartoon SVGs**: Rendered via WebView inside Compose — may have rendering delays on some devices
- **Field matching accuracy**: Improved but still can mismatch in very complex forms (see Section 9)
- **Release APK install**: Cannot install release APK directly on Android 16 device (OnePlus CPH2491) without ADB — Play Store publish needed
- **Quick Settings Tile**: Works for bubble toggle; when Accessibility is OFF, opens general Accessibility settings (not the specific app screen — Android restriction)

### ❌ NOT YET DONE

- Play Store publish (keystore created at `D:\Keystore\keystore.jks`)
- OTP/phone number login
- Onboarding "Features Tour" WebView animations need polish
- ProGuard rules may need updates for release build
- FLAG_SECURE removed for debugging — must be re-added before Play Store

---

## 3. TECH STACK

| Layer              | Choice                                  | Why                                                                                                          |
| ------------------ | --------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| Language           | Kotlin                                  | Android standard                                                                                             |
| UI                 | Jetpack Compose                         | Modern declarative UI, no XML layouts for main screens                                                       |
| Architecture       | MVVM                                    | `ProfileViewModel` + `StateFlow` + Room                                                                  |
| Database           | Room (SQLite)                           | Local, no internet needed.**SQLCipher was removed** due to Android 16 / 16KB page size incompatibility |
| Security           | EncryptedSharedPreferences (AES256-GCM) | PIN storage, biometric flag                                                                                  |
| Camera             | CameraX                                 | Modern camera API, lifecycle-aware                                                                           |
| OCR/Scan           | Google ML Kit Text Recognition (Latin)  | Free, offline, accurate for ID cards                                                                         |
| Language Detection | ML Kit Language ID                      | Detect Hindi/English etc. in forms                                                                           |
| Translation        | ML Kit Translate                        | Offline translation for 8 Indian languages                                                                   |
| Form Fill Method   | Android Accessibility Service           | Only reliable way to fill ANY app's fields                                                                   |
| Autofill Framework | Android AutofillService                 | Secondary method (keyboard-style, app must support it)                                                       |
| Serialization      | kotlinx.serialization                   | JSON for Room TypeConverters and export/import                                                               |
| DI                 | None (manual)                           | App is small enough; ViewModel factory pattern                                                               |
| Min SDK            | 26 (Android 8.0)                        | EncryptedSharedPreferences requires 23+, Camera2 26+                                                         |
| Target/Compile SDK | 36                                      | Latest; tested on Android 16 (OnePlus CPH2491)                                                               |

---

## 4. PROJECT STRUCTURE

```
app/src/main/java/com/example/smartautofiller/
├── MainActivity.kt                  # Main screen, all UI composables
├── data/
│   ├── UserProfile.kt               # Room entity + ProfileSection + SectionField models
│   ├── UserProfileDao.kt            # DAO — getAllProfiles() Flow, insert, update, delete
│   └── AppDatabase.kt               # Room DB singleton, version=5
├── security/
│   └── PinManager.kt                # PIN set/verify, lockout, timeout, biometric toggle
├── service/
│   ├── SmartAccessibilityService.kt # CORE — bubble, form fill, auto-click, text expansion
│   ├── SmartAutofillService.kt      # Android Autofill Framework service (secondary)
│   ├── AiFillTileService.kt         # Quick Settings Tile
│   └── CopyReceiver.kt              # Clipboard auto-clear BroadcastReceiver (30s)
├── ui/
│   ├── SplashActivity.kt            # Launcher, routes to onboarding or main
│   ├── OnboardingActivity.kt        # App feature tour (first launch)
│   ├── UserInfoActivity.kt          # Personal info collection (name, country, gender, DOB)
│   ├── CameraActivity.kt            # ID card scan with ML Kit
│   ├── FeaturesActivity.kt          # Features demo tour (from menu)
│   ├── PrivacyPolicyActivity.kt     # Privacy policy screen
│   └── theme/
│       └── Theme.kt                 # Light/Dark color schemes
└── viewmodel/
    └── ProfileViewModel.kt          # AndroidViewModel, StateFlow<List<UserProfile>>, export/import

app/src/main/res/
├── layout/
│   ├── layout_floating_bubble.xml   # Floating bubble (XML View, not Compose)
│   ├── layout_profile_selector.xml  # Profile selector popup
│   └── layout_profile_item.xml      # Profile item in selector
├── drawable/
│   ├── ic_launcher_foreground.xml   # Custom icon (form lines + green checkmark)
│   └── ic_launcher_background.xml
├── values/
│   ├── colors.xml
│   └── themes.xml
└── xml/
    ├── accessibility_config.xml     # Accessibility service metadata
    └── autofill_service_config.xml  # Autofill service metadata
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
    name: String,         // "10th Marksheet", "Aadhaar Card", etc.
    icon: String,         // Emoji icon: "🎓", "🪪", "🆔", etc.
    fields: List<SectionField>
)
```

### SectionField

```kotlin
SectionField(
    id: String,           // UUID
    label: String,        // "Roll Number", "Aadhaar Number", "Mathematics"
    value: String         // The actual data
)
```

**Critical note:** Both `customFields` and `sections` are stored as JSON strings in Room via `CustomFieldsConverter`. The `@ColumnInfo` annotation is required on both — without it, Room ignores the column and data silently doesn't persist. This was a major bug discovered and fixed (DB version bumped from 4 → 5).

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
    │       ├── Step 1: Country + State (India default, state picker)
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

**SharedPreferences flags (key: `autofill_prefs`):**

- `onboarding_done` (Boolean) — first launch feature tour done
- `user_info_done` (Boolean) — personal info collection done
- `bubble_enabled` (Boolean) — floating bubble state
- `theme_mode` (Int: 0=system, 1=light, 2=dark)

---

## 7. CORE LOGIC: SmartAccessibilityService

This is the most complex and important file (~600 lines). Understanding it is critical.

### How form fill works (bubble tap flow):

```
User taps bubble
    ↓
serviceScope.launch { fillFormSmart() }
    ↓
db.getAllProfiles() → detect best profile (if multiple)
    ↓
fillAllFields(profile)
    ├── findAllNodes(rootInActiveWindow) → List<AccessibilityNodeInfo>
    ├── For each editable node:
    │   ├── Get hint text + content description + view ID + best label
    │   ├── matchCustomField() → check section fields (sorted by label length, longest first)
    │   │                     → then check customFields map
    │   ├── matchStandardField() → name, email, phone, address
    │   │   NOTE: If specific keywords (aadhaar, pan, passport, etc.) detected → return null
    │   │         so custom fields handle them instead
    │   └── performAction(ACTION_SET_TEXT)
    └── For each clickable node (radio/checkbox/button):
        ├── Check gender match (male/female/other + Hindi variants)
        ├── Check country/state match
        ├── Check DOB match
        ├── Check any custom/section field value match
        └── performAction(ACTION_CLICK) if match found and not already checked
```

### Field matching priority (IMPORTANT):

1. `matchCustomField()` called FIRST — section fields (sorted longest label first) then customFields
2. `matchStandardField()` called SECOND — but returns null if "specific keywords" (aadhaar, pan, etc.) detected
3. If page language is not English → translate label to English → retry matching

This ordering was critical to fix: previously `matchStandardField` was called first, causing "phone number" to accidentally fill Aadhaar fields (both contain "number").

### Text expansion (shortcuts):

- User types `name-` in any text field → bubble tap → fills full name
- Standard shortcuts: `name-`, `email-`, `mob-`, `addr-`, `pata-`, `gmail-`
- Custom field shortcuts: any field label lowercased with spaces removed + `-`
- Section field shortcuts: same pattern — `rollnumber-`, `aadhaar-`, `pannumber-`
- Multiple lines filled at once (one bubble tap handles all shortcuts on screen)

### Language detection + translation:

- ML Kit Language ID detects form language
- 8 languages supported: Hindi, Bengali, Tamil, Telugu, Marathi, Gujarati, Kannada, English
- If non-English form → translate field labels to English → match → translate value back
- Cache: last detected language cached per page (package name + first 3 label texts)

### Profile auto-detection:

- If only 1 profile → use it
- Multiple profiles → `detectBestProfile()` scores based on:
  - Profile name keywords matching page content (job/work → professional profile)
  - Context keywords on screen

---

## 8. CAMERA SCAN (CameraActivity)

**Flow:**

```
CameraActivity opens
    ↓
Runtime camera permission check (Android 6+)
    ↓
CameraX preview + ImageCapture setup
    ↓
User taps capture button
    ↓
ML Kit TextRecognition.process(image)
    ↓
extractAllData(text: String) → ScannedData
    │
    ├── Detect document type (PAN/Aadhaar/DL/Passport/10th Marksheet/12th Marksheet)
    ├── Extract: name, fatherName, DOB, panNumber, aadhaarNumber, dlNumber, address, phone, email
    │   ├── PAN: regex on spaces-removed uppercase text (handles OCR artifacts)
    │   ├── Aadhaar: 4-4-4 digit pattern
    │   ├── DL: state code + number regex
    │   ├── DOB: dd/mm/yyyy pattern
    │   └── For marksheets: board, year, roll no, enrolment no, subject-wise marks, grand total
    └── Build sectionFields: ordered list of label-value pairs for the section
    ↓
Show results card with "Retry" or "Use Data"
    ↓
"Use Data" → setResult(RESULT_OK, intent with extras) → AddProfileDialog receives data
    ↓
AddProfileDialog creates ProfileSection automatically with correct icon
```

**Supported documents:** PAN Card 🪪, Aadhaar Card 🆔, Driving Licence 🚗, Passport 📕, Voter ID 🗳️, 10th Marksheet 🎓, 12th Marksheet 🎓

**Known OCR limitations:** Hindi text in marksheets may not scan well (ML Kit Latin recognizer used). For better Hindi support, ML Kit Devanagari recognizer would need to be added.

---

## 9. SECURITY ARCHITECTURE

```
Security Layer          Implementation
─────────────────────────────────────────
PIN Storage             EncryptedSharedPreferences (AES256-GCM key via MasterKey)
Biometric               BiometricPrompt (BIOMETRIC_STRONG) — optional, user toggles
Attempt Limiting        5 wrong PINs → 30s lockout (timestamps in EncryptedPrefs)
Lock Timeout            Configurable: Instant/1/5/15/30 min (lifecycle observer)
Screenshot Block        FLAG_SECURE — CURRENTLY REMOVED for debugging, MUST RE-ADD
Root Detection          Checks common su binary paths + Runtime.exec
Backup                  allowBackup="false" in manifest
Clipboard               CopyReceiver auto-clears clipboard after 30 seconds
```

**PIN flow:**

1. First launch → PinSetupScreen (set + confirm)
2. Subsequent launches → check `isBiometricEnabled()` → if true, show fingerprint first
3. Fallback always to PIN keypad
4. `VerifyResult` sealed class: Success | NoPinSet | WrongPin(remaining) | LockedOut(seconds)

---

## 10. KNOWN BUGS & ISSUES

### Critical (must fix before Play Store):

1. **FLAG_SECURE removed** — Uncomment in `MainActivity.kt` `onCreate()`:

   ```kotlin
   window.setFlags(FLAG_SECURE, FLAG_SECURE)
   ```
2. **Release APK on Android 16** — Cannot sideload. Needs Play Store OR ADB install:

   ```
   adb install app\release\app-release.apk
   ```

   ADB path: `C:\Users\[username]\AppData\Local\Android\sdk\platform-tools\adb.exe`
3. **16KB alignment warning** — `lib/arm64-v8a/libsqlcipher.so` — RESOLVED by removing SQLCipher entirely. `useLegacyPackaging = true` remains in build.gradle for other native libs.

### Medium priority:

4. **Field matching false positives** — "Roll Number" field in marksheets sometimes matches phone number. Fixed by checking specific keywords first, but edge cases may exist. Longer labels are matched first (sorted by label length).
5. **Auto-click reliability** — Works for simple RadioButton/CheckBox Android views. Does NOT work reliably for:

   - Custom-drawn UI (games, some banking apps)
   - WebView-based forms (Chrome, forms in WebView)
   - Spinner dropdowns (need different action)
6. **UserInfoActivity SVG cartoons** — Rendered via WebView inside Compose `AndroidView`. May have a brief blank period before rendering. Alternative: draw SVG via Canvas API in Compose.
7. **"My Info" profile name hardcoded** — The special profile is identified by `profileName == "My Info"`. If user renames it via EditProfile, it loses special status. Should use a `isSpecial: Boolean` flag instead.

### Minor:

8. **Sections stat counter** in header shows `profiles.sumOf { it.sections.size }` — this is O(n), fine for small lists.
9. **Export doesn't exclude "My Info"** from normal export. When imported on another device, it won't have special status.
10. **Quick Settings Tile subtitle** is truncated on some devices ("AI Fill Ena...").

---

## 11. BUGS WE FIXED (history for context)

| Bug                                     | Root Cause                                                                   | Fix Applied                                                                  |
| --------------------------------------- | ---------------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| Camera black screen                     | Missing runtime camera permission                                            | Added `rememberLauncherForActivityResult` for camera permission            |
| App frozen on Accessibility enable      | Transparent overlay with `FLAG_NOT_TOUCHABLE` missing                      | Removed entire transparent touch overlay approach                            |
| Sections data not saving                | Room `sections` column not defined properly — missing `@ColumnInfo`     | Added `@ColumnInfo(name = "sections")`, DB version 3→4→5                 |
| Aadhaar field gets phone number         | `matchStandardField` called before `matchCustomField`                    | Swapped order: custom fields checked first; added specific keyword blocklist |
| Release APK "App not installed"         | Google Play Protect blocking unsigned APK + Android 16 sideload restrictions | Need ADB or Play Store                                                       |
| SQLCipher build failure                 | `net.zetetic:android-database-sqlcipher:4.6.1` not in Maven                | Removed SQLCipher entirely, using plain Room                                 |
| ADB not found                           | PATH not set                                                                 | Must use full path or add to System Environment Variables                    |
| `onGesture` override error            | Method doesn't exist in AccessibilityService base class                      | Removed override                                                             |
| DB schema mismatch after sections added | fallbackToDestructiveMigration but old DB still present                      | Clear app data + reinstall                                                   |
| Phone chipka gaya (touch blocked)       | `FLAG_NOT_TOUCHABLE` not set on overlay view                               | Removed overlay entirely                                                     |

---

## 12. THINGS TRIED THAT FAILED

1. **SQLCipher for database encryption** — Incompatible with Android 16 (16KB page size), version 4.6.1 not available in Maven. Removed. Alternative: consider `androidx.security:security-crypto` database key approach in future.
2. **2-finger long press gesture for bubble toggle** — Added transparent overlay view to detect multi-touch. Caused entire phone to freeze (touches blocked). Removed. Quick Settings Tile is the alternative.
3. **Direct accessibility screen navigation from Tile** — Android doesn't allow apps to navigate directly to their own accessibility settings page. General accessibility settings page opens instead.
4. **Claude API for camera scan** — Added as enhancement but requires paid API key per scan (~$0.01/scan). Removed. ML Kit is free and offline.
5. **`onGesture()` override** — Not a valid override method in `AccessibilityService`. Caused compile error.

---

## 13. DESIGN DECISIONS

1. **Why Accessibility Service over Autofill Framework?** — Autofill Framework only works in apps that explicitly support it. Accessibility Service works everywhere — browsers, government apps, WebViews. The tradeoff is the scary-sounding permission dialog.
2. **Why "Scan & Fill" (on-demand) over "Always Active"?** — Continuous event monitoring is battery-draining, CPU-heavy, and causes Play Store rejection. Current approach: `onAccessibilityEvent` only tracks focused node and package name. Actual screen reading only happens on bubble tap.
3. **Why sections instead of flat custom fields?** — Allows structured data: "10th Marksheet" section with Roll No, Marks per subject, Total etc. Makes matching more precise (Aadhaar section's "Number" field won't match "phone number").
4. **Why Room without SQLCipher?** — SQLCipher broke on Android 16. PIN + EncryptedSharedPreferences protects the app access. Database encryption is a nice-to-have, not critical for this use case since data is already behind PIN.
5. **Why ML Kit offline?** — All processing is on-device. No data leaves the phone. This is critical for user trust with Aadhaar/PAN data.
6. **Why `fallbackToDestructiveMigration()`?** — App is in development. Proper migrations will be needed before Play Store release to avoid users losing data on updates.

---

## 14. SETUP & RUN

### Prerequisites:

- Android Studio Hedgehog or newer
- JDK 11+
- Android SDK with API 36
- Physical Android device (recommended) — Accessibility Service doesn't work well on emulator
- USB debugging enabled on device

### Steps:

1. Open project in Android Studio: `File → Open → [project folder]`
2. Wait for Gradle sync
3. Connect device via USB
4. Run: `Shift+F10` or green Run button
5. First run: grant permissions when prompted

### Important — when to Clear App Data:

**Always clear app data when:**

- DB version changes (`AppDatabase.kt` version field)
- EncryptedSharedPreferences key structure changes
- `onboarding_done` / `user_info_done` flag needs reset for testing

> Settings → Apps → AI Autofill → Clear Data

### Keystore (for signed releases):

- Location: `D:\Keystore\keystore.jks`
- Alias: `key0` (default)
- Used for: Play Store uploads
- **NEVER commit this file to git**
- Password was set during creation — keep it safe

---

## 15. FILES THAT MUST NOT BE MODIFIED WITHOUT UNDERSTANDING

| File                                        | Why Dangerous                                                               |
| ------------------------------------------- | --------------------------------------------------------------------------- |
| `AppDatabase.kt`                          | Changing `version` requires migration or data loss                        |
| `UserProfile.kt`                          | Adding/removing fields breaks existing Room DB schema                       |
| `CustomFieldsConverter` in UserProfile.kt | JSON serialization — breaking changes corrupt all stored data              |
| `PinManager.kt`                           | Changing key names in EncryptedSharedPreferences loses all PINs             |
| `SmartAccessibilityService.kt`            | Complex threading (serviceScope), window management — easy to freeze phone |
| `AndroidManifest.xml`                     | Wrong permissions = feature failure; wrong activity declarations = crashes  |
| `proguard-rules.pro`                      | Wrong rules = release build crashes with NoClassDefFoundError               |

---

## 16. COMMON MISTAKES TO AVOID

1. **Don't change `sections` or `customFields` column names in Room** without incrementing version AND writing a migration.
2. **Don't use `runOnUiThread` in AccessibilityService** — use `serviceScope.launch(Dispatchers.Main)`.
3. **Don't add `FLAG_NOT_TOUCHABLE` to any overlay window** — it will freeze the device (can only be fixed by disabling Accessibility Service).
4. **Don't call `rootInActiveWindow` outside the main thread** — it's not thread-safe.
5. **Don't forget to call `it.recycle()` on `AccessibilityNodeInfo`** after use — memory leak.
6. **`weight()` modifier only works inside `RowScope` or `ColumnScope`** — caused a compile error in `GenderCard`, fixed by making it a `RowScope` extension function.
7. **`BasicTextField` `decorationBox`**: Always call `innerTextField()` inside the box, even when showing hint — otherwise cursor won't appear.
8. **Don't set `applicationIdSuffix` in debug build type** — causes "different signature" conflict when switching between debug and release installs.

---

## 17. FUTURE FEATURES (Recommended Next Steps)

### High priority:

1. **Play Store publish** — Keystore ready at `D:\Keystore\keystore.jks`. Need Google Play Developer account ($25 one-time). Need to re-enable `FLAG_SECURE` first.
2. **Proper Room migrations** — Replace `fallbackToDestructiveMigration()` with real `Migration(oldVersion, newVersion)` objects before Play Store to prevent user data loss on updates.
3. **Spinner/Dropdown auto-select** — Currently auto-click works for RadioButton/CheckBox. Dropdown spinners need `performAction(ACTION_CLICK)` then finding the popup list and clicking the matching item.
4. **WebView form fill** — Forms inside Chrome/WebView use a different accessibility tree. Need to intercept `TYPE_VIEW_FOCUSED` in WebView context and use JavaScript injection via `AccessibilityNodeInfo` or coordinate-based clicking.

### Medium priority:

5. **OTP auto-fill** — Detect SMS OTP via `SmsRetrieverClient` + fill automatically. No phone login needed, just OTP autofill.
6. **`isSpecial` flag in UserProfile** — Replace hardcoded `"My Info"` check with a proper boolean column to identify the special personal profile.
7. **SQLCipher replacement** — When `net.zetetic:android-database-sqlcipher` adds Android 16 support, re-add it. Or use `androidx.security` database encryption approach.
8. **Smarter profile detection** — Instead of simple keyword matching, use page URL/package name → profile type mapping stored by user.
9. **Form field learning** — When user manually fills a field that wasn't auto-filled, remember the mapping (field label → profile field) for that app.
10. **Widget** — Home screen widget for quick profile switching + bubble toggle.

### Low priority:

11. **Hindi OCR in camera** — Add ML Kit Devanagari text recognizer for better marksheet scanning.
12. **QR code profile sharing** — Export profile as QR, scan on another device to import.
13. **Cloud backup** — Optional Google Drive backup of profiles (privacy-first, opt-in).

---

## 18. GITHUB SETUP

### Files to add:

```
.gitignore          # See below
README.md           # User-facing readme (different from this file)
docs/               # Screenshots, demo GIF
.env.example        # Template (currently no API keys needed)
```

### .gitignore (critical entries):

```gitignore
# Android
*.iml
.gradle/
local.properties
.idea/
build/
captures/
.externalNativeBuild/
.cxx/
*.apk
*.aab

# Keystore — NEVER COMMIT
*.jks
*.keystore
keystore.properties
release-keystore.properties

# Secrets
google-services.json
*.p8
*.p12

# Generated
app/src/main/java/com/example/smartautofiller/BuildConfig.java
```

### Files safe to commit (currently all output files are code):

All `.kt` files, `AndroidManifest.xml`, `build.gradle.kts`, resource XML files.

### Repository structure suggestion:

```
universal-ai-autofill/
├── app/                    # Android module (standard structure)
├── docs/
│   ├── screenshots/
│   ├── demo.gif
│   └── ARCHITECTURE.md
├── PROJECT_CONTEXT.md      # This file
├── README.md               # User README
├── .gitignore
└── LICENSE                 # MIT recommended
```

---

## 19. ENVIRONMENT / CONFIGURATION

### No API keys required currently (all ML Kit is local/offline)

### SharedPreferences keys reference:

```
File: "autofill_prefs"
Keys:
  onboarding_done     Boolean  Has user seen app feature tour?
  user_info_done      Boolean  Has user filled personal info?
  bubble_enabled      Boolean  Is floating bubble currently visible?
  theme_mode          Int      0=system, 1=light, 2=dark

File: "secure_pin_prefs" (EncryptedSharedPreferences)
Keys:
  user_pin            String   4-digit PIN (hashed would be better — current is plain)
  wrong_attempts      Int      Failed PIN attempts
  locked_until        Long     Timestamp of lockout expiry
  lock_timeout_mins   Int      0/1/5/15/30
  biometric_enabled   Boolean  Fingerprint lock toggle
```

**Security note:** PIN is stored as plaintext in EncryptedSharedPreferences. While EncryptedSharedPreferences protects against file-level access, consider SHA-256 hashing the PIN before storage for defense-in-depth.

---

## 20. TECHNICAL DEBT

| Item                                              | Severity | Notes                                                 |
| ------------------------------------------------- | -------- | ----------------------------------------------------- |
| `fallbackToDestructiveMigration()`              | HIGH     | Must replace with proper migrations before Play Store |
| FLAG_SECURE commented out                         | HIGH     | Re-enable before release                              |
| Plain PIN storage                                 | MEDIUM   | Should hash with SHA-256                              |
| Hardcoded "My Info" string                        | MEDIUM   | Use `isSpecial` boolean field                       |
| No unit tests                                     | MEDIUM   | ViewModel and matching logic should be tested         |
| WebView for SVG cartoons in UserInfoActivity      | LOW      | Use Canvas/Compose drawing instead                    |
| `getAllSectionFields()` debug method left in    | LOW      | Remove before release                                 |
| Coroutines not cancelled on fillAllFields failure | LOW      | Add timeout                                           |
| `detectBestProfile()` scoring is naive          | LOW      | Improve with more signals                             |

---

## 21. DEPENDENCY VERSIONS (from build.gradle.kts)

```
compileSdk = 36
minSdk = 26
targetSdk = 36

androidx.security:security-crypto:1.1.0-alpha06
androidx.sqlite:sqlite-ktx:2.4.0
com.google.mlkit:language-id:17.0.6
com.google.mlkit:translate:17.0.3
org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3
```

Room, CameraX, Biometric, Compose BOM versions are managed via `libs.versions.toml` (version catalog).

---

## 22. TESTING DEVICE

- **Device:** OnePlus Nord CPH2491
- **Android:** 16 (Android 16 / API 36)
- **Issue:** Cannot sideload APKs directly — needs ADB or Play Store
- **ADB command:** `.\adb install -r "path\to\app-release.apk"` from platform-tools directory
