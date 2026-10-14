# Nivora Launcher

Minimal Android launcher built with Kotlin and Jetpack Compose.

## Build status

GitHub Actions builds a debug APK until persistent release signing is configured. Debug APKs may not be installable as updates over APKs signed with a different debug key.

## Configure stable release signing

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

Copy the resulting string into the `NIVORA_KEYSTORE_BASE64` secret. Keep the original keystore and its passwords backed up securely: future APK updates must use the same signing key. Do not paste secrets into issues, chat, commits, or logs.

When all four secrets are present, GitHub Actions builds and uploads `nivora-release-apk`. If they are missing or incomplete, it builds `nivora-debug-apk` instead.

## Current features

- Launch installed applications and search by app name
- Grid/list layouts, favorites, icon sizing, and column count
- Saved preferences for themes, colors, wallpapers, and typeface presets

## Not implemented yet

Native Android home-screen widgets, choosing a wallpaper from the gallery, third-party icon packs, and AI-generated themes/wallpapers.
