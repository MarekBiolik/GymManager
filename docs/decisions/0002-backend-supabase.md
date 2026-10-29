# ADR 0002 — Backend: Supabase

- **Status:** accepted
- **Date:** 2026-10-29
- **Decision makers:** Marek (+ architecture planning session)

## Context

We need: a place for app data (1+ year history), sync across devices, safe sign-in with **minimum maintenance** and **minimum PII**. A tracker produces aggregating queries (sums, trends) — a relational model fits better than document. Solo/team does not want to run a custom application server at the start.

## Decision

We use **Supabase**: PostgreSQL + Auth + Row Level Security + Realtime + Storage. Clients / core connect via official / community SDKs (`supabase-kt`, `supabase-js`). Cloud Free → Pro hosting, environments, and backups: [ADR 0004](0004-supabase-hosting.md).

## Alternatives considered

1. **Firebase (Auth + Firestore)** — fast start, Google login “native” to the Google ecosystem. Rejected: stronger **vendor lock-in** (proprietary API/model); **weaker aggregations** in NoSQL for tracker history/charts.
2. **Custom Node/TS backend + Postgres + auth library** — full control. Rejected at start: highest maintenance cost (server, patching, auth hardening, backups).
3. **Supabase** — chosen: Postgres (aggregations, SQL migrations), Auth with Google/Apple, RLS for user isolation, export/self-host path (weaker lock-in than Firestore).

### Glossary (for decision context)

- **Lock-in:** cost of leaving a vendor. Firestore = rewrite data access; Supabase = standard Postgres (dump/restore to other hosting).
- **Aggregation:** queries like “daily calorie sum over a year”. SQL does this naturally; Firestore often needs denormalization and read costs.

## Consequences

- Positive: little custom infra; RLS enforces “owner-only data”; free tier to start; exit path via SQL dump.
- Negative: dependency on Supabase Auth/API product; community `supabase-kt` needs maturity monitoring; migration + RLS discipline on every table.
- Sync / offline: [ADR 0008](0008-sync-strategy-online-first.md) (OQ-03). Hosting/backups: [ADR 0004](0004-supabase-hosting.md). Sign-in method / PII: [ADR 0007](0007-auth-providers-minimum-pii.md).

## Related

- [data-and-sync](../architecture/data-and-sync.md)
- [auth](../architecture/auth.md)
- [tech-stack](../architecture/tech-stack.md)
- [ADR 0008](0008-sync-strategy-online-first.md)
