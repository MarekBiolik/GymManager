# Android environment checklist

## Required

| Tool | Expectation |
|------|-------------|
| **JDK** | 17+ (or version required by project AGP) |
| **Android SDK** | Platform SDK + build-tools matching the project |
| **Gradle wrapper** | Repo root `gradlew` / `gradlew.bat` |
| **Android Studio** (optional) | Useful for SDK manager / layout preview; CLI Gradle is enough for agents |

## Emulator / device

| Tool | Expectation |
|------|-------------|
| **AVD or physical device** | Needed for interactive smoke; not required for `assembleDebug` alone |
| **`adb`** | On `PATH` when using emulator runner skill (#50) |

Emulator boot/install/launch procedures live in the android-emulator skill — keep this checklist to toolchain presence.

## Typical commands

```text
./gradlew :apps:android:assembleDebug
./gradlew :apps:android:build
```

Prefer root scripts when they exist. Exact module paths follow `settings.gradle.kts` once scaffolded.
