# Coding standards

Base conventions. Concrete linter and formatter versions — at scaffolding (phase 1).

## General rules

1. **Domain logic only in `core`** — UI does not duplicate business rules. Web: thin TS + `supabase-js`, no parallel domain ([ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md)).
2. **Explicit names** — aligned with the [glossary](../domain/glossary.md); avoid abbreviations like `usr`, `sess2`.
3. **Small PRs** — one intent; scaffolding separate from features.
4. **Secrets** — never in the repo; `service_role` never in clients.
5. **Documentation** — architecture change = ADR or `docs/` update in the same PR.

## Kotlin (core + Android)

- Idiomatic Kotlin; prefer immutable data / value types in the domain.
- Public `core` API as narrow as practical (facade / use case), rest `internal`.
- Coroutines for I/O; avoid blocking the UI thread.
- Formatter/linter: ktlint or Spotless (choose in phase 1).

## Swift (iOS)

- SwiftUI + readable `View`s without business logic.
- UI model separated from core domain types (mapping at the edge).
- SwiftFormat / SwiftLint — enable in phase 1.

## TypeScript / React (web)

- TypeScript strict.
- Functional components; data via `supabase-js` (not via KMP binary) — [ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md).
- ESLint + Prettier.
- No `any` without a justifying comment.

## SQL / Supabase

- Every user table: `user_id` + RLS policies in the same migration.
- Table names: `snake_case`, plural (`food_entries`, `measurements`).
- Migrations forward-only; no manual prod schema drift.
- Shared web + mobile invariants → Postgres (RLS / CHECK), not a parallel TS domain — [postgres-invariants](../architecture/postgres-invariants.md).

## Comments

- A comment explains **why**, not what the code already shows.
- TODOs with an id (`OQ-NN` or issue), not bare `TODO: fix later`.
