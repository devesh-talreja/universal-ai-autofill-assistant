# User Manual

Welcome to the **Universal AI Autofill Assistant** User Manual. Follow this guide to set up your profiles, scan documents, and automate form-filling across your favorite apps and websites.

---

## 🏁 Getting Started & Onboarding

1. **First-Time Launch:** When you open the app for the first time, you will see a 4-page features tour detailing the application's key capabilities.
2. **Personal Profile Setup:** Following the tour, you will be prompted to enter basic info (Name, Country, State, Gender, Date of Birth).
   * This information automatically generates your default **"My Info"** profile, which is pinned to the top of your dashboard.
3. **Security Configuration:** Set a 4-digit PIN lock. You can also enable **Biometric Lock** (fingerprint scan) inside Settings for quicker access.

---

## 🗂️ Profile Management

Profiles represent different sets of data you might need. For example, you might create a "Work" profile for job application forms, and an "Academic" profile for university registries.

### Creating a Profile
1. On the main dashboard, tap the **"+"** Floating Action Button (FAB) at the bottom-right.
2. Enter a profile name (e.g., "Work Profile") and fill in the standard fields.
3. Tap **Save**.

### Adding Custom Fields & Sections
To store fields that are not included by default (like your GitHub profile or National ID):
1. Tap on an existing profile from the dashboard to open its details.
2. Tap **Edit Profile**.
3. Scroll down and tap **Add Section** (creates a grouped block, e.g., "Social Links") or **Add Custom Field** (adds a key-value row).
4. Enter a Label (e.g., "LinkedIn URL") and Value (e.g., "linkedin.com/in/user").
5. Tap **Save Changes**.

---

## 📷 Document Scanning (OCR)

Avoid manual data entry by using the scanner to parse physical ID cards or high-school transcripts.

1. Open the profile you want to edit.
2. Tap **Scan Document** (Camera icon).
3. Align your physical document (e.g. Identity Card, Grade Card) inside the camera viewfinder guidelines.
4. Tap the **Capture** button.
5. The application will process the text offline:
   * **If standard card (e.g. Identity/Tax Card):** Creates a section with extracted names, dates, and registration numbers.
   * **If Academic Marksheet:** Automatically compiles lists of subject names, marks, grades, and totals.
6. Verify the extracted fields on the results dialog, adjust values if necessary, and select **Use Data** to append it directly to your profile.

---

## 💬 Using the Floating Bubble Autofill

Once your profiles are set up, you can automate form filling:

### 1. Activating the Overlay
* Open the navigation menu in the app and toggle **Enable Floating Bubble**.
* Alternatively, swipe down your notification shade and tap the **AI Fill** Quick Settings Tile.
* A floating bubble icon will appear overlaying your screen.

### 2. Filling a Form
1. Open the target application or website containing the form you want to fill.
2. Click on the first input field to focus it.
3. Tap the **Floating Bubble**.
4. The service will scan the form and auto-populate all matching fields.

### 3. Switching Profiles
* If you have multiple profiles, **long-press the floating bubble** to open the profile selection overlay. Tap the profile you wish to use for the current form.

---

## ⌨️ Text Expansion Shortcuts (Power-User Feature)

If you only need to fill a single specific field instead of the entire form, you can use text expansion abbreviations.

1. Focus a text field.
2. Type one of the following shortcuts:
   * `name-` ➔ Expands to your Full Name
   * `email-` ➔ Expands to your Email Address
   * `mob-` ➔ Expands to your Phone Number
   * `addr-` ➔ Expands to your Address
   * `[custom_field_label]-` ➔ Expands to the value of that custom field (lowercase, spaces removed, ending with a hyphen. E.g., `github-`).
3. Tap the **Floating Bubble** to trigger expansion. The text shortcut will be instantly replaced by the corresponding profile value.
