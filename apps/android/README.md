# GymManager Android

Jetpack Compose client over `:core`. Auth uses the shared [AuthSession](https://github.com/MarekBiolik/GymManager/blob/master/core/src/commonMain/kotlin/com/gymmanager/core/application/auth/AuthSession.kt) facade: the app obtains a Google **id_token** (Credential Manager) and calls `completeGoogleSignIn`.

## Local config

Copy [local.properties.example](../../local.properties.example) to repo-root `local.properties` (gitignored). Required for auth:

- `gymmanager.supabase.url` — project URL (anon key only; never `service_role`)
- `gymmanager.supabase.anonKey`
- `gymmanager.google.webClientId` — Google **Web application** OAuth client ID (public; used as `serverClientId` for the ID token audience)

Rebuild after changing `local.properties` so `BuildConfig` picks up values.

## OAuth deep link

Redirect convention: `com.gymmanager.android://auth/callback` (matches `applicationId`). Register it in Supabase **Additional Redirect URLs** per [idp-config-checklist](../../docs/specs/idp-config-checklist.md).

Native Google Sign-In uses Credential Manager and does not require the deep link for the happy path; the intent filter is for browser/OAuth return flows aligned with the IdP checklist.

## Build

Use **JDK 17** for CLI Gradle (project `jvmTarget` / Android toolchain). Android Studio may use **JVM 21** for IDE sync — that is fine; do **not** point Gradle at JDK 25.

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot"
.\gradlew.bat :apps:android:assembleDebug
# or: installDebug (build + adb install when a device/emulator is connected)
.\gradlew.bat :apps:android:installDebug
```

## PATH (Cursor / PowerShell, Windows)

Add these user PATH entries so `adb` / `emulator` work without Android Studio open (`%LOCALAPPDATA%` = usually `C:\Users\<you>\AppData\Local`):

| Dir | Why |
|-----|-----|
| `%LOCALAPPDATA%\Android\Sdk\platform-tools` | `adb` |
| `%LOCALAPPDATA%\Android\Sdk\emulator` | `emulator` |

Optional (not required for daily smoke): `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest\bin` (`sdkmanager` / `avdmanager`).

Do **not** put the whole `Sdk` root or random WinGet `platform-tools` copies first on PATH if they conflict — prefer the Studio SDK path above (same as `sdk.dir` in `local.properties`).

```powershell
emulator -list-avds
emulator -avd <avd_name>   # leave running
adb devices
.\gradlew.bat :apps:android:installDebug
adb shell am start -n com.gymmanager.android/.MainActivity
```

SHA-1 for Google Cloud **Android** OAuth client (debug keystore):

```powershell
& "$env:JAVA_HOME\bin\keytool.exe" -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
# or: .\gradlew.bat :apps:android:signingReport
```

## Emulator smoke

Per the `android-emulator` skill: assemble, install, launch. **Skip live Google sign-in smoke** when IdP/secrets are missing (#73): placeholders in `local.properties.example` are enough for build and unsigned UI; end-to-end sign-in needs Google Cloud Android + Web clients (SHA-1), Supabase Google provider, and real values in gitignored `local.properties`.

## SDK / JVM cleanup notes

- Keep the existing SDK at `%LOCALAPPDATA%\Android\Sdk`. Studio only adds missing packages — **no full wipe**.
- Keep **JDK 17** for CLI builds. Keep Studio’s **JVM 21** for sync. Do not uninstall either just because the OS also has JDK 25.
- Extra platforms (`android-35` for compile, `android-37` for a modern AVD) can coexist; delete old unused system images later only if disk is tight.
