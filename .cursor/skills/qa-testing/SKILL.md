---
name: qa-testing
description: >-
  Run GymManager test gates in priority order: core unit, RLS/migrations,
  web build, Android emulator smoke — honour skip gate. Use when verifying a
  slice before PR, running the test plan, or the user says QA / test / verify.
---

# QA / testing

Coordinate **what to run** and **how to report** for a slice. Priority and policy: [testing](../../../docs/conventions/testing.md), CI goals: [ci-cd](../../../docs/conventions/ci-cd.md). Delivery skip gate: `platform-delivery`.

Does not replace `tdd` (authoring tests) or `code-review` (standards/spec review).

## Priority order

Unless the user **explicitly skips** tests (or a layer):

1. **Core unit** — `core-kmp` / `./gradlew :core:test` (new rules must have tests)
2. **Supabase / RLS** — when `supabase/` changed: lint/apply locally + isolation intent (`supabase-db`; two-JWT when harness exists)
3. **Web** — `pnpm` build (and lint if configured) via `web-react`
4. **Android smoke** — `android-emulator` when emulator/device available and UI changed
5. **iOS runtime** — out of scope until [#49](https://github.com/MarekBiolik/GymManager/issues/49); build-spec docs only via `ios-swiftui`

Flaky test = bug; do not leave permanent skip without an issue ([testing](../../../docs/conventions/testing.md)).

## Workflow

Copy and track:

```
QA gate:
- [ ] 1. Read skip gate + change set
- [ ] 2. Run applicable layers
- [ ] 3. Triage failures
- [ ] 4. Report
```

### 1. Read skip gate + change set

From the issue/PR/user: which platforms/tests are skipped? Which paths changed (`core/`, `supabase/`, `apps/*`)?

**Done when:** explicit run matrix (and skips) is written in the status note.

### 2. Run applicable layers

Dispatch cheap helpers for builds/emulator loops; keep the lead on ambiguous failures. Use area skills’ verify steps rather than inventing alternate commands.

**Done when:** every non-skipped applicable layer has a pass/fail result.

### 3. Triage failures

- Red unit/RLS → fix or block merge (do not “skip forever”)
- Tooling missing (no emulator) → report **blocked**, not green
- iOS Simulator requests while #49 open → remind build-spec mode

**Done when:** each failure is classified: product bug / test bug / env blocker.

### 4. Report

Short note on issue/PR: matrix, results, log pointers, next action. Tick AC when the gate matches what the user asked. PR/merge only if the user asked.

**Done when:** lead/user can decide merge-readiness without re-running blindly.

## Guardrails

- Honour skip gate — do not invent skips
- Missing emulator ≠ passed Android smoke
- No production Supabase in tests; no secrets in logs
- Prefer cheap models for running suites; strong model for flaky/complex failures
