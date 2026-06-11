# Development Workflows & Standards

This document establishes the Git management strategy, code verification process, and review pipelines for the team.

---

## 🔀 1. Branching Strategy
We use a simplified Git Flow model to enable team collaboration without code conflicts.

```
                  ┌──────────────────────────────┐
                  │      main (Production)       │
                  └──────────────┬───────────────┘
                                 │ (Merge PR)
                                 ▼
                  ┌──────────────────────────────┐
                  │     develop (Integration)    │
                  └──────────────┬───────────────┘
                                 │ (Branch out)
                                 ├──────────────────────────────┐
                                 ▼                              ▼
                  ┌──────────────────────────────┐┌──────────────────────────────┐
                  │    feature/accessibility     ││        feature/ocr-ml        │
                  └──────────────────────────────┘└──────────────────────────────┘
```

* **`main`:** Contains production-ready code. Merges here are restricted and only allowed via reviewed pull requests.
* **`develop`:** The central integration branch. All feature branches are merged here first for testing.
* **`feature/<name>`:** Created by individual developers for specific features or bug fixes (e.g. `feature/pin-lock`, `feature/ui-onboarding`).
* **`bugfix/<name>`:** Temporary branch to resolve a specific regression or defect (e.g. `bugfix/recycling-leak`).

---

## 💬 2. Commit Message Standards
We follow **Conventional Commits** to keep the repository history clean and easy to scan:

```
<type>: <description>
```

### Approved Types:
* **`feat`:** A new feature (e.g., `feat: integrate biometric verification`).
* **`fix`:** A bug fix (e.g., `fix: resolve marksheet OCR table alignment`).
* **`docs`:** Documentation changes (e.g., `docs: add installation instructions`).
* **`style`:** Changes that do not affect the meaning of the code (formatting, indentation).
* **`refactor`:** Code changes that neither fix a bug nor add a feature.
* **`test`:** Adding missing tests or correcting existing ones.

---

## 🔍 3. Code Review & Integration Checklist
Before merging any feature branch into `develop` or `develop` into `main`, the developer must verify the checklist below:

1. **Local Build:** Project compiles with `./gradlew assembleDebug` without warnings.
2. **Device Execution:** Run the app on a physical device; confirm permission requests function correctly.
3. **No Memory Leaks:** Verify accessibility nodes are recycled properly and no background threads remain uncancelled.
4. **Security Integrity:** Ensure PIN, SharedPreferences, and database instances use appropriate access levels.
5. **No Placeholders:** Remove diagnostic logs, mock entries, or TODO comments before submitting.
