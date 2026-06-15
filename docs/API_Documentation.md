# Internal API & Method Documentation

This document describes the key internal API classes, ViewModel methods, and utility functions powering the Universal AI Autofill Assistant.

---

## 🗃️ 1. Profile Storage Interface (`ProfileViewModel`)

Exposes database operations to the UI views via reactive StateFlow streams.

### `profiles: StateFlow<List<UserProfile>>`
Exposes the active list of user profiles loaded from the database. Observes changes reactively.

### `insertProfile(profile: UserProfile)`
Inserts a new profile record into the Room SQLite store.
* **Parameters:**
  * `profile`: The `UserProfile` entity to persist.

### `updateProfile(profile: UserProfile)`
Modifies an existing user profile record.
* **Parameters:**
  * `profile`: The modified `UserProfile` entity (matched by primary key `id`).

### `deleteProfile(profile: UserProfile)`
Removes a profile configuration from the local database.

### `exportProfilesJson(context: Context): String?`
Serializes all saved profiles (excluding auto-generated database IDs) into a JSON string suitable for backup export.
* **Returns:** JSON formatted string containing the profiles, or `null` if the export fails.

### `importProfilesJson(context: Context, jsonString: String): Boolean`
Deserializes a JSON backup payload and inserts the profiles into the local database.
* **Parameters:**
  * `jsonString`: The JSON formatted backup data.
* **Returns:** `true` if parsing and insertion succeed; `false` otherwise.

---

## 🔒 2. Credentials Verifier Interface (`PinManager`)

Handles verification locks, lockout configurations, and cryptographic checks.

### `verifyPin(enteredPin: String): VerifyResult`
Validates a user-submitted 4-digit PIN against the encrypted store.
* **Parameters:**
  * `enteredPin`: The string input from the keypad.
* **Returns:** `VerifyResult` (a sealed class representing `Success`, `WrongPin`, or `LockedOut`).

### `setPin(newPin: String): Boolean`
Sets and encrypts a new security PIN.
* **Parameters:**
  * `newPin`: A 4-digit PIN string.

### `isLockedOut(): Boolean`
Checks if the application is currently locked due to too many failed attempts.

### `getLockoutSecondsRemaining(): Long`
Returns the remaining lock duration in seconds if the user is currently locked out.

---

## ⚙️ 3. Accessibility Service Engine (`SmartAccessibilityService`)

Processes layout updates and manages the form-filling lifecycle.

### `onAccessibilityEvent(event: AccessibilityEvent)`
Triggered by the OS when focused views or window structures change. Used to cache context, such as the active package name and focused inputs.

### `fillFormSmart()`
Starts a coroutine block on the background service scope to read the current layout, detect input nodes, match labels with profile data, and perform auto-fill actions.

### `matchCustomField(node: AccessibilityNodeInfo, profile: UserProfile): String?`
Iterates through custom sections and key-value fields in a profile to find a value that matches the node's labels.
* **Parameters:**
  * `node`: The target form field node.
  * `profile`: The active profile configuration.
* **Returns:** The matching value string if a match is found; `null` otherwise.

### `matchStandardField(node: AccessibilityNodeInfo, profile: UserProfile): String?`
Performs keyword matches against standard fields (Name, Email, Phone, Address).
* **Returns:** The matching standard value, or `null` if no match is found or if specific custom keywords (e.g. Aadhaar, PAN) are detected.

---

## 📷 4. OCR Document Parsing Engine (`CameraActivity`)

Handles camera inputs and text extraction.

### `extractAllData(text: String): ScannedData`
Parses raw text blocks extracted by ML Kit OCR to identify the document type and isolate data fields.
* **Parameters:**
  * `text`: The raw string of text recognized from the document.
* **Returns:** A `ScannedData` model containing parsed details (such as names, dates, identification numbers, or marksheets).

---

## ⚡ 5. Foreground Notification Interface (`QuickCopyService`)

Coordinates the foreground service and notification panel widgets.

### `onStartCommand(intent: Intent, flags: Int, startId: Int): Int`
Initiates the notification channel and starts the foreground execution cycle, compiling a low-priority ongoing notification.

### `updateNotification()`
Fetches the primary default user profile from the database asynchronously and populates notification action buttons with the user's name, email, and phone number parameters.

---

## 🔄 6. Type Converters (`Converters`)

Encodes and decodes complex nested types for Room database serialization.

### `fromMap(value: Map<String, String>): String`
Serializes a Kotlin Map into a JSON formatted string.

### `toMap(value: String): Map<String, String>`
Deserializes a JSON formatted string back into a Kotlin Map.
