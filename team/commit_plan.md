# Git Upload & Commit Plan (Modular Structure)

This document provides a realistic, multi-phase plan to upload the project code gradually to GitHub. It simulates an active development timeline with contributions spread across your 4 team members, matching the restructured repository layout.

---

## 💡 How to Commit as Different Team Members from One Machine

If you are uploading the codebase yourself, you can simulate commits from different team members by using the `--author` flag in your local git command line. 

If the email address used matches a team member's verified GitHub email, GitHub will link that commit to their profile.

### Git Command Syntax:
```bash
git commit -m "commit message" --author="Member Name <member.email@example.com>"
```

### Setup & Upload Workflow:
1. **Initialize the local Git repository:**
   ```bash
   git init
   ```
2. **Add your remote GitHub repository:**
   ```bash
   git remote add origin <your-repository-url>
   ```
3. **Fetch the remote** (to download the remote-created `LICENSE` file):
   ```bash
   git pull origin main
   ```
4. **Create a `develop` branch** to run your integration commits:
   ```bash
   git checkout -b develop
   ```
5. Follow the step-by-step commit plan below, running each commit command in sequence.

---

## 📅 Chronological Commit Phases

### 🛠️ Phase 1: Foundation & Base Structure
*Focus: Initial directory setup, repository guidelines, database skeletons.*

#### Commit 1.1
* **Author:** Member 1 `<member1@example.com>`
* **Branch:** `develop`
* **Command:**
  ```bash
  git add .gitignore LICENSE README.md .github/ CONTRIBUTING.md .env.example
  git commit -m "chore: initial repository configuration and gitignore setup" --author="Member 1 <member1@example.com>"
  ```

---

### 🎨 Phase 2: Frontend & Design Foundation (UI)
*Focus: Setting up user interface files, colors, themes, layout XML views, and onboarding screens.*

#### Commit 2.1
* **Author:** Member 2 `<member2@example.com>`
* **Branch:** `feature/ui-onboarding` (created from `develop` via `git checkout -b feature/ui-onboarding`)
* **Command:**
  ```bash
  git add frontend/Color.kt frontend/Theme.kt frontend/Type.kt
  git commit -m "ui: implement custom dark/light theme configurations and typography guidelines" --author="Member 2 <member2@example.com>"
  ```

#### Commit 2.2
* **Author:** Member 2 `<member2@example.com>`
* **Branch:** `feature/ui-onboarding`
* **Command:**
  ```bash
  git add frontend/layout_floating_bubble.xml frontend/layout_profile_selector.xml frontend/layout_profile_item.xml frontend/autofill_item.xml
  git commit -m "ui: design floating bubble overlay layouts and popup selector view templates" --author="Member 2 <member2@example.com>"
  ```

#### Commit 2.3
* **Author:** Member 2 `<member2@example.com>`
* **Branch:** `feature/ui-onboarding`
* **Command:**
  ```bash
  git add frontend/SplashActivity.kt frontend/OnboardingActivity.kt frontend/UserInfoActivity.kt frontend/FeaturesActivity.kt frontend/PrivacyPolicyActivity.kt
  git commit -m "ui: create onboarding flows, splash launcher, and features tour widgets" --author="Member 2 <member2@example.com>"
  ```

#### Commit 2.4
* **Author:** Member 2 `<member2@example.com>`
* **Branch:** `feature/ui-onboarding`
* **Command:**
  ```bash
  # Merge feature branch back into develop
  git checkout develop
  git merge feature/ui-onboarding
  git branch -d feature/ui-onboarding
  ```

---

### 🗄️ Phase 3: Backend & Security Integration
*Focus: Accessibility service handlers, local storage interfaces, and biometric lock bounds.*

#### Commit 3.1
* **Author:** Member 3 `<member3@example.com>`
* **Branch:** `feature/backend-engine` (created from `develop` via `git checkout -b feature/backend-engine`)
* **Command:**
  ```bash
  git add backend/UserProfile.kt backend/UserProfileDao.kt backend/Converters.kt backend/AppDatabase.kt
  git commit -m "db: implement user profiles database schema, daos, and type converters" --author="Member 3 <member3@example.com>"
  ```

#### Commit 3.2
* **Author:** Member 4 `<member4@example.com>`
* **Branch:** `feature/backend-engine`
* **Command:**
  ```bash
  git add backend/PinManager.kt backend/CopyReceiver.kt
  git commit -m "security: establish PinManager validations, lockout intervals, and clipboard clears" --author="Member 4 <member4@example.com>"
  ```

#### Commit 3.3
* **Author:** Member 1 `<member1@example.com>`
* **Branch:** `feature/backend-engine`
* **Command:**
  ```bash
  git add backend/SmartAccessibilityService.kt backend/SmartAutofillService.kt backend/AiFillTileService.kt
  git commit -m "feat: implement smart accessibility form-matching traverser and quick settings tile" --author="Member 1 <member1@example.com>"
  ```

#### Commit 3.4
* **Author:** Member 1 `<member1@example.com>`
* **Branch:** `feature/backend-engine`
* **Command:**
  ```bash
  git add backend/QuickCopyService.kt
  git commit -m "feat: design QuickCopy persistent foreground notification panel actions" --author="Member 1 <member1@example.com>"
  ```

#### Commit 3.5
* **Author:** Member 4 `<member4@example.com>`
* **Branch:** `feature/backend-engine`
* **Command:**
  ```bash
  # Merge backend branch back into develop
  git checkout develop
  git merge feature/backend-engine
  git branch -d feature/backend-engine
  ```

---

### 📷 Phase 4: Core Wiring & ML Operations
*Focus: Camera scanning, ViewModel integration, main compose layouts, and project dependencies.*

#### Commit 4.1
* **Author:** Member 3 `<member3@example.com>`
* **Branch:** `feature/core-integration` (created from `develop` via `git checkout -b feature/core-integration`)
* **Command:**
  ```bash
  git add frontend/CameraActivity.kt
  git commit -m "ml: integrate CameraX frame capture and document OCR text parser" --author="Member 3 <member3@example.com>"
  ```

#### Commit 4.2
* **Author:** Member 1 `<member1@example.com>`
* **Branch:** `feature/core-integration`
* **Command:**
  ```bash
  git add core/build.gradle.kts core/settings.gradle.kts core/gradle.properties core/AppDatabase.kt core/Converters.kt core/UserProfile.kt core/UserProfileDao.kt
  git commit -m "build: configure core module dependencies, view model catalogs, and databases" --author="Member 1 <member1@example.com>"
  ```

#### Commit 4.3
* **Author:** Member 2 `<member2@example.com>`
* **Branch:** `feature/core-integration`
* **Command:**
  ```bash
  git add core/MainActivity.kt core/ProfileViewModel.kt
  git commit -m "ui: wire MainActivity compose layouts with ProfileViewModel storage flows" --author="Member 2 <member2@example.com>"
  ```

#### Commit 4.4
* **Author:** Member 1 `<member1@example.com>`
* **Branch:** `feature/core-integration`
* **Command:**
  ```bash
  # Merge core branch back into develop
  git checkout develop
  git merge feature/core-integration
  git branch -d feature/core-integration
  ```

---

### 🧪 Phase 5: Verification, Tests & Final Documentation
*Focus: Test suites, sample JSON mock tables, and documentation indices.*

#### Commit 5.1
* **Author:** Member 4 `<member4@example.com>`
* **Branch:** `develop`
* **Command:**
  ```bash
  git add database/schema.sql database/sample_data.json database/README.md
  git commit -m "docs: compile SQLite schemas and profile backup mock JSON data sheets" --author="Member 4 <member4@example.com>"
  ```

#### Commit 5.2
* **Author:** Member 4 `<member4@example.com>`
* **Branch:** `develop`
* **Command:**
  ```bash
  git add tests/Test_Cases.md tests/Test_Results.md screenshots/README.md
  git commit -m "test: catalog functional QA matrices, manual validation tests, and device performance runs" --author="Member 4 <member4@example.com>"
  ```

#### Commit 5.3
* **Author:** Member 4 `<member4@example.com>`
* **Branch:** `develop`
* **Command:**
  ```bash
  git add docs/Project_Documentation.md docs/Installation_Guide.md docs/User_Manual.md docs/Technical_Documentation.md docs/System_Architecture.md docs/API_Documentation.md docs/README.md docs/diagrams/README.md
  git commit -m "docs: finalize user guides, system architecture diagrams, and internal API documentation" --author="Member 4 <member4@example.com>"
  ```

#### Commit 5.4
* **Author:** Member 1 `<member1@example.com>`
* **Branch:** `main`
* **Command:**
  ```bash
  # Merge all developed changes into main branch and push to GitHub
  git checkout main
  git merge develop
  git push origin main
  ```
