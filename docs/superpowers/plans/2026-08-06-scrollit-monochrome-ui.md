# ScrollIt Monochrome UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the card-heavy teal UI with a compact monochrome interface, publish `v1.0.0-beta.2`, and install the published APK safely.

**Architecture:** Keep the existing Activity, Service, IDs, and control flow. Limit behavioral code to native entrance animation; implement the visual system through Android resources and XML. Keep release creation in the existing tag workflow.

**Tech Stack:** Kotlin 1.9.24, Android XML views, Material Components 1.12.0, Gradle 8.7, GitHub Actions, ADB.

---

## File map

- Create `app/src/main/res/values-night/colors.xml`: dark semantic palette.
- Create `app/src/main/res/values/styles.xml`: filled and outlined Material button styles.
- Modify `colors.xml`, both `themes.xml` files, layouts, drawables, strings, and app icon: monochrome UI.
- Modify `ScrollConfig.kt` and `OverlayService.kt`: compact dimensions and native entrance motion.
- Modify `app/build.gradle.kts`, `README.md`, and `release-apk.yml`: beta.2 metadata and release notes.
- Delete `bg_drag_handle.xml`: no longer used after removing the banner.

### Task 1: Semantic monochrome theme

**Files:**
- Create: `app/src/main/res/values-night/colors.xml`
- Create: `app/src/main/res/values/styles.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/res/values-night/themes.xml`
- Modify: `app/src/main/res/drawable/bg_overlay_panel.xml`
- Modify: `app/src/main/res/drawable/bg_overlay_bubble.xml`
- Modify: `app/src/main/res/drawable/ic_scrollit_app.xml`

- [ ] **Step 1: Define matching semantic palettes**

Use these resource names in both day and night folders:

```xml
<color name="window_background">#F7F7F5</color>
<color name="surface">#FFFFFF</color>
<color name="border">#D8D8D2</color>
<color name="text_primary">#111111</color>
<color name="text_secondary">#62625D</color>
<color name="control_primary">#111111</color>
<color name="control_on_primary">#FFFFFF</color>
<color name="control_secondary">#EEEDEA</color>
<color name="status_ok">@color/text_primary</color>
<color name="status_error">@color/text_secondary</color>
```

Night resources use `#0A0A0A`, `#141414`, `#383834`, `#F7F7F5`, `#AAA9A3`, `#F7F7F5`, `#0A0A0A`, and `#252523` respectively.

- [ ] **Step 2: Define the two reusable Material button styles**

```xml
<style name="Widget.ScrollIt.Button.Primary" parent="Widget.Material3.Button">
    <item name="android:minHeight">48dp</item>
    <item name="android:textAllCaps">false</item>
    <item name="backgroundTint">@color/control_primary</item>
    <item name="android:textColor">@color/control_on_primary</item>
    <item name="cornerRadius">10dp</item>
    <item name="strokeColor">@color/control_primary</item>
    <item name="strokeWidth">1dp</item>
</style>
<style name="Widget.ScrollIt.Button.Secondary" parent="Widget.Material3.Button.OutlinedButton">
    <item name="android:minHeight">48dp</item>
    <item name="android:textAllCaps">false</item>
    <item name="backgroundTint">@android:color/transparent</item>
    <item name="android:textColor">@color/text_primary</item>
    <item name="cornerRadius">10dp</item>
    <item name="strokeColor">@color/border</item>
    <item name="strokeWidth">1dp</item>
</style>
```

- [ ] **Step 3: Apply system-bar, surface, icon, panel, and bubble colors**

Keep `Theme.Material3.*.NoActionBar`; map bars/background and Material primary/on-primary values to the semantic colors. Set light status/navigation icon flags only in the light theme. Use a 14dp panel radius and 24dp bubble radius.

- [ ] **Step 4: Check resources compile**

Run: `.\gradlew.bat :app:processDebugResources`

Expected: `BUILD SUCCESSFUL`.

### Task 2: Flatten the main screen and compact the overlay

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/layout/overlay_controls.xml`
- Modify: `app/src/main/res/layout/overlay_bubble.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Delete: `app/src/main/res/drawable/bg_drag_handle.xml`

- [ ] **Step 1: Replace main cards with flat sections**

Preserve these IDs exactly:

```text
overlayStatusValue accessibilityStatusValue
openOverlaySettingsButton openAccessibilitySettingsButton launchOverlayButton
distanceSeekBar intervalSeekBar durationSeekBar
distanceValueText intervalValueText durationValueText permissionHelpText
```

Place Launch first as the only `Widget.ScrollIt.Button.Primary`. Use `Widget.ScrollIt.Button.Secondary` for both permission actions. Separate Permission, Tuning, and Setup with 1dp `@color/border` views.

- [ ] **Step 2: Rebuild the overlay at accessible compact sizes**

Use a 48dp drag row, a 48dp Start button, 48×48dp speed buttons, and 48dp Hide/Exit buttons. Preserve every existing overlay ID and use the two shared styles.

- [ ] **Step 3: Replace the bubble label safely**

```xml
<TextView
    android:id="@+id/bubbleLabel"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/bg_overlay_bubble"
    android:contentDescription="@string/open_overlay_controls"
    android:gravity="center"
    android:text="@string/overlay_bubble_label"
    android:textColor="@color/control_on_primary"
    android:textSize="18sp"
    android:textStyle="bold" />
```

Set `overlay_bubble_label` to `S`; add descriptive speed decrease/increase strings and apply them as content descriptions.

- [ ] **Step 4: Check obsolete resources and IDs**

Run: `rg -n "bg_drag_handle|@\+id/(overlayStatusValue|accessibilityStatusValue|launchOverlayButton|overlayDragHandle|startStopButton|speedMinusButton|speedPlusButton|hideButton|exitButton)" app/src/main`

Expected: no `bg_drag_handle`; every required ID remains.

### Task 3: Add restrained native motion

**Files:**
- Modify: `app/src/main/java/cz/teply/scrollit/ScrollConfig.kt`
- Modify: `app/src/main/java/cz/teply/scrollit/OverlayService.kt`

- [ ] **Step 1: Update only UI constants**

```kotlin
const val bubbleHeightDp = 48
const val bubbleWidthDp = 48
const val expandedEstimatedHeightDp = 224
const val expandedWidthDp = 248
const val controlAnimationDurationMs = 120L
const val overlayAnimationDurationMs = 160L
```

- [ ] **Step 2: Add one native entrance helper**

```kotlin
private fun animateOverlayIn(view: View) {
    view.alpha = 0f
    view.scaleX = 0.94f
    view.scaleY = 0.94f
    view.animate()
        .alpha(1f)
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(ScrollConfig.overlayAnimationDurationMs)
        .start()
}
```

Call it immediately after each successful `windowManager.addView(...)`. Do not delay removal, scrolling, or button actions.

- [ ] **Step 3: Crossfade the Start/Stop inversion**

Change `renderRunningState` to accept `animate: Boolean = false`. The Start/Stop click passes `true`; other callers keep the immediate default. Fade the button to `0.55f` for half of `controlAnimationDurationMs`, apply the opposite `control_primary` / `control_on_primary` background and text colors, then fade back to `1f` for the remaining half. Cancel the button's existing animator before starting so quick taps cannot queue stale transitions.

```kotlin
private fun renderRunningState(animate: Boolean = false) {
    val view = expandedView ?: return
    val running = ScrollAccessibilityService.instance?.isAutoScrollRunning() == true
    val button = view.findViewById<Button>(R.id.startStopButton)
    val updateButton = {
        button.text = getString(if (running) R.string.stop_button else R.string.start_button)
        button.backgroundTintList = ColorStateList.valueOf(
            getColor(if (running) R.color.control_on_primary else R.color.control_primary),
        )
        button.setTextColor(
            getColor(if (running) R.color.control_primary else R.color.control_on_primary),
        )
    }
    button.animate().cancel()
    if (!animate) {
        updateButton()
    } else {
        val halfDuration = ScrollConfig.controlAnimationDurationMs / 2
        button.animate().alpha(0.55f).setDuration(halfDuration).withEndAction {
            updateButton()
            button.animate().alpha(1f).setDuration(halfDuration).start()
        }.start()
    }
    if (running) {
        updateActionStatus(getString(R.string.overlay_running, selectedSpeedLevel), isError = false)
    }
}
```

- [ ] **Step 4: Run focused unit tests**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: all existing JVM tests pass.

### Task 4: Prepare and verify beta.2

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `README.md`
- Modify: `.github/workflows/release-apk.yml`

- [ ] **Step 1: Bump Android version**

```kotlin
versionCode = 2
versionName = "1.0.0-beta.2"
```

- [ ] **Step 2: Update public release metadata**

Replace beta.1 links/names with beta.2 and use `August 6, 2026`. Release notes must list the monochrome UI, compact overlay/bubble, and restrained transitions; remove “First public GitHub APK build”.

- [ ] **Step 3: Run the complete repository gate**

Run: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug`

Expected: `BUILD SUCCESSFUL`; APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 4: Review exact diff**

Run: `git diff --check`, `git diff --stat`, and `git status --short`.

Expected: only planned source/resource/docs/release files plus the already committed design/plan docs.

- [ ] **Step 5: Commit and push**

```text
git add <planned files>
git commit -m "feat: redesign ScrollIt monochrome UI"
git push origin main
```

- [ ] **Step 6: Publish and verify release**

Create annotated tag `v1.0.0-beta.2`, push only that tag, wait for `release-apk.yml`, then verify release tag, prerelease state, asset name, digest, and workflow conclusion using `gh`.

### Task 5: Replace the phone installation

**Files:**
- Local temporary backups only: `.reference/tmp/device-backup/`

- [ ] **Step 1: Resolve the exact online device and package**

Run `adb devices -l`, then query `ro.product.model` and `dumpsys package cz.teply.scrollit`. Stop if more than one online device or the package identity differs.

- [ ] **Step 2: Download and verify the published asset**

Download only `scrollit-v1.0.0-beta.2-debug.apk`; compare local SHA-256 with the GitHub asset digest.

- [ ] **Step 3: Back up before uninstall**

Pull the installed base APK and copy accessible `shared_prefs` data with `run-as`. Record the installed version and package path.

- [ ] **Step 4: Replace only ScrollIt**

Run `adb uninstall cz.teply.scrollit`, require `Success`, then install the verified beta.2 APK and require `Success`. Do not touch another package.

- [ ] **Step 5: Verify final state**

Confirm package `cz.teply.scrollit`, `versionCode=2`, `versionName=1.0.0-beta.2`, launch activity resolution, running process after launch, and no duplicate ScrollIt package.
