# Spec: Supabase environment split (`staging` / `prod`)

**Status:** SPEC ONLY — does **not** create Dashboard projects or CD jobs in this ticket.  
**Issue:** [#10](https://github.com/MarekBiolik/GymManager/issues/10)  
**Foundation:** [ADR 0004](../decisions/0004-supabase-hosting.md) (Cloud Free → Pro; at start one `dev` project)  
**Implementation (later):** provision projects + GitHub Environments + apply migrations — separate tickets after this spec is accepted; does not block phase-1 scaffold ([#14](https://github.com/MarekBiolik/GymManager/issues/14)).

This document closes the ADR 0004 follow-up: **when** to split environments, **how** to name them, **where** to keep URLs/secrets, and migration **apply order**. No code and no Dashboard clicks as part of #10.

## Goals

1. A `staging` + `prod` model (separate Cloud projects) ready to execute before public beta.
2. Secrets and URLs **per environment** (GitHub Environments or equivalent).
3. The only allowed migration apply order: **staging → prod**.
4. Criteria for upgrading **`prod` → Pro** (backups, Free pause).

## Non-goals

| Out of scope | Where |
|--------------|--------|
| Creating the second/third project in the Dashboard | separate implementation ticket after this spec |
| Dump/restore runbook | [#9](https://github.com/MarekBiolik/GymManager/issues/9) / [runbook](../runbooks/supabase-dump-restore.md) |
| Monorepo scaffold / `supabase/` day one | [#14](https://github.com/MarekBiolik/GymManager/issues/14); start = one Cloud `dev` |
| Automatic CD apply to prod on every merge | direction in [ci-cd](../conventions/ci-cd.md); enable jobs later |
| PITR / RPO < 1 day | ADR 0004 — when the requirement appears separately |
| Self-host / exit from Cloud | exit path in ADR 0004 / data-and-sync |

## When to split (trigger)

Stay on **one** Cloud `dev` project (ADR 0004) while **all** of these hold:

- Solo / trusted circle; no external beta testers with data that must not be lost.
- No need to isolate a “near-prod” schema from the developer’s personal dump.
- Free limit (2 active projects) is not yet a problem because we use one Cloud slot.

**Perform the split before** (the **first** of the following is enough):

1. **Public beta** or inviting users outside maintainers.
2. Cloud data that must not be mixed with migration experiments / developer seeds.
3. Need for a separate smoke environment before apply on real users.

The split is **not** required to close phase 1 or “hello sync” on `dev`.

## Target model (after the split)

```text
local (optional)     staging (Cloud)              prod (Cloud)
supabase start  →    schema + test data      →    beta/prod users
                     Free OK at start             Pro before real data*
```

\* Pro criteria: section below.

| Environment | Role | Plan (direction) |
|-------------|------|------------------|
| `local` | Offline migration work (`supabase start`); not the user sync SoT | CLI / Docker |
| `dev` (pre-split) | Only Cloud for scaffold and personal use | Free |
| `staging` (post-split) | Integration, migration apply, Auth/RLS smoke, optional test accounts | Free OK |
| `prod` (post-split) | Real user / beta data | **Pro** per criteria below |

### What happens to `dev` at the split

One of two paths (choose in the implementation ticket; both OK vs this spec):

1. **Rename / repurpose:** existing Cloud `dev` → `staging` (keep migration history; scrub test data if needed). Create a new empty `prod`.
2. **Fresh pair:** create `staging` + `prod` from scratch; rebuild schema from `supabase/migrations/`; **pause or delete** old `dev` to fit Free active-project limits (max 2), or keep `dev` only if `prod` is already on Pro and budget/slots allow.

After the split we do **not** keep three active Free Cloud projects as the norm. Day-to-day developer path: `local` + `staging`; `prod` only via migration promotion.

## Naming

| Layer | Convention | Example |
|-------|------------|---------|
| Supabase project (Dashboard name) | `gymmanager-<env>` | `gymmanager-staging`, `gymmanager-prod` |
| Project ref / id | from Dashboard (random); **do not** hardcode in docs | `abcdefghijkl` |
| GitHub Environment | `staging`, `production` | Settings → Environments |
| Local env files (never in git) | `.env.staging`, `.env.production` / `.env.local` | gitignore already on `master` |
| CI secret prefix (optional) | no prefix **inside** an Environment (Environment provides isolation) | `SUPABASE_URL` in env `staging` ≠ the one in `production` |

Avoid confusing `dev`/`prod` names with git branches. Branch = feature/docs; **environment = Cloud project + GitHub Environment**.

## Secrets and URLs per environment

Source of truth after the split: **GitHub Environments** (`staging`, `production`) — not one flat Secrets set for the whole repo for Cloud URLs.

### Variables / secrets (repeated in each Environment)

| Name | Type | Purpose | Allowed where |
|------|------|---------|---------------|
| `SUPABASE_URL` | secret or variable | Clients, CI smoke, local docs link | Environment; local `.env.*` |
| `SUPABASE_ANON_KEY` | secret | Clients (protection = RLS) | Environment; local `.env.*`; **not** in CI logs |
| `SUPABASE_SERVICE_ROLE_KEY` | secret | Admin / migrations / trusted job only — **never** client | Environment (restricted); local admin `.env*` |
| `SUPABASE_PROJECT_REF` | variable / secret | CLI `link` / `db push` | Environment |
| `SUPABASE_DB_PASSWORD` | secret | When CI connects to DB outside an access token | Environment |
| `SUPABASE_ACCESS_TOKEN` | secret | Personal/access token for CLI in CI (if used) | Environment / org secret; rotate |

Rules (aligned with ADR 0004):

1. Separate value sets for `staging` and `production` — **no** sharing URL/`anon` across environments in release builds.
2. `service_role` only in an Environment with review / restricted branches (e.g. only `master` → prod job).
3. Mobile/web clients: **staging** vs **prod** build gets URL+`anon` from the correct Environment (or store track / flavor) — not hardcoded in the repo.
4. In docs/PRs: placeholders (`YOUR_STAGING_URL`), never real keys.
5. Equivalent outside GitHub (other CI): same per-environment isolation; store name may differ.

Until the split: one set for Cloud `dev` (as today in ADR 0004 / playbook).

## Migration apply order

Schema source of truth: files in `supabase/migrations/` in git (after [#14](https://github.com/MarekBiolik/GymManager/issues/14) / [#21](https://github.com/MarekBiolik/GymManager/issues/21)). **Forbid** lasting drift via Dashboard SQL Editor on `staging`/`prod`.

```text
PR → merge to master
      → apply on staging
      → smoke (Auth/RLS / critical path)
      → apply **the same** migration files on prod
```

| Step | Environment | Rule |
|------|-------------|------|
| 1. Draft / local | `local` or ephemeral DB | `supabase db reset` / lint; optional |
| 2. Integration | **`staging`** | First Cloud apply after merge (or controlled job) |
| 3. Verification | `staging` | Smoke; on fail — **stop**, do not touch `prod` |
| 4. Promotion | **`prod`** | Same migrations as staging; no “hotfix only on prod” |
| 5. Rollback | — | Forward-fix preferred; restore per [#9](https://github.com/MarekBiolik/GymManager/issues/9) / Pro backups |

Hard rules:

- **Never** `prod` before `staging` for the same migration.
- **Never** a manual schema change only on `prod`.
- Data is **not** copied automatically staging → prod (separate projects; Auth users separate too). Staging seed ≠ prod.
- Pre-split: apply only on `dev` (only Cloud); after the split `dev` leaves the promotion path.

## Criteria for upgrading `prod` → Pro

Upgrade **`prod`** to Pro when **any** of these is true (do not wait for a disaster):

| Trigger | Why |
|---------|-----|
| Public beta / external users with data | Free has no dashboard daily backups; idle pause is unacceptable for a live prod |
| Data that cannot be reconstructed from memory / ad-hoc dumps | Need available daily backups (7-day retention) |
| Risk of Free pause (~1 week idle) | Prod must stay available without manual unpause |
| More DB/storage/Auth than Free | Resource limits |

**When Pro is not required immediately on `staging`:** staging may stay on Free (pause OK for maintainers). Upgrade **`prod` earlier than staging.**

PITR (add-on): only when RPO < ~1 day becomes a requirement (ADR 0004) — outside this spec.

**Cost shape:** staging Free + prod Pro is the default after public beta; two Pro only if staging also needs non-stop uptime / auto-backups.

## Implementation checklist (outside #10)

Separate ticket / code+ops PR (not this document):

- [ ] Create `gymmanager-staging` / `gymmanager-prod` projects (or rename `dev` → staging)
- [ ] GitHub Environments `staging` + `production` with the secrets table above
- [ ] Wire CLI / CI to `PROJECT_REF` per Environment
- [ ] Apply full migration chain on staging, then empty prod
- [ ] Upgrade `prod` → Pro per criteria (before real users)
- [ ] Update build flavors / web env for two URLs
- [ ] Mark old `dev` retired (pause/delete) if it is no longer `staging`

## Related

- [ADR 0004](../decisions/0004-supabase-hosting.md) — hosting, Free→Pro, baseline secrets, deferral of this split
- [data-and-sync — Hosting and backups](../architecture/data-and-sync.md#hosting-and-backups)
- [ci-cd](../conventions/ci-cd.md)
- [phase-1-scaffold-playbook](../plans/phase-1-scaffold-playbook.md) — day one still only `dev`
- [#9](https://github.com/MarekBiolik/GymManager/issues/9) dump/restore
