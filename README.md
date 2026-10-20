# Nivora Launcher

Minimal Android home launcher built with Kotlin and Jetpack Compose, aiming for the calm clarity of Niagara and Olauncher with more built-in personalization.

## Build status

GitHub Actions builds a debug APK until persistent release signing is configured. Debug APKs may not be installable as updates over APKs signed with a different debug key.

## Configure persistent release signing

Create a dedicated release keystore locally and keep a secure backup. Never commit the keystore or its passwords to this repository.

Add these four **Actions secrets** in the repository settings (**Settings → Secrets and variables → Actions**):

- `NIVORA_KEYSTORE_BASE64`: the keystore file encoded as a single-line Base64 string.
- `NIVORA_STORE_PASSWORD`: the keystore password.
- `NIVORA_KEY_ALIAS`: the key alias.
- `NIVORA_KEY_PASSWORD`: the key password.

On macOS or Linux, encode the keystore with:

```sh
base64 < nivora-release.jks | tr -d '\n'
```

Copy the result into the `NIVORA_KEYSTORE_BASE64` secret. Keep the original keystore and passwords safely backed up: future APK updates must use the same signing key. Do not paste secrets into issues, chat, commits, or logs.

When all four secrets are present, GitHub Actions builds and uploads `nivora-release-apk`. If they are missing or incomplete, it builds `nivora-debug-apk` instead.

## Current features

- Minimal home screen with a large live clock, date, and optional Nivora wordmark
- A quieter, compact Home search field that expands into the app drawer when typing
- Eight one-tap visual presets: Glass Mono, Obsidian, Arctic, Graphite, Sand, Sage, Plum, and Blue Pulse
- Optional translucent glass panels and subtle reflective borders
- Optional grayscale app icons for a cohesive monochrome look
- App search by label or package name
- App drawer with grid/list views and favorites-only filter
- Long-press an app to add or remove it from favorites
- Sort the app drawer by name, favorites, or the current fallback ordering
- Adjustable grid density, icon size, labels, typeface, glass effect, monochrome icons, and preset themes
- Preferences saved locally on the device

## Development roadmap

- [ ] Native Android home-screen widgets, including host lifecycle and widget configuration
- [ ] Choose a wallpaper from the device gallery
- [ ] Third-party icon pack integration
- [ ] Gestures and richer app shortcuts
- [ ] Optional AI-generated themes and wallpapers, with clear disclosure of any external service

## Design references

Nivora takes inspiration from broad interaction patterns rather than copying another app's visuals: fast one-handed access associated with Niagara, a restrained home screen like Olauncher, and opt-in personalization similar to Lawnchair. These projects are references for usability and direction, not dependencies.
