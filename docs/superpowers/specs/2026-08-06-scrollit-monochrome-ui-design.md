# ScrollIt monochrome UI design

## Direction

Visual thesis: a compact utility surface using near-black, near-white, thin borders, and restrained motion.

Content plan: app identity, permission readiness, one launch action, tuning controls, short setup help.

Interaction thesis: 140–180 ms fade/scale entrances, a short Start/Stop color inversion, and a compact bubble-to-panel transition. No looping animation.

## Main screen

- Preserve the existing permission, settings, and launch behavior.
- Replace stacked cards with flat sections separated by spacing and thin dividers.
- Shorten introductory and help copy.
- Use one filled launch button. Permission actions remain outlined secondary buttons.
- Provide matching day/night semantic palettes with exact light/dark inversion and readable system bars.

## Floating controls

- Reduce the expanded width from 272dp to 248dp.
- Replace the teal drag banner with a quiet grip and compact label.
- Keep status, Start/Stop, speed, Hide, and Exit controls without changing their IDs or service flow.
- Keep every interactive target at least 48dp.
- Replace the 56×92dp text pill with a 48×48dp circular edge bubble.
- Keep drag clamping, edge snapping, collapse/expand, and accessibility error handling intact.

## Motion

- Animate newly attached panel and bubble views with native alpha/scale only.
- Use 160 ms duration and standard view interpolation.
- Animate Start/Stop foreground/background inversion without delaying the action.
- Respect Android animator-duration settings; native view animation already becomes immediate when animations are disabled.

## Release

- Bump to `versionCode = 2` and `versionName = "1.0.0-beta.2"`.
- Update README release links and workflow release notes for the monochrome redesign.
- Run the repository Gradle test, lint, and APK build tasks.
- Publish tag `v1.0.0-beta.2` after the verified commit reaches `main`.
- Install only the published APK on package `cz.teply.scrollit`, backing up the old APK/settings before uninstalling it.

## Acceptance

- Day and night themes are deliberate black/white inverses.
- Main screen has one obvious primary action and no card mosaic.
- Floating panel is smaller without losing any control.
- Bubble is 48×48dp and remains draggable/clickable at either edge.
- Existing unit tests, Android lint, and debug APK build pass.
- Published release metadata, asset digest, installed version, launch, and package identity are verified.
