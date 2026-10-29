# Open questions

Areas deliberately **undecided** in iteration 1. Each item should be closed in a separate iteration (plan + ADR or docs update) before deep coding.

Statuses: `open` | `in progress` | `decided` (with link to ADR/docs).

---

## OQ-01 — Web ↔ shared core (KMP) interop

**Status:** decided — [ADR 0006](decisions/0006-web-supabase-js-without-kmp-js.md)  
**Why it matters:** KMP naturally ties Android and iOS together; web (JS/Wasm) is harder. A bad choice = duplicated domain logic or fragile interop.

**Decision (day 1):** option **B** — web → `supabase-js` + thin TS; **no** KMP→JS/Wasm in the scaffold. Guardrails and revisit criteria: ADR 0006 + [clients](architecture/clients.md) / [shared-core](architecture/shared-core.md). Operational split of DB vs core/TS rules: [postgres-invariants](architecture/postgres-invariants.md) (issue #17).

**Options (considered):**

1. Core compiled to JS/Wasm, consumed by React/TS.
2. Web talks directly to Supabase via `supabase-js`; thin logic in TS (duplication risk vs core) — **chosen for start**.
3. Hybrid: domain/validation in shared (if feasible), web networking separate.

---

## OQ-02 — Sign-in method and minimum PII

**Status:** decided — [ADR 0007](decisions/0007-auth-providers-minimum-pii.md), [auth](architecture/auth.md)  
**Decision:** Google (Android/iOS/web) + Sign in with Apple on iOS; minimum PII (`profiles` without email/name); account-deletion outline in auth.md. Beyond MVP: magic link, anon+linking, email+password.

---

## OQ-03 — Offline / sync / conflict strategy

**Status:** decided — [ADR 0008](decisions/0008-sync-strategy-online-first.md), [data-and-sync](architecture/data-and-sync.md)  
**Decision:** online-first + local read cache; SoT = Postgres; writes online-only in MVP; conflicts = LWW per row (`updated_at`), one rule for all entity types; Realtime optional; offline-write later.  
**Schema (`updated_at`, RLS):** [first migration spec](specs/first-migration-profiles-rls.md) (#6); apply = [#21](https://github.com/MarekBiolik/GymManager/issues/21).

---

## OQ-04 — Monorepo tooling and versioning

**Status:** decided — [ADR 0005](decisions/0005-monorepo-tooling.md), [overview](architecture/overview.md)  
**Decision (summary):** Gradle multi-project for `core` + `apps/android` (framework export for iOS); `apps/web` on pnpm/Vite outside Gradle; orchestration = thin root scripts (no Nx/Turborepo); core source-linked in the same commit, SemVer/publish later.

---

## OQ-05 — Supabase hosting and backups

**Status:** decided — [ADR 0004](decisions/0004-supabase-hosting.md), [data-and-sync](architecture/data-and-sync.md#hosting-and-backups)  
**Decision:** Supabase Cloud Free to start → Pro when auto-backups / no-pause are needed; one `dev` project (+ optional local CLI); secrets outside the repo; on Free, periodic `db dump`.  
**`staging`/`prod` split (follow-up):** [spec](specs/supabase-staging-prod-split.md) ([#10](https://github.com/MarekBiolik/GymManager/issues/10)) — when, naming, Environments, staging→prod migrations, Pro criteria for `prod`; project provisioning later.

---

## OQ-06 — Prototype artifact disposition

**Status:** decided — [legacy-cleanup](conventions/legacy-cleanup.md)  
**Artifacts:** `web/`, `server/`, `data/gymmanager.db*` — **removed** from the working tree (were never git-tracked). SQLite schema export skipped.

---

## OQ-07 — Food data source

**Status:** decided — [ADR 0009](decisions/0009-food-data-source-own-first.md), [data-model](domain/data-model.md#foods)  
**Decision:** MVP = **own foods only** (Postgres + RLS); no live OFF in phase 3. Later hybrid: OFF search/import → copy into the user’s `foods` with optional `external_source` / `external_id`. Macro snapshot in `food_entries` independent of any external API.

**Options (considered):**

1. Own foods only (no hybrid path).
2. OFF-only (live API).
3. Hybrid from day 1.
4. Own first + hybrid later — **chosen**.

---

## OQ-08 — Module-switching UI/UX

**Status:** open  
**Context:** one app, several areas. Navigation (tabs, drawer, “mode switch”) — detail for phase 6; does not block foundations.

---

## How to add new questions

1. Assign id `OQ-NN`.
2. Status `open`, short context, options if known, closing phase.
3. After decision: status `decided` + link to ADR or docs section.
