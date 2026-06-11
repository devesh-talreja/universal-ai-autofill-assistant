# Git Upload & Commit Plan

This document provides a realistic, multi-phase plan to upload the project code gradually to GitHub. It simulates an active development timeline with contributions spread across your 4 team members.

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
  git add .gitignore LICENSE README.md .github/
  git commit -m "chore: initial repository configuration and gitignore setup" --author="Member 1 <member1@example.com>"
  ```

#### Commit 1.2
* **Author:** Member 3 `<member3@example.com>`
* **Branch:** `develop`
* **Command:**
  ```bash
  git add build.gradle.kts settings.gradle.kts gradle.properties local.properties gradlew gradlew.bat
  git commit -m "build: configure multi-module gradle system and dependencies catalog" --author="Member 3 <member3@example.com>"
  ```

#### Commit 1.3
* **Author:** Member 1 `<member1@example.com>`
* **Branch:** `develop`
* **Command:**
  ```bash
  git add app/src/main/AndroidManifest.xml app/src/main/res/xml/
  git commit -m "setup: configure android manifest and accessibility service metadata" --author="Member 1 <member1@example.com>"
  ```

---

### 🎨 Phase 2: Frontend & Design Foundation
*Focus: Setting up Compose widgets, themes, onboarding screens, and launcher activities.*

#### Commit 2.1
* **Author:** Member 2 `<member2@example.com>`
* **Branch:** `feature/ui-foundation` (created from `develop` via `git checkout -b feature/ui-foundation`)
* **Command:**
  ```bash
  git add app/src/main/java/com/example/smartautofiller/ui/theme/
  git commit -m "ui: implement custom dark/light theme configurations and color palettes" --author="Member 2 <member2@example.com>"
  ```

#### Commit 2.2
* **Author:** Member 2 `<member2@example.com>`
* **Branch:** `feature/ui-foundation`
* **Command:**
  ```bash
  git add app/src/main/java/com/example/smartautofiller/ui/SplashActivity.kt app/src/main/java/com/example/smartautofiller/ui/OnboardingActivity.kt
  git commit -m "ui: create splash routing and 4-page onboarding features tour activity" --author="Member 2 <member2@example.com>"
  ```

#### Commit 2.3
* **Author:** Member 2 `<member2@example.com>`
* **Branch:** `feature/ui-foundation`
* **Command:**
  ```bash
  # Merge feature branch back into develop
  git checkout develop
  git merge feature/ui-foundation
  git branch -d feature/ui-foundation
  ```

---

### 🗄️ Phase 3: Storage & Security Implementations
*Focus: Room models, type converters, secure preferences, and PIN lockout locks.*

#### Commit 3.1
* **Author:** Member 3 `<member3@example.com>`
* **Branch:** `feature/security-storage` (created from `develop`)
* **Command:**
  ```bash
  git add app/src/main/java/com/example/smartautofiller/data/
  git commit -m "db: implement UserProfile Room entities and CustomFields converters" --author="Member 3 <member3@example.com>"
  ```

#### Commit 3.2
* **Author:** Member 4 `<member4@example.com>`
* **Branch:** `feature/security-storage`
* **Command:**
  ```bash
  git add app/src/main/java/com/example/smartautofiller/security/PinManager.kt app/src/main/java/com/example/smartautofiller/service/CopyReceiver.kt
  git commit -m "security: establish PinManager validator and 30s clipboard auto-clear receiver" --author="Member 4 <member4@example.com>"
  ```

#### Commit 3.3
* **Author:** Member 3 `<member3@example.com>`
* **Branch:** `feature/security-storage`
* **Command:**
  ```bash
  git add app/src/main/java/com/example/smartautofiller/viewmodel/ProfileViewModel.kt
  git commit -m "db: create profile ViewModel with backup export/import configurations" --author="Member 3 <member3@example.com>"
  ```

#### Commit 3.4
* **Author:** Member 4 `<member4@example.com>`
* **Branch:** `feature/security-storage`
* **Command:**
  ```bash
  # Merge security branch back into develop
  git checkout develop
  git merge feature/security-storage
  git branch -d feature/security-storage
  ```

---

### ⚡ Phase 4: Core Engine & Machine Learning
*Focus: Accessibility service scraping, CameraX capture, and local ML Kit processing.*

#### Commit 4.1
* **Author:** Member 1 `<member1@example.com>`
* **Branch:** `feature/autofill-engine` (created from `develop`)
* **Command:**
  ```bash
  git add app/src/main/java/com/example/smartautofiller/service/SmartAccessibilityService.kt
  git commit -m "feat: establish accessibility node hierarchy traverser and form-matching framework" --author="Member 1 <member1@example.com>"
  ```

#### Commit 4.2
* **Author:** Member 3 `<member3@example.com>`
* **Branch:** `feature/autofill-engine`
* **Command:**
  ```bash
  git add app/src/main/java/com/example/smartautofiller/ui/CameraActivity.kt
  git commit -m "feat: implement CameraX frame capture and document OCR text parser" --author="Member 3 <member3@example.com>"
  ```

#### Commit 4.3
* **Author:** Member 1 `<member1@example.com>`
* **Branch:** `feature/autofill-engine`
* **Command:**
  ```bash
  git add app/src/main/java/com/example/smartautofiller/service/AiFillTileService.kt app/src/main/java/com/example/smartautofiller/service/SmartAutofillService.kt
  git commit -m "feat: configure quick settings tile toggle and secondary autofill framework" --author="Member 1 <member1@example.com>"
  ```

#### Commit 4.4
* **Author:** Member 1 `<member1@example.com>`
* **Branch:** `feature/autofill-engine`
* **Command:**
  ```bash
  # Merge engine branch back into develop
  git checkout develop
  git merge feature/autofill-engine
  git branch -d feature/autofill-engine
  ```

---

### 🧪 Phase 5: Verification, Docs & final Polish
*Focus: Compose views integration, test plans, database schema updates, and validation logs.*

#### Commit 5.1
* **Author:** Member 2 `<member2@example.com>`
* **Branch:** `develop`
* **Command:**
  ```bash
  git add app/src/main/java/com/example/smartautofiller/MainActivity.kt app/src/main/java/com/example/smartautofiller/ui/UserInfoActivity.kt app/src/main/java/com/example/smartautofiller/ui/FeaturesActivity.kt app/src/main/java/com/example/smartautofiller/ui/PrivacyPolicyActivity.kt
  git commit -m "ui: integrate MainActivity profile dashboard and custom form selector popups" --author="Member 2 <member2@example.com>"
  ```

#### Commit 5.2
* **Author:** Member 4 `<member4@example.com>`
* **Branch:** `develop`
* **Command:**
  ```bash
  git add database/ docs/ screenshots/ tests/ team/
  git commit -m "docs: finalize developer architecture plans, verification results, and QA test matrices" --author="Member 4 <member4@example.com>"
  ```

#### Commit 5.3
* **Author:** Member 1 `<member1@example.com>`
* **Branch:** `main`
* **Command:**
  ```bash
  # Merge all developed changes into main branch and push to GitHub
  git checkout main
  git merge develop
  git push origin main
  ```
