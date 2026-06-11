# System Architecture

This document maps out the system architecture and runtime flows of the **Universal AI Autofill Assistant** using visual diagrams.

---

## 🗺️ 1. Global Application Lifecycle

This flowchart shows the navigation pathways, security checks, and activity states of a user session.

```mermaid
graph TD
    Start([Launch App]) --> Splash[SplashActivity]
    Splash --> PinCheck{PIN Configured?}
    
    %% Security Authorization
    PinCheck -- Yes --> SecurityVerify{Verify Identity}
    SecurityVerify --> |Biometric/PIN Screen| VerifySuccess{Authorized?}
    VerifySuccess -- No --> SecurityVerify
    VerifySuccess -- Yes --> MainApp[MainActivity]
    
    %% Onboarding Journey
    PinCheck -- No --> Tour[OnboardingActivity]
    Tour --> UserInfo[UserInfoActivity]
    UserInfo --> PinSet[PinSetupScreen]
    PinSet --> MainApp
    
    %% Dashboard Options
    MainApp --> Profiles[Profile Management]
    MainApp --> Scanner[CameraActivity]
    MainApp --> ServiceToggle[Toggle Accessibility Overlay]
    
    Scanner --> |ML Kit OCR Parsing| ScanResult[Profile Section Creation]
    ScanResult --> Profiles
    
    ServiceToggle --> OverlayReady[Floating Bubble Rendered]
```

---

## ⚡ 2. Smart Form-Filling Pipeline

This flow diagram illustrates how the `SmartAccessibilityService` reacts to a user tapping the floating bubble overlay, parses the screen, matches labels, and injects text values.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Bubble as Floating Bubble Overlay
    participant Service as SmartAccessibilityService
    participant Parser as Screen Parser (Node Traversal)
    participant ML as Local ML Engine (Language ID/Translate)
    participant DB as SQLite database (Room)
    participant Field as Target App Field
    
    User->>Bubble: Taps Overlay Bubble
    Bubble->>Service: Trigger Autofill
    Service->>Parser: findAllNodes(rootInActiveWindow)
    Parser-->>Service: Return Editable/Clickable Node List
    
    Service->>DB: Fetch Active Profile Data
    DB-->>Service: Return profile structure
    
    loop For each input field node
        Service->>Service: Retrieve label/hint/ID
        
        opt Label language is non-English
            Service->>ML: Detect Language & Translate label string
            ML-->>Service: Return Translated English label
        end
        
        Service->>Service: matchCustomField()
        alt Custom/Section Match Success
            Service->>Field: ACTION_SET_TEXT (custom value)
        else Custom Match Fail
            Service->>Service: matchStandardField()
            alt Standard Match Success
                Service->>Field: ACTION_SET_TEXT (standard value)
            else Standard Match Fail
                Service->>Field: No action (log debug)
            end
        end
    end
    
    Service-->>User: Form populated notification
```

---

## 📷 3. Camera Capture & OCR Pipeline

This flowchart shows how the physical document scanning feature captures images, executes OCR processing offline, and updates the local profile.

```mermaid
graph LR
    Viewfinder[Camera Viewfinder] --> Capture[Capture Button Clicked]
    Capture --> FileSaved[Save Frame to Temp Directory]
    FileSaved --> OCR[ML Kit TextRecognizer]
    OCR --> Parser{Document Classifier}
    
    Parser --> |Matched Regex Patterns| IDCard[Parse ID card: Name, ID, DOB]
    Parser --> |Matched School Keywords| Marksheet[Parse Marksheet: Subjects, Marks, Total]
    
    IDCard --> ResultScreen[Verify Results Dialog]
    Marksheet --> ResultScreen
    
    ResultScreen --> |User Confirmed| CreateSection[Generate Profile Section]
    CreateSection --> SaveDB[Update Room Database]
    SaveDB --> MainScreen[Reload Dashboard]
```
