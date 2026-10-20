# Nivora Launcher

Nivora is a minimalist Android launcher built with Kotlin and Jetpack Compose. Its current direction combines restrained monochrome design, **Prism Flow** animated glass surfaces, coordinated themes, and an offline mini-arcade.

## Current features

- Minimal home screen with live clock/date, favorites-first behavior, compact app search, and optional Nivora wordmark.
- App drawer with grid/list layouts, app label/package search, favorites, and sort by name/favorites/recent launches.
- Theme presets: Mono Glass, Frost, Ember, Moss, Violet, and Creato.
- Prism Flow: a slow moving diagonal highlight and fine edge reflection on translucent surfaces.
- Monochrome app-icon tinting, customizable font presets, icon size, grid density, and labels.
- System photo picker for a custom wallpaper. Nivora samples the image locally to derive an accent color and chooses a matching dark/light presentation and font style.
- Prism Atelier: a prompt-seeded abstract wallpaper concept generated locally, plus a coordinated accent, font preset and monochrome icon mode. This is deterministic local artwork—not a connected generative AI model.
- Prism Arcade with offline Snake and Blockfall games. The game boards, pieces, controls and highlights derive their palette from the active Nivora theme.
- Preferences, favorite app list, and recently launched app history are stored in app-local preferences.
- No analytics, ads, account registration, or app-controlled network requests in the current build.

## Build and test

- Android Gradle Plugin: 8.10.1
- Gradle: 8.11.1
- Kotlin: 2.1.10
- JDK: 17
- `minSdk = 26`, `compileSdk = 36`, `targetSdk = 36`
- App version: 0.4.0 (versionCode 4)

Run the JVM game-logic tests and debug build:

```sh
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

GitHub Actions runs unit tests and builds a debug APK. To produce a **signed Play Store Android App Bundle (.aab)**, configure these repository Actions secrets: `NIVORA_KEYSTORE_BASE64`, `NIVORA_STORE_PASSWORD`, `NIVORA_KEY_ALIAS`, and `NIVORA_KEY_PASSWORD`. Keep the release keystore and passwords private and backed up: all future updates must use the same signing identity.

## Google Play release checklist

The code and debug build are not, by themselves, a Play Store release. Before submitting:

- Configure a stable signing key and confirm the signed AAB workflow artifact.
- Test installation, launcher selection, icon tinting, wallpaper URI persistence, light/dark themes, and both games on physical devices and supported Android versions, including Android 16.
- Prepare Play Console listing material: store listing icon (512×512), feature graphic, phone screenshots, descriptions, category/contact details, and a public privacy-policy URL.
- Complete Play Console App content, Data safety, and content-rating declarations accurately.
- Review accessibility, edge cases, lifecycle/background behavior and performance on a range of screen sizes.
- Do not advertise real AI-generated images until an actual on-device model or disclosed image-generation service has been integrated and tested.

Privacy policy source: [PRIVACY_POLICY.md](PRIVACY_POLICY.md). Once this branch is merged, the policy can be linked from the Play listing using the repository's public URL.

## Still not implemented

- Native Android home-screen widgets and widget lifecycle/configuration.
- Direct integration of a generative AI image model.
- Third-party icon pack support.
- Physical-device usability/accessibility test pass and store listing assets.
