# My CV - Android

Android app for my CV: **Jetpack Compose + Material 3** UI on top of a shared **Kotlin Multiplatform** data layer.

The data layer and CV content (`cv.json`) live in [8kt8/CvApi](https://github.com/8kt8/CvApi), included here as a git submodule. The iOS app ([8kt8/MyCV-iOS](https://github.com/8kt8/MyCV-iOS)) uses the same data layer with a SwiftUI UI.

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
| `androidApp/` | Android app. UI in `ui/` (screen, components, theme with spacing tokens, icons); `CvViewModel` wraps the shared `CvService`. |
| `CvApi/` (submodule) | Shared KMP data module (`:shared`), version catalog and `cv.json`. |

Update the shared code with:

```bash
git submodule update --remote CvApi
```

## Features

- Profile header with GitHub, LinkedIn and email actions
- Experience timeline with company logos, expandable details and Google Play / App Store links
- Offline-first: CV bundled at build time, refreshed from `cv.json` on GitHub (pull to refresh)
- Material 3, light and dark theme, edge-to-edge, screen reader labels and headings

Icons: [Simple Icons](https://simpleicons.org) (CC0) and [Material Symbols](https://fonts.google.com/icons) (Apache-2.0). Brand icons are trademarks of their owners.
