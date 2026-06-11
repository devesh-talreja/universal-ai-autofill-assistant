# Contributing to Universal AI Autofill Assistant

Thank you for contributing! To maintain code quality and ensure a smooth development process for the team, please follow these guidelines.

## Code Style & Standards

### Kotlin & Jetpack Compose
* **Formatting:** Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html). Use Android Studio's code formatter (`Ctrl+Alt+L` on Windows).
* **Compose State:** Hoist state where appropriate. Always use `remember { mutableStateOf(...) }` inside composables, or backed by `MutableStateFlow` in ViewModels.
* **Coroutines:** Do not block the main thread. Use `viewModelScope` in ViewModels and `serviceScope` inside services (e.g., in `SmartAccessibilityService` using appropriate dispatchers like `Dispatchers.Default` for calculation and `Dispatchers.Main` for UI injection).
* **Recycle Nodes:** Always call `.recycle()` on `AccessibilityNodeInfo` objects to prevent memory leaks in layout traversal.

### Architecture (MVVM)
* **Model:** Room entities (`UserProfile`) must be isolated. Always ensure Room column schemas match exactly and conversions are handled via `CustomFieldsConverter`.
* **ViewModel:** ViewModels should not reference Android UI classes (`Context`, `View`). Use `AndroidViewModel` only when `Application` context is strictly necessary.
* **View:** Compose components should be stateless and react to states exposed by ViewModels.

---

## Development Workflow

### 1. Branching Strategy
We follow a simplified Git Flow model:
* `main`: Represents production-ready code. Commits here should only come from merges.
* `develop`: Integration branch for active features.
* `feature/<feature-name>`: Temporary branches for individual features or bug fixes.

### 2. Commit Message Convention
Commit messages should be clear and descriptive:
* `feat: add biometric lock support`
* `fix: prevent layout freeze by adding touch flags`
* `docs: add installation instructions`
* `test: verify profile matching algorithms`

### 3. Pull Request (PR) Process
1. Create a branch from `develop`.
2. Implement and locally test your changes on a physical device.
3. Push to your branch and open a Pull Request using the `.github/PULL_REQUEST_TEMPLATE.md`.
4. Ensure at least one team member reviews and signs off on the PR before merging.
