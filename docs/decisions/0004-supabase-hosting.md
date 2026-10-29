# ADR 0004 — Supabase hosting and backups

- **Status:** accepted
- **Date:** 2026-10-29
- **Decision makers:** Marek (+ OQ-05 closing session)

## Context

[ADR 0002](0002-backend-supabase.md) chose Supabase as BaaS but did not decide **where** we host it or how we protect data. Solo fitness-tracker maintainer: low maintenance budget, no ops team, personal data (meal/workout history) worth recovering, but this is not multi-tenant SaaS with an SLA at the start.

Forces: cost (ideally $0 to start), zero VPS/patching, exit via SQL, sensible upgrade path when data becomes “I cannot lose this”, clear secrets model before scaffolding.

## Decision

1. **Hosting:** **Supabase Cloud**, **Free** plan at the start. Upgrade to **Pro** (~$25/mo) when we need automatic daily backups, no project pause, or more resources — before production data cannot be reconstructed from memory.
2. **Environments:** at start one Cloud project = **`dev`** (scaffolding + personal use) + optional local stack (`supabase start`). Separate **`staging` / `prod`** (separate Cloud projects) — **before public beta / external users**; model, naming, per-Environment secrets, migration order (`staging` → `prod`), and Pro criteria for `prod`: [environment-split spec](../specs/supabase-staging-prod-split.md) ([#10](https://github.com/MarekBiolik/GymManager/issues/10)).
3. **Secrets:** project URL + `anon` key may live in clients (protection = RLS). `service_role` and other secrets **never** in the repo or apps — only local `.env*` (gitignore) and GitHub Secrets / **GitHub Environments** in CI (after the split: separate set per `staging` / `production`).
4. **Backups (Free):** no available automatic dashboard backups. Policy: periodic logical dump (`supabase db dump` / `pg_dump`) outside Cloud + dump before a risky migration. After Pro upgrade: daily backups (7 days) + optional PITR. Upgrade **`prod` → Pro** before real beta data (details in the split spec).

## Alternatives considered

1. **Supabase Cloud Free → Pro (chosen)** — zero ops, free start, official Auth/API, exit via Postgres dump. Cons: Free pause after ~1 week idle; no available auto-backups on Free; Free limit of 2 active projects.
2. **Cloud Pro immediately** — daily backups and no pause from day 1. Rejected at start: unnecessary cost until data is worth a paid RPO.
3. **Self-host on VPS (Docker)** — full control, no pause. Rejected at start: patching, TLS, backups, stack upgrades = ops we do not want before MVP.
4. **Two Cloud projects immediately (dev + prod)** — cleaner split. Rejected at start: Free active-project limit; staging/prod per [spec #10](../specs/supabase-staging-prod-split.md) at public beta (`prod` usually Pro).

## Consequences

- Positive: decision ready for phase 1; low cost; weaker lock-in (SQL); clear upgrade and **documented** env split (spec #10).
- Negative / costs: on Free we must dump **ourselves**; Free project may pause when idle — unpause before work; Free 2-project limit (after split: usually `staging` Free + `prod` Pro; not three active Free).
- Refine later: project provisioning checklist at scaffold; execute the split per the spec (not this ADR); PITR when RPO < 1 day becomes a requirement.
- Dump/restore operator steps: [runbook](../runbooks/supabase-dump-restore.md) ([#9](https://github.com/MarekBiolik/GymManager/issues/9)). Staging/prod split: **spec** [supabase-staging-prod-split](../specs/supabase-staging-prod-split.md) ([#10](https://github.com/MarekBiolik/GymManager/issues/10)); project/Environment implementation = separate ticket.

## Related

- [OQ-05](../open-questions.md) (decided)
- [data-and-sync](../architecture/data-and-sync.md) — Hosting and backups section
- [runbook: dump / restore (Free)](../runbooks/supabase-dump-restore.md)
- [Spec: staging/prod split](../specs/supabase-staging-prod-split.md) ([#10](https://github.com/MarekBiolik/GymManager/issues/10))
- [auth](../architecture/auth.md) — keys / secrets
- [tech-stack](../architecture/tech-stack.md)
- [ci-cd](../conventions/ci-cd.md)
- [ADR 0002](0002-backend-supabase.md)
- [#9](https://github.com/MarekBiolik/GymManager/issues/9)
