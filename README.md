# NOVA//CORE

Native Android performance dashboard using Kotlin, Android WebView, Android system APIs, Media3, MediaSession, and a real overlay service.

## Build

Open the repository in Android Studio with JDK 17 and run `./gradlew assembleDebug`. The generated APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Honest capability notes

RAM, battery, storage, display, device, CPU, thermal status, cache clearing, SAF document selection, Media3 playback, diagnostic export, and overlay permission/service flows are native. Android does not expose another app's FPS, CPU temperature, GPU identity, ping, or arbitrary RAM cleaning without privileged/vendor APIs; those values are explicitly shown as `N/A` rather than invented. Thermal status is never presented as CPU temperature.

The overlay requires the user to grant “display over other apps”. Background URIs are persisted with SAF permissions. Media playback is handled by `NovaAudioService` and MediaSession.
