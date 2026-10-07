# Project Structure & Source Code Reference

## 1. Directory Tree & Architecture Overview

```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/appcollagefotos/
│   ├── MainActivity.java
│   ├── data/
│   │   ├── model/
│   │   │   ├── Event.java
│   │   │   ├── Post.java
│   │   │   └── User.java
│   │   └── repository/
│   │       └── FirebaseRepository.java
│   ├── ui/
│   │   ├── adapter/
│   │   │   └── CollageAdapter.java
│   │   └── fragment/
│   │       ├── CameraCaptureFragment.java
│   │       ├── CollageFragment.java
│   │       ├── LoginFragment.java
│   │       ├── PostReviewFragment.java
│   │       └── QRScannerFragment.java
│   └── util/
│       └── ImageProcessor.java
└── res/
    ├── drawable/
    │   ├── ic_gallery.xml
    │   ├── ic_logout.xml
    │   ├── ic_message_indicator.xml
    │   ├── ic_switch_camera.xml
    │   └── shape_*.xml
    └── layout/
        ├── activity_main.xml
        ├── fragment_camera_capture.xml
        ├── fragment_collage.xml
        ├── fragment_login.xml
        ├── fragment_post_review.xml
        ├── fragment_qr_scanner.xml
        └── item_collage_post.xml
```

---

## 2. Java Source File Breakdown

### Main Navigation & Activity
* **[MainActivity.java](file:///C:/Users/Dylan/AndroidStudioProjects/AppCollageFotosSNT/app/src/main/java/com/example/appcollagefotos/MainActivity.java)**
  - Controls fragment navigation (`QRScannerFragment`, `LoginFragment`, `CameraCaptureFragment`, `PostReviewFragment`, `CollageFragment`).
  - Manages runtime camera permissions.
  - Intercepts Deep Links from GitHub Releases APK links (`getIntent().getData().getQueryParameter("eventId")`).
  - Switches screen orientation dynamically (`SCREEN_ORIENTATION_PORTRAIT` vs `SCREEN_ORIENTATION_LANDSCAPE`).

### Data Layer & Firebase Models
* **[Event.java](file:///C:/Users/Dylan/AndroidStudioProjects/AppCollageFotosSNT/app/src/main/java/com/example/appcollagefotos/data/model/Event.java)**
  - POJO implementing `Serializable`. Contains event metadata (`id`, `name`, `eventType`, `logoBase64`, `eventCode`, `active`, `createdAt`).
* **[Post.java](file:///C:/Users/Dylan/AndroidStudioProjects/AppCollageFotosSNT/app/src/main/java/com/example/appcollagefotos/data/model/Post.java)**
  - POJO implementing `Serializable`. Represents an image post (`id`, `eventId`, `userId`, `imageBase64`, `message`, `createdAt`, `deviceInfo`).
* **[User.java](file:///C:/Users/Dylan/AndroidStudioProjects/AppCollageFotosSNT/app/src/main/java/com/example/appcollagefotos/data/model/User.java)**
  - POJO implementing `Serializable`. Represents registered user profile (`id`, `phoneNumber`, `email`, `createdAt`).
* **[FirebaseRepository.java](file:///C:/Users/Dylan/AndroidStudioProjects/AppCollageFotosSNT/app/src/main/java/com/example/appcollagefotos/data/repository/FirebaseRepository.java)**
  - Handles Firebase Authentication & Realtime Database reads/writes.
  - Implements `SimpleCallback<T>` and `DataListener<T>` interfaces.
  - Key methods: `signInAnonymouslyIfNeeded()`, `createDemoEventsIfNotExist()`, `getEvent()`, `listenToPosts()`, `createPost()`, `saveUserProfile()`, `getUserProfile()`, `findUserByPhoneNumber()`, `signOut()`.

### UI Fragments
* **[QRScannerFragment.java](file:///C:/Users/Dylan/AndroidStudioProjects/AppCollageFotosSNT/app/src/main/java/com/example/appcollagefotos/ui/fragment/QRScannerFragment.java)**
  - Scans QR codes using CameraX & MLKit Barcode Scanning.
  - Displays session badge (`cardUserBadge`) with user phone number or `👤 Sin sesión activa`.
  - Displays expanded logout button (`btnLogout`).
* **[LoginFragment.java](file:///C:/Users/Dylan/AndroidStudioProjects/AppCollageFotosSNT/app/src/main/java/com/example/appcollagefotos/ui/fragment/LoginFragment.java)**
  - Form asking for Phone Number (username) and Email.
  - Validates input, queries `findUserByPhoneNumber()` to reuse existing profiles or creates new user profiles.
* **[CameraCaptureFragment.java](file:///C:/Users/Dylan/AndroidStudioProjects/AppCollageFotosSNT/app/src/main/java/com/example/appcollagefotos/ui/fragment/CameraCaptureFragment.java)**
  - CameraX capture in Landscape mode.
  - Features shutter button (`btnShutter`), camera switcher (`btnSwitchCamera` 🔁), and gallery picker (`btnGallery`).
  - Downsamples gallery URIs safely in background threads to 1024x768 to prevent `OutOfMemoryError`.
* **[PostReviewFragment.java](file:///C:/Users/Dylan/AndroidStudioProjects/AppCollageFotosSNT/app/src/main/java/com/example/appcollagefotos/ui/fragment/PostReviewFragment.java)**
  - Image review screen with event logo, captured image, and optional dedication text input.
  - Encodes image to Base64 in background thread and pushes post to `/posts`.
* **[CollageFragment.java](file:///C:/Users/Dylan/AndroidStudioProjects/AppCollageFotosSNT/app/src/main/java/com/example/appcollagefotos/ui/fragment/CollageFragment.java)**
  - Renders 12 widescreen photos (3x4) with zero scroll.
  - Runs spotlight-to-grid scale & translate animation (`animateSpotlightToGridSlot`).
  - Triggers 3-second gold glow border on slot `[0,0]`.
  - Runs 5-second 3D Y-axis card flip rotation for historical posts (>12 photos).
  - Handles photo clicks opening centered `layoutPhotoViewerModal` with scrollable dedication card.

### UI Adapters & Utilities
* **[CollageAdapter.java](file:///C:/Users/Dylan/AndroidStudioProjects/AppCollageFotosSNT/app/src/main/java/com/example/appcollagefotos/ui/adapter/CollageAdapter.java)**
  - `ListAdapter` for RecyclerView binding 16:9 widescreen post cards.
  - Manages `setHighlightedPostId()` for gold border glow.
  - Implements `flipCardAtPosition()` for 3D Y-axis rotation animation.
  - Binds `ivMessageIndicator` badge (`💬`) when dedication message is attached.
* **[ImageProcessor.java](file:///C:/Users/Dylan/AndroidStudioProjects/AppCollageFotosSNT/app/src/main/java/com/example/appcollagefotos/util/ImageProcessor.java)**
  - Utility class for bitmap scaling (`scaleBitmap`), JPEG compression (`compressAndEncodeBase64`), and Base64 decoding (`decodeBase64ToBitmap`).

---

## 3. Firebase Realtime Database Schema

```json
{
  "events": {
    "boda_demo": {
      "id": "boda_demo",
      "name": "Boda Mateo & Lucía",
      "eventType": "boda",
      "eventCode": "BODA123",
      "active": true,
      "logoBase64": "",
      "createdAt": 1728240000000
    },
    "graduacion_2026": {
      "id": "graduacion_2026",
      "name": "Graduación Medicina 2026",
      "eventType": "graduacion",
      "eventCode": "GRAD2026",
      "active": true,
      "logoBase64": "",
      "createdAt": 1728240000000
    }
  },
  "users": {
    "UID_ABC123": {
      "id": "UID_ABC123",
      "phoneNumber": "+51987654321",
      "email": "usuario@ejemplo.com",
      "createdAt": 1728240000000
    }
  },
  "posts": {
    "-O123456789ABC": {
      "id": "-O123456789ABC",
      "eventId": "boda_demo",
      "userId": "UID_ABC123",
      "imageBase64": "/9j/4AAQSkZJRg...",
      "message": "¡Muchas felicidades a los novios!",
      "createdAt": 1728240000000,
      "deviceInfo": null
    }
  }
}
```
