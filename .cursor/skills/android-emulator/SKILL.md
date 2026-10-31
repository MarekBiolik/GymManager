---
name: android-emulator
description: >-
  Run GymManager Android smoke on an AVD/emulator via adb: assemble, install,
  launch, capture failures. Use when smoking Android UI on emulator/device,
  verifying install/launch after an android-compose change, or the user says
  emulator / adb / Android smoke.
---

# Android emulator runner

Drive a **required smoke** on a local Android emulator or device: start AVD if needed, build debug APK, `adb` install, launch, observe crash/log. Complements `android-compose` (code) and `qa-testing` (what to run). Not a substitute for `core-kmp` unit tests.

Env: [references/env-checklist.md](references/env-checklist.md). Testing priority: [testing](../../../docs/conventions/testing.md).

## Workflow

Copy and track:

```
Emulator smoke:
- [ ] 1. Preconditions (start AVD if idle)
- [ ] 2. Build
- [ ] 3. Install + launch
- [ ] 4. Smoke path
- [ ] 5. Report
```

### 1. Preconditions — start the emulator yourself

Empty `adb devices` means **nothing is running yet**, not that smoke is impossible. **Start an AVD and wait for boot.** Do not report blocked. Do not skip.

1. `adb devices` — if a target already shows `device`, go to step 2.
2. Otherwise `emulator -list-avds`. Prefer **`Medium_Phone_API_37.0`** when present; else the first listed AVD.
3. Start and wait:

```text
emulator -avd Medium_Phone_API_37.0
adb wait-for-device
adb shell getprop sys.boot_completed   # poll until it prints 1
```

`adb` / `emulator` on `PATH` is enough. Empty `ANDROID_HOME` / `ANDROID_SDK_ROOT` is **not** “no SDK”.

**Only** if `emulator -list-avds` is empty **and** no physical device: **stop and ask** the user how to provision an AVD. Never fake a pass. Never tick “emulator smoke” AC.

**Done when:** `adb devices` lists `device` and `sys.boot_completed` is `1`.

### 2. Build

```text
.\gradlew.bat :apps:android:assembleDebug
```

(Unix: `./gradlew :apps:android:assembleDebug`.)

**Done when:** APK exists at `apps/android/build/outputs/apk/debug/android-debug.apk` (or the Gradle-reported path).

### 3. Install + launch

Maintainer flow (use this):

```text
adb uninstall com.gymmanager.android
adb install -r apps\android\build\outputs\apk\debug\android-debug.apk
adb shell am start -n com.gymmanager.android/.MainActivity
```

`uninstall` may fail if the app is not installed — continue. Confirm the process is up (`pidof` / `dumpsys activity` shows `MainActivity` resumed). Check `adb logcat` briefly for an immediate crash.

**Done when:** `MainActivity` is resumed without an immediate crash.

### 4. Smoke path

Cold start, reach the screen under test, one primary action the issue names. Capture a hard failure with a log excerpt.

**Done when:** agreed smoke steps ran, or the first hard failure is recorded.

### 5. Report

Pass/fail, AVD/device name, build used, logcat on failure. A pass requires steps 1–4 actually run.

**Done when:** `qa-testing` / the issue AC can be ticked honestly.

## Guardrails

- Smoke ≠ unit/RLS coverage
- No secrets in adb commands or logs
- **Tests are required. No skips.** Idle emulator → start it (step 1). Env truly missing → **stop and ask**.
- Prefer cheap helpers for adb wait loops; strong model for hard crash diagnosis
