# ADR 0008 — Sync strategy: online-first + read cache (MVP)

- **Status:** accepted
- **Date:** 2026-10-29
- **Decision makers:** Marek (+ OQ-03 closing session)

## Context

A fitness tracker needs the **same data** on Android, iOS, and web after sign-in ([ADR 0007](0007-auth-providers-minimum-pii.md)). Backend is Postgres via Supabase ([ADR 0002](0002-backend-supabase.md)). Forces: solo maintenance, fast phase-2 baseline (“hello sync”), avoiding offline queues and clock complexity at the start, while keeping a path to offline-write when gym-without-signal becomes real pain.

Options from [OQ-03](../open-questions.md) / earlier data-and-sync outline: online-only, online-first + read cache, local-first + write queue, Realtime between devices.

Web does not share a sync engine with KMP ([ADR 0006](0006-web-supabase-js-without-kmp-js.md)) — shared truth must live in **Postgres + RLS**, not in a second local DB as SoT.

## Decision

1. **Source of truth:** **PostgreSQL (Supabase)**. Every successful client write lands in Postgres (via RLS + JWT `user_id` = Auth account from ADR 0007). Local store is **not** a second truth.
2. **MVP baseline:** **online-first + local read cache**. Read: network when available, otherwise fresh cache (stale-while-revalidate / “show last fetched”). Write: **only when online** — API request; when offline, UI blocks write with a clear message (no silent queue).
3. **Offline-write (queue, optimistic UI with durable local commit):** **beyond MVP** — follow-up when modules (especially gym training) require it; then a separate ADR/conflict revisit.
4. **Conflicts (MVP):** one rule for all user entity types (nutrition / training / measurements / `profiles`): **last-write-wins (LWW) per row** by `updated_at` (timestamptz from server / agreed write clock). No per-field merge at start. With online-first, conflicts are rare (mainly two devices online at once); LWW is enough.
5. **Supabase Realtime:** **optional** in phase 2+ to refresh UI when another device writes — not required to accept “hello sync”. Subscriptions only for user tables, always through RLS.
6. **Clients:** mobile — KMP repositories read/write Supabase (`supabase-kt`) + read cache in core; web — same behavior via `supabase-js` + thin browser/memory cache, **without** a parallel sync domain in TS ([ADR 0006](0006-web-supabase-js-without-kmp-js.md)).
7. **Deletion:** MVP = **hard delete** of the Postgres row + cache invalidation. Tombstone / soft-delete for offline sync — only with offline-write.

## Alternatives considered

1. **Online-first + read cache (chosen)** — simple mental model; SoT = Postgres; consistent with web without KMP; enough for multi-device after sign-in. Cons: no write in the gym without network.
2. **Pure online-only (no cache)** — even simpler. Rejected as UX baseline: unnecessary spinners on every history open; read cache is small cost.
3. **Local-first + write queue** — best offline UX. Rejected for MVP: queue, idempotency, conflicts, clocks, tombstones, web↔mobile parity — too expensive before working auth/RLS/hello sync.
4. **Per-entity merge from day 1** (e.g. merging sets in a session) — more precise. Rejected at start: no proof LWW hurts; one rule means less code and less web/mobile drift.
5. **Realtime as mandatory sync engine** — live everywhere. Rejected as MVP requirement: pull/refresh + REST write is enough; Realtime = opt-in improvement.

## Consequences

### Positive

- Phase 2 can build “hello sync” without an offline engine.
- One SoT and one conflict rule — easy to describe in the first migration (`updated_at`, RLS).
- Consistent with ADR 0007 identity and ADR 0006 web path (both clients write to the same Postgres).
- Clear revisit trigger: real offline-write pain.

### Negative / costs

- In a gym without signal the user **cannot** save a workout / meal in MVP.
- Two devices editing the same row online → silent LWW (latest `updated_at` wins); no “resolve conflict” UI.
- Read cache may show stale data until the next successful fetch — UI should tolerate that (e.g. “offline / last updated” indicator).
- The `core/sync/` package at start is a **thin** cache/invalidation helper, not a full queue — the directory name does not promise offline-write.

### Schema requirements (input to #6)

- Every user table: `user_id`, `created_at`, `updated_at` (timestamptz).
- Writes update `updated_at` (trigger or explicit SET in the client — decided at migration).
- No mandatory tombstone columns in MVP.

### Offline behavior (MVP) — summary

| Operation | Online | Offline |
|-----------|--------|---------|
| Read list / detail | Fetch → refresh cache | Show cache; if empty — empty state + “no network” |
| Write / update / delete | API + RLS; on success update cache | **Reject** with message; do not queue |
| Sign-in / session refresh | Per auth.md | Without a valid session, no access to protected data |

### Beyond MVP (deliberately later)

- Offline write queue + idempotent requests
- Optimistic UI with local commit before server ACK
- Merge / CRDT / per-entity conflict UI
- Mandatory Realtime as sync bus
- Shared offline engine for web+mobile (ADR 0006 revisit condition)

## Related

- Closes [OQ-03](../open-questions.md)
- [data-and-sync](../architecture/data-and-sync.md)
- [ADR 0002](0002-backend-supabase.md), [ADR 0006](0006-web-supabase-js-without-kmp-js.md), [ADR 0007](0007-auth-providers-minimum-pii.md)
- [data-model](../domain/data-model.md), [auth](../architecture/auth.md), [postgres-invariants](../architecture/postgres-invariants.md)
- [#6](https://github.com/MarekBiolik/GymManager/issues/6) / [first migration spec](../specs/first-migration-profiles-rls.md) — profiles + RLS + `updated_at`; apply [#21](https://github.com/MarekBiolik/GymManager/issues/21)
- Follow-ups: [#19 read-cache / hello sync contract](https://github.com/MarekBiolik/GymManager/issues/19), [#20 offline-write post-MVP](https://github.com/MarekBiolik/GymManager/issues/20)
