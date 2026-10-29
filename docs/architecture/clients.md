# Clients (iOS / Android / web)

Each client is a **thin UI layer** over the shared core (on web: thin UI over Supabase, no KMP binary — [ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md)). Shared UI decision: [ADR 0003](../decisions/0003-native-ui-per-platform.md).

## Android — `apps/android`

- UI: Jetpack Compose
- Language: Kotlin
- Core integration: Gradle dependency on `:core` in the same multi-project ([ADR 0005](../decisions/0005-monorepo-tooling.md))
- Local responsibility: Activity/Navigation, theme, system permissions, optionally WorkManager for background sync

## iOS — `apps/ios`

- UI: SwiftUI
- Language: Swift
- Core integration: XCFramework / local SPM generated from KMP (same commit as the app; steps: [phase-1 playbook](../plans/phase-1-scaffold-playbook.md))
- Local responsibility: SwiftUI navigation, Keychain (if tokens outside SDK), signing, App Store
- Auth: when shipping with Google Sign-In, the App Store also requires **Sign in with Apple** — see [auth](auth.md)

## Web — `apps/web`

- UI: React + TypeScript + Vite; **pnpm** workspace ([ADR 0005](../decisions/0005-monorepo-tooling.md))
- Core integration: **no KMP binary on day 1** — web → Supabase via `supabase-js` ([ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md); closes OQ-01); outside the Gradle tree
- Local responsibility: routing, theme, session/auth glue (`supabase-js`), row/DTO → view mapping, thin UX validation

### Interop decision (OQ-01 → ADR 0006)

KMP ties **Android and iOS**. Web does **not** consume the same binary as mobile until ADR 0006 revisit criteria justify A/C.

| Path | Status | Pros | Cons |
|------|--------|------|------|
| A. Core → JS/Wasm used from React | deferred (revisit) | One domain logic | Toolchain, DX, bundle |
| B. Web → `supabase-js` + thin TS | **chosen (day 1)** | Simple DX, mature ecosystem | Risk of duplicating rules vs core |
| C. Hybrid | deferred (revisit) | Flexibility | Part of A’s cost + two I/O paths |

### What web may / must not do relative to domain

**Allowed:** `supabase-js` (RLS, anon key); UI and presentation; types generated from the Supabase schema; light form validation mirroring DB/core.

**Not allowed:** assume KMP-JS/Wasm without revisit/ADR; build a parallel TS domain (use cases, business aggregations); treat TS-only validation as a security boundary; bypass RLS.

Shared invariants needed by web + mobile → prefer **Postgres (RLS / CHECK / constraints)**. Rich / offline-first rules → **KMP core** (mobile); do not reimplement “shortcuts” on web. Details, examples (`food_entries` / `measurements`), and migration checklist: [postgres-invariants](postgres-invariants.md).

## Shared client requirements

1. No secrets in the repo (Supabase anon key + URL OK in the client; service role never).
2. User state (signed in / data): mobile from core; web from `supabase-js` session / agreed auth contract ([ADR 0007](../decisions/0007-auth-providers-minimum-pii.md)).
3. Feature flags / environment (`dev` day one; staging/prod later) — [phase-1 playbook](../plans/phase-1-scaffold-playbook.md), [ADR 0004](../decisions/0004-supabase-hosting.md).
4. Minimal “hello”: login + read/write one RLS-protected entity (online-first + read cache — [ADR 0008](../decisions/0008-sync-strategy-online-first.md)) — phase 2 goal.

## Out of UI scope (for now)

- Final design system and branding
- Module-switching details nutrition/training/measurements ([OQ-08](../open-questions.md))
- PWA / home-screen install as a substitute for native iOS
