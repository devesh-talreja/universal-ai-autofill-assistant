# Technical & Architectural Documentation

This document explains the technical implementations, algorithms, and design choices behind the core engines of the **Universal AI Autofill Assistant**.

---

## 🔍 1. Accessibility Service & Window Traversal

The primary form-filling mechanism is housed in `SmartAccessibilityService.kt`.

### Hierarchy Scraping Algorithm
Standard autofill frameworks rely on metadata tags (`autofillHints`). Because many Android applications and web pages omit these tags, the service performs manual layout scraping by traversing the **Accessibility Node Tree**:

```kotlin
private fun findAllNodes(root: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
    if (root == null) return
    if (root.isEditable || root.isClickable || root.className == "android.widget.EditText") {
        list.add(AccessibilityNodeInfo.obtain(root)) // Obtain copy to manage lifecycle
    }
    for (i in 0 until root.childCount) {
        val child = root.getChild(i)
        findAllNodes(child, list)
        child?.recycle() // Recycled immediately after traversal to prevent memory leaks
    }
}
```

### Memory Management & Recycling
Each node returned during layout queries is an active reference to system process binders. Failing to release them causes system heap exhaustion. The engine uses a strict lifecycle control:
* Nodes traversed recursively are obtained using `AccessibilityNodeInfo.obtain(node)`.
* Every intermediate node reference is recycled inside child loops.
* Leaf nodes stored in lists for matching are recycled inside the `finally` block of the fill coroutine.

---

## 🔀 2. Field Matching Priority Logic

To prevent matching conflicts (for example, accidentally filling a "Passport Number" input with a "Phone Number" because both contain the word "number"), the match engine enforces a strict priority structure:

```
                  ┌──────────────────────────────┐
                  │      Input Field Found       │
                  └──────────────┬───────────────┘
                                 ▼
                  ┌──────────────────────────────┐
                  │ 1. Match Custom Fields       │
                  │ - Custom Sections (Longest)  │
                  │ - Custom Key-Value maps      │
                  └──────────────┬───────────────┘
                                 │ (No Match)
                                 ▼
                  ┌──────────────────────────────┐
                  │ 2. Match Standard Fields     │
                  │ - Checks: Name, Email, Phone │
                  │ - Blocks if key-term matched │
                  └──────────────┬───────────────┘
                                 │ (No Match)
                                 ▼
                  ┌──────────────────────────────┐
                  │ 3. Run Regional Translation   │
                  │ - Query ML Kit Language ID   │
                  │ - Translate Label to English  │
                  │ - Retry Step 1 & 2           │
                  └──────────────────────────────┘
```

### Regional Translation Lifecycle
1. **Detection:** When the node traversal isolates a text label, the language ID model (`com.google.mlkit:language-id`) is queried.
2. **Translation:** If the language is non-English, a translation client translates the text string offline to English.
3. **Execution:** The matching pipeline is rerun using the translated label. If matched, the field is auto-populated.

---

## 📷 3. Camera Capture & OCR Pipeline

`CameraActivity.kt` coordinates the CameraX hardware controller and local ML OCR:

### Image Capture Lifecycle
1. **Viewfinder:** Displays a lifecycle-aware camera stream utilizing `androidx.camera.view.PreviewView`.
2. **Analysis:** The user snaps a photo, triggering `ImageCapture.OnImageSavedCallback`.
3. **OCR Processing:** The saved bitmap is converted to an `InputImage` and analyzed by Google ML Kit's `TextRecognizer` engine.

### Regex Extractor Heuristics
Text extracted from standard ID documents is structured via matching models. The parser runs checks:
* **Tax Cards (e.g. PAN):** Evaluates regex `[A-Z]{5}[0-9]{4}[A-Z]{1}` on strings with spaces removed.
* **National Identification (e.g. Aadhaar):** Isolates groups of 4-4-4 digits (`\b\d{4}\s\d{4}\s\d{4}\b`).
* **Dates of Birth (DOB):** Regex matching `\b\d{2}/\d{2}/\d{4}\b`.
* **Transcripts (Marksheets):** Locates keywords (e.g. "Theory", "Practical", "Total", "Grand Total"), extracts adjacent numerical columns, and groups them dynamically into an "Academic Marksheet" profile section.

---

## 🔒 4. Security Framework

The security system runs as a multi-tier sandbox:

```
┌──────────────────────────────────────────────────────────────────┐
│                         SECURITY LAYER                           │
├───────────────────┬──────────────────────────────────────────────┤
│ Mechanism         │ Implementation Method                        │
├───────────────────┼──────────────────────────────────────────────┤
│ Storage Enclave   │ EncryptedSharedPreferences (AES-256 GCM)     │
│ Entry Portal      │ BiometricPrompt (Strong) fallback to PIN key │
│ Intrusion Guard   │ Checking su binaries & system directory paths│
│ Data Leak Prevention│ FLAG_SECURE window flag                      │
│ Clipboard Purge   │ BroadcastReceiver clearing cache in 30s      │
└───────────────────┴──────────────────────────────────────────────┘
```

### Encryption Engine
Standard SharedPreferences write plaintext files in the application folder. The app bypasses this by utilizing `androidx.security:security-crypto` library configurations:
* Keys are encrypted via an Android Keystore MasterKey using AES-256-GCM.
* PIN validations and configuration flags (like biometric status) are stored inside the encrypted store.

### Automated Clipboard Clearance
When the user copies sensitive fields from the app, a 30-second timer registers. Upon firing, `CopyReceiver.kt` broadcasts a clean command, clearing the clipboard to prevent other apps from reading the copied data.
