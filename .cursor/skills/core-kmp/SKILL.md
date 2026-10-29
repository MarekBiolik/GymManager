---
name: core-kmp
description: >-
  Implement or change GymManager shared KMP core: domain, use cases,
  repositories, sync, commonTest — no UI, no JS/Wasm target. Use when editing
  core/, shared domain rules, repository ports, sync/read-cache, or core unit
  tests; after platform-delivery core slice; or the user says core / KMP.
---

# Core (KMP)

Work only in the shared **`core/`** module: domain rules, use cases, repository contracts/implementations, sync — **not** Compose/SwiftUI/React. Architecture: [shared-core](../../../docs/architecture/shared-core.md), [ADR 0001](../../../docs/decisions/0001-shared-core-kmp.md). Coding seam: [coding-standards](../../../docs/conventions/coding-standards.md). Tests: [testing](../../../docs/conventions/testing.md).

Env expectations: [references/env-checklist.md](references/env-checklist.md).

## Workflow

Copy and track:

```
Core slice:
- [ ] 1. Scope the seam
- [ ] 2. Contract first
- [ ] 3. Implement + unit tests
- [ ] 4. Verify
- [ ] 5. Hand off to platforms
```

### 1. Scope the seam

Read the issue/spec and glossary names. Decide what lives where:

| Put in core | Put elsewhere |
|-------------|----------------|
| Entities, value objects, domain errors | Widgets, colors, navigation, layout |
| Use cases / application services | Android/iOS/web UI mapping only |
| Repository ports + Supabase data impl | `service_role` / Dashboard click-ops |
| Sync / read-cache helpers (ADR 0008) | Parallel TypeScript domain (forbidden — ADR 0006) |
| UI-independent validation | Platform permissions, store signing |

Targets: **Android + iOS only**. Do not add `js()` / `wasmJs()` or assume web consumes the KMP binary ([ADR 0006](../../../docs/decisions/0006-web-supabase-js-without-kmp-js.md)).

Shared web+mobile invariants that must hold without KMP-JS → prefer Postgres RLS/CHECK ([postgres-invariants](../../../docs/architecture/postgres-invariants.md)); coordinate with supabase skill/issue when schema changes.

**Done when:** clear list of core types/use cases for this slice; UI work deferred to platform skills / `platform-delivery` step 4.

### 2. Contract first

Prefer a narrow **public** facade (use cases, observable state, commands). Keep the rest `internal`. Package outline (phase 1):

```text
core/
  domain/       # entities, rules, domain errors
  application/  # use cases, ports
  data/         # repo implementations, DTOs, supabase-kt client
  sync/         # read cache / invalidation (MVP)
```

Names match [glossary](../../../docs/domain/glossary.md). Prefer immutable domain data. Coroutines/Flow for async — no UI-thread assumptions inside core.

If the slice needs new tables/RLS, land or link the migration contract before inventing a divergent core model.

**Done when:** public API shape for this slice is written (types/signatures or equivalent in the PR); UI can call it without reaching into `data/`.

### 3. Implement + unit tests

New business rule → test in the **same** change ([testing](../../../docs/conventions/testing.md)). Prefer `commonTest` / `kotlin.test`. Prefer `tdd` when the rule is non-trivial.

Do not call production Supabase from unit tests; use fakes/mocks/local fixtures.

**Done when:** implementation matches the contract; tests cover the new rule(s); no UI code under `core/`.

### 4. Verify

Run core build and unit tests (see env checklist). Prefer root scripts when they exist.

**Done when:** `./gradlew :core:build` (or documented equivalent) and core tests pass.

### 5. Hand off to platforms

Signal that the seam is ready for Android / web / iOS (build-spec) via `platform-delivery` (or area skills when present). Short status: public API touched, tests added, any DB contract dependency.

Do not open a PR or merge unless the user asked.

**Done when:** issue AC for core are tickable; platforms know what to consume.

## Guardrails

- No Compose, SwiftUI, React, or Android `View` code in `core/`
- No KMP→JS/Wasm target on day 1
- No `service_role` in client/core shipping paths
- Prefer strong model for domain/sync design; cheap helpers for search and running Gradle
