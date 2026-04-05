# ScrollIt (Internal Samsung Auto-Scroll Tool)

[![Repository](https://img.shields.io/badge/GitHub-Majkey25%2Fscrollit-181717?logo=github)](https://github.com/Majkey25/scrollit)
[![Release](https://img.shields.io/github/v/release/Majkey25/scrollit?include_prereleases&display_name=tag)](https://github.com/Majkey25/scrollit/releases/tag/v1.0.0-beta.1)
[![Download APK](https://img.shields.io/badge/Download-APK-3DDC84?logo=android&logoColor=white)](https://github.com/Majkey25/scrollit/releases/download/v1.0.0-beta.1/scrollit-v1.0.0-beta.1-debug.apk)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](./LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%2014%2B-3DDC84?logo=android)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin)](https://kotlinlang.org)

ScrollIt is a Kotlin Android app for internal use on Samsung phones. It provides:

1. **Main app screen** for permission setup and advanced tuning.
2. **Floating overlay** for runtime control of downward auto-scroll in other apps.
<p align="center">
  <img src="docs/me.gif" alt="ScrollIt demo" width="340" />
</p>

## Download APK

ScrollIt can now be downloaded directly from GitHub Releases.

- Current GitHub prerelease: [ScrollIt v1.0.0-beta.1](https://github.com/Majkey25/scrollit/releases/tag/v1.0.0-beta.1)
- Direct APK download: [scrollit-v1.0.0-beta.1-debug.apk](https://github.com/Majkey25/scrollit/releases/download/v1.0.0-beta.1/scrollit-v1.0.0-beta.1-debug.apk)
- Published: April 5, 2026
- Package: debug-signed APK for direct testing outside Google Play


## What the app does

- Shows permission status (overlay + accessibility).
- Opens the exact Android settings screens needed to enable permissions.
- Starts a floating, draggable overlay above other apps.
- Performs repeated downward-page scrolling using `AccessibilityService` + `dispatchGesture()`.
- Supports two behavior modes from the main app:
  - **Stable** (more conservative and reliable)
  - **Smooth** (smaller/faster gesture cycle)
- Uses **10 speed levels** in overlay, controlled only by **+ / -** buttons.
- Collapses into a small edge bubble and expands back on tap.
<p align="center">
  <img src="https://github.com/user-attachments/assets/6686afe8-4153-4688-9d69-9027ce5f0b47" alt="Overlay controls" width="340" />
</p>

## Required permissions

1. **Draw over other apps** (`SYSTEM_ALERT_WINDOW`)
2. **Accessibility service** (ScrollIt Accessibility)
3. **Foreground service** (overlay runs as foreground service)

## Build APK

> Note: this repository uses text-only launcher scripts (`gradlew`, `gradlew.bat`) that call a local Gradle installation.

### Windows (recommended)

```powershell
cd C:\Users\teply\Documents\scrollit
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

### Linux/macOS

```bash
cd /workspace/scrollit
./gradlew testDebugUnitTest assembleDebug lintDebug
```

### Expected APK path

- Relative: `app/build/outputs/apk/debug/app-debug.apk`
- Example absolute (Linux in this repo): `/workspace/scrollit/app/build/outputs/apk/debug/app-debug.apk`
- Example absolute (Windows): `C:\Users\teply\Documents\scrollit\app\build\outputs\apk\debug\app-debug.apk`

## GitHub Releases

This repository now publishes installable APK files from GitHub tags that start with `v`.

### Current release

1. GitHub release page: [v1.0.0-beta.1](https://github.com/Majkey25/scrollit/releases/tag/v1.0.0-beta.1)
2. Downloaded asset name: `scrollit-v1.0.0-beta.1-debug.apk`
3. Release channel: prerelease

### How new releases are published

1. Push the commit to `main`
2. Create a tag such as `v1.0.0-beta.2` or `v1.0.0`
3. Push the tag to GitHub
4. GitHub Actions builds the APK and attaches it to the matching release

## USB install via ADB

```powershell
adb devices
adb install -r C:\Users\teply\Documents\scrollit\app\build\outputs\apk\debug\app-debug.apk
```

## Samsung setup

### Enable Developer options

1. `Settings` → `About phone` → `Software information`
2. Tap **Build number** 7 times

### Enable USB debugging

1. `Settings` → `Developer options`
2. Enable **USB debugging**
3. Connect USB and confirm RSA key prompt

### Enable overlay permission

1. Open ScrollIt
2. Tap **Open overlay permission settings**
3. Enable **Appear on top** for ScrollIt

### Enable accessibility service

1. Open ScrollIt
2. Tap **Open accessibility settings**
3. `Accessibility` → `Installed apps` → **ScrollIt Accessibility**
4. Enable service and confirm warning dialogs

## How to test all features

1. Open ScrollIt and check both status rows are **Enabled**.
2. In **Advanced tuning**, set:
   - mode (Stable / Smooth)
   - distance
   - interval
   - gesture duration
3. Tap **Launch floating overlay**.
4. Drag overlay to desired place.
5. Tap **Start** on long page (Samsung Internet / Chrome).
6. Verify page scrolls downward repeatedly.
7. Tap **+** and **-**:
   - speed number updates `1..10`
   - change applies while scrolling is already running
8. Tap **Stop** and verify scrolling stops immediately.
9. Tap **Hide** and verify bubble appears on screen edge.
10. Tap bubble and verify overlay expands back.
11. Tap **Exit** and verify overlay closes and foreground notification disappears.
12. Disable accessibility and tap **Start** again:
   - verify visible error message, not silent failure.

## Known limitations

- Some apps/screens ignore injected accessibility gestures.
- Gesture behavior differs slightly across One UI versions.
- Exact smoothness depends on app rendering and refresh timing.
- `connectedDebugAndroidTest` needs emulator or physical device.

## Project structure (key files)

```text
app/src/main/java/cz/teply/scrollit/
  MainActivity.kt
  OverlayService.kt
  ScrollAccessibilityService.kt
  ScrollMode.kt
  ScrollSettings.kt
  ScrollSettingsStore.kt
  ScrollSpeed.kt

app/src/main/res/layout/
  activity_main.xml
  overlay_controls.xml
  overlay_bubble.xml

app/src/main/res/xml/
  scroll_accessibility_service.xml
```

## License

MIT. See [LICENSE](./LICENSE).
<p align="center">
  <img src="https://github.com/user-attachments/assets/01bea554-5b92-43a6-8fcf-3f39531466f0" alt="Main settings screen" width="560" />
</p>

