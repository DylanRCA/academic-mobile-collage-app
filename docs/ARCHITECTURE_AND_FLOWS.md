# Software Architecture & Application Flows

## Project Overview
**Application:** Real-Time Interactive Event Collage Mobile App  
**Platform:** Native Android (Java)  
**Backend & Database:** Firebase Authentication (Anonymous) & Firebase Realtime Database  
**Camera & Scanning:** Android CameraX API & Google MLKit Barcode Scanning

---

## 1. System Architecture

```
                               ┌──────────────────────────────────────────────┐
                               │       GitHub Releases APK Distribution       │
                               │  https://github.com/DylanRCA/.../app.apk?   │
                               │             eventId=boda_demo                │
                               └──────────────────────┬───────────────────────┘
                                                      │
                       ┌──────────────────────────────┴──────────────────────────────┐
                       │                                                             │
            (App NOT Installed)                                            (App INSTALLED)
                       │                                                             │
                       ▼                                                             ▼
     ┌────────────────────────────────────┐                        ┌────────────────────────────────────┐
     │  Browser Downloads app-debug.apk   │                        │ Android Intent Filter Intercepts   │
     │  User Installs APK on Device       │                        │ Opens App directly into Event      │
     └─────────────────┬──────────────────┘                        └─────────────────┬──────────────────┘
                       │                                                             │
                       └──────────────────────────────┬──────────────────────────────┘
                                                      │
                                                      ▼
                                       ┌──────────────────────────────┐
                                       │       MainActivity.java      │
                                       │   Extracts eventId Parameter │
                                       └──────────────┬───────────────┘
                                                      │
                                                      ▼
                                       ┌──────────────────────────────┐
                                       │   Session & Profile Check    │
                                       └──────────────┬───────────────┘
                                                      │
                            ┌─────────────────────────┴─────────────────────────┐
                            │                                                   │
                    (No User Profile)                                    (User Registered)
                            │                                                   │
                            ▼                                                   ▼
         ┌────────────────────────────────────┐              ┌────────────────────────────────────┐
         │        LoginFragment.java          │              │    CameraCaptureFragment.java      │
         │  Asks Phone (Username) + Email     │              │   Captures 16:9 Landscape Photo    │
         │  Queries Phone in Firebase DB      │              └─────────────────┬──────────────────┘
         │  (Reuses Account or Saves Profile) │                                │
         └─────────────────┬──────────────────┘                                │
                           │                                                   │
                           └──────────────────────────┬────────────────────────┘
                                                      │
                                                      ▼
                                       ┌──────────────────────────────┐
                                       │   PostReviewFragment.java    │
                                       │  Reviews Photo & Dedication  │
                                       │  Encodes Base64 to Firebase  │
                                       └──────────────┬───────────────┘
                                                      │
                                                      ▼
                                       ┌──────────────────────────────┐
                                       │    CollageFragment.java      │
                                       │  12 Widescreen Photos (3x4)  │
                                       │  Scale + Translate Animation │
                                       │  3D Card Flip for >12 Posts  │
                                       └──────────────────────────────┘
```

---

## 2. End-to-End Application Flows

### Flow A: Single QR Scan for New Users (Download + Auto-Routing)
1. **QR Code Scanning**:
   - The QR code contains a direct URL:  
     `https://github.com/DylanRCA/album-mobile-app-apk/releases/download/tag2/app-debug-androidTest.apk?eventId=boda_demo`
2. **First Scan (App NOT Installed)**:
   - Device browser opens the GitHub link and downloads the `app-debug-androidTest.apk` file directly.
   - User taps the downloaded APK to install the application.
3. **Second Scan / Touch QR Link (App INSTALLED)**:
   - When tapping the QR link or scanning it with the phone camera, Android's `<intent-filter>` in `AndroidManifest.xml` intercepts `github.com/DylanRCA/...` instantly.
   - `MainActivity` launches, reads `data.getQueryParameter("eventId")` (`boda_demo`), fetches event metadata, and routes to registration or camera.

---

### Flow B: User Registration & Passwordless Account Recovery
1. **Input**: User enters Phone Number (used as unique username) and Email in `LoginFragment.java`.
2. **Lookup before Save**:
   - `repository.findUserByPhoneNumber(phoneNumber)` queries Firebase Realtime Database `/users` where `phoneNumber == inputPhone`.
3. **Existing Account Found**:
   - Reuses existing `User` node and logs back in without creating duplicate database records.
4. **New User**:
   - Authenticates anonymously via `auth.signInAnonymously()`.
   - Saves new user node to `/users/{userId}`.

---

### Flow C: Camera Capture & Photo Review
1. **Orientation**: `CameraCaptureFragment.java` automatically locks the screen to **Landscape (`LANDSCAPE`)** for widescreen photos (16:9).
2. **Camera Controls**:
   - `btnShutter`: Takes a photo via CameraX.
   - `btnSwitchCamera`: Toggles front/back camera (uses `ic_switch_camera` 🔁 vector icon).
   - `btnGallery`: Launches Android Gallery picker (`GetContent("image/*")`), downsampling images safely in background threads to 1024x768 to prevent `OutOfMemoryError`.
3. **Review & Publish**: `PostReviewFragment.java` displays the captured image with the event logo and an optional dedication text input. When submitted, the image is JPEG-compressed, Base64-encoded, and pushed to `/posts`.

---

### Flow D: Full-Screen Live Collage & Animations
1. **Widescreen 3x4 Grid (12 Photos)**:
   - `CollageFragment.java` renders 12 photos across 3 rows and 4 columns in 16:9 aspect ratio without scroll.
   - Replaces top header bar with a subtle glassmorphism watermark badge in the bottom-left corner (`cardWatermark`).
2. **Scale & Translate Entry Animation**:
   - Newly published photo appears in a centered Spotlight overlay (`layoutHeroSpotlight`).
   - After a 1.8s preview, the photo scales down and translates smoothly across the screen into grid slot `[0,0]` (top-left).
   - Upon landing, existing photos shift 1 position forward, and slot `[0,0]` displays a glowing gold border (`#FFD700`) for 3 seconds.
3. **3D Y-Axis Card Flip Rotation (For >12 Photos)**:
   - When total event photos exceed 12, a 5-second `Handler` timer runs `startAutomaticFlipTimer()`.
   - Selects a random card position between 1 and 11 (**slot `[0,0]` is protected and never flips**).
   - Animates the card `rotationY` from `0°` to `90°`, swaps the image for an unrendered historical photo from Firebase, and rotates `rotationY` from `-90°` back to `0°`.
4. **Photo Detail Modal & Message Indicator**:
   - Cards with dedications display a `💬` badge (`ivMessageIndicator`) in the bottom-right corner.
   - Tapping any photo card opens `layoutPhotoViewerModal` centered over a dark backdrop with full image and scrollable message card (`ScrollView`).
   - Tapping outside the photo card dismisses the modal.

---

### Flow E: QR Scanner & Logout Management
1. **Orientation**: `QRScannerFragment.java` locks screen orientation to **Portrait (`PORTRAIT`)**.
2. **Session Badges**:
   - **Active Session**: Displays top-left badge `📱 +51 987654321` + top-right button `[🚪 Cerrar sesión]`.
   - **No Active Session**: Displays top-left badge `👤 Sin sesión activa` + hides `[Cerrar sesión]`.
3. **Sign Out Action**: Tapping `btnLogout` calls `repository.signOut()` (`auth.signOut()`), hides `btnLogout`, sets badge to `👤 Sin sesión activa`, and displays "Sesión cerrada correctamente" feedback Toast.
