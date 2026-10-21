# Nivora Launcher

Nivora is a minimalist Android launcher built with Kotlin and Jetpack Compose. Its current direction combines restrained monochrome design, **Prism Flow** animated glass surfaces, coordinated themes, and an offline mini-arcade.

## Product rule: launcher first

Nivora is designed to be the device's default Android Home app. Every feature must protect the core launcher loop: show a dependable Home screen, discover and open installed launchable apps quickly, keep favorites and recents current, and make Back/Home behavior predictable. Themes, visual effects, wallpaper concepts, and games are secondary and must never block app access.

## Core launcher acceptance checklist

- Android exposes Nivora as a Home app through the `MAIN`, `HOME`, and `DEFAULT` intent categories.
- A first-time user can open the full app drawer immediately, even before saving favorites.
- Search covers the whole launcher catalog by app label or package name, independently of the favorites filter.
- The app catalog refreshes when Nivora resumes so installed or removed apps do not remain stale.
- Back dismisses the topmost dialog first, then collapses search or the app drawer; when no overlay is open, Android can leave Home as usual.
- Tapping an app opens it; long-press toggles favorites; launched apps update the recent-app order.
- User preferences persist across restarts, and inaccessible wallpaper references fall back without a blank Home screen.
- Prompt-based art is described honestly as local procedural artwork unless a real image-generation model is integrated and tested.
- Before release, test Home selection, gesture navigation, recent/favorite updates, package visibility, app install/removal, screen sizes, and accessibility on physical devices.


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
- AndroidX Activity Compose: 1.13.0
- AndroidX Lifecycle Runtime: 2.10.0
- AndroidX Graphics Path: 1.1.0
- JDK: 17
- `minSdk = 26`, `compileSdk = 36`, `targetSdk = 36`
- App version: 0.4.0 (versionCode 4)

Run the JVM game-logic tests and debug build:

```sh
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

GitHub Actions runs JVM game-logic tests, Android lint, and—after build completion—checks APK ZIP alignment and native ELF LOAD alignment for 16 KB page-size devices. It builds a debug APK when release signing is not configured. To produce a **signed Play Store Android App Bundle (.aab)**, configure these repository Actions secrets: `NIVORA_KEYSTORE_BASE64`, `NIVORA_STORE_PASSWORD`, `NIVORA_KEY_ALIAS`, and `NIVORA_KEY_PASSWORD`. Keep the release keystore and passwords private and backed up: all future updates must use the same signing identity.

## Google Play release checklist

The code and debug build are not, by themselves, a Play Store release. Before submitting:

- Configure a stable signing key and confirm the signed AAB workflow artifact.
- Test installation, launcher selection, icon tinting, wallpaper URI persistence, light/dark themes, and both games on physical devices and supported Android versions, including Android 16. CI checks packaging/ELF alignment, but an actual 16 KB emulator/device smoke test is still required.
- Prepare Play Console listing material: store listing icon (512×512), feature graphic, phone screenshots, descriptions, category/contact details, and a public privacy-policy URL.
- Complete Play Console App content, Data safety, and content-rating declarations accurately.
- Review accessibility, edge cases, lifecycle/background behavior and performance on a range of screen sizes.
- Do not advertise real AI-generated images until an actual on-device model or disclosed image-generation service has been integrated and tested.

Privacy policy source: [PRIVACY_POLICY.md](PRIVACY_POLICY.md). Public branch URL for review: https://github.com/Sonostevecazzo/NivoraLaucher/blob/feat/glass-monochrome-themes/PRIVACY_POLICY.md. Store listing draft: [store/PLAY_STORE_LISTING_IT.md](store/PLAY_STORE_LISTING_IT.md). CI renders the icon and feature graphic from [store/assets](store/assets); real device screenshots and a developer contact email still need to be supplied before submission. See [docs/RELEASE_SIGNING.md](docs/RELEASE_SIGNING.md) for secure signing setup.

## Still not implemented

- Native Android home-screen widgets and widget lifecycle/configuration.
- Direct integration of a generative AI image model.
- Third-party icon pack support.
- Physical-device usability/accessibility test pass and store listing assets.
