---
name: android-compose
description: >-
  Implement GymManager Android UI in Jetpack Compose over KMP core: thin
  presentation, navigation, permissions — no domain rules in UI. Use when
  editing apps/android/, Compose screens, Android wiring to :core, or the
  user says Android / Compose.
---

# Android (Compose)

Own `apps/android/`: Jetpack Compose UI and Android platform wiring over `:core`. Native UI decision: [ADR 0003](../../../docs/decisions/0003-native-ui-per-platform.md). Client role: [clients](../../../docs/architecture/clients.md). Coding: [coding-standards](../../../docs/conventions/coding-standards.md).

Core domain stays in `core-kmp`. Emulator smoke is a separate skill/issue (`android-emulator` / #50) — this skill covers code and toolchain, not device-farm loops.

Env: [references/env-checklist.md](references/env-checklist.md).

## Workflow

Copy and track:

```
Android slice:
- [ ] 1. Confirm core contract
- [ ] 2. Thin UI
- [ ] 3. Platform wiring
- [ ] 4. Verify build
- [ ] 5. Hand-off
```

### 1. Confirm core contract

Android **consumes** the public core API (use cases / state / commands). If the slice needs new domain rules, land them with `core-kmp` first (`platform-delivery`), or the same PR stack with core ahead of UI.

Do not reimplement macros, session completion, sync conflict rules, etc. in Compose or ViewModels as a second domain.

**Done when:** known core API (or linked core PR/issue) for this screen/flow.

### 2. Thin UI

- Jetpack Compose for UI; idiomatic Kotlin
- Map core/UI models at the edge; keep screens previewable where practical
- Navigation, theme, layout, a11y labels — yes
- Business rules, SQL, RLS assumptions — no

Prefer state hoisting / unidirectional data from core state; avoid dumping repository calls that bypass the agreed core facade.

**Done when:** UI renders and drives the core contract without new domain logic in `apps/android/`.

### 3. Platform wiring

Android-owned: Activity/Navigation, permissions, optional WorkManager for background sync later, Gradle dependency on `:core` in the same multi-project.

Secrets: anon URL/key via local config — never `service_role`.

**Done when:** wiring matches [clients](../../../docs/architecture/clients.md); no secrets in source.

### 4. Verify build

```text
./gradlew :apps:android:assembleDebug
```

(Windows: `.\gradlew.bat …`; prefer root scripts when present.)

Compose UI tests — selective later per [testing](../../../docs/conventions/testing.md). Emulator install/launch → android-emulator skill when available.

**Done when:** assembleDebug (or equivalent) succeeds for the change.

### 5. Hand-off

Short status: screens/modules touched, core API used, build result. PR/merge only if the user asked. Tick issue AC via `gh` when done.

**Done when:** Android AC for the slice are tickable; QA/emulator can smoke if requested.

## Guardrails

- Thin UI over core — no parallel domain in Android
- No Compose Multiplatform shared UI (native per platform — ADR 0003)
- No `service_role` in the app
- Prefer strong model for navigation/UX structure; cheap helpers for Gradle/search
