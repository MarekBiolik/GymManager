# Android emulator environment checklist

## Required for smoke

| Tool | Expectation |
|------|-------------|
| **Android SDK** | Platform tools including `adb` |
| **AVD or device** | At least one emulator image or physical device |
| **JDK + Gradle wrapper** | Same as `android-compose` env |

## Useful commands

```text
adb devices
emulator -list-avds
emulator -avd <name>   # or start from Android Studio
adb wait-for-device
adb logcat -d | tail
```

Exact emulator binary path varies by SDK install (`$ANDROID_HOME/emulator/emulator`).

## Not this skill

- Writing Compose UI → `android-compose`
- Core domain tests → `core-kmp`
- Deciding whether tests are required → `platform-delivery` skip gate / `qa-testing`
