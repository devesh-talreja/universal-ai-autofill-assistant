# Project Documentation

This document provides a comprehensive overview of the **Universal AI Autofill Assistant** project. It serves as the main reference for project scope, core modules, design objectives, and operational features.

---

## 📌 Project Overview
The Universal AI Autofill Assistant is a mobile utility designed to simplify form completion workflows. In contemporary digital interfaces, users are regularly required to input identical personal, professional, and academic details across web forms, job registration pages, application workflows, and verification portals. 

By cacheing user profile records locally in a structured database, the application scans the active window's visual tree, translates form labels, evaluates custom properties, and populates data using an accessibility service bridge with a single touch.

---

## 🎯 Key Objectives
1. **Universal Execution:** Bypass standard Autofill framework limitations by traversing Accessibility node trees, enabling data insertion across web views, system applications, and custom rendering environments.
2. **Offline-First Privacy:** Perform all OCR scanning, translation, parsing, and database transactions on-device. No data leaves the physical device.
3. **Seamless Extraction (OCR):** Standardize a mechanism to parse physical credentials (ID cards, marksheets) into structured digital profiles without manual typing.
4. **Localization Bridge:** Auto-fill forms written in regional languages by translating label strings into English in real-time, matching against English profiles, and typing the localized values.

---

## ⚙️ Functional Modules

The system is split into four primary functional blocks:

### 1. Profile Manager Module
* **Data Organization:** Allows users to create, modify, and delete profiles representing different personas (e.g., "Personal", "Academic Documents", "Employment").
* **Custom Customizations:** Within each profile, users can define standard parameters (Name, Email, Phone, Address) or configure nested "Sections" (e.g., "10th Grade Marksheet" with custom fields for subjects and scores).
* **Export & Import:** Allows the export of stored profiles to encrypted JSON files and imports data across devices.

### 2. OCR Scanning Module (`CameraX` & ML Kit)
* **Image Capture:** Employs Android CameraX API with a real-time viewfinder to capture high-resolution images of physical documents.
* **On-Device Text Parsing:** Employs Google ML Kit Text Recognition to parse text blocks.
* **Intelligent Document Classifier:** Uses regular expression heuristics and text boundaries to classify documents (e.g., Passports, Tax Cards, Marksheets) and automatically generates appropriate profile fields.

### 3. Smart Autofill Engine (Accessibility Service)
* **Screen Scraping:** Listens to focus and layout changes, traversing the Active Window's Accessibility Node Tree (`AccessibilityNodeInfo`).
* **Advanced Matching Algorithm:**
  1. Priority 1: Searches custom fields and structured section fields (evaluating the longest labels first).
  2. Priority 2: Matches standard parameters.
  3. Priority 3: Performs regional language translation to English if form fields are non-English, matching variables and filling inputs.
* **Auto-Click Utilities:** Performs system-directed clicks on radio groups and checkboxes (e.g., Gender cards, Nationality buttons).
* **Shortcut Expansion:** Tracks text fields. Typing dynamic shortcuts (like `name-` or `aadhaar-`) followed by a bubble tap performs local expansion.

### 4. Security Sandbox Module
* **PIN Locks & Biometrics:** Intercepts application launching using a customized Lock Screen UI, prompting for verification.
* **Lockout Mechanics:** Implements exponential backoffs and lockout intervals after repeated incorrect PIN submissions.
* **Runtime Isolation:** Detects root conditions, blocks screenshot captures (`FLAG_SECURE`), and automatically flushes system clipboards 30 seconds after data copy operations.
