# User Manual & Feature Guide

Welcome to the **Universal AI Autofill Assistant** User Manual. Follow this guide to set up your profiles, scan documents, backup data, and automate form-filling securely across your favorite apps and web browsers.

---

## 🏁 1. Initial Setup & Security Onboarding

### First-Time Wizard
When you launch the app for the first time, you are guided through a multi-step introduction detailing the application's offline capabilities. You will immediately configure your default profile:
1. **Onboarding Details:** Fill in your basic information: Name, Country, State, Gender, and Date of Birth.
2. **"My Info" Generation:** This creates your primary **"My Info"** profile. It has a special pinned status on your dashboard with a unique gradient border.

### Security Configurations
Because your profile stores sensitive data, you must establish a lock screen:
1. **4-Digit PIN:** Set and confirm a PIN. This is stored in AES-256 encrypted storage.
2. **Biometric Toggle:** Go to the hamburger menu ➔ enable **Fingerprint Lock**. Once enabled, you can bypass the PIN keypad using your fingerprint.
3. **Lock Timeout:** Set how quickly the app should lock itself when in the background (options: *Instant*, *1 min*, *5 min*, *15 min*, *30 min*).

---

## 🗂️ 2. Profile Customization

Profiles allow you to organize data for different contexts. You can maintain separate cards for "Academic Applications", "Professional Job Registrations", or "E-Commerce Shipping".

### Creating a Profile
1. On the main dashboard, tap the **"Add New"** button.
2. Set a profile name and input your name, email, phone, and address.
3. Tap **Save**.

### Adding Custom Fields & Custom Sections
For details that do not fit in the standard fields:
1. Tap a profile card ➔ select **Edit Profile** (or click the edit pencil icon).
2. Tap **Add Custom Field** to create a simple key-value row (e.g. `linkedinUrl` ➔ `linkedin.com/in/user`).
3. Tap **Add Section** to create a grouped block of fields (e.g. creating an "Academic Record" group containing fields for `Roll Number`, `GPA`, and `Major`).

---

## 📷 3. Camera Document Scanner (OCR)

To skip typing long numbers or transcript lists, use the built-in scanner:

1. Open the profile you want to edit and tap **Edit Profile**.
2. Tap the **Camera** button in the header.
3. Center your physical document (e.g., Driver's License, Tax Card, Passport, or Marksheet transcript) inside the camera frame.
4. Tap the **Capture** shutter.
5. The offline ML Kit OCR engine will detect the text:
   * **If standard ID Card:** Automatically creates section labels and values for Name, ID numbers, and DOB.
   * **If Marksheet:** Scans rows, subjects, and grades, compiling them into a tabular list.
6. A verification card will display the parsed data. Adjust any text box values if the scan had minor OCR errors, and select **"Use Data"** to import the new section into your profile.

---

## 💬 4. Smart Overlay & Form Filling

The floating bubble interface allows you to automate form-filling:

### Activating the Overlay
* Navigate to the app drawer and turn on **AI Bubble**. 
* Or, swipe down your quick settings panel and click the **AI Fill** tile.
* A floating bubble view will overlay your screen.

### Executing Autofill
1. Open any third-party app or web browser showing a form.
2. Click on the first form input field to focus it.
3. Tap the **Floating Bubble**.
4. The service will traverse the layout tree, translate the labels if the form is in a regional language, match inputs against your profile, and fill the fields.

### Switching Profiles on the Fly
* **Long-press the floating bubble** to open the Profile Selector. A popup will list all your profiles. Tap a profile to switch context and fill the form using that profile's details.

---

## 🛡️ 5. Built-in Security Controls

### Restricted Application Safety (Payment App Guard)
To protect your privacy and prevent malicious overlays or accidental submissions in sensitive fields:
* **Automatic Deactivation:** The Autofill service automatically monitors active application package names.
* **Blocklist Guard:** Autofill inputs and popup overlays are automatically blocked on secure applications. This includes:
  * **Messaging Tools:** SMS, MMS, WhatsApp, Telegram, Signal.
  * **Social Media:** Instagram, Snapchat, Facebook, Twitter/X.
  * **System Tools:** Calculator, Gallery, Camera.
  * **Payment & Banking Apps:** Google Pay, PhonePe, Paytm, and secure bank apps. If one of these packages is active, the autofill triggers will bypass execution to keep your credentials safe.

### Automatic Clipboard Purge
When copying profile fields manually to your clipboard, a system receiver (`CopyReceiver.kt`) runs a 30-second countdown in the background. After 30 seconds, your system clipboard is cleared automatically to prevent other applications from reading cached credentials.

---

## ⚡ 6. Notification Panel Quick Copy (`QuickCopyService`)

If you prefer not to use the floating overlay bubble, you can copy profile details using the Quick Copy service:

1. Enable the Quick Copy service in the settings menu.
2. A persistent notification will appear in your notification shade showing: **"Smart Autofill Active: [Profile Name]"**.
3. The notification features three quick-action buttons: **"Name"**, **"Phone"**, and **"Email"**.
4. Tap any of the buttons to copy the corresponding value to your clipboard.
5. You can paste the copied value into any input box. The 30-second auto-clear timer will protect the copied text.

---

## ⌨️ 7. Dynamic Text Expansion Shortcuts

For inline expansions, type a shortcut word in any input field:
* **Standard Shortcuts:** `name-` (full name), `email-` (email address), `mob-` (phone number), `addr-` (address).
* **Custom Field Shortcuts:** Type the lowercase custom label with spaces removed followed by a hyphen (e.g. `githuburl-` or `gpa-`).
* **Triggering:** Tap the floating bubble, and the shortcut text will be replaced with your profile value. You can use multiple shortcuts on a single page to fill several fields simultaneously.

---

## 💾 8. Local Backup & Restore (Import / Export)

To save your configurations or move them to a new device:

* **Export Profiles:** Open the settings drawer and tap **"Export Profiles"**. The app compiles your configurations into a JSON file (`profiles_export.json`) and saves it to your device's Downloads directory.
* **Import Profiles:** Tap **"Import Profiles"** in the drawer. A system file picker will open. Select your backed-up JSON file to restore your profiles.
