---
name: supabase-db
description: >-
  Change GymManager Supabase schema and RLS: migrations as code, RLS-first,
  local CLI, no secrets in git, staging→prod apply order. Use when editing
  supabase/, writing SQL migrations, RLS policies, Auth-related schema,
  dump/restore ops, or the user says Supabase / Postgres / RLS / migration.
---

# Supabase / DB

Own `supabase/`: schema and policies as **migrations in git**, RLS on every user table, secrets outside the repo. Backend decision: [ADR 0002](../../../docs/decisions/0002-backend-supabase.md). Hosting/backups: [ADR 0004](../../../docs/decisions/0004-supabase-hosting.md). Invariants vs core/TS: [postgres-invariants](../../../docs/architecture/postgres-invariants.md). Tests: [testing](../../../docs/conventions/testing.md).

Env: [references/env-checklist.md](references/env-checklist.md).

Official Supabase agent skills (`supabase`, `supabase-postgres-best-practices`) are optional extras; this skill is the GymManager contract they must not contradict.

## Workflow

Copy and track:

```
Supabase slice:
- [ ] 1. Place the rule
- [ ] 2. Write migration + RLS
- [ ] 3. Validate locally
- [ ] 4. Tests / advisors
- [ ] 5. Hand-off
```

### 1. Place the rule

| Need | Where |
|------|--------|
| Isolation / integrity for web + mobile | Postgres (RLS, CHECK, FK, trigger) |
| Rich mobile UX / offline-read helpers | KMP `core` (`core-kmp`) — not a TS domain |
| Form feedback only | Thin UI validation mirroring DB/core |

Day 1: one Cloud **`dev`** project. Split to staging/prod only per [staging/prod spec](../../../docs/specs/supabase-staging-prod-split.md). Apply order when split exists: **staging → prod** (never the reverse).

**Done when:** clear whether this slice is SQL/RLS, core-only, or both (coordinate with `core-kmp` / `platform-delivery`).

### 2. Write migration + RLS

- Source of truth = files under `supabase/migrations/` (imperative migrations). Do not treat Dashboard schema edits as truth.
- Table names: `snake_case`, plural. Every user table: `user_id` + **ENABLE ROW LEVEL SECURITY** + SELECT/INSERT/UPDATE/DELETE policies in the **same** migration ([coding-standards](../../../docs/conventions/coding-standards.md)).
- Prefer `auth.uid()` policies for authenticated user data.
- Forward-only migrations; no manual prod drift.
- Match existing specs under `docs/specs/` when the change is specified (e.g. first migration / profiles).

**Security (always):**

- Never put `service_role` in clients, app env shipped to devices, or logs
- Never authorize from user-editable `user_metadata` — use `app_metadata` / DB roles as designed
- Views: use `security_invoker = true` when they must respect RLS
- UPDATE policies need a matching SELECT policy (otherwise updates look like no-ops)
- Prefer anon key + RLS in clients; service role only in admin/CI/local secret stores

**Done when:** migration SQL is reviewable in git and RLS covers the new/changed user tables.

### 3. Validate locally

Use Supabase CLI against **local** or linked **`dev`** — not production. Typical loop: `supabase start` (when Docker available) → apply/reset → `supabase db lint` / advisors when available.

Before risky applies on shared Cloud: dump per [dump/restore runbook](../../../docs/runbooks/supabase-dump-restore.md).

**Done when:** migration applies cleanly on local/`dev`; lint/advisors reviewed for new issues introduced by this change.

### 4. Tests / advisors

- Prefer automated checks with **two user JWTs** proving RLS isolation ([testing](../../../docs/conventions/testing.md))
- Fix advisor findings that are real for this slice; do not leave “warn forever” without an issue

**Done when:** isolation intent is demonstrated or explicitly deferred with an issue; no new critical advisor hits ignored.

### 5. Hand-off

- Short status: migration name(s), tables/policies touched, apply target (`local` / `dev` / later staging)
- If mobile/web clients need new columns or contracts, signal `core-kmp` / platform skills
- PR/merge only when the user asks

**Done when:** issue AC for the DB slice are tickable; clients know the contract.

## Guardrails

- Migrations in git > Dashboard click-ops
- RLS in the same migration as the user table
- No secrets in the repo; anon URL/key placeholders only via `.env.example`
- Staging→prod only after the environment split; until then touch only `dev` / local
- Prefer strong model for RLS/security design; cheap helpers for CLI runs and search
