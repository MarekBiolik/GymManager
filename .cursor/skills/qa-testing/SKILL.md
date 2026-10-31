---
name: qa-testing
description: >-
  Run GymManager test gates in priority order: core unit, RLS/migrations,
  web build + browser smoke, Android emulator smoke — tests are required,
  no skips; blocked → stop and ask. Use when verifying a slice before PR,
  running the test plan, or the user says QA / test / verify.
---

# QA / testing

Coordinate **what to run** and **how to report** for a slice. Priority: [testing](../../../docs/conventions/testing.md). CI: [ci-cd](../../../docs/conventions/ci-cd.md).

Tests **are required**. There is **no skip gate**. If a layer cannot run, **stop and ask** — do not merge, do not tick AC, do not call it green.

Does not replace `tdd` (authoring tests) or `code-review` (standards/spec review).

## Priority order

Run every layer the change set touches:

1. **Core unit** — `core-kmp` / `./gradlew :core:testDebugUnitTest` (new rules must have tests)
2. **Supabase / RLS** — when `supabase/` changed: lint/apply locally + isolation intent (`supabase-db`; two-JWT when harness exists)
3. **Web** — `pnpm` build + lint (`web-react`) **and** browser smoke of the changed flow (user-rule browser verification)
4. **Android smoke** — `android-emulator` when `apps/android/` UI/wiring changed (that skill **starts the AVD** if `adb devices` is empty)
5. **iOS runtime** — out of scope until [#49](https://github.com/MarekBiolik/GymManager/issues/49); docs-only via `ios-swiftui` is not a test skip of Android/web

Flaky test = bug; do not leave a permanent skip without an issue ([testing](../../../docs/conventions/testing.md)).

## Workflow

Copy and track:

```
QA gate:
- [ ] 1. Change-set matrix
- [ ] 2. Run every applicable layer
- [ ] 3. Triage failures
- [ ] 4. Report
```

### 1. Change-set matrix

Which paths changed (`core/`, `supabase/`, `apps/android/`, `apps/web/`)? Those layers **must** run.

**Done when:** the run matrix is written. No skip column.

### 2. Run applicable layers

Dispatch cheap helpers for builds and emulator loops; keep the lead on ambiguous failures. Use area skills’ verify steps (`android-emulator` starts the AVD).

**Done when:** every applicable layer has a real pass/fail from actually running it.

### 3. Triage failures

- Red unit/RLS/build/smoke → **fix**, or **stop and ask** (do not merge)
- Tooling missing after `android-emulator` tried to start an AVD → **blocked**: stop and ask (how to provision). Not green.
- iOS Simulator while #49 open → remind build-spec mode; still run Android/web

**Done when:** each failure is classified: product bug / test bug / env blocker (user asked).

### 4. Report

Short note on issue/PR: matrix, results, log pointers. Tick **only** AC that actually ran. PR/merge only if the user asked **and** required layers passed.

**Done when:** lead/user can decide merge-readiness without re-running blindly.

## Guardrails

- **No skips.** Empty `adb devices` → run `android-emulator` (it starts the AVD).
- Missing tooling after a real start attempt → **stop and ask**, never a fake pass.
- No production Supabase in tests; no secrets in logs
- Prefer cheap models for running suites; strong model for flaky/complex failures
