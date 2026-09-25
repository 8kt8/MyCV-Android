# My CV - Android

Android app for my CV, built with **Kotlin Multiplatform** and **Compose Multiplatform**.

The UI, data layer and CV content live in the shared module in [8kt8/CvApi](https://github.com/8kt8/CvApi), included here as a git submodule. The iOS app is [8kt8/MyCV-iOS](https://github.com/8kt8/MyCV-iOS).

## Getting started

```bash
git clone --recurse-submodules git@github.com:8kt8/MyCV-Android.git
cd MyCV-Android
./gradlew :androidApp:installDebug
```

Already cloned without submodules? Run `git submodule update --init`.

Requirements: JDK 17+, Android SDK platform 37.

## Structure

| Path | What |
| --- | --- |
| `androidApp/` | Android application: `MainActivity` hosting the shared `App()` composable. |
| `CvApi/` (submodule) | Shared KMP module (`:shared`), version catalog and `cv.json`. |

Update the shared code with:

```bash
git submodule update --remote CvApi
```

## Features

- Profile header with GitHub, LinkedIn and email actions
- Experience timeline with company logos, expandable details and Google Play / App Store links
- Offline-first: CV bundled at build time, refreshed from `cv.json` on GitHub (pull to refresh)
- Material 3, light and dark theme, edge-to-edge, screen reader labels and headings
