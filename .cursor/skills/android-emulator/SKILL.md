---
name: android-emulator
description: >-
  Run GymManager Android smoke on an AVD/emulator via adb: assemble, install,
  launch, capture failures. Use when smoking Android UI on emulator/device,
  verifying install/launch after an android-compose change, or the user says
  emulator / adb / Android smoke.
---

# Android emulator runner

Drive a **minimal smoke** on a local Android emulator or device: build debug APK, `adb` install, launch, observe crash/log. Complements `android-compose` (code) and `qa-testing` (what to run). Not a substitute for `core-kmp` unit tests.

Env: [references/env-checklist.md](references/env-checklist.md). Testing priority: [testing](../../../docs/conventions/testing.md).

## Workflow

Copy and track:

```
Emulator smoke:
- [ ] 1. Preconditions
- [ ] 2. Build
- [ ] 3. Install + launch
- [ ] 4. Smoke path
- [ ] 5. Report
```

### 1. Preconditions

- Android SDK + `adb` on `PATH`
- An AVD exists **or** a device is connected (`adb devices` shows `device`)
- Emulator booted (or start it and wait until `adb` is ready)

If no emulator/device: stop and report blocked — do not fake a pass.

**Done when:** `adb devices` lists a ready target.

### 2. Build

```text
./gradlew :apps:android:assembleDebug
```

(Windows: `.\gradlew.bat …`; prefer root scripts when present.)

**Done when:** debug APK path is known from the Gradle output / standard `outputs/apk` location.

### 3. Install + launch

```text
adb install -r <path-to-debug-apk>
adb shell am start -n <package>/<activity>
```

Resolve package/activity from the Android manifest / Gradle applicationId — do not invent if unclear; read the project files.

**Done when:** process starts without immediate crash (check `adb logcat` briefly).

### 4. Smoke path

Keep it minimal unless the issue specifies more: cold start, reach the screen under test, one primary action if applicable. Prefer cheap model / helper for waiting and log scraping; escalate failures that need product judgement to the lead.

**Done when:** agreed smoke steps ran, or first hard failure captured with log excerpt.

### 5. Report

Return to the lead/QA: pass/fail, device/AVD name, build used, logcat snippet on failure. Do not open PR/merge unless asked.

**Done when:** result is actionable for `qa-testing` / the issue AC.

## Guardrails

- Smoke ≠ unit/RLS coverage
- No secrets in adb commands or logs pasted to issues
- Honour user skip of emulator/tests (`platform-delivery` skip gate)
- Prefer cheap helpers for adb loops; strong model only for diagnosing hard failures
