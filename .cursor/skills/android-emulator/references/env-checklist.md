# Android emulator environment checklist

## Required for smoke

| Tool | Expectation |
|------|-------------|
| **Android SDK** | `adb` + `emulator` on `PATH` (empty `ANDROID_HOME` is OK) |
| **AVD or device** | Prefer AVD `Medium_Phone_API_37.0`; start it if `adb devices` is empty |
| **JDK + Gradle wrapper** | Same as `android-compose` env |

## Useful commands

```text
adb devices
emulator -list-avds
emulator -avd Medium_Phone_API_37.0
adb wait-for-device
adb shell getprop sys.boot_completed
.\gradlew.bat :apps:android:assembleDebug
adb uninstall com.gymmanager.android
adb install -r apps\android\build\outputs\apk\debug\android-debug.apk
adb shell am start -n com.gymmanager.android/.MainActivity
adb logcat -d
```

## Not this skill

- Writing Compose UI → `android-compose`
- Core domain tests → `core-kmp`
- Gate order / report → `qa-testing` (tests required; no skips)
