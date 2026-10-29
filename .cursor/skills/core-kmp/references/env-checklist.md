# Core (KMP) environment checklist

Expectations for agents (and humans) working in `core/`. Exact JDK/AGP pins live in the Gradle/scaffold config once present in the repo.

## Required locally

| Tool | Expectation |
|------|-------------|
| **JDK** | 17+ (or the version required by the project’s AGP) |
| **Android SDK** | Needed to resolve/build the Android KMP target with `:core` |
| **Gradle** | Wrapper from repo root (`gradlew` / `gradlew.bat`) — do not invent a second Gradle root |
| **Network** | First builds may download dependencies; offline only after a warm cache |

## Optional

| Tool | When |
|------|------|
| **Xcode / macOS** | Only to verify iOS framework export from KMP — not required on Windows for core unit work |
| **Supabase CLI** | When the slice also changes `supabase/` (see supabase skill / [#47](https://github.com/MarekBiolik/GymManager/issues/47)) |

## Commands

Prefer root scripts when they exist; otherwise:

```text
./gradlew :core:build
./gradlew :core:test
```

(Windows: `.\gradlew.bat …`)

CI must expose the same core build + test tasks ([ci-cd](../../../docs/conventions/ci-cd.md)).
