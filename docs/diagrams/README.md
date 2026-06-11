# Diagrams and Visual Assets Guide

This directory holds the assets, schematics, and generation instructions for diagrams used in the project documentation.

---

## 📊 Generating Visuals with Mermaid
The system flowcharts and block diagrams inside **[System Architecture](file:///docs/System_Architecture.md)** are written in **Mermaid.js**, a markdown-compatible visual graphing syntax.

### How to View and Edit:
1. **GitHub Rendering:** GitHub natively renders Mermaid block code. When viewed on GitHub, these flowcharts will display as rendered SVG diagrams.
2. **VS Code / Android Studio Plugin:** Install the "Mermaid Preview" extension to view live changes in your IDE as you edit the markdown source.
3. **Mermaid Live Editor:**
   * Copy the Mermaid code blocks from [System Architecture](file:///docs/System_Architecture.md).
   * Paste them into the [Mermaid Live Editor](https://mermaid.live).
   * Customize styling or nodes, and export the updated graphics as PNG or SVG files.

---

## 🎨 Recommended Screenshot Assets
For evaluation, export PNG screenshots of the app from your test device and save them in the [screenshots/](file:///screenshots/README.md) directory. Recommended screenshots include:
1. **`onboarding.png`:** The 4-page features tour showing the introductory SVGs.
2. **`profiles.png`:** The main dashboard displaying profile lists ("My Info", etc.).
3. **`bubble_fill.png`:** A demonstration of the floating bubble overlay populating a form field.
4. **`ocr_results.png`:** The OCR scanner interface showing parsed fields from a document.
