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

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot"
.\gradlew.bat :apps:android:assembleDebug
```

## Emulator smoke

Per the `android-emulator` skill: assemble, install, launch. **Skip live Google sign-in smoke** when IdP/secrets are missing (#73): placeholders in `local.properties.example` are enough for build and unsigned UI; end-to-end sign-in needs Google Cloud Android + Web clients (SHA-1), Supabase Google provider, and real values in gitignored `local.properties`.
