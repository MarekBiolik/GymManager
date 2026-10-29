# GymManager

Cross-platform gym / nutrition companion: shared domain core, native UIs, Supabase backend.

## Documentation

Start at **[docs/README.md](docs/README.md)** — vision, architecture, ADRs, specs, and conventions. That index is the documentation source of truth.

## Monorepo intent

| Area | Role |
|------|------|
| `core/` | Shared Kotlin Multiplatform domain (planned) |
| `apps/android`, `apps/ios`, `apps/web` | Native UI clients — Compose / SwiftUI / React |
| `supabase/` | Postgres schema, RLS, CLI config |
| `design/` | Design-token SSOT + generator (tokens-first) |
| `docs/` | Living architecture and decisions |

Application and `core/` scaffolding may still be in progress — see [#14](https://github.com/MarekBiolik/GymManager/issues/14).

## Design tokens

Visual values are shared via a **single source of truth** under `design/tokens/`, with a generator emitting native theme artifacts per platform (Compose, SwiftUI, CSS variables + TypeScript). UI code stays platform-native ([ADR 0003](docs/decisions/0003-native-ui-per-platform.md)); only tokens are shared ([ADR 0010](docs/decisions/0010-design-tokens-ssot.md)).
