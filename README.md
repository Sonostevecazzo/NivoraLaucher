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

- Minimal home screen with a live clock and date
- Compact, low-emphasis home search that expands in the app drawer
- Optional Nivora wordmark, controlled from settings
- Glass-style translucent panels with fine edge highlights; can be disabled
- Preset appearance palettes: Glass Mono, Obsidian, Frosted Silver, Graphite, Midnight, Sage, Bronze, and Pearl
- A custom theme mode for manual light/dark, background, and accent choices
- App drawer with grid/list views, search by label or package name, and favorites-only filter
- Long-press an app to add or remove it from favorites
- Adjustable grid density, icon size, labels, typeface, and other preferences saved locally

## Development roadmap

- [ ] Native Android home-screen widgets, including host lifecycle and widget configuration
- [ ] Choose a wallpaper from the device gallery
- [ ] Third-party icon pack integration
- [ ] Gestures and richer app shortcuts
- [ ] Optional AI-generated themes and wallpapers, with clear disclosure of any external service

## Design references

Nivora takes inspiration from broad interaction patterns rather than copying another app's visuals. Niagara and Olauncher inform the focus on quick access and a calmer home screen. Revolut's public design posts describe a theme system, custom backgrounds/wallpapers, and colour themes; Nivora translates the general idea of named appearance packs into its own launcher palettes and controls. Theme availability in Revolut may vary by plan and region.

- [Revolut 10: themes, backgrounds, widgets and navigation](https://www.revolut.com/blog/post/revolut-10/)
- [Revolut Tech: custom Android UI themes](https://medium.com/revolut/customising-android-app-ui-with-themes-a251e42b1451)

These are references for customization patterns and palette direction, not copied screen layouts, assets, or brand artwork.
