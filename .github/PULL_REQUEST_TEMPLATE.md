# Pull Request (PR) Template

## 📋 Description
Provide a concise summary of the changes introduced by this PR (e.g. what features were added, or what bugs were resolved).

---

## 🛠️ Proposed Changes
- [e.g. Added biometric auth toggle in settings]
- [e.g. Modified matching order in SmartAccessibilityService to check custom fields first]
- [e.g. Added node recycling in findAllNodes() to prevent memory leaks]

---

## 🚀 Testing Accomplished
- **Device Tested On:** [e.g., OnePlus Nord, Android 16]
- **Verification Performed:**
  - [ ] App compiles cleanly without errors or warnings.
  - [ ] Tested basic onboarding flow and security checks on a physical device.
  - [ ] Verified forms are matched and populated without overlay issues.
  - [ ] Verified memory footprint of Accessibility Service using Android Studio Profiler.

---

## ⚙️ Checklist
- [ ] Coding style follows project standards outlined in [CONTRIBUTING.md](file:///CONTRIBUTING.md).
- [ ] Checked for potential memory leaks (recycled all AccessibilityNodeInfo objects).
- [ ] No plaintext storage of credentials (checked PinManager/SharedPreferences).
- [ ] Documentation updated (in [docs/](file:///docs/README.md) or main [README.md](file:///README.md)) if changes modify existing workflows.
- [ ] Local JUnit/Instrumented tests passed successfully.
