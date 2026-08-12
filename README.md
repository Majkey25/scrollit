# ScrollIt (Internal Samsung Auto-Scroll Tool)

[![Repository](https://img.shields.io/badge/GitHub-Majkey25%2Fscrollit-181717?logo=github)](https://github.com/Majkey25/scrollit)
[![Release](https://img.shields.io/github/v/release/Majkey25/scrollit?display_name=tag)](https://github.com/Majkey25/scrollit/releases/tag/v1.0.0)
[![Download APK](https://img.shields.io/badge/Download-APK-111111?logo=android&logoColor=white)](https://github.com/Majkey25/scrollit/releases/download/v1.0.0/scrollit-v1.0.0-debug.apk)
[![License: MIT](https://img.shields.io/badge/License-MIT-111111.svg)](./LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%2014%2B-111111?logo=android&logoColor=white)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-111111?logo=kotlin&logoColor=white)](https://kotlinlang.org)

ScrollIt is a Kotlin Android app for internal use on Samsung phones. It provides:

1. **Main app screen** for permission setup and advanced tuning.
2. **Floating overlay** for runtime control of downward auto-scroll in other apps.

## Download APK

ScrollIt can now be downloaded directly from GitHub Releases.

- Current stable release: [ScrollIt v1.0.0](https://github.com/Majkey25/scrollit/releases/tag/v1.0.0)
- Direct APK download: [scrollit-v1.0.0-debug.apk](https://github.com/Majkey25/scrollit/releases/download/v1.0.0/scrollit-v1.0.0-debug.apk)
- Published: August 12, 2026
- Package: debug-signed APK for direct testing outside Google Play

## What the app does

- Offers two separate scroll modes:
  - **Touch** is the default. It uses Android Accessibility and works without Shizuku.
  - **AutoScroll** uses Shizuku to hold one system-level touch and move it every 16 ms. This gives Android apps a continuous scroll stream similar to Windows middle-click scrolling.
- Shows only the permissions required by the selected mode.
- Opens the exact Android settings screens needed to enable permissions.
- Starts a floating, draggable overlay above other apps.
- Keeps the existing Accessibility gesture engine as the simple fallback.
- Uses **30 persistent speed levels**, calibrated for slow continuous scrolling.
- Collapses into a small edge bubble and expands back on tap.

## Required permissions

1. **Draw over other apps** (`SYSTEM_ALERT_WINDOW`)
2. One scroll backend:
   - **Touch mode:** Accessibility service (ScrollIt Accessibility)
   - **AutoScroll mode:** [Shizuku](https://shizuku.rikka.app/) running and ScrollIt authorized
3. **Foreground service** (overlay runs as foreground service)

## Shizuku AutoScroll setup

Touch mode needs no Shizuku. Use the steps below only for AutoScroll. AutoScroll uses a continuous touch stream because many Android apps ignore mouse-wheel events, including Ultimate Guitar Tabs.

1. Install Shizuku from the [official download page](https://shizuku.rikka.app/download/).
2. On Android 11 or newer, enable **Developer options** and **Wireless debugging**.
3. Open Shizuku. Under **Start via Wireless debugging**, tap **Pairing** and follow its pairing notification.
4. Return to Shizuku and tap **Start**. The top status must say that Shizuku is running.
5. Open ScrollIt and select **AutoScroll**.
6. Under **Setup**, tap **Set up Shizuku** and allow ScrollIt in the Shizuku permission dialog.
7. Allow **Floating controls**, open the controls, and press **Start**.

Non-root Shizuku must be started again after every phone restart. If Shizuku is stopped or access is denied, ScrollIt shows a red message and does not silently switch modes. The official illustrated instructions are in the [Shizuku user manual](https://shizuku.rikka.app/guide/setup/).

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

1. GitHub release page: [v1.0.0](https://github.com/Majkey25/scrollit/releases/tag/v1.0.0)
2. Downloaded asset name: `scrollit-v1.0.0-debug.apk`
3. Release channel: stable

### How new releases are published

1. Push the commit to `main`
2. Create a version tag such as `v1.0.1`
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

1. Open ScrollIt and select **Touch** or **AutoScroll**.
2. Check that **Floating controls** and the selected backend are **On**.
3. Choose a speed from `1..30`; optionally expand **Motion details**.
4. Tap **Open controls**.
5. Drag overlay to desired place.
6. Tap **Start** on a long page.
7. Verify the page scrolls downward continuously.
8. Tap **+** and **-**:
   - speed number updates `1..30`
   - change applies while scrolling is already running
9. Tap **Stop** and verify scrolling stops immediately.
10. Tap **Hide** and verify the bubble appears on the screen edge.
11. Tap the bubble and verify the overlay expands back.
12. Tap **Exit** and verify the overlay and foreground notification close.
13. Disable the selected backend and tap **Start** again. Verify a red error message appears and no fallback starts.

## Known limitations

- Some apps/screens ignore injected accessibility gestures.
- AutoScroll uses touch injection because many Android apps ignore mouse-wheel events.
- AutoScroll requires Shizuku because ordinary Android apps cannot inject system input events.
- Gesture behavior differs slightly across One UI versions.
- Exact smoothness depends on app rendering and refresh timing.
- `connectedDebugAndroidTest` needs emulator or physical device.

## Project structure (key files)

```text
app/src/main/java/cz/teply/scrollit/
  MainActivity.kt
  OverlayService.kt
  ShizukuAutoScrollEngine.kt
  ShizukuInputUserService.kt
  TouchScrollProfileFactory.kt
  ScrollAccessibilityService.kt
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

