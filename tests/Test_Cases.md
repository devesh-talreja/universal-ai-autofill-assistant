# Test Cases Matrix

This document lists the functional, security, and integration test cases executed during project verification.

---

## 📊 Verification Test Matrix

| Test ID | Module | Test Scenario | Input / Trigger Action | Expected Result | Actual Result | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-ON-01** | Onboarding | First launch tour navigation | Swipe through 4 onboarding screens | Core app highlights render with smooth animations; sets `onboarding_done=true` | As expected | PASS |
| **TC-ON-02** | Onboarding | Basic user details validation | Enter valid name, country, gender, DOB spinner values | Saves inputs to SharedPreferences, sets `user_info_done=true`, and auto-creates "My Info" profile | As expected | PASS |
| **TC-ON-03** | Onboarding | Re-routing evaluation | Launch app with onboarding complete but details missing | Directs user straight to `UserInfoActivity` instead of main dashboard or onboarding tour | As expected | PASS |
| **TC-PR-01** | Profile Mgr | Create manual profile | Click "+" FAB, enter Name, Email, Phone, Address, click Save | Profile card displays on dashboard; SQL row inserted in database | As expected | PASS |
| **TC-PR-02** | Profile Mgr | Edit custom field addition | Add field label "githubUrl", value "github.com/test", click Save | Custom map serialized as JSON in SQLite text column; displays correctly on reload | As expected | PASS |
| **TC-PR-03** | Profile Mgr | Export configurations | Click "Export Profiles" in menu | Generates valid JSON file excluding auto-generated IDs | As expected | PASS |
| **TC-PR-04** | Profile Mgr | Import configurations | Import exported profiles JSON | Restores profile records; regenerates local DB primary keys | As expected | PASS |
| **TC-SEC-01** | Security | Initial PIN creation | Input 4-digit PIN, confirm identical PIN | PIN saved securely inside `EncryptedSharedPreferences` | As expected | PASS |
| **TC-SEC-02** | Security | Verification success | Input correct 4-digit PIN on lock screen | Opens `MainActivity` dashboard immediately | As expected | PASS |
| **TC-SEC-03** | Security | Lockout activation | Input incorrect PIN 5 consecutive times | Blocks input, displays lockout timer, and schedules 30s timeout | As expected | PASS |
| **TC-SEC-04** | Security | Lockout persistence | Restart app during lockout period | Keypad remains locked with remaining timeout counted down correctly | As expected | PASS |
| **TC-SEC-05** | Security | Biometric fallback | Toggle Biometrics ON; verify with valid finger | Authenticates instantly without keypad PIN input | As expected | PASS |
| **TC-SEC-06** | Security | Clipboard auto-clear | Copy sensitive field from profile details | System clipboard cleared automatically after 30 seconds | As expected | PASS |
| **TC-SEC-07** | Security | Root detection alert | Launch app on rooted system environment | Displays prompt warning the user of security risks before proceeding | As expected | PASS |
| **TC-OCR-01** | OCR Scan | PAN card parsing | Capture clear photo of PAN Card | Extracts Name, Father's Name, DOB, and matches PAN regex (`[A-Z]{5}[0-9]{4}[A-Z]{1}`) | As expected | PASS |
| **TC-OCR-02** | OCR Scan | Aadhaar card parsing | Capture photo of Aadhaar Card | Identifies 12-digit number grouping (`\d{4}\s\d{4}\s\d{4}`) and name | As expected | PASS |
| **TC-OCR-03** | OCR Scan | Marksheet subject extraction | Capture photo of 10th grade marksheet | Extracts subjects (e.g. Mathematics, Science), grades, and grand total scores | As expected | PASS |
| **TC-OCR-04** | OCR Scan | Scanned section append | Click "Use Data" on scan results overlay | Automatically generates a profile section with a matching emoji icon | As expected | PASS |
| **TC-ACC-01** | Accessibility | Screen layout traversal | Focus text inputs and tap overlay bubble | Traverses window node tree, obtaining and recycling nodes correctly | As expected | PASS |
| **TC-ACC-02** | Accessibility | Standard field autofill | Tap overlay bubble on simple text inputs | Populates matching values (Name, Email, Phone) via `ACTION_SET_TEXT` | As expected | PASS |
| **TC-ACC-03** | Accessibility | Custom field matching | Tap bubble on fields with custom labels | Matches custom section field labels (sorting by length first) before standard fields | As expected | PASS |
| **TC-ACC-04** | Accessibility | Value mismatch protection | Tap bubble on "Passport Number" input | Custom passport value is filled; standard phone number is blocked from this field | As expected | PASS |
| **TC-ACC-05** | Accessibility | Selection automation | Tap bubble on forms with radio groups | Auto-clicks matching radio choices (e.g. clicks Gender selection card) | As expected | PASS |
| **TC-ACC-06** | Accessibility | Text shortcut expansion | Type `name-` in text box and tap bubble | Shortcut text is replaced with user's full name | As expected | PASS |
| **TC-ML-01** | Localization | Non-English form detection | Focus form fields written in Hindi | ML Kit detects language locale correctly | As expected | PASS |
| **TC-ML-02** | Localization | Translation autofill | Tap bubble on non-English form | Translates labels to English, matches fields, and inserts correct profile values | As expected | PASS |
