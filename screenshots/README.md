# Application Screenshots Directory

This folder holds screenshots demonstrating the application's user interface and core feature flows. These screenshots are referenced in the main [README.md](file:///README.md) file.

---

## 📸 Required Screenshots & Naming Conventions

To ensure the images render correctly on GitHub and local markdown previews, take screenshots of the application on your physical testing device and save them in this folder using the following filenames:

### 1. `onboarding.png`
* **Content:** The introductory screens showing the application's onboarding animations or the setup screen where basic user information (country, state, gender, date of birth) is collected.
* **Purpose:** Demonstrates the first-run user experience.

### 2. `profiles.png`
* **Content:** The main dashboard of the application showing the default pinned "My Info" card (with a gradient border) and other custom user profiles (e.g. Work, Academic).
* **Purpose:** Demonstrates the core profile management interface.

### 3. `bubble_fill.png`
* **Content:** A screenshot showing the active floating bubble icon overlaying another app's form, or showing the profile selection dialog triggered by a long-press on the bubble.
* **Purpose:** Demonstrates the Accessibility Service overlay and form-filling system in action.

### 4. `ocr_results.png` (Optional)
* **Content:** The CameraX scanner overlay showing detected text bounding boxes, or the results verification dialog displaying parsed fields extracted from a document.
* **Purpose:** Demonstrates document scanning capabilities.

---

## 🎨 Tips for Capturing High-Quality Screenshots
1. **Device Mockups:** Keep screenshots in standard portrait resolution (e.g. 1080x2400) for a consistent layout.
2. **Clear Mock Data:** When capturing screenshots, use the mock profile data from [sample_data.json](file:///database/sample_data.json) to avoid displaying real personal details.
3. **Overlay Trigger:** For `bubble_fill.png`, open a standard signup form (e.g., a simple HTML page) in Chrome to show the floating bubble in a realistic scenario.
