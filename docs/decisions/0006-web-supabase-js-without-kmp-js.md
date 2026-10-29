# ADR 0006 — Web: supabase-js without KMP→JS/Wasm (day 1)

- **Status:** accepted
- **Date:** 2026-10-29
- **Decision makers:** Marek (+ OQ-01 closing session)

## Context

Android and iOS consume the shared KMP core natively ([ADR 0001](0001-shared-core-kmp.md)). Web (React + TypeScript) needs a strategy toward that same core. Three paths from [OQ-01](../open-questions.md):

| Option | Outline |
|--------|---------|
| **A** | Core compiled to JS/Wasm, consumed from React |
| **B** | Web → `supabase-js` + thin TS logic |
| **C** | Hybrid: part of domain from shared (JS/Wasm), web networking separate |

Forces: **solo maintenance**, fast and predictable scaffold (phases 1–2), avoiding a fragile KMP-JS/Wasm toolchain at the start, while keeping “one domain truth” discipline where it really hurts.

## Decision

We choose **B for day 1** (and through scaffold + auth/sync baseline):

- `apps/web` talks to Supabase via **`supabase-js`** (Auth, Postgres, Realtime, Storage).
- We do **not** add a JS/Wasm target to `core` and do **not** assume web consumes the same binary as mobile.
- Domain rules still **live in KMP** for Android/iOS; shared security and data-integrity invariants are **preferred in Postgres (RLS, CHECK, constraints)** so web and mobile share the same protection without a shared binary.
- In TS only a **thin** layer is allowed: UI, routing, DTO→view mapping, session glue, optional UX validation mirroring server constraints — **no** “domain services” package parallel to core.

Path A (or a narrower C) remains a **revisit option**, not a scaffold goal.

## Alternatives considered

1. **A — Core → JS/Wasm from React** — one rules implementation on web too. Rejected for day 1: KMP-JS/Wasm toolchain maturity/DX + Vite/React integration and `supabase-kt` vs `supabase-js` is real cost for one maintainer; would delay phases 1–2 without stability guarantees.
2. **B — `supabase-js` + thin TS** — chosen: mature DX, standard web stack, simple scaffold, clear split. Cost: rule-drift risk if TS starts “growing” a domain — hence hard guardrails in client/core docs.
3. **C — Hybrid (shared domain + web networking)** — flexible, but needs part of A (JS/Wasm target) plus two I/O paths. Rejected at start as complexity without benefit until B hurts; may be an intermediate step on revisit toward A.

## Consequences

### Positive

- Web scaffold = ordinary Vite/React app + `supabase-js`; no KMP-JS blocker.
- Mobile still has one KMP core; decision does not weaken ADR 0001 for Android/iOS.
- Critical shared rules can be enforced in the DB (RLS/constraints) regardless of client.

### Negative / costs

- Web does **not** automatically share use cases or the sync engine with KMP — MVP sync is online-first on both sides ([ADR 0008](0008-sync-strategy-online-first.md)); web↔mobile **offline-write** parity may require revisit A/C.
- Risk of duplicating rules in TS if guardrail discipline slips.
- Types / DTOs on web come from the Supabase schema (e.g. generated types), not KMP models — possible name/shape drift; we mitigate with convention and migrations as schema source of truth.

### Allowed / not allowed (web)

**Allowed:**

- Call Supabase via `supabase-js` (per RLS; only anon key + URL in the client).
- Hold UI state, navigation, presentation formatting.
- Generate TS types from the DB schema.
- Mirror **light** form validation as UX only (messages before submit) when the same rule already lives in DB/core.

**Not allowed:**

- Assume KMP binary consumption (JS/Wasm) without a new ADR / revisit.
- Build in `apps/web` a domain layer parallel to `core` (business aggregations, “session completeness”, nutrition policies, etc.).
- Bypass RLS / write “trust the client” paths.
- Treat TS-only validation as the security source of truth.

Operational details: [clients](../architecture/clients.md), [shared-core](../architecture/shared-core.md).  
DB vs core/TS rule split (migration checklist): [postgres-invariants](../architecture/postgres-invariants.md).

### Revisit criteria (when to return to A or C)

We reconsider KMP→JS/Wasm (A) or hybrid (C) when **at least one** condition holds:

1. **Duplication pain** — the same business rule must be maintained in Kotlin and TS and causes real bugs or systematic drift (not a one-off UX helper).
2. **Offline-write parity** — web must share the same queue/conflict model as mobile after offline-write lands (today’s MVP: online-first, [ADR 0008](0008-sync-strategy-online-first.md) / [OQ-03](../open-questions.md) decided), and maintaining two engines costs more than the JS/Wasm toolchain.
3. **Toolchain mature for us** — green, repeatable `core` → JS or Wasm build wired into Vite, acceptable bundle size, and a data path aligned with the chosen Supabase client — proven by a spike, not an assumption.
4. **Web as a critical surface** — product-wise web catches up to mobile in domain complexity and B’s cost exceeds A/C.

Until revisit: **no** hidden assumption in scaffold, CI, or docs that web “gets core for free”.

## Related

- Closes [OQ-01](../open-questions.md)
- [ADR 0001](0001-shared-core-kmp.md), [ADR 0003](0003-native-ui-per-platform.md)
- [clients](../architecture/clients.md), [shared-core](../architecture/shared-core.md), [postgres-invariants](../architecture/postgres-invariants.md), [tech-stack](../architecture/tech-stack.md), [overview](../architecture/overview.md)
