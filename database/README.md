# Database Documentation

The Universal AI Autofill Assistant relies on a local, offline-first SQLite database managed through the Android **Room Persistence Library**. This approach guarantees that sensitive user profiles are stored purely on-device and are never uploaded to any external servers.

---

## 🏗️ Database Architecture

* **Database Class:** `AppDatabase.kt` (located under `backend/` and `core/` modules)
* **Version:** `5`
* **Entities:** `UserProfile` (`user_profiles` table)
* **DAO:** `UserProfileDao` (handles CRUD operations and exposes reactive `Flow<List<UserProfile>>` data streams)

```
┌─────────────────────────────────────────────────────────────┐
│                       user_profiles                         │
├───────────────┬──────────────┬──────────────────────────────┤
│ Column Name   │ SQLite Type  │ Description                  │
├───────────────┼──────────────┼──────────────────────────────┤
│ id            │ INTEGER (PK) │ Autoincremented identifier   │
│ profile_name  │ TEXT         │ User label (e.g. "My Info")  │
│ full_name     │ TEXT         │ Full user name               │
│ email         │ TEXT         │ Email address                │
│ phone_number  │ TEXT         │ Contact phone number         │
│ address       │ TEXT         │ Mail/Residence address       │
│ custom_fields │ TEXT (JSON)  │ Key-value map of parameters  │
│ sections      │ TEXT (JSON)  │ Nested structures of items   │
└───────────────┴──────────────┴──────────────────────────────┘
```

---

## 🔄 Type Converters

SQLite natively supports flat tabular types. To handle complex structural groupings without creating extensive multi-table foreign key constraints that would degrade local lookup latency, Room uses Kotlin Serialization to encode/decode complex properties via two converter definitions:

1. **`custom_fields` (Maps) via `Converters` / `CustomFieldsConverter`:**
   * Class Type: `Map<String, String>`
   * Database Representation: Serialized JSON String (e.g. `{"grad_year":"2026", "linkedin":"..."}`)
2. **`sections` (Nested Structure lists) via `CustomFieldsConverter`:**
   * Class Type: `List<ProfileSection>`
   * Database Representation: Serialized JSON Array string containing list items, identifiers, and nested `SectionField` field objects.

> [!WARNING]
> Changing the keys inside `customFields` or the properties inside `ProfileSection` classes without adjusting the serialization parser can lead to database corruption. Ensure that updates to the fields maintain backward compatibility.

---

## 🔒 Security Design

* **Plain-text Storage Context:** SQLCipher database encryption was removed in DB Version 5 due to Android 16 (16KB memory page size compatibility issues with native `.so` engine binaries).
* **Local Sandbox Protection:** Data files reside strictly in the app's sandboxed storage directory (`/data/data/com.example.smartautofiller/databases/`), protected by the Linux user ID isolation standard built into the Android OS.
* **Biometric & PIN:** Accessibility to the UI showing these details is protected by the cryptographic helper class `PinManager.kt`.
* **Zero Backup Export:** The app manifest restricts direct automated ADB database backups via `android:allowBackup="false"`.

---

## 📥 Import and Export

Users can manually back up their credentials through the settings drawer. The application exports a serialized JSON payload containing all profile configurations (excluding IDs so they are recreated on the destination device).

* **Format Sample:** See [sample_data.json](file:///database/sample_data.json) for a fully formatted mock import file.
* **Migration Handling:** On developer builds, destructive migrations are enabled via `fallbackToDestructiveMigration()`. Ensure this is replaced with formal incremental database migrations (`Migration(4, 5)`) before production release.
