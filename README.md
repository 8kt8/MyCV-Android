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
| `androidApp/` | Android app. UI in `ui/` (screen, components, theme with spacing tokens, icons); `CvViewModel` wraps the shared `CvService`; `AppViewModel` handles opened links; `data/` persists variants. Unit tests in `src/test` (`./gradlew :androidApp:testDebugUnitTest`). |
| `CvApi/` (submodule) | Shared KMP data module (`:shared`), version catalog and `cv.json`. |

Update the shared code with:

```bash
git submodule update --remote CvApi
```

## Opening a CV link

Per-company CV links (`https://mycv-szk.pages.dev/c/<code>`) open in the app via
[Android App Links](https://developer.android.com/training/app-links). The app shows that variant
(cached for offline use) and reopens it on later normal launches. A variant that can't be loaded shows
an error with **Try again**, never the default CV; a `/c/` link without a valid code shows
"This link is no longer active".

`MainActivity` declares an `autoVerify` intent filter for `https://mycv-szk.pages.dev/c/`. For Android
to verify it, the host must serve `https://mycv-szk.pages.dev/.well-known/assetlinks.json`
(as `application/json`, no redirects):

```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.katlewski.cv",
    "sha256_cert_fingerprints": ["AA:BB:...:FF"]
  }
}]
```

List the SHA-256 of every signing certificate that installs the app:

- Debug / local release keys: `./gradlew signingReport` (the `SHA-256` line per variant).
- Play App Signing: Play Console > your app > **App integrity > App signing**
  (the app signing key certificate; add the upload key too if you install upload-signed builds).
  Play Console also offers a ready-made `assetlinks.json` there.

Check verification on a device or emulator (Android 12+):

```bash
adb shell pm verify-app-links --re-verify com.katlewski.cv
adb shell pm get-app-links com.katlewski.cv        # mycv-szk.pages.dev should be "verified"
adb shell am start -a android.intent.action.VIEW -c android.intent.category.BROWSABLE \
  -d "https://mycv-szk.pages.dev/c/<code>"
```

Until the domain is verified (e.g. a debug build signed with a key that isn't listed), links open in
the browser; you can still test by enabling the link under the app's **Open by default** settings or with
`adb shell pm set-app-links-user-selection --user cur --package com.katlewski.cv true mycv-szk.pages.dev`.

## Features

- Profile header with GitHub, LinkedIn and email actions
- Experience timeline with company logos, expandable details and Google Play / App Store links
- Offline-first: CV bundled at build time, refreshed from the CV host (pull to refresh)
- Per-company CV variants opened from App Links, cached for offline use
- Material 3, light and dark theme, edge-to-edge, screen reader labels and headings

Icons: [Simple Icons](https://simpleicons.org) (CC0) and [Material Symbols](https://fonts.google.com/icons) (Apache-2.0). Brand icons are trademarks of their owners.
