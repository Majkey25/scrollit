# ScrollIt Auto Scroll

ScrollIt is a simple internal-use Android app written in Kotlin for Samsung phones. It shows a floating overlay above other apps and uses an `AccessibilityService` with `dispatchGesture()` to send repeated upward swipe gestures so long pages move downward in a practical reading flow.

## What the app includes

- `MainActivity` with clear permission status and quick links to settings
- `OverlayService` that shows a draggable floating controller and keeps itself alive with a foreground notification
- `ScrollAccessibilityService` that performs repeated scroll gestures
- compact floating overlay with `Start`, `Stop`, speed control, `Hide`, expand, and `Exit`
- bubble mode that snaps to the left or right edge of the screen
- buildable Gradle Android project with tests

## Required permissions

- `Draw over other apps`: required so the floating controller can appear above the browser or other apps
- `Accessibility service`: required so the app can dispatch repeated scroll gestures
- `Foreground service`: required so the overlay service can stay alive while the overlay is visible

## Open the project

1. Install Android Studio with Android SDK Platform 34 and Build-Tools 34.x.
2. Open Android Studio.
3. Choose **Open** and select `c:\Users\teply\Documents\scrollit`.
4. Let Gradle sync finish.

## Build the APK

### Android Studio

1. Open the **Build** menu.
2. Run **Build Bundle(s) / APK(s) > Build APK(s)**.

### Command line on Windows

1. Make sure JDK 17 and Android SDK are installed.
2. Run:

```powershell
cd c:\Users\teply\Documents\scrollit
.\gradlew.bat assembleDebug
```

Expected APK path after a successful debug build:

```text
c:\Users\teply\Documents\scrollit\app\build\outputs\apk\debug\app-debug.apk
```

## Install the APK over USB with ADB

```powershell
adb devices
adb install -r c:\Users\teply\Documents\scrollit\app\build\outputs\apk\debug\app-debug.apk
```

## Enable Developer options and USB debugging on Samsung phones

1. Open **Settings > About phone > Software information**.
2. Tap **Build number** seven times.
3. Go back to **Settings** and open **Developer options**.
4. Enable **Developer options**.
5. Enable **USB debugging**.
6. Connect the phone by USB and accept the RSA prompt on the phone.

## Enable draw over other apps

1. Open ScrollIt.
2. Tap **Open overlay permission settings**.
3. Find **ScrollIt**.
4. Allow **Appear on top** or **Draw over other apps**.

## Enable the accessibility service

1. Open ScrollIt.
2. Tap **Open accessibility settings**.
3. Open **Installed apps** or the Samsung accessibility service list.
4. Select **ScrollIt Accessibility**.
5. Turn it on and confirm the warning dialog.

## How to test the app

1. Open ScrollIt.
2. Confirm both status rows change to **Enabled** after permissions are granted.
3. Tap **Launch floating overlay**.
4. Verify the compact floating panel appears above the current app.
5. Drag the panel by the top handle.
6. Change the speed slider and confirm the label changes between the four speed levels.
7. Tap **Start** on a browser page with long content.
8. Confirm the page scrolls downward repeatedly.
9. Tap **Stop** and confirm scrolling stops immediately.
10. Tap **Hide** and confirm the overlay collapses into a small edge bubble.
11. Tap the bubble and confirm the full panel expands again.
12. Tap **Exit** and confirm the overlay disappears and the foreground service notification is removed.

## Known Android limitations

- Accessibility gesture scrolling depends on the current app accepting swipe gestures. Some screens will ignore gestures.
- Long gesture loops can feel slightly different across Samsung One UI versions and browser apps.
- Overlay placement can shift slightly on devices with display cutouts, gesture navigation, or unusual screen scaling.
- The emulator can confirm app structure and wiring, but real device behavior for overlay and accessibility permissions is still the most important validation.

## Expected behavior on real Samsung devices

- Samsung Internet and Chrome long pages should respond to the repeated upward swipe gestures on most normal reading pages.
- If Android kills the overlay service, reopening the app and launching the overlay again restores it.
- If the accessibility service is toggled off while the overlay is open, `Start` shows a clear error state instead of silently failing.
