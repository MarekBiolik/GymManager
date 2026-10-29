# Tech stack

Concrete library versions will be pinned at scaffolding (phase 1). Below — technology choices and roles.

## Shared core

| Technology | Role |
|------------|------|
| Kotlin Multiplatform (KMP) | Shared domain, data, and sync code |
| Kotlin Coroutines / Flow | Async and state streams |
| kotlinx.serialization | Model serialization |
| supabase-kt (community) | Supabase client from core (Android/iOS; web without KMP-JS — [ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md)) |

Boundary details: [shared-core](shared-core.md). Decision: [ADR 0001](../decisions/0001-shared-core-kmp.md).

## Android

| Technology | Role |
|------------|------|
| Kotlin | App language |
| Jetpack Compose | UI |
| Android Gradle Plugin | Build |
| core (KMP) as dependency | Logic |

## iOS

| Technology | Role |
|------------|------|
| Swift | App language |
| SwiftUI | UI |
| Xcode | Build / signing |
| core as XCFramework / SPM from KMP | Logic |

Requires an **Apple Developer Program** account (App Store publishing).

## Web

| Technology | Role |
|------------|------|
| TypeScript | Language |
| React | UI |
| Vite | Tooling / build |
| supabase-js | Only Supabase client in `apps/web` on day 1 ([ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md)) |

## Backend / data

| Technology | Role |
|------------|------|
| Supabase | BaaS: Auth, Postgres, RLS, Realtime, Storage |
| PostgreSQL | Relational DB (history, aggregations, FK) |
| Supabase Auth | Sign-in (direction: Google + Apple on iOS) |
| SQL migrations in `supabase/` | Schema as code |

Backend decision: [ADR 0002](../decisions/0002-backend-supabase.md).  
Hosting / environments / backups: [ADR 0004](../decisions/0004-supabase-hosting.md) (Cloud Free → Pro; details in [data-and-sync](data-and-sync.md#hosting-and-backups); staging/prod split: [spec](../specs/supabase-staging-prod-split.md)).

## Project tooling

Decision: [ADR 0005](../decisions/0005-monorepo-tooling.md).

| Area | Choice | Notes |
|------|--------|--------|
| Monorepo | Hybrid: **Gradle** (`core` + `apps/android`) + **pnpm** (`apps/web`) + Xcode (`apps/ios`) | No Nx/Turborepo at start |
| Orchestration | Thin root scripts (`package.json` / `scripts/`) | Invoke Gradle, pnpm, Supabase CLI |
| Core versioning | Source-linked (same commit); `core/VERSION` or `gradle.properties` | SemVer/registry publish — later when needed |
| Client versioning | Independent store / web deploy numbers | Need not equal core SemVer |
| CI | Build core + clients + lint; migrations on PR | [ci-cd](../conventions/ci-cd.md) |
| Tests | Unit in core; UI tests per client later | [testing](../conventions/testing.md) |
| Formatting | ktlint / Spotless (Kotlin); ESLint+Prettier (web); SwiftFormat (iOS) | Pin versions in phase-1 scaffold |
| JS package manager | **pnpm** (+ `pnpm-workspace.yaml`) | Workspace = `apps/web` at start |

## Deliberately not chosen (for now)

- Flutter / React Native as primary UI — rejected in favor of native UI ([ADR 0003](../decisions/0003-native-ui-per-platform.md)).
- Firebase / Firestore — worse fit for aggregations and stronger lock-in.
- Custom Node backend from scratch — too much maintenance at the start.
- KMP→JS/Wasm for web on day 1 — deferred; web = `supabase-js` ([ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md)).
- AI layer — roadmap phase 7.
